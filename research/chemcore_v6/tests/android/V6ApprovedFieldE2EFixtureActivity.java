package com.servoc.app.report;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.field.FieldCycleRepository;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * N06-E CI-ONLY Android Activity: exercise the COMPLETE production
 * FieldSessionReportExporter PDF/XLSX with a V10 SQLite fixture whose
 * APPROVED cycle is signed by the actual FieldCycleRepository.evidenceDigest.
 * The fixture is NOT a live Tank experiment, not a release APK, not G3.
 */
public final class V6ApprovedFieldE2EFixtureActivity extends Activity {
    private static final String FS="FS-N06E-APPROVED",SID="MS-N06E-01",CYCLE="FC-N06E-APPROVED";
    private static final String TIME="2026-10-30T00:00:00.000Z";
    private int asserts=0;
    private void require(boolean yes,String message){
        asserts++;
        if(!yes)throw new AssertionError("N06E "+asserts+": "+message);
    }
    private static ContentValues values(Object...pairs){
        ContentValues out=new ContentValues();
        for(int i=0;i<pairs.length;i+=2){
            String key=(String)pairs[i];Object val=pairs[i+1];
            if(val==null)out.putNull(key);
            else if(val instanceof Integer)out.put(key,(Integer)val);
            else if(val instanceof Long)out.put(key,(Long)val);
            else out.put(key,val.toString());
        }
        return out;
    }
    private byte[] fileDigest(SQLiteDatabase db)throws Exception{
        MessageDigest md=MessageDigest.getInstance("SHA-256");
        for(String table:new String[]{"sites","field_sessions","measurement_sessions",
            "telemetry_samples","field_session_measurements","field_measurement_cycles",
            "chemcore_decisions"}){
            try(Cursor c=db.rawQuery("SELECT * FROM "+table+" ORDER BY 1",null)){
                md.update(table.getBytes(StandardCharsets.UTF_8));
                for(int i=0;i<c.getCount();i++){
                    require(c.moveToNext(),"table rows stay iterable");
                    for(int j=0;j<c.getColumnCount();j++){
                        md.update((byte)(c.getType(j)+1));
                        byte[] b=c.isNull(j)?new byte[0]:c.getString(j).getBytes(StandardCharsets.UTF_8);
                        md.update(new byte[]{(byte)(b.length>>>24),(byte)(b.length>>>16),
                            (byte)(b.length>>>8),(byte)b.length});
                        md.update(b);
                    }
                }
            }
        }
        return md.digest();
    }
    private static String hex(byte[] hash){
        StringBuilder b=new StringBuilder();
        for(byte c:hash)b.append(String.format(java.util.Locale.US,"%02x",c&255));
        return b.toString();
    }
    private void fixture(ExperimentsDb db)throws Exception{
        SQLiteDatabase w=db.getWritableDatabase();
        w.insertOrThrow("sites",null,values("site_id","SITE-N06E","name","موقع اختبار N06E",
            "notes","CI TEST ONLY","created_ts",TIME,"updated_ts",TIME));
        w.insertOrThrow("field_sessions",null,values(
            "field_session_id",FS,"site_id","SITE-N06E",
            "name","جلسة تجريبية معتمدة - Test Fixture","started_ts",TIME,
            "ended_ts",TIME,"status","APPROVED","created_ts",TIME,"updated_ts",TIME));
        w.insertOrThrow("field_sessions",null,values(
            "field_session_id","FS-N06E-DRAFT","site_id","SITE-N06E",
            "name","غير معتمدة","started_ts",TIME,"status","ACTIVE",
            "created_ts",TIME,"updated_ts",TIME));
        final String terminal=new JSONObject().put("terminal_state","ready")
            .put("field_60s_capture",true).put("duration_monotonic_ms",60012)
            .put("sample_count",16)
            .put("clock_contract","UTC_EPOCH_MS_PLUS_ANDROID_ELAPSED_REALTIME")
            .toString();
        final String payload=new JSONObject().put("measurement",
            new JSONObject().put("state","idle")).put("fixture","NO_ACTUAL_TANK").toString();
        w.insertOrThrow("measurement_sessions",null,values(
            "session_id",SID,"started_ts",TIME,"ended_ts",TIME,
            "kind","field_gas","operator_role","USER","payload",payload,"analytics",terminal));
        w.insertOrThrow("field_session_measurements",null,values(
            "field_session_id",FS,"measurement_session_id",SID,"linked_ts",TIME));
        for(int i=0;i<16;i++){
            JSONObject gas=new JSONObject();
            for(String key:new String[]{"mq2","mq3","mq4","mq5","mq7","mq8","mq9","mq135"}){
                JSONObject q=new JSONObject().put("scan_cycle",i).put("ok",true)
                    .put("powered",true).put("rs",80.0+i*.8)
                    .put("r0",i%4==0?JSONObject.NULL:91.2)
                    .put("r0_source","CI_FIXTURE_RESEARCH_ONLY");
                gas.put(key,q);
            }
            JSONObject env=new JSONObject().put("temperature",25.0)
                .put("rh",48.0).put("ok",true)
                .put("last_ok_age_ms",0);
            w.insertOrThrow("telemetry_samples",null,values(
                "session_id",SID,"t_ms",100000L+i*1000L,
                "mq_payload",gas.toString(),"env_payload",env.toString(),
                "motion_payload","{}"));
        }
        // One intentionally absent analysis result is valid under SmartReport.
        // The canonical SHA includes the exact raw 16 samples and decision table.
        String device="CI-N06E-TEST-ONLY";
        Method d=FieldCycleRepository.class.getDeclaredMethod("evidenceDigest",
            SQLiteDatabase.class,String.class,String.class,String.class,String.class);
        d.setAccessible(true);
        String sha=(String)d.invoke(null,w,SID,device,terminal,payload);
        require(sha.length()==64,"canonical V10 source approval digest");
        w.insertOrThrow("field_measurement_cycles",null,values(
            "cycle_id",CYCLE,"field_session_id",FS,"sequence_no",1,
            "attempt_no",1,"measurement_session_id",SID,
            "include_gas",1,"include_particles",0,"include_physical",1,
            "state","APPROVED","started_ts",TIME,"updated_ts",TIME,
            "completed_ts",TIME,"approved_ts",TIME,"device_id",device,
            "evidence_sha256",sha));
        // Unapproved history must NOT enter V6 research output.
        w.insertOrThrow("field_measurement_cycles",null,values(
            "cycle_id","FC-N06E-FAILED","field_session_id",FS,"sequence_no",2,
            "attempt_no",1,"include_gas",1,"include_particles",0,
            "include_physical",0,"state","FAILED",
            "started_ts",TIME,"updated_ts",TIME,"fail_reason","FIXTURE_ABORTED"));
    }
    private void run()throws Exception{
        // CI-only disposable emulator database: never load a person's real DB.
        deleteDatabase("servoc.db");
        try(ExperimentsDb db=new ExperimentsDb(this)){
            fixture(db);
            SQLiteDatabase original=db.getWritableDatabase();
            FieldCycleRepository cycles=new FieldCycleRepository(db);
            require(cycles.verifyApprovedEvidence(cycles.byId(CYCLE)),
                "canonical original approved SHA verified");
            byte[] before=fileDigest(original);
            FieldSessionReportExporter.Bundle resolved=FieldSessionReportExporter.resolve(db,FS);
            require(resolved.measurementSessionIds.size()==1&&
                    SID.equals(resolved.measurementSessionIds.get(0)),
                "production facade returns exact session");
            JSONObject v6=V6FieldSessionResearchAppendix.analyzeVerified(db,resolved);
            require(v6.optInt("approved_gas_cycles",0)==1,
                "approved gas cycle replayed");
            require(v6.optJSONArray("cycles").length()==1,"one gas evidence cycle");
            require(v6.optBoolean("reportable_ppm",true)==false&&
                    "UNKNOWN".equals(v6.optString("reported_identity")),
                "no Tank G3 gas ID or ppm promoted");
            JSONObject gas=v6.optJSONArray("cycles").getJSONObject(0);
            require(gas.optInt("independently_evaluated_sensor_models",0)==8,
                "8 separately archived MQ models processed");
            // Exercise ACTUAL production export facade, not a copied renderer.
            ByteArrayOutputStream pdf=new ByteArrayOutputStream();
            FieldSessionReportExporter.writePdf(pdf,db,FS,null);
            File pdfFile=new File(getFilesDir(),"n06e_full_approved_field.pdf");
            try(FileOutputStream out=new FileOutputStream(pdfFile)){
                out.write(pdf.toByteArray());
            }
            require(pdf.size()>5500,"full PDF bytes written by approved session facade");
            ByteArrayOutputStream xlsx=new ByteArrayOutputStream();
            FieldSessionReportExporter.writeXlsx(xlsx,db,FS,null);
            File xlsxFile=new File(getFilesDir(),"n06e_full_approved_field.xlsx");
            try(FileOutputStream out=new FileOutputStream(xlsxFile)){
                out.write(xlsx.toByteArray());
            }
            require(xlsx.size()>5500,"full XLSX written by production facade");
            int pages;
            try(ParcelFileDescriptor desc=ParcelFileDescriptor.open(pdfFile,
                        ParcelFileDescriptor.MODE_READ_ONLY);
                PdfRenderer renderer=new PdfRenderer(desc)){
                pages=renderer.getPageCount();
                require(pages==6,"4 legacy analytical + 1 cycle ledger + 1 V6 appendix pages");
                for(int i=0;i<pages;i++){
                    try(PdfRenderer.Page page=renderer.openPage(i)){
                        Bitmap bitmap=Bitmap.createBitmap(842,595,Bitmap.Config.ARGB_8888);
                        page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                        require(bitmap.getPixel(20,20)!=Color.TRANSPARENT,"page rasterized");
                        bitmap.recycle();
                    }
                }
            }
            int sheets=0;boolean v6measure=false,v6source=false;
            try(ZipFile z=new ZipFile(xlsxFile)){
                for(java.util.Enumeration<? extends ZipEntry> e=z.entries();e.hasMoreElements();){
                    ZipEntry item=e.nextElement();
                    if(item.getName().startsWith("xl/worksheets/sheet")&&item.getName().endsWith(".xml"))sheets++;
                }
                java.util.zip.ZipEntry workbook=z.getEntry("xl/workbook.xml");
                require(workbook!=null,"actual workbook.xml exists");
                ByteArrayOutputStream bytes=new ByteArrayOutputStream();
                try(java.io.InputStream in=z.getInputStream(workbook)){
                    byte[] buf=new byte[8192];int n;
                    while((n=in.read(buf))>0)bytes.write(buf,0,n);
                }
                String xml=bytes.toString("UTF-8");
                v6measure=xml.contains("V6 بحثي غازات");
                v6source=xml.contains("V6 مصادر وتعارضات");
            }
            require(sheets==14&&v6measure&&v6source,
                "all 12 V5 + 2 V6 research XLSX sheets present");
            byte[] after=fileDigest(original);
            require(java.util.Arrays.equals(before,after),
                "no archived V5 decisions, approvals, Rs/R0 or telemetry modified by exports");
            require(cycles.verifyApprovedEvidence(cycles.byId(CYCLE)),
                "canonical approved SHA remains valid after real PDF+XLSX");
            boolean unapprovedRefused=false;
            try{FieldSessionReportExporter.resolve(db,"FS-N06E-DRAFT");}
            catch(IllegalStateException good){unapprovedRefused=true;}
            require(unapprovedRefused,"unapproved field report rejected");
            JSONObject status=new JSONObject()
                .put("status","PASS").put("schema","tank-v6-n06e-android-full-approval/1")
                .put("production_facade_exercised","FieldSessionReportExporter.writePdf+writeXlsx")
                .put("real_approved_v10_sqlite_fixture",true)
                .put("synthetic_test_rows",16)
                .put("approved_gas_cycles",1)
                .put("independent_source_sensor_models",8)
                .put("pdf_pages",pages).put("xlsx_sheet_count",sheets)
                .put("xlsx_v6_measurements_sheet",v6measure)
                .put("xlsx_v6_sources_sheet",v6source)
                .put("sqlite_original_approved_source_sha_before_after_equal",true)
                .put("v5_decisions_modified",false)
                .put("legacy_fallback_in_v6",false)
                .put("reported_gas_identity","UNKNOWN")
                .put("ppm_reportable",false)
                .put("tank_g3",0)
                .put("user_data_used",false)
                .put("production_apk_generated",false)
                .put("assertions",asserts)
                .put("archive_digest_before",hex(before))
                .put("archive_digest_after",hex(after));
            try(FileOutputStream o=openFileOutput("n06e_status.json",MODE_PRIVATE)){
                o.write(status.toString(2).getBytes(StandardCharsets.UTF_8));
            }
        }
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        new Thread(()->{
            try{run();}
            catch(Throwable failure){
                try{
                    JSONObject err=new JSONObject().put("status","FAIL")
                        .put("message",failure.toString());
                    try(FileOutputStream out=openFileOutput("n06e_status.json",MODE_PRIVATE)){
                        out.write(err.toString(2).getBytes(StandardCharsets.UTF_8));
                    }
                    android.util.Log.e("N06E","full approved field test failed",failure);
                }catch(Exception ignored){}
            }
            runOnUiThread(this::finish);
        }).start();
    }
}
