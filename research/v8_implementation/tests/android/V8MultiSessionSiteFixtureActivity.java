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
import com.servoc.app.chemcore.ChemCoreDecisionRepository;
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
 * V8 F05 CI-ONLY Android Activity: exercise the COMPLETE production
 * FieldSessionReportExporter PDF/XLSX with a V15 SQLite fixture whose
 * APPROVED cycle is signed by the actual FieldCycleRepository.evidenceDigest.
 * The fixture is NOT a live Tank experiment, not a release APK, not G3.
 */
public final class V8MultiSessionSiteFixtureActivity extends Activity {
    private static final String FS="FS-N06G-APPROVED",SID="MS-N06G-01",CYCLE="FC-N06G-APPROVED";
    private static final String TIME="2026-10-30T00:00:00.000Z";
    private int asserts=0;
    private int nextReportPageCount=0;
    private int nextXlsxSheetCount=0,siteXlsxSheetCount=0;
    private void require(boolean yes,String message){
        asserts++;
        if(!yes)throw new AssertionError("N06G "+asserts+": "+message);
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
        w.insertOrThrow("sites",null,values("site_id","SITE-N06G","name","موقع اختبار N06G",
            "notes","CI TEST ONLY","created_ts",TIME,"updated_ts",TIME));
        w.insertOrThrow("field_sessions",null,values(
            "field_session_id",FS,"site_id","SITE-N06G",
            "name","جلسة تجريبية معتمدة - Test Fixture","started_ts",TIME,
            "ended_ts",TIME,"status","APPROVED","created_ts",TIME,"updated_ts",TIME));
        w.insertOrThrow("field_sessions",null,values(
            "field_session_id","FS-N06G-DRAFT","site_id","SITE-N06G",
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
        // ChemCore decision schema is lazily created by its production
        // repository, not by ExperimentsDb.onCreate. The immutable source
        // digest always includes this table even when no decision exists.
        new ChemCoreDecisionRepository(db);
        String device="CI-N06G-TEST-ONLY";
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
            "cycle_id","FC-N06G-FAILED","field_session_id",FS,"sequence_no",2,
            "attempt_no",1,"include_gas",1,"include_particles",0,
            "include_physical",0,"state","FAILED",
            "started_ts",TIME,"updated_ts",TIME,"fail_reason","FIXTURE_ABORTED"));
    }

    /** TEST-ONLY Next receipt for the second independently approved field
     * session. Written BEFORE sealing the cycle digest, never injected into
     * an already approved real measurement or inferred from a PDF.
     */
    private void addNextResearchSourceFixture(ExperimentsDb db,String sid)
        throws Exception{
        var reading=new com.hazrob.tank.chemcore.next.CoreContracts.Reading(
            "CI-MQ2-PHYSICAL-1",1000,1.83,81.0,94.0,null,
            com.hazrob.tank.chemcore.next.CoreContracts.CalibrationReference.AIR_RS_MEAN,
            "MEASURED",com.hazrob.tank.chemcore.next.CoreContracts.SampleQuality.VALID);
        var snapshot=new com.hazrob.tank.chemcore.next.CoreContracts.Snapshot(
            "CI-NEXT-ARCHIVE-"+sid,"CI-N06G-SECOND-DEVICE",
            com.hazrob.tank.chemcore.next.CoreContracts.Mode.FIELD_SESSION,
            120000L,24.0,49.0,true,java.util.List.of(reading),
            "a".repeat(64));
        var evidence=new com.hazrob.tank.chemcore.next.CoreContracts.EvidenceNode(
            "CI-RESEARCH-ROOT-ONE","SOURCE_CURVE",
            "CI-MQ2-PHYSICAL-1",414L,
            com.hazrob.tank.chemcore.next.CoreContracts.EvidencePolarity.POSITIVE,
            "source-only experiment fixture, NOT physical calibration");
        var candidate=new com.hazrob.tank.chemcore.next.CoreContracts.Candidate(
            414L,"conditional source curve evidence only",
            java.util.List.of("CI-RESEARCH-ROOT-ONE"));
        var next=new com.hazrob.tank.chemcore.next.CoreContracts.Result(
            "CI-CHEMCORE-NEXT-RESEARCH-1",snapshot,
            com.hazrob.tank.chemcore.next.ChemCoreNext.snapshotHash(snapshot),
            com.hazrob.tank.chemcore.next.CoreContracts.DecisionStatus.RESEARCH_CANDIDATES,
            java.util.List.of(evidence),java.util.List.of(candidate),
            java.util.List.of("test-only source caveat"));
        var receipt=new com.servoc.app.chemcore.nextintegration.NextDecisionStore(db)
            .append(sid,1793664000000L,snapshot,next);
        require(receipt!=null&&receipt.sessionId.equals(sid)&&
            !receipt.originalDecision.reportablePpm&&
            !receipt.originalDecision.physicallyValidatedG3,
            "second field session has immutable Next source-only receipt");
    }

