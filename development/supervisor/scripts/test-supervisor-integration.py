"""Real Java -> HTTP wrapper -> child process integration. Requires a running supervisor."""
import copy
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import time
import urllib.error
import urllib.request

base = sys.argv[1].rstrip('/')
out = Path(sys.argv[2]).resolve()
out.mkdir(parents=True, exist_ok=True)
project = Path(__file__).resolve().parents[1]


def call(method, path, data=None, expected=200, destination=None):
    request = urllib.request.Request(base + path, method=method,
        data=None if data is None else json.dumps(data).encode(), headers={'Content-Type': 'application/json'})
    try:
        with urllib.request.urlopen(request, timeout=20) as response:
            code, body = response.status, response.read()
    except urllib.error.HTTPError as error:
        with error:
            code, body = error.code, error.read()
    if destination:
        destination.write_bytes(body)
    assert code == expected, (method, path, code, body[:1000])
    try:
        return json.loads(body)
    except ValueError:
        return body


def status(identifier):
    return call('GET', f'/api/experiments/{identifier}/status')


def wait(identifier, folder, cancel_at=None):
    states = []
    cancelled = False
    deadline = time.monotonic() + 600
    while time.monotonic() < deadline:
        value = status(identifier)
        states.append(value)
        if cancel_at and not cancelled and cancel_at(value):
            call('POST', f'/api/experiments/{identifier}/cancel', expected=202,
                 destination=folder / 'cancel-response.json')
            cancelled = True
        if value['status'] in ('COMPLETED', 'FAILED', 'CANCELLED'):
            (folder / 'status-history.json').write_text(json.dumps(states, indent=2))
            (folder / 'status.json').write_text(json.dumps(value, indent=2))
            return value
        time.sleep(.05)
    raise AssertionError('Integration observation deadline exceeded; inspect running experiment')


def start(config, name):
    folder = out / name
    folder.mkdir()
    (folder / 'request.json').write_text(json.dumps(config, indent=2))
    t = time.monotonic()
    response = call('POST', '/api/experiments/start', config, expected=202,
                    destination=folder / 'registration.json')
    elapsed = time.monotonic() - t
    assert elapsed < 5, ('Registration blocks on calculations', elapsed)
    identifier = response['experimentId']
    (folder / 'registration-seconds.txt').write_text(str(elapsed))
    return identifier, folder


call('GET', '/swagger-ui/index.html', destination=out / 'swagger-ui.html')
api = call('GET', '/v3/api-docs', destination=out / 'openapi.json')
assert '202' in api['paths']['/api/experiments/start']['post']['responses']
assert api['paths']['/api/experiments/start']['post']['requestBody']['content']['application/json']['example']['models'][0]['language'] == 'PYTHON'
example = call('GET', '/api/experiments/example', destination=out / 'example.json')
conflict = copy.deepcopy(example)
conflict['models'][1]['parameters']['E_start'] = 30
call('POST', '/api/experiments/start', conflict, expected=400, destination=out / 'binding-conflict.json')

identifier, folder = start(example, 'success')
final = wait(identifier, folder)
assert final['status'] == 'COMPLETED', final
assert [x['status'] for x in final['modelStatuses'].values()] == ['SUCCEEDED', 'SUCCEEDED']
python_result = call('GET', f'/api/experiments/{identifier}/models/1/result', destination=folder / 'python-result.json')
c_result = call('GET', f'/api/experiments/{identifier}/models/2/result', destination=folder / 'c-result.json')
c_request = call('GET', f'/api/experiments/{identifier}/models/2/request', destination=folder / 'c-request.json')
for field in ('E_start', 'E_end'):
    assert c_request['parameters'][field] == python_result[field]
assert len(c_result['img']) == 988 and all(len(row) == 988 for row in c_result['img'])
results = call('GET', f'/api/experiments/{identifier}/results', destination=folder / 'results.json')
assert all(x['metadata']['downloadUrl'] == x['resultUrl'] for x in results)
assert all('img' not in x['metadata'] for x in results)
call('GET', f'/api/experiments/{identifier}/logs', destination=folder / 'events.json')
call('GET', f'/api/experiments/{identifier}/models/1/logs', destination=folder / 'python-logs.jsonl')
call('GET', f'/api/experiments/{identifier}/models/2/logs', destination=folder / 'c-logs.jsonl')
containers = subprocess.check_output(['docker', 'ps', '-aq', '--filter', f'label=skif.supervisor.experiment={identifier}'], text=True)
assert not containers.strip(), 'Containers remain after successful experiment'
assert call('GET', f'/api/experiments/{identifier}/models/2/result') == c_result
print('success:', identifier, 'Python energy:', python_result, 'C energy matches; containers removed', flush=True)

failure = copy.deepcopy(example)
failure['models'][0]['parameters'].update(E_input=1e308, h_y_1=1e308)
identifier, folder = start(failure, 'python-failure')
final = wait(identifier, folder)
assert final['status'] == 'FAILED', final
assert final['modelStatuses']['2']['status'] == 'SKIPPED', final
call('GET', f'/api/experiments/{identifier}/models/2/request', expected=409)
call('GET', f'/api/experiments/{identifier}/models/1/logs', destination=folder / 'python-logs.jsonl')
print('Python failure skips C:', identifier, flush=True)

# Cancel an actual, long C calculation; no test model or precomputed Python output.
cancel = copy.deepcopy(example)
cancel['models'][1]['parameters']['t'] = 100000
identifier, folder = start(cancel, 'cancel-running-c')
conflict_result = call('POST', '/api/experiments/start', example, expected=409,
                       destination=folder / 'concurrent-start-conflict.json')
final = wait(identifier, folder, lambda s: s['currentOrder'] == 2 and s['stage'] == 'MODEL_RUNNING')
assert final['status'] == 'CANCELLED', final
assert final['modelStatuses']['1']['status'] == 'SUCCEEDED', final
assert not subprocess.check_output(['docker','ps','-aq','--filter',f'label=skif.supervisor.experiment={identifier}'],text=True).strip()
call('GET', f'/api/experiments/{identifier}/models/1/result', destination=folder / 'preserved-python-result.json')
call('GET', f'/api/experiments/{identifier}/models/2/logs', destination=folder / 'c-logs.jsonl')
call('GET', f'/api/experiments/{identifier}/models/2/result', expected=409)
print('Actual running C cancelled; Python result preserved:', identifier, flush=True)

identifier, folder = start(example, 'cancel-before-c')
final = wait(identifier, folder, lambda s: s['currentOrder'] <= 1)
assert final['status'] == 'CANCELLED', final
assert final['modelStatuses']['2']['status'] == 'SKIPPED', final
call('GET', f'/api/experiments/{identifier}/models/2/request', expected=409)
print('Early cancellation skips C:', identifier, flush=True)
(out / 'PASS').write_text('Real Java -> Python -> C integration passed\n')
