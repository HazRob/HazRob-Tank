package com.servoc.app.report;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import com.servoc.app.chemical.ChemicalSeedBootstrap;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.chemical.ChemicalRepository;
import com.servoc.app.chemical.GhsPictograms;
import com.servoc.app.report.TankInstantReportPdf;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import org.json.JSONArray;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import com.hazrob.tank.chemcore.v6.V6ScientificThresholdPolicy;
import org.json.JSONObject;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Disposable API31 emulator ONLY: actual V11 SQLite and official APK
 * NIOSH/OSHA imports, no actual Tank gas readings or operator alarm.
 */
public final class V6ScientificRiskDbFixtureActivity extends Activity {
    private int assertions=0;
    private void check(boolean yes,String why){
        assertions++;if(!yes)throw new AssertionError("N09 "+assertions+" "+why);
    }
    private static long num(SQLiteDatabase db,String sql){
        try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):-1;}
    }
    private static double amount(SQLiteDatabase db,String sql){
        try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getDouble(0):Double.NaN;}
    }
    private void writeStatus(JSONObject d)throws Exception{
        try(FileOutputStream s=openFileOutput("n09_scientific_risk_db.json",MODE_PRIVATE)){
            s.write(d.toString(2).getBytes(StandardCharsets.UTF_8));
        }
    }
    private int checkRealOfflineGhsGraphics(ExperimentsDb db,long benzeneUid,long oxygenUid)
        throws Exception {
        GhsPictograms.bindContext(this);
        List<String> benzene=GhsPictograms.forChemical(db,benzeneUid);
        List<String> oxygen=GhsPictograms.forChemical(db,oxygenUid);
        check(benzene.contains("GHS02") && benzene.contains("GHS08"),
            "benzene has original verified flame and chronic health pictograms");
        check(oxygen.contains("GHS03") && !oxygen.contains("GHS02"),
            "O2 displays oxidizer, never flammable");
        check(!GhsPictograms.valid("GHS10"),"radiation not a fabricated GHS code");
        check(GhsPictograms.forQualifiedAlarm(db,benzeneUid,false,false,false).isEmpty(),
            "unqualified MQ research result cannot receive gas alarm GHS badge");
        check(GhsPictograms.forQualifiedAlarm(db,benzeneUid,true,false,false).isEmpty(),
            "identity alone cannot authorize gas alarm");
        check(GhsPictograms.forQualifiedAlarm(db,benzeneUid,true,true,false).isEmpty(),
            "operator hazard alarm not certified until independent authorization");
        for(int n=1;n<=9;n++){
            String id=String.format(java.util.Locale.US,"ghs_%02d",n);
            int res=getResources().getIdentifier(id,"drawable",getPackageName());
            check(res!=0,"all 9 bundled offline GHS drawables exist: "+id);
            Drawable drawable=getDrawable(res);
            check(drawable!=null,"vector parses: "+id);
            Bitmap b=Bitmap.createBitmap(128,128,Bitmap.Config.ARGB_8888);
            Canvas canvas=new Canvas(b);
            canvas.drawColor(0xFFFFFFFF);
            drawable.setBounds(0,0,128,128);
            drawable.draw(canvas);
            int red=0,black=0;
            for(int y=0;y<128;y+=2)for(int x=0;x<128;x+=2){
                int pixel=b.getPixel(x,y);
                int rr=(pixel>>16)&255,gg=(pixel>>8)&255,bb=pixel&255;
                if(rr>145&&rr>gg*1.35&&rr>bb*1.35)red++;
                if(rr<115&&gg<115&&bb<115)black++;
            }
            check(red>50&&black>20,"nonblank red diamond and black hazard glyph "+id+
                " red="+red+" black="+black);
            b.recycle();
        }
        JSONObject report=new JSONObject();
        report.put("schema","hazrob-tank-v6-android-telemetry-bridge/1");
        report.put("candidates",new JSONArray().put(new JSONObject()
            .put("chemical_uid",benzeneUid).put("label","Benzene")
            .put("ghs_pictograms",GhsPictograms.asJson(benzene))));
        report.put("source_curve_hypotheses",new JSONArray());
        File file=new File(getFilesDir(),"n09_ghs_report.pdf");
        try(FileOutputStream out=new FileOutputStream(file)){
            TankInstantReportPdf.write(out,new JSONObject().put("mq",new JSONObject()),
                report,"INSTANT-GHS-TEST",1761782400000L);
        }
        int red=0,black=0;
        try(ParcelFileDescriptor descriptor=ParcelFileDescriptor.open(file,
                ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer pdf=new PdfRenderer(descriptor)){
            check(pdf.getPageCount()==2,
                "GHS source-qualified candidate creates 2nd report page");
            try(PdfRenderer.Page p=pdf.openPage(1)){
                Bitmap page=Bitmap.createBitmap(595,842,Bitmap.Config.ARGB_8888);
                page.eraseColor(0xFFFFFFFF);
                p.render(page,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                for(int y=140;y<280;y++)for(int x=40;x<230;x++){
                    int pixel=page.getPixel(x,y);
                    int rr=(pixel>>16)&255,gg=(pixel>>8)&255,bb=pixel&255;
                    if(rr>145&&rr>gg*1.35&&rr>bb*1.35)red++;
                    if(rr<100&&gg<100&&bb<100)black++;
                }
                page.recycle();
            }
        }
        check(red>40&&black>40,
            "actual PDF page contains rasterized official GHS vector artwork");
        return red;
    }

    private void execute()throws Exception {
        deleteDatabase("servoc.db");
        JSONObject out=new JSONObject();
        try(ExperimentsDb db=new ExperimentsDb(this)){
            SQLiteDatabase sql=db.getWritableDatabase();
            check(sql.getVersion()==13,"SQLite V13");
            long original=num(sql,"SELECT COUNT(*) FROM chemicals");
            check(original>=140,"legacy coverage preserved");
            check(num(sql,"SELECT COUNT(*) FROM chemical_risk_profiles")==original,
                "all legacy chemicals receive profile");
            check(num(sql,"SELECT COUNT(*) FROM chemical_background_ranges")>=1,
                "only source-qualified ambient reference ranges may be preseeded");
            check(num(sql,"SELECT COUNT(*) FROM exposure_limits")==0,
                "new test SQLite starts without fake numeric exposure limits");
            ChemicalSeedBootstrap.ensureNiosh(this,db);
            long materials=num(sql,"SELECT COUNT(*) FROM chemicals");
            long profiles=num(sql,"SELECT COUNT(*) FROM chemical_risk_profiles");
            long limits=num(sql,"SELECT COUNT(*) FROM exposure_limits");
            long osha=num(sql,"SELECT COUNT(*) FROM exposure_limits WHERE source_id='OSHA_Z1'");
            long nioshIdlh=num(sql,"SELECT COUNT(*) FROM exposure_limits WHERE source_id='NIOSH_IDLH'");
            check(materials>=652,"NIOSH 652 official records imported + legacy");
            check(profiles==materials,"trigger covers 100% chemical identities");
            long unified=num(sql,"SELECT COUNT(*) FROM v11_unified_chemical_catalog");
            long unique=num(sql,"SELECT COUNT(DISTINCT uid) FROM v11_unified_chemical_catalog");
            long legacy=num(sql,"SELECT COUNT(*) FROM v11_unified_chemical_catalog WHERE original_144_member=1");
            check(unified==materials&&unique==materials,"one selectable row per identity, no join duplication");
            check(legacy==144,"original 144 are retained as in-catalog membership flags");
            // V13 hazard and chemistry metrics are one-to-one projections,
            // never duplicate chemicals or convert UNKNOWN to nonhazardous.
            long columns=num(sql,"SELECT COUNT(*) FROM pragma_table_info('v11_unified_chemical_catalog')");
            check(columns>=75,"V13 scientific columns exposed in all chemical menus");
            long lfl=num(sql,"SELECT COUNT(*) FROM v11_unified_chemical_catalog WHERE lfl_vol_pct IS NOT NULL");
            long ufl=num(sql,"SELECT COUNT(*) FROM v11_unified_chemical_catalog WHERE ufl_vol_pct IS NOT NULL");
            long unknown=num(sql,"SELECT COUNT(*) FROM v11_unified_chemical_catalog "+
                "WHERE source_qualified_hazard_count=0");
            check(unknown>0,"missing safety sources remain UNKNOWN, never forced false");
            long oxygenUid=num(sql,"SELECT uid FROM chemicals WHERE cas='7782-44-7' LIMIT 1");
            long benzeneUid=num(sql,"SELECT uid FROM chemicals WHERE cas='71-43-2' LIMIT 1");
            long methaneUid=num(sql,"SELECT uid FROM chemicals WHERE cas='74-82-8' LIMIT 1");
            check(oxygenUid>0&&benzeneUid>0&&methaneUid>0,"source-qualified example CAS found");
            int pdfGhsRedPixels=checkRealOfflineGhsGraphics(db,benzeneUid,oxygenUid);
            check(num(sql,"SELECT is_oxidizer FROM v11_unified_chemical_catalog WHERE uid="+oxygenUid)==1,
                "oxygen documented oxidizer");
            check(num(sql,"SELECT is_carcinogen FROM v11_unified_chemical_catalog WHERE uid="+benzeneUid)==1,
                "benzene documented carcinogen");
            check(num(sql,"SELECT is_asphyxiant FROM v11_unified_chemical_catalog WHERE uid="+methaneUid)==1,
                "methane documented oxygen displacement hazard");
            check(Math.abs(amount(sql,"SELECT flash_point_c FROM v11_unified_chemical_catalog WHERE uid="+benzeneUid)+11)<0.001,
                "OSHA benzene flash point -11 C sourced");
            check(num(sql,"SELECT COUNT(*) FROM chemical_hazard_classifications WHERE evidence_status='SOURCE_QUALIFIED'")>=7,
                "curated scientific hazard classifications imported");
            check(num(sql,"SELECT COUNT(*) FROM chemcore_technique_evidence WHERE validation_gate='G3_PHYSICAL_PASSED'")==0,
                "no G3 scientific technique promoted");
            check(num(sql,"SELECT COUNT(*) FROM v11_unified_chemical_catalog WHERE chemcore_m4_status='G3_PHYSICAL_PASSED'")==0,
                "no fictitious M4 successes");
            check(num(sql,"SELECT COUNT(*) FROM v11_unified_chemical_catalog WHERE original_144_member=1")==144,
                "V13 keeps 144 original identities exactly once");

            ChemicalRepository unifiedRepo=new ChemicalRepository(db);
            check(unifiedRepo.count()==materials,"actual UI repository exposes whole catalog");
            JSONObject oxygenProfile=unifiedRepo.scientificProfile(oxygenUid);
            JSONObject benzeneProfile=unifiedRepo.scientificProfile(benzeneUid);
            check(oxygenProfile.length()>=75 && oxygenProfile.optInt("is_oxidizer",0)==1,
                "whole unified profile is accessible to operator/lab by master UID");
            check(benzeneProfile.optInt("is_carcinogen",0)==1 &&
                "1".equals(benzeneProfile.optString("iarc_group","")),
                "source-coded IARC Group 1 preserved by shared repository");
            check(oxygenProfile.has("lfl_vol_pct") &&
                oxygenProfile.isNull("lfl_vol_pct"),
                "nonflammable oxygen has UNKNOWN/not-applicable LFL, never numeric 0");
            check(oxygenProfile.has("chemcore_m4_status") &&
                "NOT_G3_VALIDATED".equals(oxygenProfile.optString("chemcore_m4_status","")),
                "M4 disclosure same in all menus");

            check(unifiedRepo.search("",5000).size()==materials,
                "default unfiltered menu returns all master identities");
            check(unifiedRepo.byLegacyId(23)!=null &&
                unifiedRepo.byLegacyId(23).uid>0,"old index maps to SAME master UID");
            check(unifiedRepo.search("124-38-9",20).size()==1,
                "CAS search returns one CO2 row");
            check(num(sql,"SELECT COUNT(*) FROM chemical_hazard_boundaries "+
                "WHERE hazard_type='OXYGEN_DEFICIENCY'")>=1,
                "OSHA low oxygen reference is preserved");
            check(num(sql,"SELECT COUNT(*) FROM chemical_hazard_boundaries "+
                "WHERE hazard_type='OXYGEN_ENRICHMENT'")>=1,
                "OSHA high oxygen reference is preserved");
            check(num(sql,"SELECT COUNT(*) FROM chemical_ambient_benchmarks")>=1,
                "natural ambient benchmark separate from safety limit");
            check(lfl>=50&&ufl>=50,"existing pinned NIOSH LFL/UFL imported into unified view");
            check(num(sql,"SELECT COUNT(*) FROM v11_unified_chemical_catalog WHERE lfl_vol_pct IS NULL")>0,
                "nonflammable or missing LFL is NULL rather than false 0");
            check(osha>=500,"OSHA Z-1 verified packaged seed is loaded");
            check(nioshIdlh>=300,"NIOSH IDLH reference imported");
            check(limits>=osha+nioshIdlh,"all authority-specific entries retained");
            check(num(sql,"SELECT COUNT(*) FROM chemical_background_ranges")>=1,
                "only provenance-bound natural ranges; unlisted materials have NULL limits");
            long co2Uid=num(sql,"SELECT uid FROM chemicals WHERE cas='124-38-9' LIMIT 1");
            check(co2Uid>0,"CO2 CAS identity linked, no name-based guessing");
            double co2Pel=amount(sql,"SELECT exposure_reference_value FROM "+
                "v11_exposure_early_boundary WHERE chemical_uid="+co2Uid+
                " AND source_id='OSHA_Z1' AND UPPER(limit_type)='TWA' AND unit='ppm' LIMIT 1");
            double co2Early=amount(sql,"SELECT early_exposure_value FROM "+
                "v11_exposure_early_boundary WHERE chemical_uid="+co2Uid+
                " AND source_id='OSHA_Z1' AND UPPER(limit_type)='TWA' AND unit='ppm' LIMIT 1");
            check(Math.abs(co2Pel-5000)<.0001,"OSHA authoritative CO2 5000 ppm TWA");
            check(Math.abs(co2Early-4500)<.0001,"90 percent referenced CO2 PEL");
            check(num(sql,"SELECT COUNT(*) FROM chemical_risk_profiles WHERE "+
                "occupational_alarm_authorized!=0")==0,"G3=0; no certified alarms");
            check(num(sql,"SELECT COUNT(*) FROM chemical_risk_profiles WHERE "+
                "review_status='UNREVIEWED'")==materials,"no false human review claim");
            V6ScientificThresholdPolicy.Decision reached=
                V6ScientificThresholdPolicy.compare(5000,co2Pel,"TWA",0.90);
            check(reached.sitePotentialHazard&&!reached.regulatoryTwaExceeded,
                "instant concentration reaching PEL is site warning not TWA proof");
            out.put("status","PASS");
            out.put("sqlite_version",13).put("legacy_rows",original)
                .put("post_authority_import_materials",materials)
                .put("covered_profiles",profiles)
                .put("unified_chemical_menu_rows",unified)
                .put("unique_master_uids",unique)
                .put("original_144_members",legacy)
                .put("unified_view_columns",columns)
                .put("materials_with_lfl",lfl)
                .put("materials_with_ufl",ufl)
                .put("hazard_source_classification_rows",num(sql,
                    "SELECT COUNT(*) FROM chemical_hazard_classifications"))
                .put("chemcore_g3_techniques",0)
                .put("bundled_ghs_vector_assets",9)
                .put("ghs_pdf_vector_raster_red_pixels",pdfGhsRedPixels)
                .put("ghs_pdf_page_count",2)
                .put("coverage_fraction",profiles/(double)materials)
                .put("reference_limit_rows",limits)
                .put("osha_z1_rows",osha).put("niosh_idlh_rows",nioshIdlh)
                .put("co2_source_pel_ppm",co2Pel)
                .put("co2_early_warn_ppm",co2Early)
                .put("normal_ranges_with_source",num(sql,
                    "SELECT COUNT(*) FROM chemical_background_ranges"))
                .put("physical_tank_g3",0)
                .put("certified_gas_alerts",false)
                .put("assertions",assertions);
        }
        writeStatus(out);
    }
    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        new Thread(()->{
            try{execute();}
            catch(Throwable t){
                try{writeStatus(new JSONObject().put("status","FAIL")
                    .put("error",t.toString()).put("assertions",assertions));}
                catch(Exception ignored){}
                android.util.Log.e("N09V11","Failed source-qualified SQLite",t);
            }
        },"V6-risk-ci").start();
    }
}
