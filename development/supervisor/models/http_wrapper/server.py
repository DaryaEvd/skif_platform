"""Single-run model API using only the Python standard library."""
import hashlib
import json
import os
import re
import signal
import subprocess
import threading
import time
import uuid
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlsplit

from adapters import Adapter, InvalidParameters

TERMINAL = {'SUCCEEDED', 'FAILED', 'CANCELLED'}


def now():
    return datetime.now(timezone.utc).isoformat().replace('+00:00', 'Z')


def encode(value):
    return json.dumps(value, allow_nan=False, separators=(',', ':')).encode()


def atomic(path, data):
    temp = path.with_suffix(path.suffix + '.tmp')
    with temp.open('wb') as stream:
        stream.write(data)
        stream.flush()
        os.fsync(stream.fileno())
    temp.replace(path)


class Conflict(Exception):
    pass


class Runtime:
    def __init__(self, adapter, root):
        self.adapter = adapter
        self.root = Path(root)
        self.root.mkdir(parents=True, exist_ok=True)
        self.lock = threading.RLock()
        self.run = None
        self.request = None
        self.process = None
        self.work = None
        self.metadata = None
        self.logs = []
        self.publications = []
        self.cancel_event = threading.Event()

    def state(self, status, error=None):
        # All callers hold lock. Terminal transitions cannot be overwritten.
        if self.run['status'] in TERMINAL:
            return
        self.run['status'] = status
        self.run['error'] = error
        if status in TERMINAL:
            self.run['finishedAt'] = now()
        self.run['resultAvailable'] = status == 'SUCCEEDED'
        atomic(self.work / 'status.json', encode(self.run))

    def accept(self, request):
        if not isinstance(request, dict) or set(request) != {'runId', 'experimentId', 'parameters'}:
            raise InvalidParameters('Expected runId, experimentId and parameters')
        for key in ('runId', 'experimentId'):
            if not isinstance(request[key], str) or not re.fullmatch(r'[A-Za-z0-9_-]{1,128}', request[key]):
                raise InvalidParameters(key + ' must be 1..128 letters, digits, underscores or hyphens')
        fingerprint = json.dumps(request, sort_keys=True, allow_nan=False, separators=(',', ':')).encode()
        with self.lock:
            if self.run is not None:
                if request['runId'] != self.run['runId']:
                    raise Conflict('CONTAINER_ALREADY_USED')
                if fingerprint != json.dumps(self.request, sort_keys=True, allow_nan=False, separators=(',', ':')).encode():
                    raise Conflict('RUN_ID_CONFLICT')
                return dict(self.run), False
            if not self.adapter.ready():
                raise RuntimeError('MODEL_NOT_READY')
            self.adapter.validate(request['parameters'])
            self.work = self.root / str(uuid.uuid4())
            self.work.mkdir()
            atomic(self.work / 'request.json', fingerprint)
            self.request = request
            self.run = {'runId': request['runId'], 'status': 'ACCEPTED', 'acceptedAt': now(),
                        'startedAt': None, 'finishedAt': None, 'lastPublicationSeq': 0,
                        'resultAvailable': False, 'error': None}
            atomic(self.work / 'status.json', encode(self.run))
            accepted = dict(self.run)
            threading.Thread(target=self.execute, daemon=True).start()
            return accepted, True

    def log(self, source, message):
        with self.lock:
            item = {'seq': len(self.logs) + 1, 'timestamp': now(), 'stream': source, 'message': message}
            with (self.work / 'logs.jsonl').open('ab') as stream:
                stream.write(encode(item) + b'\n')
                stream.flush()
            self.logs.append(item)

    def read_log(self, pipe, source):
        with pipe:
            for line in iter(pipe.readline, b''):
                self.log(source, line.decode('utf-8', errors='replace').rstrip('\r\n'))

    def stop_group(self, process):
        try:
            os.killpg(process.pid, signal.SIGTERM)
        except ProcessLookupError:
            return
        deadline = time.monotonic() + 2
        while time.monotonic() < deadline:
            try:
                os.killpg(process.pid, 0)
            except ProcessLookupError:
                return
            time.sleep(0.05)
        try:
            os.killpg(process.pid, signal.SIGKILL)
        except ProcessLookupError:
            pass

    def execute(self):
        process = None
        readers = []
        try:
            command, env = self.adapter.prepare(self.work, self.request['parameters'])
            with self.lock:
                if self.cancel_event.is_set():
                    self.state('CANCELLED')
                    return
                process = subprocess.Popen(command, cwd=self.work, env=env, stdout=subprocess.PIPE,
                                           stderr=subprocess.PIPE, start_new_session=True)
                self.process = process
                self.run['startedAt'] = now()
                self.state('RUNNING')
            self.log('wrapper', 'Process started pid=' + str(process.pid))
            for source, pipe in [('stdout', process.stdout), ('stderr', process.stderr)]:
                thread = threading.Thread(target=self.read_log, args=(pipe, source), daemon=True)
                thread.start()
                readers.append(thread)
            while process.poll() is None:
                if self.cancel_event.wait(0.05):
                    break
            # Reap the leader while terminating its remaining process group as well.
            if self.cancel_event.is_set():
                self.stop_group(process)
            code = process.wait()
            self.stop_group(process)
            for thread in readers:
                thread.join()
            self.log('wrapper', 'Process exited code=' + str(code))
            with self.lock:
                if self.cancel_event.is_set():
                    self.state('CANCELLED')
                    return
                if code != 0:
                    self.state('FAILED', {'code': 'PROCESS_CRASHED' if code < 0 else 'PROCESS_EXIT_NONZERO',
                                          'message': 'Calculation process failed', 'exitCode': code})
                    return
                self.state('FINALIZING')
            try:
                result, metadata = self.adapter.result(self.work)
            except FileNotFoundError as error:
                self.fail_result('RESULT_MISSING', error)
                return
            except (ValueError, TypeError, KeyError, OverflowError) as error:
                self.fail_result('RESULT_INVALID', error)
                return
            payload = encode(result)
            with self.lock:
                if self.cancel_event.is_set():
                    self.state('CANCELLED')
                    return
                target = self.work / 'result.json'
                atomic(target, payload)
                target.chmod(0o444)
                metadata.update(runId=self.run['runId'], mediaType='application/json', sizeBytes=len(payload),
                                sha256=hashlib.sha256(payload).hexdigest(),
                                downloadUrl='/api/v1/runs/' + self.run['runId'] + '/result')
                atomic(self.work / 'metadata.json', encode(metadata))
                self.metadata = metadata
                self.state('SUCCEEDED')
        except Exception as error:
            if process is not None:
                self.stop_group(process)
                process.wait()
            for thread in readers:
                thread.join()
            with self.lock:
                self.log('wrapper', str(error))
                self.state('CANCELLED' if self.cancel_event.is_set() else 'FAILED',
                           None if self.cancel_event.is_set() else
                           {'code': 'INTERNAL_ERROR', 'message': str(error)})

    def fail_result(self, code, error):
        with self.lock:
            if self.cancel_event.is_set():
                self.state('CANCELLED')
            else:
                self.state('FAILED', {'code': code, 'message': str(error)})

    def cancel(self):
        with self.lock:
            if self.run['status'] not in TERMINAL:
                self.cancel_event.set()
                self.state('CANCELLING')
            return dict(self.run)


