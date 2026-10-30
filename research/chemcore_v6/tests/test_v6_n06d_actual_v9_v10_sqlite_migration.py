#!/usr/bin/env python3
"""N06-D actual Java-defined SQLite V9 -> V10 migration source replay.

The SQL is extracted verbatim from ExperimentsDb.java, not retyped/faked.
This uses Python SQLite, not Android SQLiteOpenHelper or a real user DB.
No migrations to V11 or V6 report changes. G3 and original evidence untouched.
"""
import csv
import hashlib
import json
from pathlib import Path
import re
import sqlite3
import sys
import tempfile
import unittest

SOURCE = Path("android/app/src/main/java/com/servoc/app/data/ExperimentsDb.java")
V6_APPENDIX = Path("android/app/src/main/java/com/servoc/app/report/V6FieldSessionResearchAppendix.java")
REPORT = Path("android/app/src/main/java/com/servoc/app/report/FieldSessionReportExporter.java")
REPORTS = []

def sql_from_java_method(text: str, name: str) -> list[str]:
    pattern = r"\b(?:private|public)\s+static\s+void\s+" + re.escape(name) + r"\("
    match = re.search(pattern, text)
    if not match:
        raise AssertionError(f"Missing canonical ExperimentsDb.java method {name}")
    start = text.index("{", match.end())
    depth, end = 0, None
    for i in range(start, len(text)):
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                end = i
                break
    if end is None:
        raise AssertionError("Unbalanced canonical Java method braces")
    body = text[start+1:end]
    result = []
    # This method contains only literal SQL fragments concatenated with +.
    for match in re.finditer(r"db\.execSQL\((.*?)\);", body, flags=re.S):
        inner = match.group(1)
        fragments = re.findall(r'"(?:\\.|[^"\\])*"', inner)
        if not fragments:
            raise AssertionError(f"Missing SQL literal in {name}")
        command = "".join(json.loads(t) for t in fragments)
        if not (command.startswith("CREATE TABLE") or command.startswith("CREATE INDEX")
                or command.startswith("INSERT OR IGNORE")):
            raise AssertionError(f"Unexpected dynamic SQL in {name}: {command[:80]}")
        result.append(command)
    if not result:
        raise AssertionError(f"No original SQL extracted from {name}")
    return result

def source_sql():
    original = SOURCE.read_text(encoding="utf8")
    v6 = sql_from_java_method(original, "createV6Tables")
    table = lambda name: next(
        x for x in v6 if x.startswith("CREATE TABLE IF NOT EXISTS " + name + "("))
    return (table("measurement_sessions"), table("telemetry_samples"),
            sql_from_java_method(original, "createV9UserFieldTables"),
            sql_from_java_method(original, "createV10FieldCycleTables"),
            sql_from_java_method(original, "importV9FieldCycleLinks"),
            hashlib.sha256(original.encode()).hexdigest())

def digest_rows(connection, table):
    rows = connection.execute(f'SELECT * FROM "{table}" ORDER BY 1').fetchall()
    data = json.dumps(rows, separators=(',', ':'), ensure_ascii=False)
    return hashlib.sha256(data.encode("utf8")).hexdigest()

