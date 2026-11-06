#!/usr/bin/env python3
"""Portable V9 R3 integration contracts on exact committed Android Java sources.

SQLite checks execute the ACTUAL Java-defined V15/V16 DDL (no copied schema).
No claim of emulator/device, live Tank, manufacturer R0 or physical G3.
"""
import pathlib,re,sqlite3,unittest

ROOT=pathlib.Path(__file__).resolve().parents[3]
JAVA=ROOT/"android/app/src/main/java"
def read(path):
    return (JAVA/path).read_text(encoding="utf-8")
def java_sql(source,constant):
    match=re.search(r"public\\s+static\\s+final\\s+String\\s+"+
                    re.escape(constant)+r"\\s*=",source)
    if not match:
        raise AssertionError("missing original Java DDL "+constant)
    # The trigger SQL string contains semicolons; find the terminating Java
    # semicolon OUTSIDE quoted source strings instead of using a lazy regex.
    quoted=False
    escaped=False
    end=None
    tail=source[match.end():]
    for i,ch in enumerate(tail):
        if escaped:
            escaped=False
        elif ch=="\\\\" and quoted:
            escaped=True
        elif ch=='"':
            quoted=not quoted
        elif ch==";" and not quoted:
            end=i
            break
    if end is None or quoted:
        raise AssertionError("unterminated original SQL literal "+constant)
    parts=re.findall(r'"((?:[^"\\\\]|\\\\.)*)"',tail[:end])
    if not parts:
        raise AssertionError("no SQL string parts "+constant)
    return "".join(part.replace('\\\\"','"').replace('\\\\\\\\','\\\\')
        for part in parts)