class Handler(BaseHTTPRequestHandler):
    server_version = 'ModelAPI/1'

    def log_message(self, *args):
        pass

    def send_json(self, code, value, location=None):
        data = encode(value)
        self.send_response(code)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Content-Length', str(len(data)))
        if location:
            self.send_header('Location', location)
        self.end_headers()
        self.wfile.write(data)

    def error(self, status, code, message):
        self.send_json(status, {'error': {'code': code, 'message': message}})

    def do_POST(self):
        self.handle_api('POST')

    def do_GET(self):
        self.handle_api('GET')

    def handle_api(self, method):
        try:
            self.dispatch(method)
        except InvalidParameters as error:
            self.error(422, 'INVALID_PARAMETERS', str(error))
        except Conflict as error:
            self.error(409, str(error), str(error))
        except (ValueError, UnicodeError) as error:
            self.error(400, 'INVALID_REQUEST', str(error))
        except (BrokenPipeError, ConnectionResetError):
            pass
        except Exception as error:
            self.error(500, 'INTERNAL_ERROR', str(error))

    def dispatch(self, method):
        runtime = self.server.runtime
        url = urlsplit(self.path)
        path = url.path
        if method == 'GET' and path == '/api/v1/readiness':
            with runtime.lock:
                ready = runtime.run is None and runtime.adapter.ready()
                self.send_json(200 if ready else 503, {'apiVersion': '1', 'modelId': runtime.adapter.kind,
                    'ready': ready, 'reason': None if ready else
                    ('CONTAINER_ALREADY_USED' if runtime.run else 'MODEL_NOT_READY'),
                    'activeRunId': runtime.run['runId'] if runtime.run and
                    runtime.run['status'] not in TERMINAL else None})
            return
        if method == 'POST' and path == '/api/v1/runs':
            if self.headers.get_content_type() != 'application/json':
                self.error(415, 'UNSUPPORTED_MEDIA_TYPE', 'Use application/json')
                return
            size = int(self.headers.get('Content-Length', '0'))
            if not 0 < size <= 1024 * 1024:
                self.error(400, 'INVALID_REQUEST', 'Body size must be 1..1048576 bytes')
                return
            def invalid(value):
                raise ValueError('Non-finite JSON constant: ' + value)
            request = json.loads(self.rfile.read(size), parse_constant=invalid)
            try:
                state, created = runtime.accept(request)
            except RuntimeError as error:
                self.error(503, 'MODEL_NOT_READY', str(error))
                return
            self.send_json(202 if created else 200, state, '/api/v1/runs/' + state['runId'])
            return
        match = re.fullmatch(r'/api/v1/runs/([A-Za-z0-9_-]+)(/cancel|/publications|/result/metadata|/result|/logs)?', path)
        if not match:
            self.error(404, 'NOT_FOUND', 'Unknown endpoint')
            return
        run_id, suffix = match.groups()
        with runtime.lock:
            if runtime.run is None or run_id != runtime.run['runId']:
                self.error(404, 'RUN_NOT_FOUND', 'Unknown runId')
                return
            if suffix == '/cancel' and method == 'POST':
                state = runtime.cancel()
                self.send_json(200 if state['status'] in TERMINAL else 202, state)
            elif method != 'GET' or suffix == '/cancel':
                self.error(405, 'METHOD_NOT_ALLOWED', 'Unsupported method')
            elif suffix is None:
                self.send_json(200, dict(runtime.run))
            elif suffix in ('/publications', '/logs'):
                query = parse_qs(url.query)
                after = int(query.get('after', ['0'])[0])
                limit = int(query.get('limit', ['100'])[0])
                if after < 0 or not 1 <= limit <= 1000:
                    raise ValueError('after >= 0 and limit in 1..1000 required')
                source = runtime.logs if suffix == '/logs' else runtime.publications
                items = [item for item in source if item['seq'] > after][:limit]
                next_after = items[-1]['seq'] if items else after
                self.send_json(200, {'runId': run_id, 'items': items, 'nextAfter': next_after,
                    'hasMore': bool(source and source[-1]['seq'] > next_after),
                    'closed': runtime.run['status'] in TERMINAL})
            elif runtime.run['status'] != 'SUCCEEDED':
                terminal = runtime.run['status'] in TERMINAL
                self.error(409, 'RESULT_UNAVAILABLE' if terminal else 'RESULT_NOT_READY',
                           'Result is not available')
            elif suffix == '/result/metadata':
                self.send_json(200, runtime.metadata)
            else:
                result_path = runtime.work / 'result.json'
                size = runtime.metadata['sizeBytes']
                # Release lock before streaming a potentially large result.
                self.stream_path = result_path
                self.stream_size = size
        if hasattr(self, 'stream_path'):
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.send_header('Content-Length', str(self.stream_size))
            self.end_headers()
            with self.stream_path.open('rb') as stream:
                while chunk := stream.read(65536):
                    self.wfile.write(chunk)
            del self.stream_path


def main():
    runtime = Runtime(Adapter(os.environ.get('MODEL_KIND', 'python')), os.environ.get('RUNS_ROOT', '/runs'))
    server = ThreadingHTTPServer(('0.0.0.0', int(os.environ.get('PORT', '8080'))), Handler)
    server.runtime = runtime
    def shutdown(signum, frame):
        with runtime.lock:
            if runtime.run and runtime.run['status'] not in TERMINAL:
                runtime.cancel()
        threading.Thread(target=server.shutdown, daemon=True).start()
    signal.signal(signal.SIGTERM, shutdown)
    signal.signal(signal.SIGINT, shutdown)
    server.serve_forever()
    # Keep the process alive until the worker confirms termination.
    while runtime.run and runtime.run['status'] not in TERMINAL:
        time.sleep(0.05)
    server.server_close()


if __name__ == '__main__':
    main()
