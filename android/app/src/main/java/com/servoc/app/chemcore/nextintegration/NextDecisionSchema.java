package com.servoc.app.chemcore.nextintegration;

import android.database.sqlite.SQLiteDatabase;

/** V8 P07.2 additive immutable, research-only decision store. No old tables altered. */
public final class NextDecisionSchema {
  public static final int VERSION=15;
  // These SQL literals are also consumed by an independent Python SQLite
  // migration-contract test. Keep one exact source of truth, no duplicated DDL.
  // BEGIN_SQL_V15
  public static final String CREATE_TABLE="CREATE TABLE IF NOT EXISTS next_research_decisions(decision_uid TEXT PRIMARY KEY NOT NULL,session_id TEXT NOT NULL CHECK(length(session_id)>0),snapshot_uid TEXT NOT NULL CHECK(length(snapshot_uid)>0),snapshot_sha256 TEXT NOT NULL CHECK(length(snapshot_sha256)=64),stable_sha256 TEXT NOT NULL CHECK(length(stable_sha256)=64),engine_version TEXT NOT NULL CHECK(length(engine_version)>0),decision_status TEXT NOT NULL CHECK(decision_status IN ('NO_INFERENCE_MODEL','INSUFFICIENT_EVIDENCE','UNKNOWN','RESEARCH_CANDIDATES','MIXTURE_UNRESOLVED','INVALID_INPUT')),snapshot_replay TEXT NOT NULL,result_replay TEXT NOT NULL,created_utc_ms INTEGER NOT NULL CHECK(created_utc_ms>=0),candidate_count INTEGER NOT NULL CHECK(candidate_count>=0),research_only INTEGER NOT NULL DEFAULT 1 CHECK(research_only=1),ppm_reportable INTEGER NOT NULL DEFAULT 0 CHECK(ppm_reportable=0),g3_physical INTEGER NOT NULL DEFAULT 0 CHECK(g3_physical=0),UNIQUE(session_id,snapshot_sha256,engine_version))";
  public static final String CREATE_INDEX="CREATE INDEX IF NOT EXISTS idx_next_research_session ON next_research_decisions(session_id,created_utc_ms DESC)";
  public static final String IMMUTABLE_UPDATE="CREATE TRIGGER IF NOT EXISTS trg_next_research_immutable_update BEFORE UPDATE ON next_research_decisions BEGIN SELECT RAISE(ABORT,'NEXT_RESEARCH_DECISION_IMMUTABLE'); END";
  public static final String IMMUTABLE_DELETE="CREATE TRIGGER IF NOT EXISTS trg_next_research_immutable_delete BEFORE DELETE ON next_research_decisions BEGIN SELECT RAISE(ABORT,'NEXT_RESEARCH_DECISION_IMMUTABLE'); END";
  // END_SQL_V15
  private NextDecisionSchema(){}
  public static void ensure(SQLiteDatabase db){
    if(db==null)throw new IllegalArgumentException("SQLite connection missing");
    db.execSQL(CREATE_TABLE);
    db.execSQL(CREATE_INDEX);
    db.execSQL(IMMUTABLE_UPDATE);
    db.execSQL(IMMUTABLE_DELETE);
  }
}
