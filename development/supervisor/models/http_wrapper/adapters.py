"""Model-specific validation and commands; no orchestration or HTTP here."""
import json
import math
import os
import shutil
import sys
from pathlib import Path


class InvalidParameters(ValueError):
    pass


def number(value):
    return type(value) in (int, float) and math.isfinite(value)


def load_json(path):
    def invalid(value):
        raise ValueError('Non-finite JSON constant: ' + value)
    return json.loads(Path(path).read_text(), parse_constant=invalid)


class Adapter:
    def __init__(self, kind):
        if kind not in ('python', 'c'):
            raise ValueError('MODEL_KIND must be python or c')
        self.kind = kind
        self.source = Path(os.environ.get('MODEL_SOURCE', '/opt/model'))
        self.material = Path(os.environ.get('SAMPLE_MATERIAL', '/opt/data/sampleMaterial.json'))

    def ready(self):
        required = [self.source / ('test.py' if self.kind == 'python' else 'difract')]
        if self.kind == 'c':
            required.append(self.material)
        return all(p.is_file() for p in required)

    def validate(self, params):
        fields = ('E_input h_y_1 h_y_2 h_x_1 h_x_2' if self.kind == 'python' else
                  'c_x c_y c_z s_x s_y s_z omega kappa phi xSampleSize ySampleSize zSampleSize '
                  'd_x d_y d_z theta beta gammaValue sU sB sR sL E_start E_end t').split()
        if not isinstance(params, dict):
            raise InvalidParameters('parameters must be an object')
        for name in fields:
            if name not in params or not number(params[name]):
                raise InvalidParameters(name + ' must be a finite number')
        unknown = set(params) - set(fields)
        if unknown:
            raise InvalidParameters('Unknown parameters: ' + ', '.join(sorted(unknown)))
        if self.kind == 'c':
            for name in ('t', 'xSampleSize', 'ySampleSize', 'zSampleSize'):
                if type(params[name]) is not int or not 0 < params[name] <= 2147483647:
                    raise InvalidParameters(name + ' must be a positive 32-bit integer')
            # newSigma interpolates between adjacent material rows without a bounds check.
            material = load_json(self.material)
            energies = [row[0] * 1000 for row in material['cross_sections']]
            if not energies[0] <= params['E_start'] <= params['E_end'] < energies[-1]:
                raise InvalidParameters('Energy interval must fit material table: [%s, %s)' %
                                        (energies[0], energies[-1]))
            if params['sU'] + params['sB'] <= 0 or params['sR'] + params['sL'] <= 0:
                raise InvalidParameters('Slit opening must be positive')
            n = 22000000000 * params['t'] * ((min(params['sU'], 350000) + min(params['sB'], 350000)) *
                (min(params['sR'], 5000000) + min(params['sL'], 5000000))) / 7e12
            if not math.isfinite(n) or not 1 <= round(n) < 2**64:
                raise InvalidParameters('Parameters produce an invalid photon count')

    def prepare(self, work, params):
        for name in ('input', 'output', 'json'):
            (work / name).mkdir()
        (work / 'input/start.json').write_text(json.dumps(params, allow_nan=False))
        if self.kind == 'c':
            shutil.copyfile(self.material, work / 'json/sampleMaterial.json')
        env = os.environ.copy()
        env.update(MODEL_INPUT=str(work / 'input/start.json'),
                   MODEL_OUTPUT=str(work / 'output/end.json'),
                   MODEL_MATERIAL=str(work / 'json/sampleMaterial.json'),
                   MODEL_ERROR_RATE=str(work / 'json/error_rate.json'), PYTHONUNBUFFERED='1')
        command = ([sys.executable, '-u', str(self.source / 'test.py')] if self.kind == 'python'
                   else [str(self.source / 'difract')])
        return command, env

    def result(self, work):
        result = load_json(work / 'output/end.json')
        if not isinstance(result, dict):
            raise ValueError('Result must be an object')
        required = ('E_start', 'E_end') if self.kind == 'python' else ('max_dA', 'min_dA')
        for name in required:
            if not number(result.get(name)):
                raise ValueError('Invalid result field: ' + name)
        summary = {key: result[key] for key in required}
        metadata = {'schema': self.kind + '-result/v1', 'summary': summary}
        if self.kind == 'c':
            matrix = result.get('img')
            if not isinstance(matrix, list) or len(matrix) != 988 or any(
                    not isinstance(row, list) or len(row) != 988 or
                    any(not number(v) for v in row) for row in matrix):
                raise ValueError('img must be a finite numeric 988 x 988 matrix')
            errors = load_json(work / 'json/error_rate.json')
            keys = ('max_err_abs', 'mean', 'max_err', 'min_err', 'inters_err')
            if not isinstance(errors, dict) or any(not number(errors.get(k)) for k in keys):
                raise ValueError('Invalid error_rate.json')
            # Include the additional output in the immutable HTTP result.
            result['error_rate'] = errors
            metadata['matrix'] = {'field': 'img', 'rows': 988, 'columns': 988,
                                  'elementType': 'number'}
            metadata['additionalOutputs'] = ['error_rate']
        return result, metadata
