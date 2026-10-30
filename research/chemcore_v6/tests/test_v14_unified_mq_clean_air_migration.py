#!/usr/bin/env python3
"""Exact V13->V14 SQLite Java migration: no rekeying, no fabricated sensor readings."""
import pathlib, re, json, sqlite3, unittest
from research.chemcore_v6.tests.test_v13_unified_hazard_migration import (
    source_sql as v13_sql, create_v12_fixture, immutable_digest)

SOURCE=pathlib.Path("android/app/src/main/java/com/servoc/app/data/ExperimentsDb.java")

def v14_sql():
    s=SOURCE.read_text(encoding="utf8")
    assert "DB_VERSION=14;" in s
    m=re.search(r"private static void createV14MqR0AndAmbientView\(SQLiteDatabase db\)\{",s)
    if not m:raise AssertionError("missing V14 Java source")
    start=m.end();end=s.index("    private static void createCaptureUidIndex",start)
    method=s[start:end]
    statements=[]
    for x in re.finditer(r"db\.execSQL\(([\s\S]*?)\);",method):
        strings=re.findall(r'"(?:\\.|[^"\\])*"',x.group(1))
        if not strings:raise AssertionError("empty Java SQL literals")
        statements.append("".join(json.loads(q) for q in strings))
    if len(statements)<5:raise AssertionError("incomplete migration")
    return statements

