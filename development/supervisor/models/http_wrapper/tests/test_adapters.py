import json
from pathlib import Path
import sys
import tempfile
import unittest
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
if Path('/opt/wrapper').exists():
    sys.path.insert(0, '/opt/wrapper')
from adapters import Adapter


class ResultValidation(unittest.TestCase):
    def test_c_matrix_shape_and_numeric_values(self):
        adapter = Adapter('c')
        with tempfile.TemporaryDirectory() as directory:
            work = Path(directory)
            (work / 'output').mkdir()
            (work / 'json').mkdir()
            errors = dict(max_err_abs=1, mean=1, max_err=1, min_err=1, inters_err=1)
            (work / 'json/error_rate.json').write_text(json.dumps(errors))
            for matrix in ([[0]], [[0] * 988] * 987, [[None] * 988] * 988):
                (work / 'output/end.json').write_text(json.dumps(dict(max_dA=1, min_dA=0, img=matrix)))
                with self.assertRaises(ValueError):
                    adapter.result(work)
            (work / 'output/end.json').write_text(json.dumps(dict(max_dA=1, min_dA=0, img=[[0] * 988] * 988)))
            result, metadata = adapter.result(work)
            self.assertEqual(metadata['matrix']['rows'], 988)
            self.assertEqual(result['error_rate'], errors)


if __name__ == '__main__':
    unittest.main(verbosity=2)