def fixture():
    measurement, telemetry, v9, v10, backfill, java_sha = source_sql()
    conn = sqlite3.connect(":memory:")
    conn.execute("PRAGMA foreign_keys=ON")
    conn.execute(measurement)
    conn.execute(telemetry)
    for sql in v9:
        conn.execute(sql)
    conn.execute("PRAGMA user_version=9")
    ts = "2026-10-22T08:00:00Z"
    conn.executemany(
        "INSERT INTO sites(site_id,name,created_ts,updated_ts) VALUES (?,?,?,?)",
        [("S-1", "المختبر الأصلي", ts, ts),
         ("S-2", "الموقع البديل", ts, ts)])
    conn.executemany(
        """INSERT INTO field_sessions(field_session_id,site_id,name,
            started_ts,ended_ts,status,created_ts,updated_ts)
            VALUES (?,?,?,?,?,?,?,?)""",
        [("FS-APP", "S-1", "جلسة معتمدة قديمة", ts, ts, "APPROVED", ts, ts),
         ("FS-CLOSED", "S-2", "جلسة لم تعتمد", ts, ts, "CLOSED", ts, ts)])
    for k in range(6):
        session_id = "OLD-"+str(k)
        conn.execute(
            """INSERT INTO measurement_sessions(session_id,started_ts,ended_ts,
               kind,operator_role,payload,analytics) VALUES (?,?,?,?,?,?,?)""",
            (session_id, ts, ts, "field_gas", "USER",
             json.dumps({"original": k, "id": session_id}), '{"legacy":"V9"}'))
        for j in range(14):
            source = {"mq4":{"scan_cycle":j,"rs":80.25+k+j,
                    "r0":None if j % 3 == 0 else 90.5,
                    "ok":True,"powered":True}}
            # Preserve source JSON text exactly, including nulls and scan cycles.
            conn.execute("""INSERT INTO telemetry_samples(session_id,t_ms,env_payload,
                        mq_payload,motion_payload) VALUES (?,?,?,?,?)""",
                 (session_id,1000+j*100,json.dumps({"temperature":25.0}),
                  json.dumps(source), '{"original_motion":"V9"}'))
        parent = "FS-APP" if k < 4 else "FS-CLOSED"
        linked = "2026-10-22T09:00:00Z" if k in (0,1) else "2026-10-22T09:00:0"+str(k)+"Z"
        conn.execute("""INSERT INTO field_session_measurements(
                  field_session_id,measurement_session_id,linked_ts) VALUES(?,?,?)""",
                     (parent, session_id, linked))
    conn.commit()
    hashes={table:digest_rows(conn,table) for table in (
        "sites","field_sessions","measurement_sessions","telemetry_samples",
        "field_session_measurements")}
    initial_counts={table:conn.execute(f"SELECT COUNT(*) FROM {table}").fetchone()[0]
                    for table in hashes}
    export_dir=Path(__import__("os").environ.get("N06D_EVIDENCE_DIR","")).resolve() if __import__("os").environ.get("N06D_EVIDENCE_DIR") else None
    if export_dir:
        export_dir.mkdir(parents=True,exist_ok=True)
        with sqlite3.connect(str(export_dir/"v9_before_approved_fixture.sqlite3")) as stored:
            conn.backup(stored)
    return conn, hashes, initial_counts, v9, v10, backfill, java_sha

