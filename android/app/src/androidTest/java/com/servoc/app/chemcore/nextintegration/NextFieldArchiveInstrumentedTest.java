package com.servoc.app.chemcore.nextintegration;

import static org.junit.Assert.*;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.servoc.app.data.ExperimentsDb;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Synthetic Android12 SQLite/JSON/real 60-second sequencing contract. NOT a
 * real Tank exposure experiment, NOT ChemCore G2 or physical G3 qualification.
 */
@RunWith(AndroidJUnit4.class)
public final class NextFieldArchiveInstrumentedTest {
  private Context ctx;
  private ExperimentsDb helper;
  private static final String SESSION="FIELD-CYCLE-ANDROID-TEST";
  @Before public void setUp(){
    ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
    ctx.deleteDatabase("servoc.db");
    SQLiteDatabase original=ctx.openOrCreateDatabase("servoc.db",Context.MODE_PRIVATE,null);
    original.execSQL("CREATE TABLE measurement_sessions(session_id TEXT PRIMARY KEY,started_ts TEXT NOT NULL,ended_ts TEXT,kind TEXT NOT NULL,operator_role TEXT NOT NULL,payload TEXT,analytics TEXT)");
    original.execSQL("CREATE TABLE telemetry_samples(id INTEGER PRIMARY KEY AUTOINCREMENT,session_id TEXT NOT NULL,t_ms INTEGER NOT NULL,env_payload TEXT,mq_payload TEXT,motion_payload TEXT)");
    original.execSQL("CREATE TABLE field_measurement_cycles(cycle_id TEXT PRIMARY KEY,measurement_session_id TEXT,include_gas INTEGER,state TEXT,sequence_no INTEGER)");
    original.execSQL("INSERT INTO measurement_sessions(session_id,started_ts,kind,operator_role) VALUES('"+SESSION+"','test','field_60s_cycle','USER')");
    original.execSQL("INSERT INTO field_measurement_cycles VALUES('FC-ANDR', '"+SESSION+"',1,'SAMPLING',1)");
    original.setVersion(14);original.close();
    helper=new ExperimentsDb(ctx);
  }
  @After public void clean(){if(helper!=null)helper.close();ctx.deleteDatabase("servoc.db");}
  private void append(int ms,int physicalCycle,double rs,boolean knownAge) {
    String src="{\"mq2\":{\"ok\":true,\"powered\":true,\"scan_cycle\":"+physicalCycle+
      ",\"rs\":"+rs+(knownAge?",\"last_ok_age_ms\":100":"")+"}}";
    ContentValues v=new ContentValues();
    v.put("session_id",SESSION);v.put("t_ms",ms);
    v.put("env_payload","{\"temperature\":24,\"ok\":true}");
    v.put("mq_payload",src);
    v.put("motion_payload","{\"sample_seq\":"+(ms/1000)+"}");
    long row=helper.getWritableDatabase().insertOrThrow("telemetry_samples",null,v);
    assertTrue(row>0);
  }
  @Test public void realStoredSourceRowsProduceDeduplicatedMedianAndSha(){
    assertTrue(NextFieldArchiveBridge.requiresGasDecision(helper,SESSION));
    for(int i=0;i<=60;i++)append(i*1000,i,40.+i,true);
    var r=NextFieldArchiveBridge.fromStoredSamples(helper,SESSION);
    assertTrue(r.hasMeasuredEvidence());
    assertEquals(61,r.rawRows);assertEquals(60_000L,r.coverageMs);
    assertEquals(61,r.distinctGlobalGasScanCycles);
    assertEquals(70.0,r.qualifiedChannels.get("mq2").medianRsKohm,0.00001);
    assertEquals(1,r.qualifiedChannels.size());
    assertEquals(64,r.sourceSha256.length());
    assertEquals(r.sourceSha256,NextFieldArchiveBridge.fromStoredSamples(helper,SESSION).sourceSha256);
  }
  @Test public void repeatedPhonePollsNotSixtyOneIndependentPhysicalScans(){
    for(int i=0;i<=60;i++)append(i*1000,i/10,40.+i,true);
    var r=NextFieldArchiveBridge.fromStoredSamples(helper,SESSION);
    assertEquals(61,r.rawRows);
    assertEquals(7,r.distinctGlobalGasScanCycles);
    assertFalse(r.hasMeasuredEvidence());
    assertTrue(r.qualifiedChannels.isEmpty());
  }
  @Test public void unknownFirmwareAgePreservesSourceEvidenceWithoutFakeFreshness(){
    for(int i=0;i<=60;i++)append(i*1000,i,80.+i,false);
    var r=NextFieldArchiveBridge.fromStoredSamples(helper,SESSION);
    assertTrue(r.hasMeasuredEvidence());
    assertEquals(-1L,r.qualifiedChannels.get("mq2").lastAgeMs);
    var digest=r.sourceSha256;
    helper.getWritableDatabase().execSQL(
      "UPDATE telemetry_samples SET env_payload='{\"temperature\":66}' WHERE t_ms=30000");
    assertNotEquals(digest,NextFieldArchiveBridge.fromStoredSamples(helper,SESSION).sourceSha256);
  }
}
