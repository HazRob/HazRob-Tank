package com.servoc.app.report;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import com.servoc.app.chemical.ChemicalSeedBootstrap;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.chemical.ChemicalRepository;
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
    private void execute()throws Exception {
        deleteDatabase("servoc.db");
        JSONObject out=new JSONObject();
        try(ExperimentsDb db=new ExperimentsDb(this)){
            SQLiteDatabase sql=db.getWritableDatabase();
            check(sql.getVersion()==12,"SQLite V12");
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
            ChemicalRepository unifiedRepo=new ChemicalRepository(db);
            check(unifiedRepo.count()==materials,"actual UI repository exposes whole catalog");
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
            check(osha>=500,"OSHA Z-1 verified packaged seed is loaded");
            check(nioshIdlh>=300,"NIOSH IDLH reference imported");
            check(limits>=osha+nioshIdlh,"all authority-specific entries retained");
            check(num(sql,"SELECT COUNT(*) FROM chemical_background_ranges")==0,
                "unknown natural boundaries explicitly stay null");
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
            out.put("sqlite_version",12).put("legacy_rows",original)
                .put("post_authority_import_materials",materials)
                .put("covered_profiles",profiles)
                .put("unified_chemical_menu_rows",unified)
                .put("unique_master_uids",unique)
                .put("original_144_members",legacy)
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
