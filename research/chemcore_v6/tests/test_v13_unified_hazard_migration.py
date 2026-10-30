#!/usr/bin/env python3
"""Exact source-SQL V12->V13 migration regression, no real robot or user DB."""
import hashlib
import json
import pathlib
import re
import sqlite3
import unittest

SOURCE=pathlib.Path("android/app/src/main/java/com/servoc/app/data/ExperimentsDb.java")
METHOD="createV13ChemicalHazardAndTechniques"

def source_sql():
    source=SOURCE.read_text(encoding="utf8")
    assert re.search(r"DB_VERSION=1[3-9];",source), "schema must retain V13 migrations"
    m=re.search(r"private static void "+METHOD+r"\s*\(SQLiteDatabase db\)\s*\{",source)
    if m is None:raise AssertionError("missing V13 Java migration method")
    start=m.end()-1;level=0;end=None
    for i in range(start,len(source)):
        if source[i]=="{":level+=1
        elif source[i]=="}":
            level-=1
            if not level:end=i;break
    if end is None:raise AssertionError("unbalanced method")
    body=source[start+1:end]
    sql=[]
    for z in re.finditer(r"db\.execSQL\((.*?)\);",body,re.S):
        fragments=re.findall(r'"(?:\\.|[^"\\])*"',z.group(1))
        if not fragments:raise AssertionError("Java SQL literals missing")
        sql.append("".join(json.loads(x) for x in fragments))
    if len(sql)<8:raise AssertionError("truncated SQL schema")
    return sql,hashlib.sha256(source.encode()).hexdigest()

def create_v12_fixture(conn):
    statements=[
        "CREATE TABLE chemicals(uid INTEGER PRIMARY KEY,legacy_id INTEGER,cas TEXT,pubchem_cid INTEGER,name_en TEXT,name_ar TEXT,formula TEXT,molar_mass REAL,chemical_class TEXT,record_source TEXT,active INTEGER NOT NULL DEFAULT 1)",
        "CREATE TABLE scientific_sources(source_id TEXT PRIMARY KEY,title TEXT)",
        "CREATE TABLE chemical_risk_profiles(chemical_uid INTEGER PRIMARY KEY,early_exposure_fraction REAL,background_status TEXT,review_status TEXT,FOREIGN KEY(chemical_uid) REFERENCES chemicals(uid))",
        "CREATE TABLE exposure_limits(id INTEGER PRIMARY KEY,chemical_uid INTEGER,authority TEXT,source_id TEXT,limit_type TEXT,unit TEXT,value REAL,FOREIGN KEY(chemical_uid) REFERENCES chemicals(uid))",
        "CREATE TABLE chemical_background_ranges(id INTEGER PRIMARY KEY,chemical_uid INTEGER,range_min REAL,range_max REAL,environment_type TEXT,source_id TEXT,unit TEXT,FOREIGN KEY(chemical_uid) REFERENCES chemicals(uid))",
        "CREATE TABLE chemical_hazard_boundaries(id INTEGER PRIMARY KEY,chemical_uid INTEGER,hazard_type TEXT,comparison_operator TEXT,threshold REAL,unit TEXT,FOREIGN KEY(chemical_uid) REFERENCES chemicals(uid))",
        "CREATE TABLE chemical_ambient_benchmarks(id INTEGER PRIMARY KEY,chemical_uid INTEGER,nominal_value REAL,FOREIGN KEY(chemical_uid) REFERENCES chemicals(uid))",
        "CREATE TABLE sensor_evidence(id INTEGER PRIMARY KEY,chemical_uid INTEGER,evidence_type TEXT,equation TEXT,status TEXT,FOREIGN KEY(chemical_uid) REFERENCES chemicals(uid))",
        "CREATE TABLE reference_artifacts(id INTEGER PRIMARY KEY,chemical_uid INTEGER,kind TEXT,active INTEGER,FOREIGN KEY(chemical_uid) REFERENCES chemicals(uid))",
        "CREATE TABLE fingerprints(id INTEGER PRIMARY KEY,chemical_id INTEGER,label TEXT)",
        "CREATE TABLE calibration_curves(id INTEGER PRIMARY KEY,chemical_id INTEGER,active INTEGER)",
        "CREATE TABLE models(id INTEGER PRIMARY KEY,chemical_id INTEGER,active INTEGER,model_type TEXT)",
        "CREATE TABLE field_sessions(field_session_id TEXT PRIMARY KEY,status TEXT NOT NULL)",
        "CREATE TABLE measurement_sessions(session_id TEXT PRIMARY KEY,payload TEXT NOT NULL)",
        "CREATE TABLE telemetry_samples(id INTEGER PRIMARY KEY,session_id TEXT,source_rs TEXT NOT NULL,FOREIGN KEY(session_id) REFERENCES measurement_sessions(session_id))",
        "CREATE TABLE experiments(id INTEGER PRIMARY KEY,chemical_uid INTEGER,session_id TEXT,FOREIGN KEY(chemical_uid) REFERENCES chemicals(uid))",
        "CREATE VIEW v11_unified_chemical_catalog AS SELECT uid,name_en,active FROM chemicals WHERE active=1"
    ]
    for q in statements:conn.execute(q)
    conn.executemany("INSERT INTO chemicals(uid,legacy_id,cas,name_en) VALUES(?,?,?,?)",[
        (11,34,"71-43-2","Benzene"),
        (22,22,"7782-44-7","Oxygen"),
        (930,None,"74-82-8","Methane")])
    conn.executemany("INSERT INTO chemical_risk_profiles VALUES(?,?,?,?)",[
        (11,.9,"UNREVIEWED","UNREVIEWED"),(22,.9,"UNREVIEWED","UNREVIEWED"),(930,.9,"UNREVIEWED","UNREVIEWED")])
    conn.execute("INSERT INTO scientific_sources VALUES('NIOSH_NPG_FLAMMABILITY','NIOSH NPG')")
    conn.execute("INSERT INTO exposure_limits VALUES(1,930,'NIOSH','NIOSH_NPG_FLAMMABILITY','LFL','%vol',5.0)")
    conn.execute("INSERT INTO exposure_limits VALUES(2,930,'NIOSH','NIOSH_NPG_FLAMMABILITY','UFL','%vol',15.0)")
    conn.execute("INSERT INTO field_sessions VALUES('FS-FROZEN','APPROVED')")
    conn.execute("INSERT INTO measurement_sessions VALUES('MS-FROZEN','{\"immutable\":true,\"r0\":[11,22]}')")
    conn.execute("INSERT INTO telemetry_samples VALUES(1,'MS-FROZEN','{\"mq4\":{\"rs\":0.01,\"r0\":0.02}}')")
    conn.execute("INSERT INTO experiments VALUES(1,930,'MS-FROZEN')")
    conn.execute("PRAGMA user_version=12")
    conn.commit()

