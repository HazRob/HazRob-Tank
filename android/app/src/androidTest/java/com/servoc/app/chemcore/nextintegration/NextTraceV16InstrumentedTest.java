package com.servoc.app.chemcore.nextintegration;

import static org.junit.Assert.*;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.hazrob.tank.chemcore.next.ChemCoreNext;
import com.hazrob.tank.chemcore.next.CoreContracts;
import com.hazrob.tank.chemcore.next.NextAnalysisTrace;
import com.servoc.app.data.ExperimentsDb;
import java.util.List;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;

/**
 * Emulator integration: V15 -> V16 preserving user information; original
 * R3 evidence, all events and SHA survive persistence + restart.
 * Never uses a live Tank or attributes laboratory gas accuracy.
 */
@RunWith(AndroidJUnit4.class)
public final class NextTraceV16InstrumentedTest {
  private static final String SHA="abababababababababababababababababababababababababababababababab";
  private Context context;
  private ExperimentsDb helper;
  @Before public void init(){
    context=InstrumentationRegistry.getInstrumentation().getTargetContext();
    context.deleteDatabase("servoc.db");
  }
  @After public void cleanup(){
    if(helper!=null)helper.close();
    context.deleteDatabase("servoc.db");
  }
  private void v15Database(){
    SQLiteDatabase old=context.openOrCreateDatabase("servoc.db",Context.MODE_PRIVATE,null);
    old.execSQL("CREATE TABLE experiments(id INTEGER PRIMARY KEY,payload TEXT NOT NULL)");
    old.execSQL("CREATE TABLE measurement_sessions(session_id TEXT PRIMARY KEY)");
    old.execSQL("CREATE TABLE meta(k TEXT PRIMARY KEY,v TEXT NOT NULL)");
    old.execSQL("INSERT INTO experiments VALUES(37,'owner-original-capture')");
    old.execSQL("INSERT INTO measurement_sessions VALUES('owner-existing-session')");
    old.execSQL("INSERT INTO meta VALUES('owner_r_air_reference','preserve-owner-R0')");
    NextDecisionSchema.ensure(old);
    old.setVersion(15);
    old.close();
  }
  private static CoreContracts.Snapshot snap(){
    return new CoreContracts.Snapshot("CAPTURE-V9-SOURCE-001","virtual-r3-qa",
      CoreContracts.Mode.FIELD_SESSION,120000L,25.4,50.0,true,List.of(),SHA);
  }
  private static CoreContracts.Result result(CoreContracts.Snapshot s){
    return new CoreContracts.Result(ChemCoreNext.VERSION,s,ChemCoreNext.snapshotHash(s),
      CoreContracts.DecisionStatus.UNKNOWN,List.of(),List.of(),
      List.of("NO_INDEPENDENT_SOURCE_G3"));
  }
  private static List<NextAnalysisTrace.Event> trace(){
    return List.of(
      new NextAnalysisTrace.Event("INPUT","CAPTURE",SHA,"virtual-r3-qa",null,
        "HASH_VERIFIED","actual snapshot SHA, not a cryptographic signature",null),
      new NextAnalysisTrace.Event("CURVE","EXTERNAL_RESEARCH_01","ROOT_01","S-MQ2",200L,
        "ABSTAIN","SOURCE_R0_NOT_PROVEN_FOR_DEVICE",null),
      new NextAnalysisTrace.Event("COHERENCE","ROOT_01","ROOT_01","",null,
        "SINGLE_SENSOR_NOT_DISCRIMINATIVE","no source transfer claimed",null),
      new NextAnalysisTrace.Event("FINAL","STATUS","","",null,
        "UNKNOWN","NO_SOURCE_G3_VALIDATED",null));
  }
  @Test public void v15ToV16KeepsOwnerDataAndDoesNotRewriteV15DecisionTable(){
    v15Database();
    helper=new ExperimentsDb(context);
    SQLiteDatabase db=helper.getWritableDatabase();
    assertEquals(16,db.getVersion());
    try(Cursor c=db.rawQuery("SELECT payload FROM experiments WHERE id=37",null)){
      assertTrue(c.moveToFirst());assertEquals("owner-original-capture",c.getString(0));
    }
    try(Cursor c=db.rawQuery("SELECT v FROM meta WHERE k='owner_r_air_reference'",null)){
      assertTrue(c.moveToFirst());assertEquals("preserve-owner-R0",c.getString(0));
    }
    try(Cursor c=db.rawQuery("SELECT session_id FROM measurement_sessions",null)){
      assertTrue(c.moveToFirst());assertEquals("owner-existing-session",c.getString(0));
    }
    try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM next_research_trace_v16",null)){
      assertTrue(c.moveToFirst());assertEquals(0,c.getInt(0));
    }
  }
  @Test public void archivedTraceAndDecisionRemainAtomicImmutableAndLossless()throws Exception{
    v15Database();
    helper=new ExperimentsDb(context);
    var snap=snap();
    var output=result(snap);
    var events=trace();
    var store=new NextDecisionStore(helper);
    var first=store.appendWithTrace("owner-existing-session",1050L,snap,output,events);
    assertNotNull(first.executionTrace);
    assertEquals(4,first.executionTrace.size());
    assertEquals("VERIFIED_R3_IMMUTABLE_TRACE",first.traceAvailability);
    assertEquals(64,first.traceSha256.length());
    var repeat=store.appendWithTrace("owner-existing-session",1070L,snap,output,events);
    assertEquals(first.decisionUid,repeat.decisionUid);
    assertEquals(first.traceSha256,repeat.traceSha256);
    JSONObject screen=NextDecisionEnvelope.fromReceipt(first);
    assertEquals(4,screen.getInt("trace_event_count"));
    assertEquals("UNKNOWN",screen.getString("reported_identity"));
    assertFalse(screen.getBoolean("ppm_reportable"));
    JSONObject exporter=NextDecisionEnvelope.fromReceipt(store.byId(first.decisionUid));
    NextDecisionEnvelope.assertParity(screen,exporter);
    JSONObject modified=new JSONObject(exporter.toString());
    modified.getJSONArray("next_execution_trace").getJSONObject(1)
        .put("status","FALSELY_VERIFIED");
    boolean detected=false;
    try{NextDecisionEnvelope.assertParity(screen,modified);}
    catch(IllegalStateException mismatch){detected=true;}
    assertTrue("PDF and UI cannot silently disagree about trace",detected);
    SQLiteDatabase db=helper.getWritableDatabase();
    for(String query:new String[]{
       "UPDATE next_research_trace_v16 SET event_count=0",
       "DELETE FROM next_research_trace_v16"
    }){
      boolean blocked=false;
      try{db.execSQL(query);}
      catch(android.database.SQLException ex){blocked=true;}
      assertTrue("original R3 trace rows are immutable",blocked);
    }
    helper.close();helper=null;
    helper=new ExperimentsDb(context);
    var reopened=new NextDecisionStore(helper).latestForSession("owner-existing-session");
    assertNotNull(reopened);
    assertEquals(first.traceSha256,reopened.traceSha256);
    assertEquals(4,reopened.executionTrace.size());
    assertEquals("SOURCE_R0_NOT_PROVEN_FOR_DEVICE",
        reopened.executionTrace.get(1).reason);
    try(Cursor c=helper.getReadableDatabase().rawQuery(
        "SELECT COUNT(*) FROM next_research_decisions",null)){
      assertTrue(c.moveToFirst());assertEquals(1,c.getInt(0));
    }
    try(Cursor c=helper.getReadableDatabase().rawQuery(
        "SELECT COUNT(*) FROM next_research_trace_v16",null)){
      assertTrue(c.moveToFirst());assertEquals(1,c.getInt(0));
    }
  }
  @Test public void reopeningSameHistoricalMeasurementPrefersNewR3TraceOnEqualCaptureTime(){
    v15Database();
    helper=new ExperimentsDb(context);
    var shot=snap();
    var old=new CoreContracts.Result("chemcore-next-p05-decision/0.2-g1-source-coherence",
       shot,ChemCoreNext.snapshotHash(shot),CoreContracts.DecisionStatus.UNKNOWN,
       List.of(),List.of(),List.of("OLD_RESEARCH_ONLY"));
    var archive=new NextDecisionStore(helper);
    var legacy=archive.append("owner-existing-session",1200L,shot,old);
    assertNull(legacy.executionTrace);
    var integrated=archive.appendWithTrace("owner-existing-session",1200L,
        shot,result(shot),trace());
    assertNotEquals(legacy.decisionUid,integrated.decisionUid);
    var latest=archive.latestForSession("owner-existing-session");
    assertEquals(integrated.decisionUid,latest.decisionUid);
    assertNotNull(latest.executionTrace);
  }
  @Test public void oldDecisionWithoutTraceIsNotFalselyReconstructed()throws Exception{
    v15Database();
    helper=new ExperimentsDb(context);
    var snap=snap();
    var saved=new NextDecisionStore(helper).append("OLD-V15-SESSION",10L,snap,result(snap));
    assertNull(saved.executionTrace);
    JSONObject ui=NextDecisionEnvelope.fromReceipt(saved);
    assertEquals("NOT_ARCHIVED_BEFORE_V9_R3",ui.getString("trace_status"));
    assertEquals(0,ui.getInt("trace_event_count"));
    assertTrue(ui.getBoolean("trace_missing_is_not_zero_executed_techniques"));
  }
}