    private void addSecondApprovedSession(ExperimentsDb db)throws Exception{
        SQLiteDatabase w=db.getWritableDatabase();
        final String fs2="FS-N06G-APPROVED-2";
        final String sid2="MS-N06G-02";
        final String cycle2="FC-N06G-APPROVED-2";
        w.insertOrThrow("field_sessions",null,values(
            "field_session_id",fs2,"site_id","SITE-N06G",
            "name","Second independently approved field fixture",
            "started_ts",TIME,"ended_ts",TIME,"status","APPROVED",
            "created_ts",TIME,"updated_ts",TIME));
        final String terminal=new JSONObject().put("terminal_state","ready")
            .put("field_60s_capture",true).put("duration_monotonic_ms",60020)
            .put("sample_count",16)
            .put("clock_contract","UTC_EPOCH_MS_PLUS_ANDROID_ELAPSED_REALTIME")
            .toString();
        final String payload=new JSONObject().put("measurement",
            new JSONObject().put("state","idle")).put("fixture","NO_ACTUAL_TANK").toString();
        w.insertOrThrow("measurement_sessions",null,values(
            "session_id",sid2,"started_ts",TIME,"ended_ts",TIME,
            "kind","field_gas","operator_role","USER","payload",payload,
            "analytics",terminal));
        w.insertOrThrow("field_session_measurements",null,values(
            "field_session_id",fs2,"measurement_session_id",sid2,"linked_ts",TIME));
        for(int i=0;i<16;i++){
            JSONObject gas=new JSONObject();
            for(String key:new String[]{"mq2","mq3","mq4","mq5","mq7","mq8","mq9","mq135"}){
                gas.put(key,new JSONObject().put("scan_cycle",i).put("ok",true)
                    .put("powered",true).put("rs",79.0+i*.7)
                    .put("r0",94.0).put("r0_source","CI_FIXTURE_RESEARCH_ONLY"));
            }
            JSONObject env=new JSONObject().put("temperature",24.0)
                .put("rh",49.0).put("ok",true).put("last_ok_age_ms",0);
            w.insertOrThrow("telemetry_samples",null,values(
                "session_id",sid2,"t_ms",200000L+i*1000L,
                "mq_payload",gas.toString(),"env_payload",env.toString(),
                "motion_payload","{}"));
        }
        // This receipt is part of the approved evidence SHA. Adding it AFTER
        // approval is prohibited and should trigger the existing tamper guard.
        addNextResearchSourceFixture(db,sid2);
        Method d=FieldCycleRepository.class.getDeclaredMethod("evidenceDigest",
            SQLiteDatabase.class,String.class,String.class,String.class,String.class);
        d.setAccessible(true);
        String device="CI-N06G-SECOND-DEVICE";
        String sha=(String)d.invoke(null,w,sid2,device,terminal,payload);
        require(sha.length()==64,"second session canonical source SHA");
        w.insertOrThrow("field_measurement_cycles",null,values(
            "cycle_id",cycle2,"field_session_id",fs2,"sequence_no",1,
            "attempt_no",1,"measurement_session_id",sid2,
            "include_gas",1,"include_particles",0,"include_physical",1,
            "state","APPROVED","started_ts",TIME,"updated_ts",TIME,
            "completed_ts",TIME,"approved_ts",TIME,"device_id",device,
            "evidence_sha256",sha));
        require(new FieldCycleRepository(db).verifyApprovedEvidence(
            new FieldCycleRepository(db).byId(cycle2)),
            "second independently approved session verified by production digest");
    }
    private void verifyMultiSessionSite(ExperimentsDb db,byte[] baseline)throws Exception{
        SiteSummaryReportExporter.SiteBundle site=
            SiteSummaryReportExporter.resolve(db,"SITE-N06G");
        require(site.sessions.size()==2,
            "site summary retains exactly two independent approved sessions");
        require(site.sessions.stream().filter(x->x.hasNext).count()==1,
            "second source-qualified Next measurement is not replaced by legacy chemistry");
        require(com.servoc.app.report.NextSiteEvidenceCharts.independentRoots(
                site.sessions)==1,
            "site counts one actual independent Next research source root");
        require(site.sessions.get(0).bundle.measurementSessionIds.size()==1&&
                site.sessions.get(1).bundle.measurementSessionIds.size()==1,
            "site summary never merges underlying measurement sessions");
        for(SiteSummaryReportExporter.SessionSummary session:site.sessions){
            require(!session.ppmReportable,
                "without physical M4 reference no site-level reportable ppm");
            require(session.cycles.size()>=1,"original cycle ledger retained");
        }
        ByteArrayOutputStream pdf=new ByteArrayOutputStream();
        SiteSummaryReportExporter.writePdf(pdf,db,"SITE-N06G");
        File siteFile=new File(getFilesDir(),"n06g_site_two_approved.pdf");
        try(FileOutputStream out=new FileOutputStream(siteFile)){
            out.write(pdf.toByteArray());
        }
        require(pdf.size()>5500,"production site PDF emitted for two sessions");
        int pageCount=0;
        try(ParcelFileDescriptor fd=ParcelFileDescriptor.open(siteFile,
                    ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer r=new PdfRenderer(fd)){
            pageCount=r.getPageCount();
            require(pageCount==5,"site PDF 5 pages: 3 fixed, 1 sessions matrix, 1 cycles matrix");
            for(int i=0;i<pageCount;i++){
                try(PdfRenderer.Page page=r.openPage(i)){
                    Bitmap bmp=Bitmap.createBitmap(842,595,Bitmap.Config.ARGB_8888);
                    bmp.eraseColor(Color.WHITE);
                    page.render(bmp,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    int ink=0;
                    for(int yy=45;yy<535;yy+=8)
                        for(int xx=45;xx<800;xx+=8){
                            int p=bmp.getPixel(xx,yy);
                            if(Color.red(p)<230||Color.green(p)<230||Color.blue(p)<230)ink++;
                        }
                    require(ink>=15,"site PDF page "+i+" not blank");
                    bmp.recycle();
                }
            }
        }
        // CI-only: render second independently approved session whose Next
        // receipt is inside its original hashed source archive.
        ByteArrayOutputStream nextPdf=new ByteArrayOutputStream();
        FieldSessionReportExporter.writePdf(nextPdf,db,"FS-N06G-APPROVED-2",null);
        File nextFile=new File(getFilesDir(),"n06g_next_approved_field.pdf");
        try(FileOutputStream output=new FileOutputStream(nextFile)){
            output.write(nextPdf.toByteArray());
        }
        require(nextPdf.size()>5500,"real source-bound Next report bytes exist");
        // Exercise the same production public XLSX facade as the operator.
        // Next workbook analytical sheets MUST use Next receipts; no legacy
        // 144/M4/old ChemCore worksheet on the approved V8 measurement.
        ByteArrayOutputStream nextXlsx=new ByteArrayOutputStream();
        FieldSessionReportExporter.writeXlsx(nextXlsx,db,
            "FS-N06G-APPROVED-2",null);
        File nextXlsxFile=new File(getFilesDir(),"n06g_next_approved_field.xlsx");
        try(FileOutputStream output=new FileOutputStream(nextXlsxFile)){
            output.write(nextXlsx.toByteArray());
        }
        require(nextXlsx.size()>5500,"Next field XLSX source-bound production bytes");
        ByteArrayOutputStream siteXlsx=new ByteArrayOutputStream();
        SmartSiteXlsxWriter.write(siteXlsx,
            SiteSummaryReportExporter.resolve(db,"SITE-N06G"));
        File siteXlsxFile=new File(getFilesDir(),"n06g_next_site_two_approved.xlsx");
        try(FileOutputStream output=new FileOutputStream(siteXlsxFile)){
            output.write(siteXlsx.toByteArray());
        }
        require(siteXlsx.size()>3000,"Next site XLSX source-bound production bytes");
        try(ZipFile sheetZip=new ZipFile(nextXlsxFile);
            ZipFile siteZip=new ZipFile(siteXlsxFile)){
            for(java.util.Enumeration<? extends ZipEntry> entries=sheetZip.entries();
                    entries.hasMoreElements();){
                String entry=entries.nextElement().getName();
                if(entry.matches("xl/worksheets/sheet[0-9]+\\.xml"))
                    nextXlsxSheetCount++;
            }
            for(java.util.Enumeration<? extends ZipEntry> entries=siteZip.entries();
                    entries.hasMoreElements();){
                String entry=entries.nextElement().getName();
                if(entry.matches("xl/worksheets/sheet[0-9]+\\.xml"))
                    siteXlsxSheetCount++;
            }
            require(nextXlsxSheetCount==20&&siteXlsxSheetCount==6,
                "approved Next 20-sheet field and 6-sheet site Excel styles retained");
        }
        try(ParcelFileDescriptor descriptor=ParcelFileDescriptor.open(nextFile,
                    ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer nextRenderer=new PdfRenderer(descriptor)){
            nextReportPageCount=nextRenderer.getPageCount();
            require(nextReportPageCount>=5,
                "Next PDF retains existing landscape page layout and appendices");
            for(int pageIndex:new int[]{2,3}){
                try(PdfRenderer.Page page=nextRenderer.openPage(pageIndex)){
                    Bitmap image=Bitmap.createBitmap(842,595,Bitmap.Config.ARGB_8888);
                    try{
                        page.render(image,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                        int ink=0;
                        for(int yy=85;yy<520;yy+=7)
                            for(int xx=48;xx<800;xx+=7){
                                int pixel=image.getPixel(xx,yy);
                                if(Color.red(pixel)<215||Color.green(pixel)<215||
                                   Color.blue(pixel)<215)ink++;
                            }
                        require(ink>=30,
                            "Next PDF analytical page contains source-bound ink");
                    }finally{image.recycle();}
                }
            }
        }
        String sid2="MS-N06G-02";
        String saved;
        try(Cursor c=db.getWritableDatabase().rawQuery(
            "SELECT mq_payload FROM telemetry_samples WHERE session_id=? "+
            "ORDER BY t_ms,id LIMIT 1",new String[]{sid2})){
            require(c.moveToFirst(),"second source row exists");
            saved=c.getString(0);
        }
        try{
            require(db.getWritableDatabase().update("telemetry_samples",
                values("mq_payload",saved+" "),"session_id=? AND t_ms=?",
                new String[]{sid2,"200000"})==1,"tamper only second archived session");
            boolean denied=false;
            try{SiteSummaryReportExporter.resolve(db,"SITE-N06G");}
            catch(IllegalStateException expected){denied=true;}
            require(denied,"site summary rejects ANY altered approved child session");
            boolean pdfDenied=false;
            try{SiteSummaryReportExporter.writePdf(new ByteArrayOutputStream(),
                db,"SITE-N06G");}
            catch(IllegalStateException expected){pdfDenied=true;}
            require(pdfDenied,"site PDF refuses tampered second child session");
        }finally{
            db.getWritableDatabase().update("telemetry_samples",
                values("mq_payload",saved),"session_id=? AND t_ms=?",
                new String[]{sid2,"200000"});
        }
        require(java.util.Arrays.equals(baseline,fileDigest(db.getWritableDatabase())),
            "restoration leaves original two signed sessions and source tables byte-identical");
        require(SiteSummaryReportExporter.resolve(db,"SITE-N06G").sessions.size()==2,
            "both sessions recover after exact original byte restoration");
    }
    private void run()throws Exception{
        // CI-only disposable emulator database: never load a person's real DB.
        deleteDatabase("servoc.db");
        try(ExperimentsDb db=new ExperimentsDb(this)){
            SQLiteDatabase v14=db.getWritableDatabase();
            require(v14.getVersion()==15,
                "approved field export must run on current V15 database, not stale V14");
            try(Cursor count=v14.rawQuery(
                    "SELECT COUNT(*) FROM v14_unified_chemical_catalog",null);
                Cursor columns=v14.rawQuery(
                    "SELECT COUNT(*) FROM pragma_table_info('v14_unified_chemical_catalog')",
                    null)){
                require(count.moveToFirst()&&count.getInt(0)==144,
                    "V14 field fixture starts with 144 canonical legacy UIDs");
                require(columns.moveToFirst()&&columns.getInt(0)==97,
                    "V14 unified view retains 97 material fields during approved exports");
            }
            fixture(db);
            addSecondApprovedSession(db);
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
            File pdfFile=new File(getFilesDir(),"n06g_full_approved_field.pdf");
            try(FileOutputStream out=new FileOutputStream(pdfFile)){
                out.write(pdf.toByteArray());
            }
            require(pdf.size()>5500,"full PDF bytes written by approved session facade");
            ByteArrayOutputStream xlsx=new ByteArrayOutputStream();
            FieldSessionReportExporter.writeXlsx(xlsx,db,FS,null);
            File xlsxFile=new File(getFilesDir(),"n06g_full_approved_field.xlsx");
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
                        bitmap.eraseColor(Color.WHITE);
                        page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                        int visible=0;
                        // PDF margins/corners can legitimately be transparent.
                        // Measure actual content in the main page rather than
                        // expecting a painted pixel at (20,20).
                        for(int yy=40;yy<550;yy+=6)
                            for(int xx=40;xx<805;xx+=6){
                                int pixel=bitmap.getPixel(xx,yy);
                                if(Color.red(pixel)<225||Color.green(pixel)<225||
                                   Color.blue(pixel)<225)visible++;
                            }
                        require(visible>=25,"page "+i+" almost blank: "+visible);
                        bitmap.recycle();
                    }
                }
            }
            int sheets=0;boolean v6measure=false,v6source=false,envFull=false,adcFull=false,auxFull=false,jsonFull=false;
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
                envFull=xml.contains("V8 البيئة الأصلية");
                adcFull=xml.contains("V8 ADC ADS الأصلية");
                auxFull=xml.contains("V8 القنوات المساعدة");
                jsonFull=xml.contains("V8 JSON كامل");
            }
            require(sheets==20&&v6measure&&v6source&&envFull&&adcFull&&auxFull&&jsonFull,
                "14 original + 2 V6 + 4 V8 raw environmental/ADS/auxiliary/JSON XLSX sheets present");
            byte[] after=fileDigest(original);
            require(java.util.Arrays.equals(before,after),
                "no archived V5 decisions, approvals, Rs/R0 or telemetry modified by exports");
            require(cycles.verifyApprovedEvidence(cycles.byId(CYCLE)),
                "canonical approved SHA remains valid after real PDF+XLSX");
            boolean unapprovedRefused=false;
            try{FieldSessionReportExporter.resolve(db,"FS-N06G-DRAFT");}
            catch(IllegalStateException good){unapprovedRefused=true;}
            require(unapprovedRefused,"unapproved field report rejected");
            verifyMultiSessionSite(db,before);
            JSONObject status=new JSONObject()
                .put("status","PASS")
                .put("site_approved_independent_sessions",2)
                .put("site_pdf_pages",5)
                .put("site_second_session_tamper_rejected",true)
                .put("site_reportable_ppm",false).put("schema","tank-v8-f05-approved-site-android/1")
                .put("production_facade_exercised","FieldSessionReportExporter.writePdf+writeXlsx")
                .put("real_approved_v15_sqlite_fixture",true)
                .put("runtime_sqlite_schema_version",15)
                .put("v14_unified_catalog_columns",97)
                .put("synthetic_test_rows",16)
                .put("approved_gas_cycles",1)
                .put("independent_source_sensor_models",8)
                .put("pdf_pages",pages).put("xlsx_sheet_count",sheets)
                .put("next_field_pdf_pages",nextReportPageCount)
                .put("next_field_xlsx_sheets",nextXlsxSheetCount)
                .put("next_site_xlsx_sheets",siteXlsxSheetCount)
                .put("site_next_source_roots",1)
                .put("next_second_session_source_verified",true)
                .put("xlsx_v6_measurements_sheet",v6measure)
                .put("xlsx_v6_sources_sheet",v6source)
                .put("xlsx_v8_environment_sheet",envFull)
                .put("xlsx_v8_adc_ads_sheet",adcFull)
                .put("xlsx_v8_auxiliary_sheet",auxFull)
                .put("xlsx_v8_original_json_sheet",jsonFull)
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
            try(FileOutputStream o=openFileOutput("n06g_status.json",MODE_PRIVATE)){
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
                    Throwable root=failure;
                    while(root.getCause()!=null&&root.getCause()!=root)root=root.getCause();
                    java.io.StringWriter sw=new java.io.StringWriter();
                    failure.printStackTrace(new java.io.PrintWriter(sw));
                    JSONObject err=new JSONObject().put("status","FAIL")
                        .put("message",failure.toString())
                        .put("root_cause",root.toString())
                        .put("stacktrace",sw.toString())
                        .put("assertions_before_failure",asserts);
                    try(FileOutputStream out=openFileOutput("n06g_status.json",MODE_PRIVATE)){
                        out.write(err.toString(2).getBytes(StandardCharsets.UTF_8));
                    }
                    android.util.Log.e("N06G","full approved field test failed",failure);
                }catch(Exception ignored){}
            }
            runOnUiThread(this::finish);
        }).start();
    }
}