def immutable_digest(conn):
    values={}
    for table in ["chemicals","exposure_limits","field_sessions","measurement_sessions","telemetry_samples","experiments"]:
        values[table]=hashlib.sha256(repr(conn.execute("SELECT * FROM "+table+" ORDER BY 1").fetchall()).encode()).hexdigest()
    return values

class V13SourcedScientificMigration(unittest.TestCase):
    def test_legacy_approved_evidence_survives_exact_java_source_migration(self):
        sql,java_sha=source_sql()
        conn=sqlite3.connect(":memory:")
        conn.execute("PRAGMA foreign_keys=ON")
        create_v12_fixture(conn)
        before=immutable_digest(conn)
        self.assertEqual(conn.execute("PRAGMA user_version").fetchone()[0],12)
        for command in sql:conn.execute(command)
        conn.execute("PRAGMA user_version=13")
        conn.commit()
        self.assertEqual(before,immutable_digest(conn))
        self.assertEqual(conn.execute("PRAGMA foreign_key_check").fetchall(),[])
        self.assertEqual(conn.execute("PRAGMA integrity_check").fetchone()[0],"ok")
        self.assertEqual(conn.execute("PRAGMA user_version").fetchone()[0],13)
        n=conn.execute("SELECT COUNT(*) FROM v11_unified_chemical_catalog").fetchone()[0]
        self.assertEqual(n,3)
        self.assertEqual(conn.execute("SELECT COUNT(DISTINCT uid) FROM v11_unified_chemical_catalog").fetchone()[0],3)
        names=[x[1] for x in conn.execute("PRAGMA table_info(v11_unified_chemical_catalog)")]
        self.assertEqual(len(names),82)
        for col in ["lfl_vol_pct","ufl_vol_pct","is_carcinogen","is_radioactive",
                    "is_oxidizer","chemcore_source_fingerprint_count",
                    "chemcore_source_curve_count","chemcore_logarithmic_model_count",
                    "chemcore_m4_g3_validation_count","health_effects","natural_min"]:
            self.assertIn(col,names)
        limits=conn.execute("SELECT lfl_vol_pct,ufl_vol_pct,flammable_precaution_10pct_lfl_vol FROM v11_unified_chemical_catalog WHERE uid=930").fetchone()
        self.assertEqual(limits,(5.,15.,.5))
        missing=conn.execute("SELECT is_carcinogen,is_radioactive,lfl_vol_pct,chemcore_m4_status FROM v11_unified_chemical_catalog WHERE uid=22").fetchone()
        self.assertEqual(missing,(None,None,None,"NOT_G3_VALIDATED"))
        self.assertFalse(any("UPDATE chemicals" in command or "DELETE FROM" in command or "DROP TABLE" in command for command in sql))
        # Replaying CREATE IF NOT EXISTS and view replacement is idempotent.
        for command in sql:conn.execute(command)
        self.assertEqual(conn.execute("SELECT COUNT(*) FROM v11_unified_chemical_catalog").fetchone()[0],3)
        self.assertEqual(before,immutable_digest(conn))
        self.assertEqual(conn.execute("PRAGMA foreign_key_check").fetchall(),[])
        print("V13 EXACT JAVA MIGRATION PASS",json.dumps(dict(original_chemical_rows=3,
              preserved_approved_sessions=1,view_columns=len(names),
              lfl=limits[0],ufl=limits[1],unknown_is_null=True,java_sha256=java_sha)))
        conn.close()

if __name__=="__main__":
    unittest.main()
