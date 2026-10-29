"""Unity through the real existing Java API; requires a freshly built Linux player."""
import copy,json,subprocess,sys,time,urllib.request,urllib.error
from pathlib import Path
base=sys.argv[1].rstrip('/');out=Path(sys.argv[2]).resolve();out.mkdir(parents=True,exist_ok=False)
def call(method,path,data=None,expected=200,file=None):
    req=urllib.request.Request(base+path,method=method,data=None if data is None else json.dumps(data).encode(),headers={'Content-Type':'application/json'})
    try:
        with urllib.request.urlopen(req,timeout=20) as r:code,body=r.status,r.read()
    except urllib.error.HTTPError as e:
        with e:code,body=e.code,e.read()
    if file:file.write_bytes(body)
    assert code==expected,(code,body[:1000])
    if '/models/' in path and (path.endswith('/publications') or path.endswith('/logs')):return body
    try:return json.loads(body)
    except ValueError:return body
call('GET','/swagger-ui/index.html',file=out/'swagger-ui.html')
api=call('GET','/v3/api-docs',file=out/'openapi.json')
assert len(api['paths']['/api/experiments/start']['post']['requestBody']['content']['application/json']['examples'])==2
example=call('GET','/api/experiments/example/unity',file=out/'example.json')
for mode in ('success','cancel'):
    folder=out/mode;folder.mkdir();request=copy.deepcopy(example)
    if mode=='cancel':request['models'][0]['parameters'].update(angleDeg=180.5,speedDegPerSec=.5)
    response=call('POST','/api/experiments/start',request,expected=202,file=folder/'registration.json')
    identifier=response['experimentId'];states=[];cancelled=False;live_publications=[]
    deadline=time.monotonic()+240
    while time.monotonic()<deadline:
        state=call('GET',f'/api/experiments/{identifier}/status');states.append(state)
        if state['stage']=='MODEL_RUNNING':
            raw=call('GET',f'/api/experiments/{identifier}/models/1/publications')
            if raw:live_publications=raw.decode().splitlines()
            if mode=='cancel' and len(live_publications)>2 and not cancelled:
                call('POST',f'/api/experiments/{identifier}/cancel',expected=202,file=folder/'cancel.json');cancelled=True
        if state['status'] in ('COMPLETED','CANCELLED','FAILED'):break
        time.sleep(.08)
    else:raise AssertionError('Supervisor did not finish Unity experiment')
    (folder/'status-history.json').write_text(json.dumps(states,indent=2));(folder/'status.json').write_text(json.dumps(state,indent=2))
    assert state['status']==('CANCELLED' if mode=='cancel' else 'COMPLETED'),state
    assert any(s['stage']=='MODEL_RUNNING' for s in states)
    assert len(live_publications)>1,'No real publications during calculation'
    raw=call('GET',f'/api/experiments/{identifier}/models/1/publications',file=folder/'publications.jsonl')
    assert len(raw.decode().splitlines())>2
    call('GET',f'/api/experiments/{identifier}/models/1/logs',file=folder/'logs.jsonl')
    assert not subprocess.check_output(['docker','ps','-aq','--filter',f'label=skif.supervisor.experiment={identifier}'],text=True).strip()
    if mode=='success':
        result=call('GET',f'/api/experiments/{identifier}/models/1/result',file=folder/'result.json')
        assert result['angleDeg']==15.5 and result['speedDegPerSec']==7.75
        assert abs(result['accumulatedRotationDeg']-15.5)<=.01
        entries=call('GET',f'/api/experiments/{identifier}/results',file=folder/'results.json')
        assert call('GET',entries[0]['resultUrl'])==result
        print('Unity via Java PASS',identifier,result,flush=True)
    else:
        call('GET',f'/api/experiments/{identifier}/models/1/result',expected=409)
        print('Unity via Java CANCELLED',identifier,flush=True)
(out/'PASS').write_text('Unity via Java real integration passed\n')