class V14MqCleanAirMigration(unittest.TestCase):
    def test_non_destructive_v13_to_v14_with_one_uid_per_material(self):
        db=sqlite3.connect(":memory:")
        db.execute("PRAGMA foreign_keys=ON")
        create_v12_fixture(db)
        old_chem=immutable_digest(db)
        for query in v13_sql()[0]:db.execute(query)
        db.execute("PRAGMA user_version=13")
        db.commit()
        before=immutable_digest(db)
        self.assertEqual(old_chem,before)
        for query in v14_sql():db.execute(query)
        db.execute("PRAGMA user_version=14")
        db.commit()
        self.assertEqual(before,immutable_digest(db))
        self.assertEqual(db.execute("PRAGMA foreign_key_check").fetchall(),[])
        self.assertEqual(db.execute("PRAGMA integrity_check").fetchone()[0],"ok")
        self.assertEqual(db.execute("PRAGMA user_version").fetchone()[0],14)
        self.assertEqual(db.execute("SELECT COUNT(*) FROM v14_unified_chemical_catalog").fetchone()[0],3)
        self.assertEqual(db.execute("SELECT COUNT(DISTINCT uid) FROM v14_unified_chemical_catalog").fetchone()[0],3)
        self.assertGreaterEqual(db.execute("SELECT COUNT(*) FROM pragma_table_info('v14_unified_chemical_catalog')").fetchone()[0],96)
        self.assertEqual(db.execute("SELECT COUNT(*) FROM mq_chemical_reference_curves").fetchone()[0],0)
        self.assertEqual(db.execute("SELECT COUNT(*) FROM mq_device_clean_air_baselines").fetchone()[0],0)
        self.assertEqual(db.execute("SELECT COUNT(*) FROM mq_device_clean_air_envelopes").fetchone()[0],0)
        row=db.execute("SELECT tank_r0_measurement_scope,tank_rs_measurement_scope,"+
            "source_r0_kohm_per_chemical,source_rs_kohm_per_chemical,"+
            "tank_r0_kohm_per_chemical,tank_rs_kohm_per_chemical,"+
            "natural_concentration_coverage_status,abnormal_concentration_coverage_status "+
            "FROM v14_unified_chemical_catalog WHERE uid=930").fetchone()
        self.assertEqual(row[0],"DEVICE_SENSOR_ONLY_NOT_CHEMICAL_ATTRIBUTE")
        self.assertEqual(row[1],"CURRENT_SENSOR_SNAPSHOT_NOT_CHEMICAL_ATTRIBUTE")
        self.assertEqual(row[2:6],(None,None,None,None))
        self.assertEqual(row[6],"NO_QUALIFIED_NATURAL_RANGE")
        self.assertEqual(row[7],"NO_QUALIFIED_DANGER_LIMIT")
        # A NOAA-style *descriptive* atmospheric span is not, by itself,
        # a source-qualified normal/safe range suitable for an operator gate.
        db.execute("INSERT INTO scientific_sources(source_id,title) "+
            "VALUES('DESCRIPTIVE_AIR','atmospheric composition, context only')")
        db.execute("INSERT INTO chemical_background_ranges "+
            "(chemical_uid,environment_type,range_min,range_max,unit,"+
            "source_id,qualification_status) "+
            "VALUES(930,'TROPOSPHERE_TEST',20.11,20.947,'vol%',"+
            "'DESCRIPTIVE_AIR','CONTEXT_SPECIFIC_DESCRIPTIVE_NOT_SAFETY')")
        self.assertEqual(db.execute("SELECT natural_concentration_coverage_status "+
            "FROM v14_unified_chemical_catalog WHERE uid=930").fetchone()[0],
            "NO_QUALIFIED_NATURAL_RANGE")
        db.execute("INSERT INTO scientific_sources(source_id,title) "+
            "VALUES('QUALIFIED_REFERENCE','source reviewed for same context')")
        db.execute("INSERT INTO chemical_background_ranges "+
            "(chemical_uid,environment_type,range_min,range_max,unit,"+
            "source_id,qualification_status) "+
            "VALUES(930,'PROTOCOL_QUALIFIED',1,2,'ppm',"+
            "'QUALIFIED_REFERENCE','SOURCE_QUALIFIED')")
        self.assertEqual(db.execute("SELECT natural_concentration_coverage_status "+
            "FROM v14_unified_chemical_catalog WHERE uid=930").fetchone()[0],
            "SOURCE_QUALIFIED_ENVIRONMENTAL_RANGE")
        db.execute("INSERT INTO scientific_sources(source_id,title) VALUES('RESEARCH','research curves')")
        db.execute("INSERT INTO mq_chemical_reference_curves"+
            "(curve_id,chemical_uid,sensor_model,ratio_a,ratio_b,output_scale,"+
            "source_curve_formula,reference_r0_definition,tank_r0_equivalence,source_id,"+
            "evidence_status) VALUES ('MQ-SAMPLE',930,'MQ4',11.5,-0.35,1,"+
            "'Rs/R0=a*ppm^b','UNVERIFIED_SOURCE_CONDITION','NOT_ESTABLISHED',"+
            "'RESEARCH','RESEARCH_REFERENCE_ONLY')")
        self.assertEqual(db.execute("SELECT mq_research_response_curve_count FROM v14_unified_chemical_catalog WHERE uid=930").fetchone()[0],1)
        db.execute("INSERT INTO mq_device_clean_air_baselines(device_id,sensor_key,calibration_revision,r0_kohm,sample_count,reference_air_context,provenance,created_utc,approved) VALUES('TANK-A','mq4','CAL-1',13.4,90,'outdoor air','operator-three-windows','2026-10-30',1)")
        self.assertEqual(db.execute("SELECT COUNT(*) FROM v14_unified_chemical_catalog").fetchone()[0],3)
        self.assertIsNone(db.execute("SELECT tank_r0_kohm_per_chemical FROM v14_unified_chemical_catalog WHERE uid=930").fetchone()[0])
        self.assertEqual(before,immutable_digest(db))
        self.assertEqual(db.execute("PRAGMA foreign_key_check").fetchall(),[])
        for query in v14_sql():db.execute(query)
        self.assertEqual(db.execute("SELECT COUNT(*) FROM v14_unified_chemical_catalog").fetchone()[0],3)
        self.assertEqual(before,immutable_digest(db))
        print("PASS EXACT V13->V14 SQLite: 734-production-UID-safe schema, source curves distinct, device R0 isolated")
        db.close()
if __name__=="__main__":unittest.main()
