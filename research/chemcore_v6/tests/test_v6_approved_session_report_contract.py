#!/usr/bin/env python3
"""N06-B evidence contract checks, plus SQL fixture for legacy immutable data.

These are NOT Android emulator or JVM UI/PDF rendering tests. They confirm
source invariants and that the read-only SELECT can consume V5 archived
mq_payload/env_payload rows without altering source DB tables.
"""
import json
import pathlib
import sqlite3
import unittest

BASE=pathlib.Path("android/app/src/main/java/com/servoc/app")
ADAPTER=BASE/"report/V6FieldSessionResearchAppendix.java"
PDF=BASE/"report/SmartFieldSessionPdfWriter.java"
XLSX=BASE/"report/SmartFieldSessionXlsxWriter.java"
FACADE=BASE/"report/FieldSessionReportExporter.java"
POLICY=BASE/"field/FieldCycleRepository.java"
LEGACY=BASE/"chemcore/CompletedSessionChemCoreAnalyzer.java"

class N06BApprovedReportResearch(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.adapter=ADAPTER.read_text(encoding="utf8")
        cls.pdf=PDF.read_text(encoding="utf8")
        cls.xlsx=XLSX.read_text(encoding="utf8")
        cls.facade=FACADE.read_text(encoding="utf8")
        cls.policy=POLICY.read_text(encoding="utf8")
        cls.legacy=LEGACY.read_text(encoding="utf8")

    def test_approval_sha_guard_before_any_new_v6_report(self):
        self.assertIn("verifyApprovedEvidence(cycle)",self.facade)
        self.assertIn("Approved cycle evidence or ChemCore result changed",self.facade)
        self.assertIn("FieldSessionReportExporter.resolve(db,fieldSessionId)",self.pdf)
        self.assertIn("analyzeVerified(db,b)",self.pdf)
        self.assertLess(self.pdf.index("FieldSessionReportExporter.resolve(db,fieldSessionId)"),
                        self.pdf.index("analyzeVerified(db,b)"))
        self.assertIn("Bundle b=resolve(db,fieldSessionId)",self.facade)
        self.assertIn("analyzeVerified(db,b)",self.xlsx)

    def test_read_only_no_schema_migration_and_no_legacy_fallback(self):
        self.assertIn("db.getReadableDatabase().rawQuery(",self.adapter)
        for unsafe in ("getWritableDatabase(", "execSQL(", ".insert(", "w.update(",
                       ".delete(", "ChemCoreInferenceEngine", "TankStableRuntimeCatalog",
                       "CompletedSessionChemCoreAnalyzer", "FoodFreshness"):
            self.assertNotIn(unsafe,self.adapter)
        self.assertIn('root.put("no_db_write",true)',self.adapter)
        self.assertIn('root.put("legacy_fallback_for_v6",false)',self.adapter)
        self.assertIn('root.put("reported_identity","UNKNOWN")',self.adapter)
        self.assertIn('root.put("reportable_ppm",false)',self.adapter)

    def test_unique_firmware_scans_and_original_r0_preserved(self):
        self.assertIn('ORDER BY t_ms ASC,id ASC',self.adapter)
        self.assertIn('q.has("scan_cycle")',self.adapter)
        self.assertIn('a.distinct.putIfAbsent(unique,new Sample(t,unique,q,env))',self.adapter)
        self.assertIn('a.distinct.size()<10',self.adapter)
        self.assertIn('Sample original=uniqueSamples.get(uniqueSamples.size()/2)',self.adapter)
        self.assertIn('new JSONObject(original.observed.toString())',self.adapter)
        self.assertIn('TankV6TelemetryAdapter.analyzeSnapshot(one)',self.adapter)
        self.assertIn('TankV6TelemetryAdapter.toJson(result)',self.adapter)
        self.assertNotIn('observedRatiosAcrossFieldSamples(',self.adapter)
        self.assertIn('selected_original_r0',self.adapter)
        self.assertIn('research_source_sha256',self.adapter)

    def test_approved_only_no_superseded_deleted(self):
        self.assertIn('if(!cycle.approved()||!cycle.modes.gas)continue;',self.adapter)
        self.assertIn('approved.measurementSessionIds.contains(sid)',self.adapter)
        self.assertIn('!approved.fieldSession.approved()',self.adapter)
        self.assertIn('root.put("v5_approval_and_decisions_unchanged",true)',self.adapter)

    def test_pdf_is_additive_and_v5_pages_not_replaced(self):
        self.assertIn('int total=basePages+cyclePages+v6Pages',self.pdf)
        self.assertIn('v6ResearchAppendixPage(p.getCanvas(),b,v6ReadOnly,chunk)',self.pdf)
        self.assertIn('executiveOverview(p.getCanvas(),b,all,primary,plan,hazard,cs)',self.pdf)
        self.assertIn('cycleLedgerPage(p.getCanvas(),b,chunk)',self.pdf)
        self.assertIn('READ ONLY • G3=0 • PPM NOT REPORTABLE',self.pdf)

    def test_xlsx_is_additive_separate_sheet(self):
        self.assertIn('new SmartXlsxPackage.Sheet("V6 بحثي غازات"',self.xlsx)
        self.assertIn('V6FieldSessionResearchAppendix.analyzeVerified(db,b)',self.xlsx)
        self.assertIn('s.optInt("distinct_original_scan_cycles",0)',self.xlsx)
        self.assertIn('cycle.optString("research_source_sha256","")',self.xlsx)
        self.assertIn('SmartXlsxPackage.Sheet("عقد التحليل"',self.xlsx)
        self.assertIn('SmartXlsxPackage.Sheet("دورات القياس"',self.xlsx)

    def test_archived_sql_fixture_independent_scan_cycle(self):
        db=sqlite3.connect(":memory:")
        db.execute("CREATE TABLE telemetry_samples(id INTEGER PRIMARY KEY,"
                   "session_id TEXT NOT NULL,t_ms INTEGER NOT NULL,"
                   "mq_payload TEXT,env_payload TEXT)")
        original=[]
        for scan in range(15):
            q={"mq4":{"ok":True,"powered":True,"scan_cycle":scan,
                      "rs":85.0+scan,"r0":None if scan%3==0 else 92.0}}
            env={"temperature":24.0+scan/10}
            for duplicate in range(3):
                original.append(("MS-1",1000+scan*100+duplicate,
                                 json.dumps(q),json.dumps(env)))
        # Non-source/malformed values must remain archived but NOT enter V6.
        original.append(("MS-1",9000,json.dumps({"mq4":{"ok":False,
                          "powered":True,"scan_cycle":18,"rs":100.0}}),"{}"))
        original.append(("MS-1",9100,'{bad JSON',"{}"))
        original.append(("MS-2",1000,json.dumps({"mq4":{"ok":True,
                          "powered":True,"scan_cycle":2,"rs":90.0}}),"{}"))
        db.executemany("INSERT INTO telemetry_samples(session_id,t_ms,mq_payload,env_payload)"
                       "VALUES(?,?,?,?)",original)
        db.commit()
        rows=db.execute("SELECT t_ms,mq_payload,env_payload FROM telemetry_samples "
                        "WHERE session_id=? ORDER BY t_ms ASC,id ASC",("MS-1",)).fetchall()
        self.assertEqual(len(rows),47)
        chosen={}
        for t,raw,env in rows:
            try:
                q=json.loads(raw).get("mq4")
                if not q or not q.get("ok") or not q.get("powered"):continue
                scan=q.get("scan_cycle")
                if not isinstance(scan,int) or scan<0:continue
                if q.get("rs",0)<=0:continue
                chosen.setdefault(scan,(t,q))
            except ValueError:
                pass
        self.assertEqual(len(chosen),15)
        middle=list(chosen.values())[len(chosen)//2]
        self.assertEqual(middle[1]["scan_cycle"],7)
        self.assertEqual(middle[1]["rs"],92.0)
        self.assertEqual(middle[1]["r0"],92.0)
        after=db.execute("SELECT t_ms,mq_payload,env_payload FROM telemetry_samples "
                         "WHERE session_id=? ORDER BY t_ms ASC,id ASC",("MS-1",)).fetchall()
        self.assertEqual(rows,after)
        self.assertEqual(db.execute("SELECT COUNT(*) FROM telemetry_samples").fetchone()[0],48)
        db.close()

if __name__=="__main__":
    unittest.main()
