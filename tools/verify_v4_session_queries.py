#!/usr/bin/env python3
"""V4 non-destructive field-session count / empty-site regression using *actual* Java SQL.

This is SQLite query-level evidence only, NOT an Android database migration/device test.
"""
from __future__ import annotations

import re
import sqlite3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "android/app/src/main/java/com/servoc/app/field/FieldSessionRepository.java"


def source_count_sql() -> str:
    source = SOURCE.read_text(encoding="utf-8")
    start = source.index("measurementCountsForSite(String siteId)")
    end = source.index("try(Cursor c=", start)
    body = source[start:end]
    match = re.search(r"String sql=(.*?);", body, re.S)
    assert match, "Cannot locate Java grouped query"
    parts = re.findall(r'"([^"]*)"', match.group(1))
    assert len(parts) >= 3, "Unexpected Java SQL string construction"
    sql = "".join(parts)
    assert "LEFT JOIN field_session_measurements" in sql
    assert "GROUP BY f.field_session_id" in sql
    assert "WHERE f.site_id=?" in sql
    return sql


def run() -> None:
    sql = source_count_sql()
    db = sqlite3.connect(":memory:")
    db.executescript("""
        CREATE TABLE sites(site_id TEXT PRIMARY KEY,name TEXT);
        CREATE TABLE field_sessions(
            field_session_id TEXT PRIMARY KEY, site_id TEXT NOT NULL, status TEXT);
        CREATE TABLE field_session_measurements(
            field_session_id TEXT NOT NULL,measurement_session_id TEXT NOT NULL);
    """)
    # First install, and site with no sessions, must remain distinct from errors.
    assert db.execute(sql, ("none",)).fetchall() == []
    db.execute("INSERT INTO sites VALUES('empty','Empty')")
    assert db.execute(sql, ("empty",)).fetchall() == []

    db.execute("INSERT INTO sites VALUES('north','North')")
    db.execute("INSERT INTO sites VALUES('south','South')")
    db.executemany("INSERT INTO field_sessions VALUES(?,?,?)",
                   ((f"north-{i}", "north", "APPROVED" if i%3==0 else "CLOSED")
                    for i in range(650)))
    db.executemany("INSERT INTO field_sessions VALUES(?,?,?)",
                   ((f"south-{i}", "south", "CLOSED") for i in range(25)))
    db.executemany("INSERT INTO field_session_measurements VALUES(?,?)",
                   ((f"north-{i}", f"telemetry-{i}-{j}")
                    for i in range(650) for j in range(i%4)))
    db.executemany("INSERT INTO field_session_measurements VALUES(?,?)",
                   ((f"south-{i}", f"other-{i}") for i in range(25)))
    north = dict(db.execute(sql, ("north",)).fetchall())
    south = dict(db.execute(sql, ("south",)).fetchall())
    assert len(north) == 650 and len(south) == 25
    assert north["north-0"] == 0
    assert north["north-1"] == 1
    assert north["north-2"] == 2
    assert north["north-3"] == 3
    assert north["north-648"] == 0
    assert sum(north.values()) == sum(i%4 for i in range(650))
    assert all(v == 1 for v in south.values())
    # Grouped count is read-only; no site/session/reference data is modified.
    assert db.execute("SELECT COUNT(*) FROM field_sessions").fetchone()[0] == 675
    assert db.execute("SELECT COUNT(*) FROM sites").fetchone()[0] == 3
    db.close()
    print("PASS: source-derived grouped SQLite query: 0, 650/25 sessions, isolated IDs, no writes")


if __name__ == "__main__":
    run()
