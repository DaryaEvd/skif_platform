"""Run an actual model via HTTP; save evidence without printing the matrix."""
import hashlib
import json
from pathlib import Path
import sys
import time
import urllib.error
import urllib.request

base, example, destination = sys.argv[1:]
request_data = json.loads(Path(example).read_text())
run_id = request_data['runId']
out = Path(destination)
out.mkdir(parents=True, exist_ok=True)


def call(path, body=None):
    request = urllib.request.Request(base + '/api/v1/' + path,
        data=None if body is None else json.dumps(body).encode(),
        headers={'Content-Type': 'application/json'})
    try:
        with urllib.request.urlopen(request, timeout=15) as response:
            return response.status, response.read()
    except urllib.error.HTTPError as error:
        with error:
            return error.code, error.read()


for _ in range(100):
    try:
        code, payload = call('readiness')
        if code == 200:
            break
    except OSError:
        pass
    time.sleep(.1)
else:
    raise RuntimeError('API did not become ready')
invalid = dict(request_data, parameters={})
assert call('runs', invalid)[0] == 422
assert call('runs', request_data)[0] == 202
states = []
for _ in range(1200):
    code, payload = call('runs/' + run_id)
    assert code == 200
    state = json.loads(payload)
    states.append(state['status'])
    if state['status'] in ('SUCCEEDED', 'CANCELLED', 'FAILED'):
        break
    time.sleep(.1)
else:
    raise RuntimeError('Smoke test observation deadline exceeded; no cancellation was sent')
(out / 'status.json').write_bytes(payload)
assert state['status'] == 'SUCCEEDED', state
assert call('runs', request_data)[0] == 200
changed = dict(request_data, experimentId='different')
assert call('runs', changed)[0] == 409
assert call('runs', dict(request_data, runId='new-run'))[0] == 409
code, result = call('runs/' + run_id + '/result')
assert code == 200
(out / 'result.json').write_bytes(result)
code, raw_metadata = call('runs/' + run_id + '/result/metadata')
assert code == 200
metadata = json.loads(raw_metadata)
(out / 'metadata.json').write_bytes(raw_metadata)
assert metadata['sha256'] == hashlib.sha256(result).hexdigest()
assert metadata['sizeBytes'] == len(result)
assert call('runs/' + run_id + '/result')[1] == result
for endpoint in ('logs', 'publications'):
    code, payload = call('runs/' + run_id + '/' + endpoint)
    assert code == 200
    (out / (endpoint + '.json')).write_bytes(payload)
    assert json.loads(payload)['closed']
logs = json.loads((out / 'logs.json').read_bytes())['items']
assert sum(x['message'].startswith('Process started pid=') for x in logs) == 1
assert json.loads((out / 'publications.json').read_bytes())['items'] == []
print(json.dumps({'runId': run_id, 'observedStatuses': list(dict.fromkeys(states)),
                  'status': state, 'metadata': metadata}, indent=2))
