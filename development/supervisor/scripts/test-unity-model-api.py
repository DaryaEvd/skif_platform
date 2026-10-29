"""Actual Unity HTTP numerical test; one fresh container per invocation."""
import hashlib,json,math,sys,time,urllib.request,urllib.error
from pathlib import Path
base=sys.argv[1].rstrip('/');out=Path(sys.argv[2]);out.mkdir(parents=True,exist_ok=False)
mode=sys.argv[3] if len(sys.argv)>3 else 'success'
angle=360.0 if mode=='wrap' else (180.5 if mode=='cancel' else 15.5)
speed=0.5 if mode=='cancel' else (36.0 if mode=='wrap' else 7.75)
def call(method,path,body=None,expected=(200,)):
    req=urllib.request.Request(base+path,method=method,data=None if body is None else json.dumps(body).encode(),headers={'Content-Type':'application/json'})
    try:
        with urllib.request.urlopen(req,timeout=5) as r: code,payload=r.status,r.read()
    except urllib.error.HTTPError as e:
        with e:code,payload=e.code,e.read()
    assert code in expected,(method,path,code,payload[:1000])
    return json.loads(payload),payload
for i in range(150):
    try:
        ready,_=call('GET','/api/v1/readiness',expected=(200,503))
        if ready['ready']:break
    except OSError:pass
    time.sleep(.1)
else:raise AssertionError('Unity scene not ready')
(out/'readiness.json').write_text(json.dumps(ready,indent=2))
time.sleep(.4)
ready2,_=call('GET','/api/v1/readiness')
if 'currentLocalOrientation' in ready2:assert ready2['currentLocalOrientation']==ready['currentLocalOrientation']
request={'runId':'unity-1','experimentId':'unity-test','parameters':{'angleDeg':angle,'speedDegPerSec':speed}}
for field,value in [('angleDeg',0),('angleDeg',.005),('angleDeg',361),('speedDegPerSec',0),('speedDegPerSec',37)]:
    bad=json.loads(json.dumps(request));bad['parameters'][field]=value
    call('POST','/api/v1/runs',bad,expected=(422,))
assert call('GET','/api/v1/readiness')[0]['ready']
accepted,_=call('POST','/api/v1/runs',request,expected=(202,));assert accepted['status']=='ACCEPTED'
call('POST','/api/v1/runs',request)
bad=json.loads(json.dumps(request));bad['parameters']['angleDeg']=angle-1
call('POST','/api/v1/runs',bad,expected=(409,))
call('POST','/api/scanner/start',{},expected=(409,))
states=[];cancelled=False
for i in range(1500):
    state,_=call('GET','/api/v1/runs/unity-1');states.append(state)
    if mode=='cancel' and state['status']=='RUNNING' and state['snapshot']['accumulatedRotationDeg']>0.2 and not cancelled:
        call('POST','/api/v1/runs/unity-1/cancel',{},expected=(200,202));cancelled=True
    if state['status'] in ('SUCCEEDED','FAILED','CANCELLED'):break
    time.sleep(.03)
else:raise AssertionError('Unity did not terminate')
(out/'status-history.json').write_text(json.dumps(states,indent=2));(out/'status.json').write_text(json.dumps(state,indent=2))
assert any(s['status']=='RUNNING' for s in states)
assert state['status']==('CANCELLED' if mode=='cancel' else 'SUCCEEDED'),state
publications=[];cursor=0
while True:
    page,_=call('GET',f'/api/v1/runs/unity-1/publications?after={cursor}&limit=3')
    publications+=page['items'];cursor=page['nextAfter']
    if not page['hasMore']:break
assert page['closed'] and len(publications)>2
assert [p['seq'] for p in publications]==list(range(1,len(publications)+1))
assert any(p['values']['accumulatedRotationDeg']>0 for p in publications)
(out/'publications.json').write_text(json.dumps(publications,indent=2))
logs,_=call('GET','/api/v1/runs/unity-1/logs');(out/'logs.json').write_text(json.dumps(logs,indent=2))
assert sum('Relative Phi started' in x['message'] for x in logs['items'])==1
terminal_before=state['snapshot'];time.sleep(.35)
assert call('GET','/api/v1/runs/unity-1')[0]['snapshot']==terminal_before
call('POST','/api/v1/runs',request)
other=dict(request,runId='new-run');call('POST','/api/v1/runs',other,expected=(409,))
if mode=='cancel':
    call('GET','/api/v1/runs/unity-1/result',expected=(409,))
else:
    result,payload=call('GET','/api/v1/runs/unity-1/result');metadata,_=call('GET','/api/v1/runs/unity-1/result/metadata')
    assert metadata['sha256']==hashlib.sha256(payload).hexdigest() and metadata['sizeBytes']==len(payload)
    assert result['angleDeg']==angle and result['speedDegPerSec']==speed
    assert abs(result['accumulatedRotationDeg']-angle)<=result['toleranceDeg']==.01
    assert abs(result['remainingAngleDeg'])<=.01
    assert result['modelElapsedSeconds']>0 and result['realElapsedSeconds']>0
    assert call('GET','/api/v1/runs/unity-1/result')[1]==payload
    (out/'result.json').write_bytes(payload);(out/'metadata.json').write_text(json.dumps(metadata,indent=2))
print(json.dumps({'mode':mode,'status':state['status'],'publications':len(publications),'snapshot':state['snapshot']},indent=2))
