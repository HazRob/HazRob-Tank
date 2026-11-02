package com.servoc.app.chemcore.nextintegration;

import static org.junit.Assert.*;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.hazrob.tank.chemcore.next.ChemCoreNext;
import com.hazrob.tank.chemcore.next.CoreContracts;
import com.servoc.app.data.ExperimentsDb;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * V8 device/emulator-only SQLite 14->15 migration plus replay/readback test.
 * Uses an isolated fresh emulator app database. Never accesses owner Tank.
 */
@RunWith(AndroidJUnit4.class)
public final class NextDecisionStoreInstrumentedTest {
  private static final String SHA="aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
  private Context ctx;
  private ExperimentsDb helper;
  @Before public void before(){
    ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
    ctx.deleteDatabase("servoc.db");
  }
  @After public void after(){
    if(helper!=null)helper.close();
    ctx.deleteDatabase("servoc.db");
  }
  private static void seedV14(Context ctx){
    SQLiteDatabase original=ctx.openOrCreateDatabase("servoc.db",Context.MODE_PRIVATE,null);
    original.execSQL("CREATE TABLE experiments(id INTEGER PRIMARY KEY,payload TEXT NOT NULL)");
    original.execSQL("CREATE TABLE measurement_sessions(session_id TEXT PRIMARY KEY)");
    original.execSQL("CREATE TABLE field_measurement_cycles(cycle_id TEXT PRIMARY KEY)");
    original.execSQL("INSERT INTO experiments VALUES(91,'owner-v7-raw-do-not-delete')");
    original.execSQL("INSERT INTO measurement_sessions VALUES('owner-v7-session')");
    original.execSQL("INSERT INTO field_measurement_cycles VALUES('owner-v7-cycle')");
    original.setVersion(14);original.close();
  }
  private static CoreContracts.Snapshot capture(){
    return new CoreContracts.Snapshot("CAPTURE-NEXT-001","tank-physical-verified",
      CoreContracts.Mode.FIELD_SESSION, 320_000L,25.2,57.1,true,List.of(),SHA);
  }
  private static CoreContracts.Result unknown(CoreContracts.Snapshot snapshot){
    return new CoreContracts.Result(ChemCoreNext.VERSION,snapshot,
      ChemCoreNext.snapshotHash(snapshot),CoreContracts.DecisionStatus.UNKNOWN,
      List.of(),List.of(),List.of("REFERENCE_CHEMICAL_UNQUALIFIED"));
  }
  @Test public void upgradeV14ToV15PreservesThreeOldTablesAndCreatesNext(){
    seedV14(ctx);
    helper=new ExperimentsDb(ctx);
    SQLiteDatabase db=helper.getWritableDatabase();
    assertEquals(15,db.getVersion());
    try(Cursor c=db.rawQuery("SELECT payload FROM experiments WHERE id=91",null)){
      assertTrue(c.moveToFirst());assertEquals("owner-v7-raw-do-not-delete",c.getString(0));
    }
    try(Cursor c=db.rawQuery("SELECT session_id FROM measurement_sessions",null)){
      assertTrue(c.moveToFirst());assertEquals("owner-v7-session",c.getString(0));
    }
    try(Cursor c=db.rawQuery("SELECT cycle_id FROM field_measurement_cycles",null)){
      assertTrue(c.moveToFirst());assertEquals("owner-v7-cycle",c.getString(0));
    }
    try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM next_research_decisions",null)){
      assertTrue(c.moveToFirst());assertEquals(0,c.getInt(0));
    }
    var snap=capture();var decision=unknown(snap);
    var saved=new NextDecisionStore(helper).append("owner-v7-session",1234L,snap,decision);
    assertNotNull(saved);assertEquals(CoreContracts.DecisionStatus.UNKNOWN,saved.status);
    assertTrue(saved.decisionUid.startsWith("NEXT-"));
  }
  @Test public void duplicateAttemptIsIdempotentAndReadbackSurvivesReopen(){
    seedV14(ctx);
    helper=new ExperimentsDb(ctx);
    var snap=capture();var result=unknown(snap);
    var writer=new NextDecisionStore(helper);
    var first=writer.append("FIELD-REAL-SESSION",12345L,snap,result);
    var repeat=writer.append("FIELD-REAL-SESSION",12347L,snap,result);
    assertEquals(first.decisionUid,repeat.decisionUid);
    assertEquals(0,first.originalDecision.rankedEligibleCandidates.size());
    helper.close();helper=null;
    helper=new ExperimentsDb(ctx);
    var reopened=new NextDecisionStore(helper).latestForSession("FIELD-REAL-SESSION");
    assertNotNull(reopened);assertEquals(first.decisionUid,reopened.decisionUid);
    assertEquals(CoreContracts.DecisionStatus.UNKNOWN,reopened.status);
    assertEquals(ChemCoreNext.snapshotHash(snap),reopened.snapshotSha256);
    try(Cursor c=helper.getReadableDatabase().rawQuery("SELECT count(*) FROM next_research_decisions",null)){
      assertTrue(c.moveToFirst());assertEquals(1,c.getInt(0));
    }
  }
  @Test public void oldDecisionsCannotBeOverwrittenOrDeleted(){
    seedV14(ctx);
    helper=new ExperimentsDb(ctx);
    var store=new NextDecisionStore(helper);
    var saved=store.append("FIELD-1",22222L,capture(),unknown(capture()));
    SQLiteDatabase db=helper.getWritableDatabase();
    boolean updateBlocked=false,deleteBlocked=false;
    try{db.execSQL("UPDATE next_research_decisions SET decision_status='RESEARCH_CANDIDATES'");}
    catch(android.database.SQLException ex){updateBlocked=true;}
    try{db.execSQL("DELETE FROM next_research_decisions");}
    catch(android.database.SQLException ex){deleteBlocked=true;}
    assertTrue(updateBlocked);assertTrue(deleteBlocked);
    assertEquals(saved.decisionUid,store.byId(saved.decisionUid).decisionUid);
  }
}
