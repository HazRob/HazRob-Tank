package com.servoc.app.chemcore.nextintegration;

import static org.junit.Assert.*;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.servoc.app.measurement.InstantCaptureGate;
import com.servoc.app.report.TankInstantReportPdf;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Synthetic Android 12 instrumented contract only: timing/cycles and actual PDF
 * opening. NOT physical MQ calibration, firmware verification or a G3 result.
 */
@RunWith(AndroidJUnit4.class)
public final class NextInstantCapturePdfInstrumentedTest {
  private static JSONObject diag(long scan,boolean relayOn,boolean valid)throws Exception{
    JSONObject source=new JSONObject();
    source.put("relay",new JSONObject().put("on",relayOn));
    JSONObject mq=new JSONObject();
    JSONObject mq3=new JSONObject().put("ok",valid).put("rs",23.4);
    if(scan>=0)mq3.put("scan_cycle",scan);
    mq.put("mq3",mq3);
    source.put("mq",mq);
    return source;
  }
  @Test public void notReadyBeforeWarmupSamplingAndObservedSourceSpan()throws Exception{
    long start=100_000L;
    InstantCaptureGate gate=new InstantCaptureGate(start,diag(0,true,true));
    gate.observe(start+59_999L,diag(100,true,true));
    assertFalse(gate.isReady(start+120_000L));
    // Ten independent firmware cycles, spanning more than 58s within capture.
    for(int i=0;i<10;i++){
      long t=start+60_000L+i*6_500L;
      gate.observe(t,diag(i+1,true,true));
      gate.observe(t+150L,diag(i+1,true,true)); // duplicate frame
    }
    assertFalse(gate.isReady(start+119_999L));
    assertTrue(gate.isReady(start+120_000L));
    JSONObject evidence=gate.verifiedEvidence(start+120_000L);
    assertEquals("hazrob.tank.v8.instant_source_capture/1",evidence.getString("schema"));
    assertEquals(10,evidence.getInt("distinct_scan_cycles"));
    assertEquals(10,evidence.getJSONArray("source_rows").length());
    assertTrue(evidence.getLong("source_observed_span_ms")>=58_000L);
    assertFalse(evidence.getBoolean("has_validated_gas_identity"));
  }
  @Test public void noScanCycleOrRelayInterruptionFailsClosed()throws Exception{
    long start=300_000L;
    InstantCaptureGate noCycles=new InstantCaptureGate(start,diag(5,true,true));
    for(int i=0;i<20;i++)noCycles.observe(start+60_000L+i*3_100L,diag(-1,true,true));
    assertFalse(noCycles.isReady(start+200_000L));
    assertTrue(noCycles.readinessMessage(start+200_000L).contains("0/10"));
    InstantCaptureGate lost=new InstantCaptureGate(start,diag(5,true,true));
    lost.observe(start+62_000L,diag(6,false,true));
    assertTrue(lost.hasFailure());
    assertFalse(lost.isReady(start+200_000L));
    InstantCaptureGate gone=new InstantCaptureGate(start,diag(5,true,true));
    gone.observe(start+65_000L,null);
    assertTrue(gone.hasFailure());
  }
  @Test public void nextPdfOpensWithZeroTwelveAndEightyCandidates()throws Exception{
    for(int count:new int[]{0,1,12,80}){
      JSONObject original=diag(11,true,true);
      JSONObject next=new JSONObject()
        .put("schema","hazrob.tank.v8.next.decision.presentation/1")
        .put("decision_uid","NEXT-READONLY-"+count)
        .put("decision_status",count==0?"UNKNOWN":"RESEARCH_ONLY")
        .put("snapshot_sha256",repeat('a',64))
        .put("stable_manifest_sha256",repeat('b',64))
        .put("model_version","chemcore-next/research")
        .put("candidate_count",count);
      JSONArray rows=new JSONArray();
      for(int i=0;i<count;i++)rows.put(new JSONObject()
        .put("rank",i+1).put("chemical_uid",1000+i)
        .put("evidence_summary","synthetic test, not chemical evidence")
        .put("source_roots",new JSONArray().put("synthetic")));
      next.put("candidates",rows);
      File pdf=File.createTempFile("tank-v8-next-", ".pdf",
        InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir());
      try{
        try(FileOutputStream out=new FileOutputStream(pdf)){
          TankInstantReportPdf.write(out,original,next,"SYNTHETIC-TEST",1730000000000L);
        }
        try(ParcelFileDescriptor pfd=ParcelFileDescriptor.open(pdf,
          ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer renderer=new PdfRenderer(pfd)){
          assertEquals(1+(count+13)/14,renderer.getPageCount());
          PdfRenderer.Page p=renderer.openPage(0);
          assertEquals(595,p.getWidth());assertEquals(842,p.getHeight());
          p.close();
        }
      }finally{pdf.delete();}
    }
  }
  @Test public void referenceThresholdsPreserveConditionalPercentages(){
    var m=com.hazrob.tank.chemcore.next.ReferenceThresholdEngine.class;
    assertEquals(10.0,
      com.hazrob.tank.chemcore.next.ReferenceThresholdEngine.percent(10,100),0.000001);
    assertEquals("EARLY_WARNING",
      com.hazrob.tank.chemcore.next.ReferenceThresholdEngine.level(12,100,true,false));
    assertEquals("EARLY_WARNING",
      com.hazrob.tank.chemcore.next.ReferenceThresholdEngine.level(95,100,false,false));
    assertEquals("AT_OR_ABOVE_REFERENCE",
      com.hazrob.tank.chemcore.next.ReferenceThresholdEngine.level(15,10,false,true));
    assertEquals(15000.0,
      com.hazrob.tank.chemcore.next.ReferenceThresholdEngine.ppmReference(
         1.5,"vol%",true),0.01);
    assertNull(com.hazrob.tank.chemcore.next.ReferenceThresholdEngine.ppmReference(
         20,"mg/m3",false));
  }

  @Test public void brandedNextPdfContainsOriginalSourceAndRiskAppendices()throws Exception{
    JSONObject original=diag(11,true,true);
    JSONObject candidate=new JSONObject().put("rank",1)
      .put("chemical_uid",1001)
      .put("evidence_summary","synthetic research source for report layout");
    JSONObject science=new JSONObject();
    JSONArray curves=new JSONArray();
    for(int i=0;i<2;i++)curves.put(new JSONObject()
      .put("source_curve_id","MQDATA43-SYN-"+i)
      .put("sensor_model","MQ3").put("gas_or_class","synthetic test")
      .put("source_root","TEST_REFERENCE")
      .put("estimated_ppm",120.0+i).put("status","RESEARCH_WITHIN_SOURCE_DOMAIN"));
    science.put("all_reference_models",curves);
    science.put("computed_ppm_count",2);
    JSONObject ref=new JSONObject().put("reference_role","LFL")
      .put("material_label","synthetic test").put("source_curve_id","TEST_REF")
      .put("level","EARLY_WARNING").put("estimated_ppm",120.0)
      .put("reference_ppm",1000.0).put("percent_of_reference",12.0);
    JSONObject risks=new JSONObject().put("comparisons",new JSONArray().put(ref))
      .put("material_ghs_references",new JSONArray().put(
          new JSONObject().put("chemical_uid",1001)
          .put("reference_material_label","synthetic test")
          .put("ghs_pictograms",new JSONArray().put("GHS02"))));
    JSONObject next=new JSONObject()
      .put("schema","hazrob.tank.v8.next.decision.presentation/1")
      .put("decision_uid","NEXT-SYN-001").put("decision_status","RESEARCH_CANDIDATES")
      .put("snapshot_sha256",repeat('a',64))
      .put("stable_manifest_sha256",repeat('b',64))
      .put("model_version","chemcore-next-test")
      .put("candidate_count",1).put("candidates",new JSONArray().put(candidate))
      .put("scientific_source_diagnostics",science)
      .put("scientific_reference_screening",risks);
    File pdf=File.createTempFile("hazrob-next-branded", ".pdf",
      InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir());
    try{
      try(FileOutputStream output=new FileOutputStream(pdf)){
        TankInstantReportPdf.write(output,original,next,"SIMULATED",1730000000000L);
      }
      try(ParcelFileDescriptor fd=ParcelFileDescriptor.open(pdf,ParcelFileDescriptor.MODE_READ_ONLY);
          PdfRenderer renderer=new PdfRenderer(fd)){
        assertEquals(5,renderer.getPageCount());
        for(int i=0;i<renderer.getPageCount();i++){
          PdfRenderer.Page page=renderer.openPage(i);
          assertEquals(595,page.getWidth());assertEquals(842,page.getHeight());
          page.close();
        }
      }
    }finally{pdf.delete();}
  }

  private static String repeat(char c,int n){
    char[] a=new char[n];java.util.Arrays.fill(a,c);return new String(a);
  }
}
