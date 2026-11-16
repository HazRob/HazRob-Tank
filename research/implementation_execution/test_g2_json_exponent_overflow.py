"""G2 JSON exponent-overflow fail-closed CLI regression (stdlib only)."""
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).with_name("audit_g2_independent_evidence.py")
SCHEMA = "hazrob.g2.independent-evidence.v1"

class NumericOverflowRegression(unittest.TestCase):
    def test_overflow_exponents_blocked_at_any_depth(self):
        for token in ("1e309", "-1e309", "9.99e9999", "1E+999"):
            for location in ("top", "record"):
                with self.subTest(token=token, location=location):
                    payload = ('{"schema":"' + SCHEMA + '","records":[],"extra":' + token + '}'
                               if location == "top" else
                               '{"schema":"' + SCHEMA + '","records":[{"extra":' + token + '}]}')
                    with tempfile.TemporaryDirectory() as folder:
                        path = Path(folder) / "evidence.json"
                        path.write_text(payload, encoding="utf-8")
                        completed = subprocess.run(
                            [sys.executable, str(SCRIPT), "--manifest", str(path), "--json"],
                            capture_output=True, text=True, check=False)
                    self.assertEqual(completed.returncode, 2, completed.stderr)
                    result = json.loads(completed.stdout)
                    self.assertEqual(result["status"], "BLOCKED")
                    self.assertFalse(result["scientifically_validated"])
                    self.assertTrue(any("non-finite JSON number" in e for e in result["errors"]))

if __name__ == "__main__":
    unittest.main()
