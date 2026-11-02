"""Re-derive frozen beef Gaussian source model from TS1-TS4 CSV, no device data.
Confirms Java model numerical parameters are source-traceable, not fabricated.
"""
import math,re,statistics,unittest
from pathlib import Path
from test_beef_grouped_alternative_models import read_cut,LABELS
ROOT=Path(__file__).resolve().parents[2]
JAVA=ROOT/"android/app/src/main/java/com/hazrob/tank/foodfreshness/meat/BeefGaussianResearchEngine.java"
def extract(name):
    source=JAVA.read_text(encoding="utf-8")
    p=re.search(r"private static final double\[\]\[\] "+name+r"=\{(.*?)\};",source,re.S)
    if p is None:raise AssertionError(name+" missing")
    return [[float(y.strip()) for y in block.split(",")]
            for block in re.findall(r"\{([^{}]+)\}",p.group(1))]
class BeefGaussianSourceParity(unittest.TestCase):
    def test_all_four_means_and_variances_fitted_exclusively_on_ts1_to_ts4(self):
        train=[sample for i in range(1,5) for sample in read_cut(i)]
        self.assertEqual(len(train),8640)
        means=extract("MEANS");variances=extract("VARIANCES")
        self.assertEqual((len(means),len(variances)),(4,4))
        expected_count=[1320,2280,1680,3360]
        for ci,label in enumerate(LABELS):
            rows=[x for category,x in train if category==label]
            self.assertEqual(len(rows),expected_count[ci])
            for j,col in enumerate(zip(*rows)):
                mean=statistics.mean(col)
                var=statistics.mean((v-mean)**2 for v in col)
                self.assertAlmostEqual(means[ci][j],mean,places=12)
                self.assertAlmostEqual(variances[ci][j],var,places=12)
        self.assertIn("SOURCE_DIAGNOSTIC_ONLY",JAVA.read_text())
        self.assertIn("TANK_VALIDATED=false",JAVA.read_text())
if __name__=="__main__":unittest.main()
