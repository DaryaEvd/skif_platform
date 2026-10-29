"""Run in the C API image: verify real C error propagation without HTTP validation."""
import json
import os
from pathlib import Path
import subprocess
import tempfile

params = dict(c_x=0,c_y=3950000000,c_z=0,s_x=0,s_y=0,s_z=0,omega=0,kappa=0,phi=0,
    xSampleSize=600000,ySampleSize=600000,zSampleSize=600000,d_x=0,d_y=55000000,d_z=0,
    theta=0,beta=0,gammaValue=0,sU=5000,sB=5000,sR=5000,sL=5000,E_start=30,E_end=30,t=1)
with tempfile.TemporaryDirectory() as directory:
    root = Path(directory)
    start = root / 'start.json'
    start.write_text(json.dumps(params))
    base = dict(os.environ, MODEL_INPUT=str(start), MODEL_OUTPUT=str(root / 'end.json'),
        MODEL_ERROR_RATE=str(root / 'error_rate.json'), MODEL_MATERIAL='/opt/data/sampleMaterial.json')
    cases = [('missing_input', dict(base, MODEL_INPUT=str(root / 'absent.json'))),
             ('output_write_failure', dict(base, MODEL_OUTPUT=str(root / 'absent/end.json'))),
             ('additional_output_failure', dict(base, MODEL_ERROR_RATE=str(root / 'absent/error_rate.json')))]
    for name, env in cases:
        process = subprocess.run(['/opt/model/difract'], env=env, capture_output=True, timeout=30)
        assert process.returncode != 0, (name, process.returncode)
        print(name, 'exitCode=', process.returncode)
