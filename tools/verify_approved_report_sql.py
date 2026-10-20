#!/usr/bin/env python3
"""Exercise the EXACT Java approved-report SQL with real SQLite (no emulator).

Fails if drafts, developer rows, unlinked instant snapshots leak into final reports,
or >100 newer drafts starve older approved measurements.
"""
from __future__ import annotations

import re
import sqlite3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "android/app/src/main/java/com/servoc/app/data/ExperimentQueries.java"


def approved_report_sql(limit: int) -> str:
    src = SOURCE.read_text(encoding="utf-8")
    cols_match = re.search(r'private static final String COLS="([^"]+)"', src)
    assert cols_match, "No SQL record-columns contract"
    column_selection = "e." + cols_match.group(1).replace(",", ",e.")
    start = src.index("public static List<Record> approvedFieldReports(")
    block = src[start:src.index("/** Developer-only", start)]
    expr = block.split("String sql=", 1)[1].split(";", 1)[0]
    parts = re.findall(r'"([^"]*)"', expr)
    assert len(parts) >= 3 and parts[0] == "SELECT ", "Unexpected approved-query construction"
    assert "ORDER BY e.id DESC LIMIT " in parts[-1]
    sql = parts[0] + column_selection + "".join(parts[1:]) + str(limit)
    assert "fs.status='APPROVED'" in sql
    assert "m.operator_role='USER'" in sql
    assert "m.kind LIKE 'field_%'" in sql
    return sql


def exercise() -> None:
    db = sqlite3.connect(":memory:")
    db.executescript("""
        CREATE TABLE experiments(
          id INTEGER PRIMARY KEY, ts TEXT, payload TEXT, protocol TEXT,
          analytics TEXT, approved INTEGER, approved_ts TEXT, reference_name TEXT,
          session_id TEXT, capture_id INTEGER, chemical_uid INTEGER,
          reference_kind TEXT, known_ppm REAL
        );
        CREATE TABLE measurement_sessions(session_id TEXT PRIMARY KEY,
          operator_role TEXT, kind TEXT);
        CREATE TABLE field_sessions(field_session_id TEXT PRIMARY KEY,status TEXT);
        CREATE TABLE field_session_measurements(field_session_id TEXT,
          measurement_session_id TEXT);
    """)

    def add(id: int, role: str, kind: str, field_status: str | None) -> None:
        sid = f"sample-{id}"
        db.execute("INSERT INTO measurement_sessions VALUES(?,?,?)", (sid,role,kind))
        db.execute("INSERT INTO experiments(id,ts,session_id) VALUES(?,?,?)", (id,"2026-10-20",sid))
        if field_status is not None:
            fs = f"field-{id}"
            db.execute("INSERT INTO field_sessions VALUES(?,?)", (fs,field_status))
            db.execute("INSERT INTO field_session_measurements VALUES(?,?)", (fs,sid))

    add(1,"USER","field_mq_cycle","CLOSED")
    add(2,"USER","field_mq_cycle","APPROVED")
    add(3,"DEVELOPER","developer_mq_cycle","APPROVED")
    add(4,"USER","field_mq_cycle","APPROVED")
    add(5,"USER","field_mq_cycle",None)
    add(6,"USER","field_mq_cycle","ACTIVE")
    add(7,"DEVELOPER","field_mq_cycle","APPROVED")
    add(8,"USER","instant_snapshot","APPROVED")
    db.execute("INSERT INTO experiments(id,ts,protocol) VALUES(?,?,?)",
               (9,"2026-10-20","TANK_V3_INSTANT_SNAPSHOT"))
    for id in range(10,165):
        add(id,"USER","field_mq_cycle","CLOSED")

    sql = approved_report_sql(100)
    got = [row[0] for row in db.execute(sql).fetchall()]
    assert got == [4,2], f"Nonapproved leakage or draft starvation: {got!r}"
    got_one = [row[0] for row in db.execute(approved_report_sql(1)).fetchall()]
    assert got_one == [4], f"LIMIT applied before APPROVED eligibility: {got_one!r}"
    print("PASS: actual source SQL excludes all draft/developer/instant rows; "
          "155 later drafts cannot starve approved older user captures")
    db.close()


if __name__ == "__main__":
    exercise()