class V9R3Integration(unittest.TestCase):
    def test_original_sqlite_v15_to_v16_preserves_immutable_decisions_and_owner_data(self):
        parent=read(pathlib.Path("com/servoc/app/chemcore/nextintegration/NextDecisionSchema.java"))
        child=read(pathlib.Path("com/servoc/app/chemcore/nextintegration/NextTraceSchema.java"))
        con=sqlite3.connect(":memory:")
        con.execute("PRAGMA foreign_keys=ON")
        con.execute("CREATE TABLE owner_sessions(id TEXT PRIMARY KEY, r_air TEXT NOT NULL)")
        con.execute("INSERT INTO owner_sessions VALUES('SAVED_SESSION','owner-R_air-stable')")
        for name in ("CREATE_TABLE","CREATE_INDEX","IMMUTABLE_UPDATE","IMMUTABLE_DELETE"):
            con.execute(java_sql(parent,name))
        uid="NEXT-"+"c"*64
        hash="b"*64
        con.execute("INSERT INTO next_research_decisions(decision_uid,session_id,snapshot_uid,"
            "snapshot_sha256,stable_sha256,engine_version,decision_status,"
            "snapshot_replay,result_replay,created_utc_ms,candidate_count)"
            " VALUES(?,?,?,?,?,?,?,?,?,?,?)",
            (uid,"SAVED_SESSION","SAVED_CAP",hash,"a"*64,"R3_ENGINE","UNKNOWN",
             "immutable_saved_source","immutable_saved_decision",110,0))
        con.commit()
        for name in ("CREATE_TABLE","CREATE_INDEX","IMMUTABLE_UPDATE","IMMUTABLE_DELETE"):
            con.execute(java_sql(child,name))
        con.execute("INSERT INTO next_research_trace_v16(decision_uid,snapshot_sha256,"
            "engine_version,trace_schema,trace_sha256,trace_json,event_count,created_utc_ms)"
            " VALUES(?,?,?,?,?,?,?,?)",
            (uid,hash,"R3_ENGINE","r3trace","d"*64,"[{\"original\":true}]",2,111))
        con.commit()
        self.assertEqual(con.execute("SELECT r_air FROM owner_sessions").fetchone()[0],
                         "owner-R_air-stable")
        self.assertEqual(con.execute(
            "SELECT snapshot_replay,result_replay FROM next_research_decisions"
        ).fetchone(),("immutable_saved_source","immutable_saved_decision"))
        self.assertEqual(con.execute("SELECT event_count FROM next_research_trace_v16"
            ).fetchone()[0],2)
        for sql in ("UPDATE next_research_trace_v16 SET event_count=0",
                    "DELETE FROM next_research_trace_v16",
                    "UPDATE next_research_decisions SET decision_status='INVALID_INPUT'",
                    "DELETE FROM next_research_decisions"):
            with self.assertRaises(sqlite3.DatabaseError):
                con.execute(sql)
        self.assertEqual(con.execute("SELECT COUNT(*) FROM next_research_trace_v16"
            ).fetchone()[0],1)
        con.close()

    def test_java_migration_is_additive_and_original_engine_integrity_is_active(self):
        db=read(pathlib.Path("com/servoc/app/data/ExperimentsDb.java"))
        self.assertIn("DB_VERSION=16",db)
        self.assertIn("if(oldVersion<16)createV16NextResearchTraceTables(db);",db)
        self.assertIn("createV15NextResearchDecisionTables(db);createV16NextResearchTraceTables(db);",db)
        self.assertNotRegex(db,r"(?i)DROP TABLE\s+next_research_decisions")
        router=read(pathlib.Path("com/servoc/app/chemcore/nextintegration/NextEngineRouter.java"))
        self.assertIn("ChemCoreNext.analyze(research)",router)
        self.assertIn("appendWithTrace(",router)
        self.assertIn("result.executionTrace",router)
        store=read(pathlib.Path("com/servoc/app/chemcore/nextintegration/NextDecisionStore.java"))
        self.assertIn("w.beginTransaction();",store)
        self.assertIn("NextResearchTraceStore.append(w,",store)
        self.assertIn("w.setTransactionSuccessful();",store)
        self.assertIn("readBack(w,uid)",store)
        self.assertIn("NEXT_R3_ATOMIC_TRACE_READBACK_FAILED",store)
        trace=read(pathlib.Path("com/servoc/app/chemcore/nextintegration/NextResearchTraceStore.java"))
        self.assertIn('MessageDigest.getInstance("SHA-256")',trace)
        self.assertIn("NEXT_R3_TRACE_SHA256_MISMATCH",trace)
        self.assertIn("NEXT_R3_TRACE_PROVENANCE_MISMATCH",trace)
        self.assertIn("List.copyOf(events)",trace)

    def test_every_output_uses_same_original_archived_trace(self):
        envelope=read(pathlib.Path("com/servoc/app/chemcore/nextintegration/NextDecisionEnvelope.java"))
        self.assertIn('out.put("next_execution_trace",NextResearchTraceStore.asJson(receipt.executionTrace));',envelope)
        self.assertIn('out.put("trace_sha256"',envelope)
        self.assertIn("source technique traces changed across views",envelope)
        ui=read(pathlib.Path("com/servoc/app/ui/MeasurementsTab.java"))
        self.assertIn('JSONArray execution=p.optJSONArray("next_execution_trace")',ui)
        pdf=read(pathlib.Path("com/servoc/app/report/TankInstantV7PdfWriter.java"))
        self.assertIn('JSONArray trace=next.optJSONArray("next_execution_trace");',pdf)
        self.assertIn("int total=1+candidatePages+curvePages+riskPages+ghsPages+tracePages",pdf)
        field=read(pathlib.Path("com/servoc/app/report/SmartFieldSessionPdfWriter.java"))
        self.assertIn("V9NextR3TracePdfAnnex.pages(next)",field)
        self.assertIn("V9NextR3TracePdfAnnex.draw(",field)
        site=read(pathlib.Path("com/servoc/app/report/SiteSummaryReportExporter.java"))
        self.assertIn("V9NextR3TracePdfAnnex.pages(session.next)",site)
        self.assertIn("V9NextR3TracePdfAnnex.draw(",site)
        fieldxlsx=read(pathlib.Path("com/servoc/app/report/V8NextFieldXlsxWriter.java"))
        self.assertIn('new SmartXlsxPackage.Sheet("تتبع تقنيات R3"',fieldxlsx)
        self.assertIn("for(com.hazrob.tank.chemcore.next.NextAnalysisTrace.Event e:next.executionTrace)",fieldxlsx)
        sitexlsx=read(pathlib.Path("com/servoc/app/report/V8NextSiteXlsxWriter.java"))
        self.assertIn('new SmartXlsxPackage.Sheet("تتبع R3 للموقع"',sitexlsx)
        self.assertIn("for(var e:receipt.executionTrace)",sitexlsx)

    def test_versions_and_qualification_never_faked(self):
        release=read(pathlib.Path("com/servoc/app/ReleaseAudit.java"))
        self.assertIn('VERSION="9.1"',release)
        self.assertIn("DATABASE_SCHEMA=16",release)
        self.assertIn("NEXT_R3_TANK_G3_VALIDATED=false",release)
        self.assertIn("GAS_RESPONSE_AUDIBLE_ALERT=false",release)
        core=read(pathlib.Path("com/hazrob/tank/chemcore/next/ChemCoreNext.java"))
        decision=read(pathlib.Path("com/hazrob/tank/chemcore/next/NextResearchDecisionPipeline.java"))
        self.assertIn("SNAPSHOT_HASH_MISMATCH",core)
        self.assertIn("SourceRootCoherence.evaluate(sourceCurveSamples)",core)
        self.assertIn("VERSION=ChemCoreNext.VERSION;",decision)
        self.assertIn("ppmAuthorized=false;identityConfirmed=false;g3Qualified=false;",decision)
        self.assertIn("fieldSafetyAlarmEnabled=false;physicalG3Qualified=false;",decision)

if __name__=="__main__":
    unittest.main()
