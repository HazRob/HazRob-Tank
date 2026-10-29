"""Verify all 43 Android Java coefficient rows against immutable source data.

Only numeric/source-data corruption fails. Incomplete R0, heater, label or
out-of-source-range metadata never excludes the research hypothesis.
"""
import json
import re
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/"source_snapshots/hr069/mq_research_curves_43.json"
JAVA=ROOT.parents[1]/"android/app/src/main/java/com/servoc/app/chemcore/SourceCurveRegistryV6.java"

class JavaSourceCurveParity(unittest.TestCase):
    def test_all_43_android_coefficients_match_pinned_historical_reference(self):
        reference=json.loads(SOURCE.read_text())["curves"]
        text=JAVA.read_text()
        pattern=re.compile(
            r'new Entry\((\d+), "([^"]+)", "([^"]+)", (\d+), '
            r'([-+.\dEe]+), ([-+.\dEe]+), ([-+.\dEe]+), '
            r'([-+.\dEe]+), ([-+.\dEe]+)\)')
        rows=pattern.findall(text)
        self.assertEqual(len(rows),43)
        self.assertEqual(len(reference),43)
        for i,(android,upstream) in enumerate(zip(rows,reference),start=1):
            idx,sensor,gas,uid,a,b,low,high,scale=android
            with self.subTest(source_curve=i):
                self.assertEqual(int(idx),i)
                self.assertEqual(sensor,upstream["sensor"])
                self.assertEqual(gas,upstream["gas_label"])
                self.assertEqual(int(uid),upstream["legacy_id"])
                self.assertEqual(float(a),upstream["ratio_a"])
                self.assertEqual(float(b),upstream["ratio_b"])
                self.assertEqual(float(low),upstream["min_ppm"])
                self.assertEqual(float(high),upstream["max_ppm"])
                self.assertEqual(float(scale),upstream["output_scale"])
        self.assertIn("bd597ffe9ecaee6a0ccfddc5ec1b89c1cccc0ba1",text)

if __name__=="__main__":
    unittest.main()
