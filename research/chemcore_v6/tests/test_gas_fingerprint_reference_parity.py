"""Pin every numeric V6 gas fingerprint prototype to original research JSON.

Food data is a separate application feature and MUST NOT be imported here.
"""
import json
import math
import re
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
ASSETS=ROOT/"android/app/src/main/assets/chemcore_v6/reference"
JAVA=ROOT/"android/app/src/main/java/com/hazrob/tank/chemcore/v6/GasFingerprintCatalogV6.java"
MQ=ASSETS/"multimodal_mq_transfer_fingerprints_v1.json"
ROBO=ASSETS/"robofusion_hr069_gasleak_operational_transfer_v1.json"
UCI=ASSETS/"uci_mq135_lowconcentration_transfer_fingerprints_v1.json"

PATTERN=re.compile(
    r'new Signature\("([^"]+)", "([^"]+)", "([^"]+)", "([^"]+)", '
    r'(-?\d+)L, (Double.NaN|[+\-\d.eE]+), (\d+), '
    r'new double\[\]\{([^}]+)\}, new double\[\]\{([^}]+)\}\),?'
)

def array(x):
    return [float("nan") if part.strip()=="Double.NaN" else float(part.strip())
            for part in x.split(",")]

def expected():
    a=json.loads(MQ.read_text())
    b=json.loads(ROBO.read_text())
    c=json.loads(UCI.read_text())
    result=[]
    for i,r in enumerate(a["records"],1):
        result.append((f"MQDS-ADC-{i}","MQDS_6400_SOURCE_ADC",
            "ADC_LOG_GEOMEAN_CLASS",r["class"],-1,float("nan"),r["sample_count"],
            r["log10_cross_sensor_geomean_shape_hr069_order"],
            r["log10_class_mean_over_nogas_mean_hr069_order"]))
    for i,r in enumerate(b["records"],1):
        result.append((f"ROBOFUSION-{i}","ROBOFUSION_H1_OPS",
            "OPERATIONAL_LOG1PLUS_CLR",r["class"],-1,float("nan"),r["rows_used"],
            r["prototype_clr_log10_1plus"],r["sigma_robust_clr_log10_1plus"]))
    for i,r in enumerate(c["records"],1):
        fs=r["features"]
        keys=("endpoint_over_baseline","endpoint_delta_fraction",
              "endpoint_slope_fraction_per_s")
        result.append((f"UCI1081-{i}","UCI1081_MQ135_TEMPORAL",
            "MQ135_BASELINE_NORMALIZED_TEMPORAL",r["gas"],r["legacy_id"],
            r["concentration_ppm"],r["replicate_count"],
            [fs[k]["mean"] for k in keys],[fs[k]["sd"] for k in keys]))
    return result

class FingerprintSourceParity(unittest.TestCase):
    def test_original_json_has_expected_three_separate_vector_spaces(self):
        srcs=expected()
        self.assertEqual(len(srcs),24)
        self.assertEqual(sum(x[2]=="ADC_LOG_GEOMEAN_CLASS" for x in srcs),4)
        self.assertEqual(sum(x[2]=="OPERATIONAL_LOG1PLUS_CLR" for x in srcs),2)
        self.assertEqual(sum(x[2]=="MQ135_BASELINE_NORMALIZED_TEMPORAL" for x in srcs),18)

    def test_all_java_prototype_coefficients_match_original_json(self):
        lines=[x.strip() for x in JAVA.read_text().splitlines() if 'new Signature("' in x]
        actual=[]
        for line in lines:
            m=PATTERN.fullmatch(line)
            self.assertIsNotNone(m, "unrecognized Java reference row: "+line[:90])
            groups=m.groups()
            actual.append((groups[0],groups[1],groups[2],groups[3],int(groups[4]),
                           float("nan") if groups[5]=="Double.NaN" else float(groups[5]),
                           int(groups[6]),array(groups[7]),array(groups[8])))
        self.assertEqual(len(actual),24)
        for idx,(got,want) in enumerate(zip(actual,expected()),1):
            with self.subTest(record=idx):
                self.assertEqual(got[:5],want[:5])
                self.assertEqual(got[6],want[6])
                self.assertTrue((math.isnan(got[5]) and math.isnan(want[5])) or
                                 got[5]==want[5])
                for fvalues,wvalues in zip(got[7:],want[7:]):
                    self.assertEqual(len(fvalues),len(wvalues))
                    for p,q in zip(fvalues,wvalues):
                        self.assertTrue((math.isnan(p) and q is None) or p==q,
                                        f"reference source mismatch at {idx}: {p} vs {q}")

    def test_adc_missing_mq4_and_mq9_channels_not_synthesized(self):
        for r in json.loads(MQ.read_text())["records"]:
            vec=r["log10_cross_sensor_geomean_shape_hr069_order"]
            self.assertIsNone(vec[2])
            self.assertIsNone(vec[6])
            self.assertEqual(len(vec),8)

    def test_uci_unit_is_source_ppb_not_tank_certified_ppm(self):
        src=json.loads(UCI.read_text())
        self.assertEqual(src["concentrations_ppm"],[0.05,0.1,0.2])
        self.assertFalse(src["quantitative_ppm"])
        self.assertFalse(src["m4"])
        self.assertEqual(len(src["records"]),18)

    def test_no_food_references_or_prior_engine_imports(self):
        sources=[p.read_text() for p in
            [JAVA,ROOT/"android/app/src/main/java/com/hazrob/tank/chemcore/v6/FingerprintEngineV6.java"]]
        for text in sources:
            self.assertNotIn("foodfreshness",text.lower())
            self.assertNotIn("com.servoc.app.chemcore",text)
            self.assertNotIn("import android.",text)

if __name__=="__main__":
    unittest.main()
