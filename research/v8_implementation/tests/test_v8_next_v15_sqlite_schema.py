#!/usr/bin/env python3
"""Use SQLite3 engine on the actual Java V15 DDL, without faking Android instrumentation."""
import re
import sqlite3
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[3]
SCHEMA=ROOT/"android/app/src/main/java/com/servoc/app/chemcore/nextintegration/NextDecisionSchema.java"
DB=ROOT/"android/app/src/main/java/com/servoc/app/data/ExperimentsDb.java"
EXPECTED=["CREATE_TABLE","CREATE_INDEX","IMMUTABLE_UPDATE","IMMUTABLE_DELETE"]
def statements():
    text=SCHEMA.read_text()
    return {name:re.search(r"public static final String "+name+r'="([^"]+)";',text).group(1)
            for name in EXPECTED}
SHA="a"*64
STABLE="f"*64
REPLAY="HCNEXT-REPLAY/1:"+"a"*64+":dGVzdA"
def sample(uid="NEXT-UID-1",session="SESSION-1",status="UNKNOWN",ppm=0,g3=0):
    return (uid,session,"CAPTURE-1",SHA,STABLE,"chemcore-next-v8-research/1",status,
            REPLAY,REPLAY,100000,0,1,ppm,g3)
INSERT=("INSERT INTO next_research_decisions(decision_uid,session_id,snapshot_uid,"
        "snapshot_sha256,stable_sha256,engine_version,decision_status,"
        "snapshot_replay,result_replay,created_utc_ms,candidate_count,"
        "research_only,ppm_reportable,g3_physical) VALUES("+",".join(["?"]*14)+")")

class V15Schema(unittest.TestCase):
    def setUp(self):
        self.db=sqlite3.connect(":memory:")
        self.db.execute("PRAGMA foreign_keys=ON")
        self.db.execute("CREATE TABLE experiments(id INTEGER PRIMARY KEY,payload TEXT NOT NULL)")
        self.db.execute("CREATE TABLE measurement_sessions(session_id TEXT PRIMARY KEY)")
        self.db.execute("CREATE TABLE field_measurement_cycles(cycle_id TEXT PRIMARY KEY)")
        self.db.execute("INSERT INTO experiments VALUES(1,'legacy-v7-preserved')")
        self.db.execute("INSERT INTO measurement_sessions VALUES('old-session')")
        self.db.execute("INSERT INTO field_measurement_cycles VALUES('old-cycle')")
        for stmt in statements().values():self.db.execute(stmt)
        self.db.commit()  # Persist V14 seed and V15 DDL before testing transactional rollback.
    def tearDown(self):self.db.close()
    def test_v14_tables_and_records_preserved(self):
        self.assertEqual(self.db.execute("SELECT payload FROM experiments").fetchone()[0],"legacy-v7-preserved")
        self.assertEqual(self.db.execute("SELECT session_id FROM measurement_sessions").fetchone()[0],"old-session")
        self.assertEqual(self.db.execute("SELECT cycle_id FROM field_measurement_cycles").fetchone()[0],"old-cycle")
    def test_additive_repeat_idempotent_schema(self):
        for stmt in statements().values():self.db.execute(stmt)
        self.db.execute(INSERT,sample())
        self.assertEqual(self.db.execute("SELECT count(*) FROM next_research_decisions").fetchone()[0],1)
    def test_unique_immutable_decision_per_capture(self):
        self.db.execute(INSERT,sample())
        with self.assertRaises(sqlite3.IntegrityError):self.db.execute(INSERT,sample())
        with self.assertRaises(sqlite3.IntegrityError):
            self.db.execute(INSERT,sample(uid="NEXT-UID-2"))
    def test_reject_reportable_ppm(self):
        with self.assertRaises(sqlite3.IntegrityError):self.db.execute(INSERT,sample(ppm=1))
    def test_reject_fake_g3(self):
        with self.assertRaises(sqlite3.IntegrityError):self.db.execute(INSERT,sample(g3=1))
    def test_reject_invalid_state_and_sha(self):
        with self.assertRaises(sqlite3.IntegrityError):self.db.execute(INSERT,sample(status="CONFIRMED_GAS"))
        wrong=list(sample());wrong[3]="not-a-sha"
        with self.assertRaises(sqlite3.IntegrityError):self.db.execute(INSERT,wrong)
    def test_reject_altered_original_and_deletion(self):
        self.db.execute(INSERT,sample())
        with self.assertRaisesRegex(sqlite3.IntegrityError,"IMMUTABLE"):
            self.db.execute("UPDATE next_research_decisions SET decision_status='RESEARCH_CANDIDATES'")
        with self.assertRaisesRegex(sqlite3.IntegrityError,"IMMUTABLE"):
            self.db.execute("DELETE FROM next_research_decisions")
        self.assertEqual(self.db.execute("SELECT decision_status FROM next_research_decisions").fetchone()[0],"UNKNOWN")
    def test_transaction_rollback_preserves_no_partial_row(self):
        try:
            with self.db:
                self.db.execute(INSERT,sample())
                self.db.execute(INSERT,sample(uid="duplicate",ppm=1))
        except sqlite3.IntegrityError:pass
        self.assertEqual(self.db.execute("SELECT count(*) FROM next_research_decisions").fetchone()[0],0)
    def test_java_upgrade_calls_v15(self):
        text=DB.read_text()
        self.assertIn("DB_VERSION=15",text)
        self.assertIn("if(oldVersion<15)",text)
        self.assertIn("createV15NextResearchDecisionTables(db);",text)
        self.assertIn("NextDecisionSchema.ensure(db)",text)

if __name__=="__main__":
    unittest.main(verbosity=2)
