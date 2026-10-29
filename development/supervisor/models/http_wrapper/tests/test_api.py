"""HTTP integration tests with explicitly isolated synthetic subprocesses."""
import concurrent.futures
import json
import os
from pathlib import Path
import sys
import tempfile
import threading
import time
import unittest
import urllib.error
import urllib.request

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
if Path('/opt/wrapper').exists():
    sys.path.insert(0, '/opt/wrapper')
from adapters import Adapter
from server import Handler, Runtime, ThreadingHTTPServer

PARAMS = dict(E_input=130, h_y_1=.8, h_y_2=.8, h_x_1=.8, h_x_2=.1)


class TestAdapter(Adapter):
    def __init__(self, source):
        super().__init__('python')
        self.source = source


class API(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.base = Path(self.temp.name)
        self.source = self.base / 'model'
        self.source.mkdir()
        self.script = self.source / 'test.py'
        self.script.write_text('')
        self.runtime = Runtime(TestAdapter(self.source), self.base / 'runs')
        self.server = ThreadingHTTPServer(('127.0.0.1', 0), Handler)
        self.server.runtime = self.runtime
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()
        self.url = 'http://127.0.0.1:' + str(self.server.server_port)
        self.request = dict(runId='test-1', experimentId='test', parameters=PARAMS)

    def tearDown(self):
        if self.runtime.run and self.runtime.run['status'] not in {'SUCCEEDED', 'FAILED', 'CANCELLED'}:
            self.runtime.cancel()
            self.wait()
        self.server.shutdown()
        self.server.server_close()
        self.thread.join()
        self.temp.cleanup()

    def call(self, path='', data=None):
        request = urllib.request.Request(self.url + '/api/v1/' + path,
            data=None if data is None else json.dumps(data).encode(),
            headers={'Content-Type': 'application/json'})
        try:
            with urllib.request.urlopen(request, timeout=5) as response:
                return response.status, json.load(response)
        except urllib.error.HTTPError as error:
            with error:
                return error.code, json.load(error)

    def wait(self):
        deadline = time.monotonic() + 12
        while time.monotonic() < deadline:
            code, state = self.call('runs/test-1')
            if state.get('status') in {'SUCCEEDED', 'FAILED', 'CANCELLED'}:
                return state
            time.sleep(.02)
        self.fail('Calculation did not terminate')

    def program(self, text):
        self.script.write_text('import os, json, time, subprocess, sys\n' + text)

    def test_success_idempotency_conflicts_and_retention(self):
        self.program("print('CALLED', flush=True)\njson.dump({'E_start': 1, 'E_end': 2}, open(os.environ['MODEL_OUTPUT'], 'w'))\n")
        self.assertEqual(self.call('runs', self.request)[0], 202)
        self.assertEqual(self.wait()['status'], 'SUCCEEDED')
        self.assertEqual(self.call('runs', self.request)[0], 200)
        changed = dict(self.request, parameters=dict(PARAMS, E_input=131))
        self.assertEqual(self.call('runs', changed)[0], 409)
        self.assertEqual(self.call('runs', dict(self.request, runId='another'))[0], 409)
        logs = self.call('runs/test-1/logs')[1]['items']
        self.assertEqual(sum(x['message'] == 'CALLED' for x in logs), 1)
        self.assertEqual(self.call('runs/test-1/result')[1], {'E_start': 1, 'E_end': 2})
        self.assertEqual(self.call('runs/test-1/result/metadata')[0], 200)
        self.assertTrue(self.call('runs/test-1/publications')[1]['closed'])
        self.assertEqual(self.call('runs/test-1/publications')[1]['items'], [])
        self.assertFalse(self.call('readiness')[1]['ready'])

    def test_concurrent_registration_starts_once(self):
        self.program("print('CALLED', flush=True)\ntime.sleep(.3)\njson.dump({'E_start': 1, 'E_end': 2}, open(os.environ['MODEL_OUTPUT'], 'w'))\n")
        with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
            responses = list(pool.map(lambda _: self.call('runs', self.request)[0], range(8)))
        self.assertEqual(responses.count(202), 1)
        self.assertEqual(self.wait()['status'], 'SUCCEEDED')
        logs = self.call('runs/test-1/logs')[1]['items']
        self.assertEqual(sum(x['message'] == 'CALLED' for x in logs), 1)

    def test_invalid_input_does_not_consume_container(self):
        invalid = dict(self.request, parameters={'E_input': 'bad'})
        self.assertEqual(self.call('runs', invalid)[0], 422)
        self.assertTrue(self.call('readiness')[1]['ready'])

    def test_nonzero_exit(self):
        self.program("print('failure', file=sys.stderr)\nsys.exit(7)\n")
        self.call('runs', self.request)
        state = self.wait()
        self.assertEqual(state['error']['code'], 'PROCESS_EXIT_NONZERO')
        self.assertEqual(state['error']['exitCode'], 7)
        self.assertEqual(self.call('runs/test-1/result')[0], 409)

    def test_signal_crash(self):
        self.program('import signal\nos.kill(os.getpid(), signal.SIGKILL)\n')
        self.call('runs', self.request)
        self.assertEqual(self.wait()['error']['code'], 'PROCESS_CRASHED')

    def test_missing_result(self):
        self.program('pass\n')
        self.call('runs', self.request)
        self.assertEqual(self.wait()['error']['code'], 'RESULT_MISSING')

    def test_invalid_result(self):
        self.program("open(os.environ['MODEL_OUTPUT'], 'w').write('{bad')\n")
        self.call('runs', self.request)
        self.assertEqual(self.wait()['error']['code'], 'RESULT_INVALID')

    def test_nonfinite_result(self):
        self.program("json.dump({'E_start': float('nan'), 'E_end': 2}, open(os.environ['MODEL_OUTPUT'], 'w'))\n")
        self.call('runs', self.request)
        self.assertEqual(self.wait()['error']['code'], 'RESULT_INVALID')

    def test_cancel_during_finalization_cannot_become_success(self):
        entered = threading.Event()
        release = threading.Event()
        original = self.runtime.adapter.result
        def delayed(work):
            entered.set()
            release.wait(5)
            return original(work)
        self.runtime.adapter.result = delayed
        self.program("json.dump({'E_start': 1, 'E_end': 2}, open(os.environ['MODEL_OUTPUT'], 'w'))\n")
        self.call('runs', self.request)
        self.assertTrue(entered.wait(5))
        self.assertEqual(self.call('runs/test-1')[1]['status'], 'FINALIZING')
        self.assertEqual(self.call('runs/test-1/cancel', {})[1]['status'], 'CANCELLING')
        release.set()
        self.assertEqual(self.wait()['status'], 'CANCELLED')
        self.assertEqual(self.call('runs/test-1/result')[0], 409)

    def test_cancel_process_group_and_status_while_running(self):
        self.program("child = subprocess.Popen([sys.executable, '-c', 'import time; time.sleep(120)'])\n"
                     "print('CHILD=' + str(child.pid), flush=True)\n"
                     "time.sleep(120)\n")
        self.call('runs', self.request)
        deadline = time.monotonic() + 5
        child = None
        while time.monotonic() < deadline:
            logs = self.call('runs/test-1/logs')[1]['items']
            for log in logs:
                if log['message'].startswith('CHILD='):
                    child = int(log['message'].split('=')[1])
            if child:
                break
            time.sleep(.02)
        self.assertIsNotNone(child)
        self.assertEqual(self.call('runs/test-1')[1]['status'], 'RUNNING')
        self.assertEqual(self.call('runs/test-1/result')[0], 409)
        self.assertEqual(self.call('runs/test-1/cancel', {})[0], 202)
        self.assertEqual(self.wait()['status'], 'CANCELLED')
        self.assertIsNotNone(self.runtime.process.poll())
        stat = Path('/proc') / str(child) / 'stat'
        self.assertTrue(not stat.exists() or stat.read_text().split()[2] == 'Z', 'Child still executing')
        self.assertEqual(self.call('runs', self.request)[1]['status'], 'CANCELLED')
        self.assertEqual(self.call('runs/test-1/result')[0], 409)
        self.assertTrue(self.call('runs/test-1/logs')[1]['closed'])


if __name__ == '__main__':
    unittest.main(verbosity=2)
