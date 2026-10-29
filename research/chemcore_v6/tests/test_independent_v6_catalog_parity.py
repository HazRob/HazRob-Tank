"""Independent ChemCore Tank V6 source catalog must match all original 43 curves."""
import json
import re
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
REFERENCE=ROOT/"source_snapshots/hr069/mq_research_curves_43.json"
CATALOG=ROOT.parents[1]/"android/app/src/main/java/com/hazrob/tank/chemcore/v6/SourceCatalog.java"
ENGINE=ROOT.parents[1]/"android/app/src/main/java/com/hazrob/tank/chemcore/v6/Engine.java"

class IndependentEngineSourceParity(unittest.TestCase):
    def test_01_every_coefficient_agrees_with_original_research_archive(self):
        original=json.loads(REFERENCE.read_text())["curves"]
        code=CATALOG.read_text()
        pattern=re.compile(
          r'new Curve\((\d+), "([^"]+)", (\d+)L, "([^"]+)", '
          r'([-+0-9.eE]+), ([-+0-9.eE]+), ([-+0-9.eE]+), ([-+0-9.eE]+), ([-+0-9.eE]+)\)')
        embedded=pattern.findall(code)
        self.assertEqual(len(embedded),43)
        for i,(row,src) in enumerate(zip(embedded,original),1):
            idx,sensor,uid,label,a,b,minimum,maximum,scale=row
            self.assertEqual(int(idx),i)
            self.assertEqual(sensor,src["sensor"])
            self.assertEqual(int(uid),src["legacy_id"])
            self.assertEqual(label,src["gas_label"])
            self.assertEqual((float(a),float(b),float(minimum),float(maximum),float(scale)),
               (src["ratio_a"],src["ratio_b"],src["min_ppm"],src["max_ppm"],src["output_scale"]))
        self.assertIn("bd597ffe9ecaee6a0ccfddc5ec1b89c1cccc0ba1",code)

    def test_02_new_kernel_has_no_predecessor_engine_imports(self):
        s=ENGINE.read_text()
        self.assertIn("package com.hazrob.tank.chemcore.v6;",s)
        self.assertNotIn("com.servoc.",s)
        self.assertNotIn("ChemCoreInferenceEngine",s)
        self.assertNotIn("TankInstantAnalyzer",s)
        self.assertNotIn("TankStableRuntimeCatalog",s)
        self.assertIn("SourceCatalog.curves()",s)
        self.assertIn("rankedCandidates",s)

    def test_03_only_invalid_arithmetic_is_excluded(self):
        s=ENGINE.read_text()
        self.assertNotIn("MIN_MATCH",s)
        self.assertNotIn("MIN_SENSOR_BREADTH",s)
        self.assertNotIn("OPEN_SET_THRESHOLD",s)
        self.assertIn("Double.isFinite",s)
        self.assertIn("allSourceFits",s)

if __name__=="__main__":
    unittest.main()
