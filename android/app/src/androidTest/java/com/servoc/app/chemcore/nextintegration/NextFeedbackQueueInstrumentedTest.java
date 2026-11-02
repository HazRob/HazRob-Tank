package com.servoc.app.chemcore.nextintegration;

import static org.junit.Assert.*;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.hazrob.tank.chemcore.next.ChemCoreNext;
import com.hazrob.tank.chemcore.next.CoreContracts;
import com.servoc.app.data.ExperimentsDb;
import java.util.List;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Android 12 local-only synthetic feedback outbox tests. No network upload. */
@RunWith(AndroidJUnit4.class)
public final class NextFeedbackQueueInstrumentedTest {
  private Context context;
  private ExperimentsDb main;
  private static final String STABLE=
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
  @Before public void before(){
    context=InstrumentationRegistry.getInstrumentation().getTargetContext();
    context.deleteDatabase("servoc.db");
    context.deleteDatabase("hazrob_feedback_outbox.db");
    SQLiteDatabase original=context.openOrCreateDatabase("servoc.db",
      Context.MODE_PRIVATE,null);
    original.execSQL("CREATE TABLE experiments(id INTEGER PRIMARY KEY,payload TEXT NOT NULL)");
    original.execSQL("INSERT INTO experiments VALUES(1,'old V7 raw retained')");
    original.setVersion(14);original.close();
    main=new ExperimentsDb(context);
  }
  @After public void after(){
    if(main!=null)main.close();
    context.deleteDatabase("servoc.db");
    context.deleteDatabase("hazrob_feedback_outbox.db");
  }
  private NextDecisionStore.Receipt originalDecision(){
    var reading=new CoreContracts.Reading("tank:MQ3",450,2.0,25.0,50.0,null,
      CoreContracts.CalibrationReference.AIR_RS_MEAN,
      "SOURCE_HEATER_UNQUALIFIED_FOR_G3",CoreContracts.SampleQuality.VALID);
    var snapshot=new CoreContracts.Snapshot("INSTANT-FEEDBACK-001","tank-device",
      CoreContracts.Mode.INSTANT,245000L,24.5,48.0,true,
      List.of(reading),STABLE);
    var research=new CoreContracts.Result(ChemCoreNext.VERSION,snapshot,
      ChemCoreNext.snapshotHash(snapshot),
      CoreContracts.DecisionStatus.UNKNOWN,List.of(),List.of(),
      List.of("UNKNOWN_NO_TANK_G3"));
    return new NextDecisionStore(main).append("INSTANT-1",1730000000000L,
      snapshot,research);
  }
  @Test public void noConsentIsLocalOnlyAndSurvivesDatabaseReopen()throws Exception{
    var decision=originalDecision();
    String id;
    try(NextFeedbackQueue q=new NextFeedbackQueue(context)){
      var r=q.add(context,decision,"الرأي: الإيثانول مع دخان محتمل",false);
      id=r.id;
      assertEquals(NextFeedbackQueue.LOCAL_ONLY,r.status);
      assertFalse(r.publicConsent);
      assertEquals(decision.decisionUid,r.decisionUid);
      assertEquals(decision.snapshotSha256,r.snapshotSha256);
      assertEquals(0,q.pending(10).size());
      var json=new JSONObject(r.payloadJson);
      assertEquals("hazrob.tank.v8.feedback/1",json.getString("schema"));
      assertEquals("UNKNOWN",json.getString("decision_status"));
      assertEquals(1,json.getJSONArray("original_mq_measurements").length());
      assertEquals(25.0,json.getJSONArray("original_mq_measurements")
        .getJSONObject(0).getDouble("rs_kohm"),0.000001);
      assertEquals("MQ3",json.getJSONArray("original_mq_measurements")
        .getJSONObject(0).getString("sensor_model"));
      assertEquals(ChemCoreNext.VERSION,json.getString("model_version"));
      assertFalse(json.getBoolean("ppm_certified"));
      assertFalse(json.getBoolean("training_source_promotion_authorized"));
    }
    try(NextFeedbackQueue restarted=new NextFeedbackQueue(context)){
      assertNotNull(restarted.get(id));
      assertEquals(0,restarted.pending(10).size());
    }
    assertEquals(CoreContracts.DecisionStatus.UNKNOWN,
      new NextDecisionStore(main).byId(decision.decisionUid).status);
  }
  @Test public void consentCreatesPendingAuthNotFalseUploadAndOnlyRealReceiptMarksSent()
      throws Exception {
    var decision=originalDecision();
    try(NextFeedbackQueue q=new NextFeedbackQueue(context)){
      var r=q.add(context,decision,"أرى أن النتيجة تحتاج إعادة معايرة",true);
      assertEquals(NextFeedbackQueue.PENDING_AUTH,r.status);
      assertTrue(r.publicConsent);
      assertEquals(1,q.pending(10).size());
      assertFalse(q.confirmRemoteReceipt(r.id,"https://example.net/fake",r.payloadSha256));
      assertEquals(NextFeedbackQueue.PENDING_AUTH,q.get(r.id).status);
      assertFalse(q.confirmRemoteReceipt(r.id,
        "https://github.com/HazRob/HazRob-Tank/blob/main/feedback/records/fake.json",
        "not-original-sha"));
      assertEquals(NextFeedbackQueue.PENDING_AUTH,q.get(r.id).status);
      assertTrue(q.confirmRemoteReceipt(r.id,
        "https://github.com/HazRob/HazRob-Tank/blob/main/feedback/records/2026/11/"+r.id+".json",
        r.payloadSha256));
      assertEquals(NextFeedbackQueue.SENT,q.get(r.id).status);
      assertEquals(0,q.pending(10).size());
    }
  }
  @Test public void feedbackPayloadIsAppendOnlyAndCannotOverwriteMeasuredDecision()
      throws Exception {
    var decision=originalDecision();
    try(NextFeedbackQueue q=new NextFeedbackQueue(context)){
      var r=q.add(context,decision,"النسبة المرشحة لا تطابق الواقع",true);
      boolean blocked=false;
      try{q.getWritableDatabase().execSQL(
        "UPDATE feedback_outbox SET payload_json='{}' WHERE feedback_id=?",
        new Object[]{r.id});
      }catch(android.database.SQLException expected){blocked=true;}
      assertTrue(blocked);
      blocked=false;
      try{q.getWritableDatabase().execSQL(
        "DELETE FROM feedback_outbox WHERE feedback_id=?",
        new Object[]{r.id});
      }catch(android.database.SQLException expected){blocked=true;}
      assertTrue(blocked);
      assertEquals(r.payloadSha256,q.get(r.id).payloadSha256);
      assertEquals(decision.decisionUid,
        new NextDecisionStore(main).byId(decision.decisionUid).decisionUid);
    }
  }
  @Test public void blankCommentCannotEnterOutbox()throws Exception {
    try(NextFeedbackQueue q=new NextFeedbackQueue(context)){
      boolean blocked=false;
      try{q.add(context,originalDecision(),"  ",true);}
      catch(IllegalArgumentException expected){blocked=true;}
      assertTrue(blocked);
      assertTrue(q.pending(10).isEmpty());
    }
  }
}
