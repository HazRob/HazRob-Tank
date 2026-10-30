package com.servoc.app.report;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import com.servoc.app.chemcore.TankInstantAnalyzer;
import com.servoc.app.data.ExperimentsDb;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;

/** N08: SAME CLEAN AIR immediately after device R0 clean-air calibration.
 * Disposable emulator fixture, not physical tank measurement.
 * Invoke actual production instant analyzer and real V6 instant report writer.
 */
public final class V6CleanAirInstantFixtureActivity extends Activity {
  private int checks=0;
  private void require(boolean good,String message){
    checks++;if(!good)throw new AssertionError("V6 CLEAN AIR "+checks+": "+message);
  }
  private void write(String name,byte[] bytes)throws Exception{
    try(FileOutputStream o=openFileOutput(name,MODE_PRIVATE)){o.write(bytes);}
  }
  private void run()throws Exception{
    deleteDatabase("servoc.db");
    double[] r0={11,15,18,13,27,22,19,31};
    String[] keys={"mq2","mq3","mq4","mq5","mq7","mq8","mq9","mq135"};
    JSONObject mq=new JSONObject();
    for(int i=0;i<keys.length;i++){
      mq.put(keys[i],new JSONObject().put("rs",r0[i]).put("r0",r0[i])
        .put("ok",true).put("valid",true).put("powered",true)
        .put("r0_source","automatic_clean_air_field_r0")
        .put("r0_provenance_complete",true).put("scan_cycle",42));
    }
    JSONObject original=new JSONObject().put("mq",mq)
      .put("env",new JSONObject().put("temperature",25.0).put("humidity",48.0));
    long now=1793322000000L;
    try(ExperimentsDb db=new ExperimentsDb(this)){
      TankInstantAnalyzer.Result output=TankInstantAnalyzer.analyze(
        db,original,now);
      JSONObject result=output.analysis;
      require(output.channels==8,"all eight raw channel readings present");
      require(result.optInt("observed_mq_channels",-1)==8,"original V6 interface reads 8");
      require(result.optInt("ratio_ready_mq_channels",-1)==8,"R0 accepted for 8");
      require(result.optInt("raw_only_mq_channels",-1)==0,"no missing R0");
      JSONArray curves=result.optJSONArray("source_curve_hypotheses");
      JSONArray candidates=result.optJSONArray("candidates");
      JSONArray ledger=result.optJSONArray("chemical_evidence_ledger");
      require(curves!=null&&curves.length()==49,"49 source curve fits for clean air");
      require(candidates!=null&&candidates.length()==49,"UI candidate projection counts 49 per-curve rows");
      require(result.optInt("candidate_count",-1)==49,"candidate_count computed as source rows");
      require(ledger!=null&&ledger.length()==13,"13 chemical reference groups NOT physical presence");
      require("UNKNOWN".equals(result.optString("reported_identity")),"actual result UNKNOWN");
      require(!result.optBoolean("ppm_reportable",true),"certified ppm not available");
      require(!result.optBoolean("reportable_ppm",true),"research bridge never certifies ppm");
      require(result.optBoolean("report_research_only",false),"result marked research only");
      require(!result.optBoolean("legacy_inference_fallback_used",true),"new engine only");
      require(!result.optBoolean("gas_food_isolated",false)==false,"freshness never attached");
      require(result.optJSONArray("fingerprint_feature_spaces")!=null&&
        result.optJSONArray("fingerprint_feature_spaces").length()==0,
        "single snapshot does not invent time series");
      for(int i=0;i<curves.length();i++){
        JSONObject fit=curves.getJSONObject(i);
        require(Math.abs(fit.getDouble("rs_over_r0")-1.0)<1e-12,
          "clean-air ratio unity for curve "+i);
      }
      ByteArrayOutputStream pdf=new ByteArrayOutputStream();
      TankInstantReportPdf.write(pdf,output.original,output.analysis,output.id,now);
      require(pdf.size()>5000,"production V6 instant PDF exists");
      write("v6_clean_air_instant.pdf",pdf.toByteArray());
      File file=new File(getFilesDir(),"v6_clean_air_instant.pdf");
      int pages;
      try(ParcelFileDescriptor fd=ParcelFileDescriptor.open(file,
          ParcelFileDescriptor.MODE_READ_ONLY);PdfRenderer rdr=new PdfRenderer(fd)){
        pages=rdr.getPageCount();
        require(pages==1,"one-shot V6 clean air report one page");
        try(PdfRenderer.Page page=rdr.openPage(0)){
          Bitmap bmp=Bitmap.createBitmap(595,842,Bitmap.Config.ARGB_8888);
          bmp.eraseColor(Color.WHITE);
          page.render(bmp,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
          int ink=0;
          for(int y=70;y<810;y+=8)for(int x=45;x<555;x+=8){
            int p=bmp.getPixel(x,y);
            if(Color.red(p)<230||Color.green(p)<230||Color.blue(p)<230)ink++;
          }
          require(ink>40,"PDF visibly contains source observations");
          bmp.recycle();
        }
      }
      JSONObject summary=new JSONObject().put("status","PASS")
        .put("case","R0_CLEAN_AIR_REMEASURE_SAME_CLEAN_AIR_SIMULATED")
        .put("genuine_tank_exposure",false).put("original_production_analyzer_used",true)
        .put("observed_mq",output.channels).put("ratio_ready",8).put("source_curves",curves.length())
        .put("ui_candidates",candidates.length()).put("distinct_chemical_groups",ledger.length())
        .put("reported_identity",result.optString("reported_identity"))
        .put("certified_ppm",result.optBoolean("ppm_reportable",true))
        .put("pdf_pages",pages).put("check_count",checks)
        .put("research_ppm_example_mq2_lpg",curves.getJSONObject(0).optDouble("source_estimate_ppm_research"));
      write("v6_clean_air_instant_status.json",
        summary.toString(2).getBytes(StandardCharsets.UTF_8));
      write("v6_clean_air_instant_result.json",
        result.toString(2).getBytes(StandardCharsets.UTF_8));
    }
  }
  @Override public void onCreate(Bundle state){
    super.onCreate(state);
    new Thread(()->{
      try{run();}
      catch(Throwable failure){
        try{
          JSONObject err=new JSONObject().put("status","FAIL")
            .put("error",failure.toString()).put("checks_completed",checks);
          write("v6_clean_air_instant_status.json",
            err.toString(2).getBytes(StandardCharsets.UTF_8));
        }catch(Exception ignored){}
        android.util.Log.e("V6CleanAirInstant","N08 experiment failed",failure);
      }
      runOnUiThread(this::finish);
    }).start();
  }
}
