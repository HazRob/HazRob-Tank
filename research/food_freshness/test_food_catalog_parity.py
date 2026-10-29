"""Verify no source transformation/accidental mixing of food into gas ChemCore."""
import json
import math
import re
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
SOURCE=ROOT/"research/food_freshness/source_snapshots/food_freshness_headspace_fingerprints_v1.json"
JAVA=ROOT/"android/app/src/main/java/com/hazrob/tank/foodfreshness/FoodReferenceCatalog.java"
ENGINE=ROOT/"android/app/src/main/java/com/hazrob/tank/foodfreshness/FoodFreshnessEngine.java"
GAS=ROOT/"android/app/src/main/java/com/hazrob/tank/chemcore/v6"

class FoodCatalogParity(unittest.TestCase):
    def test_source_reference_59_patterns_are_unmodified_in_independent_module(self):
        payload=json.loads(SOURCE.read_text())
        source=JAVA.read_text()
        matches=re.findall(r'new Pattern\("([^"]+)",new double\[\]\{([^}]+)\}\)',source)
        self.assertEqual(len(matches),len(payload["sessions"]))
        self.assertEqual(len(matches),59)
        for (name,raw),(expected_label,expected_vector) in zip(matches,payload["sessions"]):
            with self.subTest(food=expected_label):
                self.assertEqual(name,expected_label)
                self.assertEqual(len(expected_vector),8)
                self.assertEqual([float(x.strip()) for x in raw.split(",")],expected_vector)
    def test_scientific_separation_in_source_and_analysis_engine(self):
        for name in (JAVA,ENGINE):
            text=name.read_text()
            self.assertIn("package com.hazrob.tank.foodfreshness;",text)
            self.assertNotIn("import com.hazrob.tank.chemcore",text)
            self.assertNotIn("import com.servoc.app.chemcore",text)
        for p in GAS.glob("*.java"):
            text=p.read_text().lower()
            self.assertNotIn("foodfreshness",text)
            self.assertNotIn("food_freshness",text)
    def test_reference_labels_do_not_claim_calibrated_edibility(self):
        self.assertIn("foodSafetyCertification=false",ENGINE.read_text())
        self.assertIn("microbiologyMeasured=false",ENGINE.read_text())
        self.assertIn("gasIdentity=false",ENGINE.read_text())
        j=json.loads(SOURCE.read_text())
        self.assertIs(j["quantitative_ppm"],False)
        self.assertIs(j["pure_chemical_identity"],False)

if __name__=="__main__":
    unittest.main()
