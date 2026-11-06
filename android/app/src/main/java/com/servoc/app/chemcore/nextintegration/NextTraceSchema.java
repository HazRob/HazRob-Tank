package com.servoc.app.chemcore.nextintegration;

import android.database.sqlite.SQLiteDatabase;

/** V9 / DB v16: additive immutable R3 execution evidence.
 *  Old next_research_decisions and all session/R0 tables remain untouched.
 */
public final class NextTraceSchema {
  public static final int VERSION=16;
  // BEGIN_SQL_V16
  public static final String CREATE_TABLE=
      "CREATE TABLE IF NOT EXISTS next_research_trace_v16("+
      "decision_uid TEXT PRIMARY KEY NOT NULL CHECK(length(decision_uid)>5),"+
      "snapshot_sha256 TEXT NOT NULL CHECK(length(snapshot_sha256)=64),"+
      "engine_version TEXT NOT NULL CHECK(length(engine_version)>0),"+
      "trace_schema TEXT NOT NULL,"+
      "trace_sha256 TEXT NOT NULL CHECK(length(trace_sha256)=64),"+
      "trace_json TEXT NOT NULL CHECK(length(trace_json)>2),"+
      "event_count INTEGER NOT NULL CHECK(event_count>0),"+
      "created_utc_ms INTEGER NOT NULL CHECK(created_utc_ms>=0),"+
      "FOREIGN KEY(decision_uid) REFERENCES next_research_decisions(decision_uid))";
  public static final String CREATE_INDEX=
      "CREATE INDEX IF NOT EXISTS idx_next_trace_v16_engine ON next_research_trace_v16(engine_version,created_utc_ms)";
  public static final String IMMUTABLE_UPDATE=
      "CREATE TRIGGER IF NOT EXISTS trg_next_trace_v16_immutable_update BEFORE UPDATE ON next_research_trace_v16 "+
      "BEGIN SELECT RAISE(ABORT,'NEXT_R3_TRACE_IMMUTABLE'); END";
  public static final String IMMUTABLE_DELETE=
      "CREATE TRIGGER IF NOT EXISTS trg_next_trace_v16_immutable_delete BEFORE DELETE ON next_research_trace_v16 "+
      "BEGIN SELECT RAISE(ABORT,'NEXT_R3_TRACE_IMMUTABLE'); END";
  // END_SQL_V16
  private NextTraceSchema(){}
  public static void ensure(SQLiteDatabase db){
    if(db==null)throw new IllegalArgumentException("SQLite required");
    db.execSQL(CREATE_TABLE);
    db.execSQL(CREATE_INDEX);
    db.execSQL(IMMUTABLE_UPDATE);
    db.execSQL(IMMUTABLE_DELETE);
  }
}