class ActualV9ToV10Migrator(unittest.TestCase):
    def test_archived_source_tampering_is_detectable_without_mutation(self):
        conn, before, counts, v9, v10, backfill, java_sha=fixture()
        original=before["telemetry_samples"]
        self.assertEqual(counts["telemetry_samples"],84)
        # A single tampered Rs stored in V9 must change its source hash.
        conn.execute("UPDATE telemetry_samples SET mq_payload=? WHERE id=1",
                     ('{"mq4":{"scan_cycle":0,"rs":999999,"ok":true}}',))
        self.assertNotEqual(original,digest_rows(conn,"telemetry_samples"))
        conn.rollback()
        # Recreate a pristine source instead of pretending rollback restores
        # fixture data which was committed before deliberate mutation.
        conn.close()
        pristine, original_hashes, _, _, _, _, _=fixture()
        self.assertEqual(original, digest_rows(pristine,"telemetry_samples"))
        pristine.close()

    def test_actual_canonical_Java_SQL_and_no_destructive_changes(self):
        conn, before, counts, v9, v10, backfill, java_sha=fixture()
        self.assertEqual(conn.execute("PRAGMA user_version").fetchone()[0],9)
        self.assertEqual(len(backfill),1)
        self.assertTrue(any(x.startswith("CREATE TABLE IF NOT EXISTS field_measurement_cycles(")
                            for x in v10))
        self.assertIn("LEGACY_APPROVED",backfill[0])
        self.assertIn("INSERT OR IGNORE INTO field_measurement_cycles(",backfill[0])
        self.assertEqual(counts["telemetry_samples"],84)
        self.assertEqual(counts["measurement_sessions"],6)
        sql_source_hashes={
            "v9":hashlib.sha256(("\n".join(v9)).encode()).hexdigest(),
            "v10":hashlib.sha256(("\n".join(v10)).encode()).hexdigest(),
            "v9_backfill":hashlib.sha256(backfill[0].encode()).hexdigest()
        }
        # Exactly the Java upgrade path: create V10 cycles -> import V9 links.
        for sql in v10:conn.execute(sql)
        for sql in backfill:conn.execute(sql)
        conn.execute("PRAGMA user_version=10")
        conn.commit()
        after={name:digest_rows(conn,name) for name in before}
        self.assertEqual(before,after,"V9 approved/source/metadata raw rows must be byte-logically preserved")
        cycles=conn.execute("""SELECT cycle_id,field_session_id,sequence_no,state,
                           measurement_session_id,evidence_sha256,approved_ts,
                           include_gas,include_particles,include_physical
                           FROM field_measurement_cycles
                           ORDER BY field_session_id,sequence_no""").fetchall()
        self.assertEqual(len(cycles),6)
        for cycle_id,fs,seq,state,session,sha,approved,gas,particles,physical in cycles:
            self.assertEqual(cycle_id,"LEGACY-"+session)
            self.assertEqual((gas,particles,physical),(1,0,1))
            self.assertIsNone(sha,"V9 historical evidence SHA cannot be fabricated")
            if fs=="FS-APP":
                self.assertEqual(state,"LEGACY_APPROVED")
                self.assertIsNotNone(approved)
            else:
                self.assertEqual(state,"LEGACY_IMPORTED")
                self.assertIsNone(approved)
        for fs, count in (("FS-APP",4),("FS-CLOSED",2)):
            ranks=[r[0] for r in conn.execute(
                "SELECT sequence_no FROM field_measurement_cycles "
                "WHERE field_session_id=? ORDER BY sequence_no",(fs,))]
            self.assertEqual(ranks,list(range(1,count+1)))
        # The same original importer must be idempotent, not duplicate old cycles.
        for sql in v10:conn.execute(sql)
        for sql in backfill:conn.execute(sql)
        conn.commit()
        self.assertEqual(conn.execute("SELECT COUNT(*) FROM field_measurement_cycles").fetchone()[0],6)
        self.assertEqual(before,{table:digest_rows(conn,table) for table in before})
        self.assertEqual(conn.execute("PRAGMA integrity_check").fetchone()[0],"ok")
        self.assertEqual(conn.execute("PRAGMA foreign_key_check").fetchall(),[])
        export_dir=Path(__import__("os").environ.get("N06D_EVIDENCE_DIR","")).resolve() if __import__("os").environ.get("N06D_EVIDENCE_DIR") else None
        if export_dir:
            with sqlite3.connect(str(export_dir/"v10_after_migration_fixture.sqlite3")) as stored:
                conn.backup(stored)
            for fname in ("v9_before_approved_fixture.sqlite3",
                          "v10_after_migration_fixture.sqlite3"):
                check=sqlite3.connect(str(export_dir/fname))
                self.assertEqual(check.execute("PRAGMA integrity_check").fetchone()[0],"ok")
                check.close()
        # V6 is explicitly gated from inventing G3 for legacy V9 cycles without SHA.
        v6=V6_APPENDIX.read_text(encoding="utf8")
        self.assertIn('if("LEGACY_APPROVED".equals(cycle.state))',v6)
        self.assertIn("legacy_approved_gas_cycles_without_sha_skipped",v6)
        self.assertIn("V9_LEGACY_APPROVED_HAS_NO_CANONICAL_SHA256_NOT_ELIGIBLE_FOR_NEW_V6_REPLAY",v6)
        # IMPORTANT: canonical legacy verifyApprovedEvidence returns true under
        # legacy policy; it is not a cryptographic proof. Keep this explicit.
        legacy_repo=Path("android/app/src/main/java/com/servoc/app/field/FieldCycleRepository.java").read_text(encoding="utf8")
        self.assertIn('if("LEGACY_APPROVED".equals(cycle.state))return true',legacy_repo)
        manifest={
          "schema":"hazrob-v6-n06d-Java-exact-v9-to-v10-sql-migration/1",
          "java_experiments_db_sha256":java_sha,
          "original_SQL_source_hashes":sql_source_hashes,
          "source_db_user_version":9,"upgraded_db_user_version":10,
          "v9_historical_measurements":6,"v9_historical_telemetry_rows":84,
          "v9_historical_approved_sessions":1,"v9_other_unapproved_sessions":1,
          "preserved_tables_original_sha256":before,
          "preserved_tables_after_sha256":after,
          "source_sha_preserved":before==after,
          "imported_legacy_approved_without_fabricated_sha":4,
          "imported_legacy_other_without_fabricated_sha":2,
          "backfill_idempotent":True,"foreign_key_integrity":True,
          "v6_replay_of_unhashed_legacy_approval_is_forbidden":True,
          "not_Android_SQLiteOpenHelper_test":True,
          "not_original_user_personal_database":True,
          "not_proof_of_physical_R0_or_gas_ppm":True,
          "tank_g3":0,
          "verified_sqlite_files":{
              name:hashlib.sha256((export_dir/name).read_bytes()).hexdigest()
              for name in ("v9_before_approved_fixture.sqlite3",
                           "v10_after_migration_fixture.sqlite3")
          } if export_dir else None
        }
        output=Path("research/chemcore_v6/results/n06d_v9_v10_sqlite_source_migration_2026-10-30.json")
        # CI will pass an explicit output destination; never overwrite
        # the tracked research source result from an incidental local test.
        if len(sys.argv)>1 and sys.argv[1].startswith("--manifest="):
            output=Path(sys.argv[1].split("=",1)[1])
        output.parent.mkdir(parents=True,exist_ok=True)
        output.write_text(json.dumps(manifest,indent=2,ensure_ascii=False)+"\n",encoding="utf8")
        conn.close()

if __name__=="__main__":
    # unittest must not try to interpret custom --manifest option.
    argv=[sys.argv[0]]+[x for x in sys.argv[1:] if not x.startswith("--manifest=")]
    unittest.main(argv=argv)
