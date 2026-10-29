"""Fresh-container real Unity tests. No existing containers or volumes are reused."""
import json,subprocess,sys,uuid
from pathlib import Path
root=Path(__file__).resolve().parents[1]
out=Path(sys.argv[1]).resolve();out.mkdir(parents=True,exist_ok=False)
image=sys.argv[2] if len(sys.argv)>2 else 'skif-model-unity-api:test'
session='unity-check-'+uuid.uuid4().hex[:10]
for mode in ('success','wrap','cancel'):
    folder=out/mode;folder.mkdir()
    name=session+'-'+mode
    created=False
    try:
        command=['docker','run','-d','--init','--restart=no','--name',name,'--label','skif.unity.test='+session,'-p','127.0.0.1::8080',image]
        subprocess.run(command,check=True,stdout=(folder/'container.id').open('w'))
        created=True
        port=subprocess.check_output(['docker','port',name,'8080/tcp'],text=True).strip()
        (folder/'base-url.txt').write_text('http://'+port)
        result=subprocess.run([sys.executable,str(root/'scripts/test-unity-model-api.py'),'http://'+port,str(folder/'http'),mode],stdout=(folder/'test.log').open('w'),stderr=subprocess.STDOUT)
        (folder/'exit-code.txt').write_text(str(result.returncode))
        assert result.returncode==0,(mode,(folder/'test.log').read_text())
        print(mode,'PASS',flush=True)
    finally:
        if created:
            inspect=json.loads(subprocess.check_output(['docker','inspect',name]))
            assert inspect[0]['Config']['Labels']['skif.unity.test']==session
            (folder/'inspect.json').write_text(json.dumps(inspect,indent=2))
            subprocess.run(['docker','logs',name],stdout=(folder/'docker.log').open('w'),stderr=subprocess.STDOUT,check=True)
            subprocess.run(['docker','cp',name+':/runs',str(folder/'runs-snapshot')],check=True)
            subprocess.run(['docker','stop','--time','3',name],check=True,stdout=subprocess.DEVNULL)
            subprocess.run(['docker','rm',name],check=True,stdout=subprocess.DEVNULL)
(out/'PASS').write_text('Fresh Unity Linux Docker tests passed\n')
