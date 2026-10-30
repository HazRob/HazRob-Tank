"""NIST SRD69 source qualified identity audit. NOT a gas concentration calibration."""
from pathlib import Path
import re, unittest
ROOT=Path(__file__).resolve().parents[3]
S=(ROOT/"android/app/src/main/java/com/servoc/app/chemical/ChemicalNistIdentityCompletionV6.java").read_text()
B=(ROOT/"android/app/src/main/java/com/servoc/app/chemical/ChemicalSeedBootstrap.java").read_text()
FIX=(ROOT/"research/chemcore_v6/tests/android/V6ScientificRiskDbFixtureActivity.java").read_text()
R=(ROOT/"research/chemcore_v6/tests/android/run_n09_scientific_risk_db.sh").read_text()
def checksum(cas):
    s=cas.replace('-','')
    return sum(i*int(d) for i,d in enumerate(reversed(s[:-1]),1))%10==int(s[-1])
class NistMolecularIdentity(unittest.TestCase):
    def test_27_individual_cas_masses_are_source_backed(self):
        rows=re.findall(r'\{"(\d+)","([^"]+)","([^"]+)","([\d-]+)","([\d.]+)"\}',S)
        self.assertEqual(len(rows),27)
        self.assertEqual(len({int(x[0]) for x in rows}),27)
        self.assertEqual(len({x[3] for x in rows}),27)
        self.assertTrue(all(checksum(cas) for _,_,_,cas,_ in rows))
        self.assertTrue(all(float(mw)>0 for *_,mw in rows))
        self.assertEqual(next(x for x in rows if x[1]=='Hydrogen')[3],'1333-74-0')
        self.assertEqual(next(x for x in rows if x[1]=='R1234yf')[3],'754-12-1')
        self.assertEqual(next(x for x in rows if x[1]=='R1234ze(E)')[3],'29118-24-9')
    def test_only_missing_cas_mass_provenance_preserved(self):
        self.assertIn("ChemicalIdentitySchema.ensure(db)",S)
        self.assertIn("source_id\",\"NIST_WEBBOOK",S)
        self.assertIn("https://webbook.nist.gov/cgi/cbook.cgi?ID=",S)
        self.assertIn('if(oldCas.isEmpty())',S)
        self.assertIn('if(c.isNull(3)||c.getDouble(3)<=0)',S)
        self.assertIn("WHERE cas=? AND uid<>?",S)
        self.assertIn("Aniline (and homologs)",S)
        self.assertIn('"53".equals(uid)',S)
        self.assertIn('other.getLong(0)==486L',S)
        self.assertIn("db.beginTransaction()",S)
        self.assertIn("db.setTransactionSuccessful()",S)
        self.assertNotIn('v.put("r0"',S)
        self.assertNotIn('INSERT INTO calibration_curves',S)
        self.assertIn("ChemicalNistIdentityCompletionV6.ensure(db)",B)
        self.assertIn('nistAudit.proposed==27&&nistAudit.matched==27',FIX)
        self.assertIn("materials_with_molar_mass']==638",R)
        self.assertIn("individual_materials_without_linked_cas']==0",R)
if __name__=="__main__":unittest.main()
