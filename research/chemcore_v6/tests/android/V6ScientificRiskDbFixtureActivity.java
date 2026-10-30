package com.servoc.app.report;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import com.servoc.app.chemical.ChemicalSeedBootstrap;
import com.servoc.app.chemical.ChemicalReferenceSyncV6;
import com.servoc.app.chemical.ChemicalExperimentStatusRepository;
import com.servoc.app.chemical.ChemicalIdentityEquivalenceV6;
import com.servoc.app.chemical.ChemicalIdentityCategoryV6;
import com.servoc.app.chemical.ChemicalMolecularProjectionV6;
import com.servoc.app.chemical.ChemicalAliasMolecularProjectionV6;
import com.servoc.app.chemical.ChemicalNistIdentityCompletionV6;
import com.servoc.app.chemical.ChemicalNistEssentialGasMassV6;
import com.servoc.app.chemical.ChemicalCasCorrectionV6;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.data.MqDeviceR0LedgerV14;
import com.servoc.app.data.MqExternalR0RsSeedV14;
import com.servoc.app.calibration.ManualR0Calibration;
import com.servoc.app.calibration.FirmwareR0Contract;
import java.util.LinkedHashMap;
import java.util.Map;
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
    private static boolean casChecksumValid(String cas){
        if(cas==null||!cas.matches("[0-9]{2,7}-[0-9]{2}-[0-9]"))return false;
        String clean=cas.replace("-","");
        int sum=0;
        for(int i=clean.length()-2,w=1;i>=0;i--,w++)
            sum+=(clean.charAt(i)-'0')*w;
        return sum%10==(clean.charAt(clean.length()-1)-'0');
    }
    private static String scalarText(SQLiteDatabase db,String sql){
        try(Cursor c=db.rawQuery(sql,null)){
            return c.moveToFirst()&&!c.isNull(0)?c.getString(0):null;
        }
    }
    private static double amount(SQLiteDatabase db,String sql){
        try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getDouble(0):Double.NaN;}
    }
    /**
     * Material-by-material inventory, not a misleading claim that 734/734
     * substances have complete toxicity, ambient or sensor-response data.
     * One CSV row per canonical UID; missing source values remain explicitly
     * absent, never converted to zero, nonhazardous or clean air.
     */
    private static String csv(String s){
        if(s==null)return "";
        return "\"" + s.replace("\"","\"\"")
            .replace("\r"," ").replace("\n"," ") + "\"";
    }
    private JSONObject writeCoverageAudit(SQLiteDatabase sql)throws Exception {
        String query="SELECT c.uid,c.cas,c.name_en,c.name_ar,c.formula,"+
            "c.molar_mass,c.chemical_class,c.record_source,c.lfl_vol_pct,"+
            "c.ufl_vol_pct,c.mq_research_response_curve_count,"+
            "c.natural_concentration_coverage_status,c.source_qualified_hazard_count,"+
            "(SELECT COUNT(*) FROM exposure_limits e WHERE e.chemical_uid=c.uid),"+
            "(SELECT COUNT(*) FROM chemical_hazard_classifications h WHERE h.chemical_uid=c.uid),"+
            "(SELECT COUNT(*) FROM chemical_background_ranges b WHERE "+
                "b.chemical_uid=c.uid AND b.qualification_status='SOURCE_QUALIFIED'),"+
            "(SELECT x.canonical_uid FROM chemical_identity_equivalence_v6 x "+
            "WHERE x.alias_uid=c.uid),"+
            "(SELECT x.source_cas FROM chemical_identity_equivalence_v6 x "+
            "WHERE x.alias_uid=c.uid) "+
            "FROM v14_unified_chemical_catalog c ORDER BY c.uid";
        StringBuilder csv=new StringBuilder(
            "uid,cas,name_en,name_ar,formula,molar_mass,chemical_class,record_source,"+
            "lfl_vol_pct,ufl_vol_pct,mq_curve_count,natural_range_status,"+
            "source_hazard_count,exposure_limit_count,hazard_class_count,"+
            "qualified_background_range_count,canonical_uid_niosh,niosh_equivalent_cas\n");
        int rows=0,cas=0,english=0,arabic=0,formula=0,molarMass=0;
        int lfl=0,ufl=0,mq=0,exposure=0,hazard=0,background=0;
        int equivalentCas=0,conceptualNonSingle=0;
        int duplicateUids=0;
        java.util.HashSet<Long> seen=new java.util.HashSet<>();
        try(Cursor c=sql.rawQuery(query,null)){
            while(c.moveToNext()){
                rows++;
                long uid=c.getLong(0);
                if(!seen.add(uid))duplicateUids++;
                if(!c.isNull(1)&&!c.getString(1).trim().isEmpty())cas++;
                if(!c.isNull(2)&&!c.getString(2).trim().isEmpty())english++;
                if(!c.isNull(3)&&!c.getString(3).trim().isEmpty())arabic++;
                if(!c.isNull(4)&&!c.getString(4).trim().isEmpty())formula++;
                if(!c.isNull(5))molarMass++;
                if(!c.isNull(8))lfl++;
                if(!c.isNull(9))ufl++;
                if(c.getInt(10)>0)mq++;
                if(c.getInt(13)>0)exposure++;
                if(c.getInt(14)>0||c.getInt(12)>0)hazard++;
                if(c.getInt(15)>0)background++;
                if(!c.isNull(16)&&!c.isNull(17))equivalentCas++;
                String category=c.isNull(6)?"":c.getString(6);
                if(category.startsWith("MIXTURE_")||
                    category.startsWith("AGGREGATE_")||
                    "MEASUREMENT_INDEX_NOT_CHEMICAL".equals(category))
                    conceptualNonSingle++;
                for(int k=0;k<c.getColumnCount();k++){
                    if(k>0)csv.append(',');
                    if(c.isNull(k))continue;
                    if(k==0||k==5||k>=8&&k!=11)
                        csv.append(c.getString(k));
                    else csv.append(csv(c.getString(k)));
                }
                csv.append('\n');
            }
        }
        try(FileOutputStream out=openFileOutput(
                "v6_chemical_catalog_coverage.csv",MODE_PRIVATE)){
            out.write(csv.toString().getBytes(StandardCharsets.UTF_8));
        }
        return new JSONObject()
            .put("schema","hazrob-tank-v6-material-coverage-audit/1")
            .put("materials",rows).put("unique_uids",seen.size())
            .put("duplicate_uids",duplicateUids)
            .put("materials_with_cas",cas)
            .put("materials_with_niosh_cas_equivalence",equivalentCas)
            .put("materials_with_direct_or_linked_cas",cas+equivalentCas)
            .put("materials_without_direct_or_linked_cas",rows-cas-equivalentCas)
            .put("non_single_substance_labels",conceptualNonSingle)
            .put("individual_materials_without_linked_cas",
                rows-cas-equivalentCas-conceptualNonSingle)
            .put("materials_with_name_en",english)
            .put("materials_with_name_ar",arabic)
            .put("materials_with_formula",formula)
            .put("materials_with_molar_mass",molarMass)
            .put("materials_with_lfl",lfl).put("materials_with_ufl",ufl)
            .put("materials_with_mq_curve",mq)
            .put("materials_with_occupational_limit",exposure)
            .put("materials_with_hazard_classification",hazard)
            .put("materials_with_qualified_ambient_range",background)
            .put("scientific_fields_exhaustive",false)
            .put("g3_validated_tank_measurements",0)
            .put("missing_values_policy","NULL_NOT_ZERO_NO_SAFETY_INFERENCE");
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
        // The user requires more than CAS identity: an independently
        // calibrated concentration must exceed the appropriate sourced
        // hazard limit before a GHS icon appears in a FINDING report.
        // This fixture explicitly supplies unverified research candidates,
        // therefore it MUST NOT print ANY warning pictograms.
        int red=0;
        try(ParcelFileDescriptor descriptor=ParcelFileDescriptor.open(file,
                ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer pdf=new PdfRenderer(descriptor)){
            check(pdf.getPageCount()==1,
                "unverified candidate MUST NOT generate GHS report appendix");
            try(PdfRenderer.Page p=pdf.openPage(0)){
                Bitmap page=Bitmap.createBitmap(595,842,Bitmap.Config.ARGB_8888);
                page.eraseColor(0xFFFFFFFF);
                p.render(page,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                for(int y=0;y<842;y+=2)for(int x=0;x<595;x+=2){
                    int pixel=page.getPixel(x,y);
                    int rr=(pixel>>16)&255,gg=(pixel>>8)&255,bb=pixel&255;
                    if(rr>145&&rr>gg*1.35&&rr>bb*1.35)red++;
                }
                page.recycle();
            }
        }
        check(red==0,"GHS icon red diamond absent from unconfirmed report");
        check(GhsPictograms.forQualifiedAlarm(db,benzeneUid,true,true,true).isEmpty(),
            "legacy boolean-only alarm API cannot bypass pictogram threshold");
        java.util.Map<String,Boolean> failed=new java.util.HashMap<>();
        failed.put("GHS02",false);failed.put("GHS08",false);
        check(GhsPictograms.forQualifiedAlarm(db,benzeneUid,true,true,true,failed).isEmpty(),
            "known toxic substance with no exceeded exposure threshold has no report icon");
        return red;
    }

    private void execute()throws Exception {
        deleteDatabase("servoc.db");
        JSONObject out=new JSONObject();
        try(ExperimentsDb db=new ExperimentsDb(this)){
            SQLiteDatabase sql=db.getWritableDatabase();
            check(sql.getVersion()==14,"SQLite V14");
            long original=num(sql,"SELECT COUNT(*) FROM chemicals");
            check(original>=140,"legacy coverage preserved");
            check(num(sql,"SELECT COUNT(*) FROM chemical_risk_profiles")==original,
                "all legacy chemicals receive profile");
            check(num(sql,"SELECT COUNT(*) FROM chemical_background_ranges")>=1,
                "only source-qualified ambient reference ranges may be preseeded");
            check(num(sql,"SELECT COUNT(*) FROM exposure_limits")==0,
                "new test SQLite starts without fake numeric exposure limits");
            ChemicalSeedBootstrap.ensureNiosh(this,db);
            ChemicalNistEssentialGasMassV6.Result essentialGasReplay=
                ChemicalNistEssentialGasMassV6.ensure(db);
            check(essentialGasReplay.verified==2&&
                essentialGasReplay.filled==0&&
                essentialGasReplay.rejected==0,
                "NIST oxygen/methane mass import is source backed and idempotent");
            check(Math.abs(Double.parseDouble(scalarText(sql,
                "SELECT molar_mass FROM chemicals WHERE uid=23"))-31.9988)<0.0001,
                "NIST pure oxygen O2 molecular weight 31.9988 g/mol retained");
            check(Math.abs(Double.parseDouble(scalarText(sql,
                "SELECT molar_mass FROM chemicals WHERE uid=77"))-16.0425)<0.0001,
                "NIST methane CH4 molecular weight 16.0425 g/mol retained");
            check(num(sql,"SELECT COUNT(*) FROM chemical_sources "+
                "WHERE source_id='NIST_WEBBOOK' AND source_key IN "+
                "('MASS_CAS:7782-44-7','MASS_CAS:74-82-8')")==2,
                "both official NIST source links retained for essential gases");
            out.put("nist_essential_gas_mass_idempotence",
                essentialGasReplay.summary());
            ChemicalNistIdentityCompletionV6.Result nistAudit=
                ChemicalNistIdentityCompletionV6.ensure(db);
            check(nistAudit.proposed==27&&nistAudit.matched==27&&
                nistAudit.rejected==0&&nistAudit.casFilled==0&&
                nistAudit.massFilled==0,
                "27 NIST CAS idempotence "+nistAudit.summary()+
                    "; bootstrap="+db.getMeta("v6_chemical_nist_cas_completion","missing"));
            check(num(sql,"SELECT COUNT(*) FROM chemical_sources "+
                "WHERE source_id='NIST_WEBBOOK' AND "+
                "source_key LIKE 'CAS:%' AND "+
                "record_url LIKE 'https://webbook.nist.gov/cgi/cbook.cgi?ID=%'")==27,
                "27 original pure substances retain dedicated permanent NIST CAS source URLs");
            check(num(sql,"SELECT COUNT(*) FROM chemical_sources "+
                "WHERE source_id='NIST_WEBBOOK' AND "+
                "source_key='V6_INVALID_CAS_REPAIR_UID:715' AND "+
                "record_url='https://webbook.nist.gov/cgi/cbook.cgi?ID=100-47-0'")==1,
                "Benzonitrile repair has independent 28th NIST SRD69 citation");
            check(num(sql,"SELECT COUNT(*) FROM chemicals WHERE "+
                "uid IN (38,41,42,45,46,47,53,57,58,76,78,83,84,89,90,"+
                "91,93,121,124,126,127,128,129,131,132,133,134) "+
                "AND cas IS NOT NULL AND molar_mass>0")==27,
                "all formerly missing single-molecule CAS rows now qualify by NIST");
            check("1333-74-0".equals(scalarText(sql,"SELECT cas FROM chemicals WHERE uid=76")),
                "hydrogen CAS and mole fraction not confused with a gas calibration");
            check("29118-24-9".equals(scalarText(sql,"SELECT cas FROM chemicals WHERE uid=132")),
                "trans R1234ze(E) is not conflated with isomer R1234yf");
            out.put("nist_27_cas_identity_idempotence",nistAudit.summary());
            ChemicalCasCorrectionV6.Result repaired=
                ChemicalCasCorrectionV6.ensure(db);
            check(repaired.inspected==2&&repaired.corrected==0&&
                repaired.confirmed==2&&repaired.rejected==0,
                "two source-audited invalid CAS corrections replay without changes: "+
                repaired.summary()+"; startup="+
                db.getMeta("v6_invalid_cas_source_repairs","not_started"));
            check(num(sql,"SELECT COUNT(*) FROM chemical_cas_correction_audit_v6")==2,
                "original malformed CAS values retained only as quarantined source audit");
            check("100-47-0".equals(scalarText(sql,
                "SELECT cas FROM chemicals WHERE uid=715"))&&
                "C7H5N".equals(scalarText(sql,
                "SELECT formula FROM chemicals WHERE uid=715")),
                "NIST Benzonitrile CAS 100-47-0 and formula C7H5N");
            check("65996-93-2".equals(scalarText(sql,
                "SELECT cas FROM chemicals WHERE uid=724"))&&
                num(sql,"SELECT COUNT(*) FROM chemical_cas_correction_audit_v6 "+
                    "WHERE chemical_uid=724 AND original_invalid_cas='65966-93-2' "+
                    "AND source_id='NIOSH_NPG'")==1,
                "OSHA/NIOSH correct coal-tar pitch volatiles CAS without hiding original typo");
            check(num(sql,"SELECT COUNT(*) FROM chemicals WHERE cas='null' "+
                "OR cas='65966-93-2'")==0,
                "invalid CAS strings cannot remain in operative unified identity");
            check(num(sql,"SELECT COUNT(*) FROM chemical_identifiers WHERE "+
                "scheme='CAS' AND value IN ('null','65966-93-2')")==0,
                "malformed CAS cannot appear in chemical identifier search paths");
            int validCasRows=0,invalidCasRows=0;
            try(Cursor cc=sql.rawQuery(
                "SELECT uid,cas FROM chemicals WHERE cas IS NOT NULL AND TRIM(cas)<>''",
                null)){
                while(cc.moveToNext()){
                    if(casChecksumValid(cc.getString(1)))validCasRows++;
                    else invalidCasRows++;
                }
            }
            check(invalidCasRows==0,
                "every nonempty direct CAS passes CAS digit checksum (invalid="+
                invalidCasRows+")");
            out.put("cas_checksum_valid_direct_rows",validCasRows);
            out.put("cas_checksum_invalid_direct_rows",invalidCasRows);
            out.put("cas_correction_idempotence",repaired.summary());

            ChemicalIdentityCategoryV6.Result categoryAudit=
                ChemicalIdentityCategoryV6.ensure(db);
            check(categoryAudit.proposed==9&&categoryAudit.already==9&&
                categoryAudit.tagged==0&&categoryAudit.refused==0,
                "nine distinct non-single-substance categories persist exactly once");
            check(num(sql,"SELECT COUNT(*) FROM chemicals "+
                "WHERE chemical_class IN ('MIXTURE_FUEL','MIXTURE_COMPLEX_AEROSOL',"+
                "'AGGREGATE_VOC_METRIC','AGGREGATE_REACTIVE_GAS_FAMILY',"+
                "'MEASUREMENT_INDEX_NOT_CHEMICAL','MIXTURE_REFRIGERANT')")==9,
                "no fake CAS: all nine conceptual legacy source labels typed");
            check(num(sql,"SELECT COUNT(*) FROM chemicals "+
                "WHERE uid IN (136,137,138,139,140,141,142,143,144) "+
                "AND cas IS NOT NULL AND TRIM(cas)<>''")==0,
                "mixtures, NOx family, TVOC and LEL do not receive fictitious CAS");
            out.put("non_single_substance_classification",categoryAudit.summary());
            ChemicalIdentityEquivalenceV6.Result synonymAudit=
                ChemicalIdentityEquivalenceV6.ensure(db);
            long verifiedSynonyms=num(sql,
                "SELECT COUNT(*) FROM chemical_identity_equivalence_v6");
            check(verifiedSynonyms==synonymAudit.verified&&
                synonymAudit.inserted==0,
                "source qualified CAS synonym crosswalk repeatable without rekeying");
            check(verifiedSynonyms==27,
                "all 27 independently NIOSH-confirmed canonical synonym links present");
            long actualMasterRows=num(sql,"SELECT COUNT(*) FROM chemicals");
            check(actualMasterRows==734,
                "legacy identity count expected 734, actual="+actualMasterRows+
                "; bootstrap NIST="+db.getMeta("v6_chemical_nist_cas_completion","missing")+
                "; additional="+
                scalarText(sql,"SELECT GROUP_CONCAT(uid||':'||name_en||':'||COALESCE(cas,'')) "+
                    "FROM chemicals WHERE uid>734"));
            check(num(sql,"SELECT COUNT(*) FROM chemical_identity_equivalence_v6 "+
                "WHERE alias_uid=104 AND canonical_uid=499 AND source_cas='64-17-5'")==1,
                "Ethanol links to official NIOSH ethyl alcohol authority");
            check(num(sql,"SELECT COUNT(*) FROM chemical_identity_equivalence_v6 "+
                "WHERE alias_uid=64 AND canonical_uid=557 AND source_cas='75-09-2'")==1,
                "Dichloromethane links to official NIOSH methylene chloride authority");
            check(num(sql,"SELECT COUNT(*) FROM chemical_identity_equivalence_v6 "+
                "WHERE alias_uid=123 AND canonical_uid=280 AND source_cas='123-91-1'")==1,
                "1,4-Dioxane maps to one NIOSH Dioxane identity and sources");
            out.put("authoritative_niosh_alias_links",verifiedSynonyms);
            out.put("niosh_alias_idempotence",synonymAudit.summary());
            ChemicalMolecularProjectionV6.Result projection=
                ChemicalMolecularProjectionV6.ensure(db);
            check(projection.afterFormula==projection.beforeFormula&&
                projection.afterMass==projection.beforeMass,
                "restarting official molecular identity projection is idempotent");
            out.put("pubchem_molecular_projection_idempotence",
                projection.summary());
            int totalMassBeforeAliasReplay=(int)num(sql,
                "SELECT COUNT(*) FROM chemicals WHERE molar_mass>0");
            ChemicalAliasMolecularProjectionV6.Result aliasMassReplay=
                ChemicalAliasMolecularProjectionV6.ensure(db);
            check(aliasMassReplay.filled==0&&aliasMassReplay.eligibleAfter==0,
                "verified historical aliases receive PubChem mass at bootstrap, "+
                "not later through an unproven fallback");
            check(aliasMassReplay.totalWithMass==totalMassBeforeAliasReplay,
                "repeating CAS-linked alias mass projection never overwrites stored properties");
            check(num(sql,"SELECT COUNT(*) FROM chemicals WHERE uid IN "+
                "(SELECT alias_uid FROM chemical_identity_equivalence_v6) "+
                "AND molar_mass>0")>0,
                "at least one NIOSH synonym is enriched by PubChem molecular evidence");
            out.put("niosh_pubchem_mass_projection_idempotence",
                aliasMassReplay.summary());
            long peerR0=num(sql,
                "SELECT COUNT(*) FROM sensor_evidence "+
                "WHERE evidence_type='EXTERNAL_PEER_R0_RS' AND chemical_uid IS NULL");
            long disputedPeerR0=num(sql,
                "SELECT COUNT(*) FROM sensor_evidence "+
                "WHERE evidence_type='EXTERNAL_PEER_R0_RS' "+
                "AND status='REJECTED_FOR_AUTOMATIC_CALIBRATION'");
            check(peerR0==25,"all 25 external MQ R0/Rs sensor reference rows imported");
            check(disputedPeerR0==5,
                "disputed peer R0 arithmetic and methodological errors remain flagged");
            check(MqExternalR0RsSeedV14.ensure(this,db)==0,
                "source-qualified research R0/Rs importer idempotent");
            check(num(sql,"SELECT COUNT(*) FROM sensor_evidence "+
                "WHERE evidence_type='EXTERNAL_PEER_R0_RS' AND chemical_uid IS NOT NULL")==0,
                "external R0 stays sensor-instance research, never chemical R0");
            // Android 12 system SQLite does not guarantee JSON1 enabled.
            // Parse source study JSON using the bundled org.json instead.
            try(Cursor q=sql.rawQuery(
                "SELECT coefficients FROM sensor_evidence "+
                "WHERE source_id='CAL-R0-2022-TELKOM' AND sensor_key='MQ135' "+
                "AND evidence_type='EXTERNAL_PEER_R0_RS' LIMIT 1",null)){
                check(q.moveToFirst(),"Telkom peer observation exists by source");
                JSONObject telkom=new JSONObject(q.getString(0));
                check(Math.abs(telkom.getDouble("r0_kohm")-18.59)<1e-5 &&
                    Math.abs(telkom.getDouble("rs_air_kohm")-66.92)<1e-5,
                    "MQ135 external R0=18.59 and Rs_air=66.92 kOhm correctly preserved");
            }
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

            JSONObject coverage=writeCoverageAudit(sql);
            check(coverage.getInt("materials")==materials&&
                coverage.getInt("unique_uids")==materials&&
                coverage.getInt("duplicate_uids")==0,
                "734 rows each have exactly one stable canonical UID");
            check(coverage.getInt("materials_with_molar_mass")>=637,
                "27 NIST-backed masses plus additional NIOSH/CAS-qualified PubChem "+
                "alias enrichments must preserve at least 637 source-backed values");
            check(coverage.getInt("materials_with_cas")==698 &&
                coverage.getInt("materials_with_niosh_cas_equivalence")==27 &&
                coverage.getInt("materials_with_direct_or_linked_cas")==725,
                "direct NIST and NIOSH-linked CAS covers all 725 single-material records");
            check(coverage.getInt("non_single_substance_labels")==9 &&
                coverage.getInt("individual_materials_without_linked_cas")==0,
                "9 non-single-chemical concepts correctly lack a unique CAS");
            check(coverage.getInt("materials_with_name_en")==materials,
                "no unnamed material silently appears as a complete identity");
            check(!coverage.getBoolean("scientific_fields_exhaustive"),
                "identity coverage must never claim complete chemical science");
            long v14Rows=num(sql,"SELECT COUNT(*) FROM v14_unified_chemical_catalog");
            long v14Columns=num(sql,
                "SELECT COUNT(*) FROM pragma_table_info('v14_unified_chemical_catalog')");
            long mappedCurves=num(sql,"SELECT COUNT(*) FROM mq_chemical_reference_curves");
            long unverifiedR0=num(sql,
                "SELECT COUNT(*) FROM mq_chemical_reference_curves "+
                "WHERE reference_r0_definition='UNVERIFIED_SOURCE_CONDITION' "+
                "AND reference_r0_kohm IS NULL AND reference_rs_kohm IS NULL");
            check(v14Rows==materials,"V14 one chemical identity per view row");
            check(v14Columns>=96,"V14 adds actual R0/Rs provenance and naturality metadata");
            check(mappedCurves==49,"all 43 plus six alternate source equations map to one master UID");
            check(unverifiedR0==mappedCurves,
                "no historical manufacturer R0 or Rs has been fabricated as a Tank baseline");
            check(num(sql,"SELECT COUNT(*) FROM mq_device_clean_air_baselines")==0,
                "new Tank database does not invent field clean-air R0 readings");
            check(num(sql,"SELECT COUNT(*) FROM mq_device_clean_air_envelopes")==0,
                "sensor specific normal air confidence requires repeated blanks");
            Map<String,Double> syntheticR0=new LinkedHashMap<>();
            Map<String,Integer> syntheticCounts=new LinkedHashMap<>();
            for(String key:ManualR0Calibration.keys()){
                syntheticR0.put(key,12.0+syntheticR0.size()*3.5);
                syntheticCounts.put(key,15);
            }
            String simulatorDevice="EMULATOR-NOT-A-PHYSICAL-TANK";
            check(ManualR0Calibration.saveFirmware(
                getSharedPreferences("N09_V14_SIMULATED_FIRMWARE_TEST",MODE_PRIVATE),
                syntheticR0,simulatorDevice,syntheticCounts),
                "synthetic firmware NVS data is accepted in disposable test prefs");
            int simulatedBaselineRows=MqDeviceR0LedgerV14.recordFirmwareApproved(
                db,simulatorDevice,syntheticR0,syntheticCounts);
            check(simulatedBaselineRows==8,"all eight confirmed R0 channels recorded atomically");
            check(MqDeviceR0LedgerV14.recordFirmwareApproved(
                db,simulatorDevice,syntheticR0,syntheticCounts)==0,
                "same NVS revision cannot create duplicate R0 records");
            check(num(sql,"SELECT COUNT(*) FROM mq_device_clean_air_baselines")==8,
                "eight sensor-specific approved operational R0 records retained");
            check(num(sql,"SELECT COUNT(*) FROM mq_device_clean_air_baselines "+
                "WHERE device_id='EMULATOR-NOT-A-PHYSICAL-TANK' AND "+
                "window_count=1 AND approved=1")==8,
                "quick firmware baseline is only one window, NOT misreported as approved 3 x 3 min");
            check(num(sql,"SELECT COUNT(*) FROM mq_device_clean_air_envelopes")==0,
                "no fictional empirical clean-air normality band from 8 R0 values");
            // V6 source-specific clean-air Rs mean must be preserved in a
            // DIFFERENT immutable revision; legacy V5 factor-scaled values
            // may not be overwritten or re-labeled after migration.
            Map<String,Integer> simulatedThreeWindowCounts=new LinkedHashMap<>();
            for(String key:ManualR0Calibration.keys())
                simulatedThreeWindowCounts.put(key,45);
            check(ManualR0Calibration.saveFirmware(
                getSharedPreferences("N09_V14_SIMULATED_FIRMWARE_TEST",MODE_PRIVATE),
                syntheticR0,simulatorDevice,simulatedThreeWindowCounts,
                "DEVICE_CLEAN_AIR_RS_MEAN",3),
                "V6 explicit 3-window source definition accepted by phone");
            int threeWindowRows=MqDeviceR0LedgerV14.recordFirmwareApproved(
                db,simulatorDevice,syntheticR0,simulatedThreeWindowCounts);
            check(threeWindowRows==8,"eight V6 3-window Rs-air records stored atomically");

            JSONObject v6Pending=new JSONObject().put("schema",FirmwareR0Contract.V6_SCHEMA)
                .put("state","resting").put("pending",false).put("committed",false)
                .put("warmup_ms",300000).put("duration_ms",180000)
                .put("window_count",3).put("window_index",2)
                .put("r0_definition","DEVICE_CLEAN_AIR_RS_MEAN")
                .put("protocol","V6_5M_WARM_3X3M_MEASURE_2X3M_POWERED_REST");
            JSONObject v6Channels=new JSONObject();
            for(String key:ManualR0Calibration.keys())
                v6Channels.put(key,new JSONObject().put("samples",15)
                    .put("r0",JSONObject.NULL));
            v6Pending.put("channels",v6Channels);
            FirmwareR0Contract.Progress rest=FirmwareR0Contract.progress(v6Pending);
            check(rest.active()&&"resting".equals(rest.state)&&
                rest.windowCount==3&&rest.windowIndex==2,
                "V6 powered-rest phase recognized, with three-window progress");
            JSONObject v6Committed=new JSONObject(v6Pending.toString());
            v6Committed.put("state","ready").put("committed",true);
            for(String key:ManualR0Calibration.keys())
                v6Committed.getJSONObject("channels").getJSONObject(key)
                    .put("samples",45).put("r0",syntheticR0.get(key));
            check(FirmwareR0Contract.committedValues(v6Committed).size()==8,
                "V6 NVS readback requires 8 channels with complete source definition");
            boolean rejectedWrongDefinition=false;
            try{
                JSONObject bad=new JSONObject(v6Committed.toString());
                bad.put("r0_definition","MQ_FACTOR_SCALED_R0_NOT_AIR_RS");
                FirmwareR0Contract.committedValues(bad);
            }catch(IllegalArgumentException expected){rejectedWrongDefinition=true;}
            check(rejectedWrongDefinition,
                "V6 schema cannot smuggle legacy V5 factor-scaled R0 under clean-air label");
            ManualR0Calibration.load(
                getSharedPreferences("N09_V14_SIMULATED_FIRMWARE_TEST",MODE_PRIVATE));
            check("DEVICE_CLEAN_AIR_RS_MEAN".equals(ManualR0Calibration.getDefinition("mq2"))&&
                ManualR0Calibration.getWindowCount("mq2")==3,
                "phone reboot reload preserves explicit V6 clean-air R0 definition");

            check(num(sql,"SELECT COUNT(*) FROM mq_device_clean_air_baselines "+
                "WHERE device_id='EMULATOR-NOT-A-PHYSICAL-TANK' AND "+
                "r0_definition='DEVICE_CLEAN_AIR_RS_MEAN' AND "+
                "window_count=3 AND sample_count=45")==8,
                "explicit V6 new calibration and 3 windows never confused with legacy");
            check(num(sql,"SELECT COUNT(*) FROM mq_device_clean_air_baselines "+
                "WHERE device_id='EMULATOR-NOT-A-PHYSICAL-TANK' AND "+
                "r0_definition='LEGACY_MQ_FACTOR_SCALED_R0_NOT_AIR_RS' AND "+
                "window_count=1")==8,
                "eight old factor-scaled values preserved without redefinition");
            check(num(sql,"SELECT COUNT(*) FROM mq_device_clean_air_baselines")==16,
                "both distinct calibration revisions coexist; no history overwritten");

            check(num(sql,"SELECT COUNT(*) FROM v14_unified_chemical_catalog "+
                "WHERE natural_concentration_coverage_status='NO_QUALIFIED_NATURAL_RANGE'")>700,
                "unknown ambient concentration is NOT labeled naturally zero");
            check(num(sql,"SELECT COUNT(*) FROM v14_unified_chemical_catalog "+
                "WHERE source_r0_kohm_per_chemical IS NOT NULL "+
                "OR tank_r0_kohm_per_chemical IS NOT NULL "+
                "OR tank_rs_kohm_per_chemical IS NOT NULL")==0,
                "sensor R0/Rs not masqueraded as a chemical-identity-wide constant");

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
            check("CAS مرجعي NIOSH: 64-17-5".equals(
                    unifiedRepo.catalogCasLabel(unifiedRepo.byUid(104))),
                "historical ethanol displays qualified NIOSH CAS without modifying its master row");
            check("64-17-5".equals(unifiedRepo.sourceLinkedCas(104)),
                "ethanol CAS sourced from verified NIOSH identity crosswalk");
            check(unifiedRepo.search("64-17-5",100).stream().anyMatch(q->q.uid==104),
                "search by authority CAS also finds the historical ethanol alias");
            check(unifiedRepo.search("Ethyl alcohol",100).stream().anyMatch(q->q.uid==104),
                "search by official NIOSH primary name reaches historical ethanol alias");
            check(unifiedRepo.search("Methanol",100).stream().anyMatch(q->q.uid==103),
                "search by legacy compound name still resolves original UID");
            check(unifiedRepo.sourceLinkedCas(143)==null &&
                "—".equals(unifiedRepo.catalogCasLabel(unifiedRepo.byUid(143))),
                "LEL remains a measurement index, never a fictional CAS chemical");
            check(unifiedRepo.count()==734,
                "source-linked search has not changed stored master chemical count");
            JSONObject hydrogenNist=unifiedRepo.scientificProfile(76)
                .optJSONObject("nist_direct_cas_source");
            check(hydrogenNist!=null&&
                "1333-74-0".equals(hydrogenNist.optString("cas",""))&&
                "NIST_WEBBOOK".equals(hydrogenNist.optString("source_id",""))&&
                hydrogenNist.optString("source_url","")
                    .equals("https://webbook.nist.gov/cgi/cbook.cgi?ID=1333-74-0")&&
                !hydrogenNist.optBoolean("gas_detected",true),
                "per-substance NIST source visible without fabricating Tank detection");
            JSONObject ethanolAlias=unifiedRepo.scientificProfile(104);
            check(ethanolAlias.getJSONArray("niosh_alias_pubchem_mass_sources")
                .length()>0,
                "ethanol historical UID exposes NIOSH CAS-linked PubChem source evidence");
            check(ethanolAlias.getJSONArray("niosh_alias_pubchem_mass_sources")
                .getJSONObject(0).getLong("canonical_uid")==499,
                "transferred ethanol molecular weight retains canonical authority identity");
            check(ethanolAlias.getJSONArray("source_qualified_identity_aliases")
                .length()==1,
                "historical ethanol UID displays one NIOSH source-linked canonical identity");
            check(ethanolAlias.getJSONArray("source_qualified_identity_aliases")
                .getJSONObject(0).getLong("canonical_uid")==499,
                "user sees canonical identity while historical UID stays intact");
            JSONObject benzeneStudyProfile=unifiedRepo.scientificProfile(benzeneUid);
            check(benzeneStudyProfile.getJSONArray("external_mq_r0_rs_sensor_studies")
                .length()>0,"one unified material profile links relevant external sensor context");
            check(benzeneStudyProfile.optBoolean(
                "external_r0_rs_not_material_constants",false),
                "external sensor R0 never becomes a benzene R0 property");

            check(benzeneStudyProfile.getJSONArray("mq_reference_curve_evidence")
                .length()>0,"same master UID exposes each linked MQ source equation");
            check(benzeneStudyProfile.getJSONArray("occupational_exposure_source_limits")
                .length()>0,"source-qualified occupational limits share material UID");
            check(benzeneStudyProfile.getJSONArray("source_qualified_hazard_classifications")
                .length()>0,"GHS/IARC source classification shares material UID");
            check(benzeneStudyProfile.optBoolean("qualified_tank_g3",true)==false &&
                !benzeneStudyProfile.optBoolean("derived_hazard_notifications",true),
                "a catalog evidence record cannot claim current Tank gas detection");
            JSONObject benzeneCurve=benzeneStudyProfile.getJSONArray(
                "mq_reference_curve_evidence").getJSONObject(0);
            check(!benzeneCurve.getBoolean("can_calibrate_tank_r0") &&
                !benzeneCurve.getBoolean("can_certify_gas_ppm"),
                "research curve with unverified source R0 cannot certify Tank readings");
            check(benzeneCurve.isNull("source_r0_kohm") &&
                benzeneCurve.isNull("source_rs_kohm"),
                "unknown source R0/Rs are null, never manufactured numeric zero");
            check("hazrob-tank-unified-chemical-v14-plus-evidence/1".equals(
                benzeneStudyProfile.optString("source_profile_schema","")),
                "one UID-based material profile carries original source evidence");

                        JSONObject oxygenProfile=unifiedRepo.scientificProfile(oxygenUid);
            check("NO_QUALIFIED_NATURAL_RANGE".equals(
                oxygenProfile.optString("natural_concentration_coverage_status","")),
                "descriptive NOAA 20.11-20.947% O2 context cannot be declared a "+
                "source-qualified clean-air safety range");
            check(num(sql,"SELECT COUNT(*) FROM v14_unified_chemical_catalog "+
                "WHERE natural_concentration_coverage_status="+
                "'SOURCE_QUALIFIED_ENVIRONMENTAL_RANGE'")==0,
                "no chemical has a safety-authorized normal-atmosphere range in "+
                "the current dataset");
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
            // N17: The central scientific library publishes capability
            // availability BEFORE download. Android mirrors availability,
            // not a second qualification of fingerprints, PPM or M4.
            // This is a disposable emulator fixture, never production data.
            ChemicalReferenceSyncV6.ensure(sql);
            ChemicalExperimentStatusRepository readiness=
                new ChemicalExperimentStatusRepository(db);
            JSONObject ref1=new JSONObject();
            ref1.put("uid",77).put("cas","74-82-8");
            JSONArray capabilities=new JSONArray();
            String[] kinds={"fingerprint_reference","mq_curve_reference",
                "ppm_reference","m4_reference"};
            for(String capability:kinds){
                capabilities.put(new JSONObject()
                    .put("key",capability)
                    .put("text","N17 test-only central published source")
                    .put("library_status","m4_reference".equals(capability)?
                        "QUALIFIED":"AVAILABLE")
                    .put("source_id","HAZROB_RESEARCH")
                    .put("source_url","https://github.com/HazRob/HazRob-Tank"));
            }
            ref1.put("facts",capabilities);
            byte[] before=ref1.toString().getBytes(
                java.nio.charset.StandardCharsets.UTF_8);
            byte[] h=java.security.MessageDigest.getInstance("SHA-256").digest(before);
            StringBuilder hex=new StringBuilder();
            for(byte b:h)hex.append(String.format(java.util.Locale.US,"%02x",b&0xff));
            ChemicalReferenceSyncV6.applyVerifiedRecord(
                db,before,hex.toString(),"N17-REFERENCE-ONE");
            check(num(sql,"SELECT COUNT(*) FROM chemical_reference_updates_v6 "+
                "WHERE chemical_uid=77")==4,
                "all four published capability flags stored as one record transaction");
            ChemicalExperimentStatusRepository.Row updated=
                readiness.status(unifiedRepo.byUid(77));
            check(updated.referenceFingerprint&&updated.referenceCurve&&
                updated.referencePpm&&updated.referenceM4&&
                updated.publishedM4Qualified,
                "fingerprint/curve/PPM/M4 refresh immediately without scientific reapproval");
            check(updated.m4Mark().contains("مكتبة")&&!updated.m4,
                "library M4 qualification not conflated with device M4 calibration");
            check(unifiedRepo.scientificProfile(77)
                .getJSONArray("queried_reference_updates").length()==4,
                "all four source capabilities appear in material details immediately");
            check(!unifiedRepo.scientificProfile(77)
                .optBoolean("queried_reference_updates_safety_qualified",true),
                "central published reference cannot secretly qualify on-device gas alarms");
            boolean tamperedRejected=false;
            try{
                ChemicalReferenceSyncV6.applyVerifiedRecord(
                    db,(" "+ref1).getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    hex.toString(),"N17-BAD");
            }catch(IllegalArgumentException expected){tamperedRejected=true;}
            check(tamperedRejected&&num(sql,
                "SELECT COUNT(*) FROM chemical_reference_updates_v6 "+
                "WHERE chemical_uid=77")==4,
                "damaged transport is rejected before changing material statuses");
            // A second AVAILABLE publisher cannot downgrade an already QUALIFIED
            // capability: source order must not decide the central status.
            JSONArray multiFacts=new JSONArray();
            for(int i=0;i<capabilities.length();i++)
                multiFacts.put(i==0
                    ?new JSONObject(capabilities.getJSONObject(i).toString())
                        .put("library_status","QUALIFIED")
                    :capabilities.getJSONObject(i));
            multiFacts.put(new JSONObject()
                .put("key","fingerprint_reference")
                .put("text","additional available publisher (test only)")
                .put("source_id","SECONDARY_TEST_SOURCE")
                .put("source_url","https://github.com/HazRob/HazRob-Tank")
                .put("library_status","AVAILABLE"));
            JSONObject refMulti=new JSONObject().put("uid",77).put("cas","74-82-8")
                .put("facts",multiFacts);
            byte[] multiBytes=refMulti.toString().getBytes(
                java.nio.charset.StandardCharsets.UTF_8);
            h=java.security.MessageDigest.getInstance("SHA-256").digest(multiBytes);
            hex=new StringBuilder();
            for(byte b:h)hex.append(String.format(java.util.Locale.US,"%02x",b&0xff));
            ChemicalReferenceSyncV6.applyVerifiedRecord(
                db,multiBytes,hex.toString(),"N17-TWO-SOURCES");
            check(readiness.status(unifiedRepo.byUid(77)).publishedFingerprintQualified &&
                num(sql,"SELECT COUNT(*) FROM chemical_reference_updates_v6 "+
                    "WHERE chemical_uid=77")==5,
                "secondary available source never downgrades qualified fingerprint");
            JSONObject ref2=new JSONObject().put("uid",77).put("cas","74-82-8")
                .put("facts",new JSONArray().put(capabilities.getJSONObject(0)));
            byte[] after=ref2.toString().getBytes(
                java.nio.charset.StandardCharsets.UTF_8);
            h=java.security.MessageDigest.getInstance("SHA-256").digest(after);
            hex=new StringBuilder();
            for(byte b:h)hex.append(String.format(java.util.Locale.US,"%02x",b&0xff));
            ChemicalReferenceSyncV6.applyVerifiedRecord(
                db,after,hex.toString(),"N17-REFERENCE-TWO");
            ChemicalExperimentStatusRepository.Row withdrawn=
                readiness.status(unifiedRepo.byUid(77));
            check(withdrawn.referenceFingerprint&&!withdrawn.referenceCurve&&
                !withdrawn.referencePpm&&!withdrawn.referenceM4&&
                !withdrawn.publishedM4Qualified,
                "withdrawn M4, PPM and curve availability disappear in next query");
            check(num(sql,"SELECT COUNT(*) FROM chemical_reference_updates_v6 "+
                "WHERE chemical_uid=77")==1,
                "per-UID replacement removes withdrawn capabilities, never stale flags");
            // An empty, SHA-verified central record is a legitimate withdrawal
            // of ALL published capabilities for this UID, not a malformed record.
            JSONObject refEmpty=new JSONObject().put("uid",77).put("cas","74-82-8")
                .put("facts",new JSONArray());
            byte[] emptyBytes=refEmpty.toString().getBytes(
                java.nio.charset.StandardCharsets.UTF_8);
            h=java.security.MessageDigest.getInstance("SHA-256").digest(emptyBytes);
            hex=new StringBuilder();
            for(byte b:h)hex.append(String.format(java.util.Locale.US,"%02x",b&0xff));
            ChemicalReferenceSyncV6.applyVerifiedRecord(
                db,emptyBytes,hex.toString(),"N17-ALL-WITHDRAWN");
            ChemicalExperimentStatusRepository.Row allWithdrawn=
                readiness.status(unifiedRepo.byUid(77));
            check(!allWithdrawn.referenceFingerprint&&!allWithdrawn.referenceCurve&&
                !allWithdrawn.referencePpm&&!allWithdrawn.referenceM4 &&
                num(sql,"SELECT COUNT(*) FROM chemical_reference_updates_v6 "+
                    "WHERE chemical_uid=77")==0,
                "empty central publication withdraws all four capabilities");
            check(num(sql,"SELECT COUNT(*) FROM chemicals")==734 &&
                num(sql,"SELECT COUNT(*) FROM chemical_risk_profiles "+
                "WHERE occupational_alarm_authorized!=0")==0,
                "reference sync never changes central identities or gas alarm approval");
            // The historical ethanol name UID104 is tied to NIOSH canonical
            // ethyl-alcohol UID499, without rekeying any past experiment.
            JSONObject aliasPublication=new JSONObject().put("uid",499)
                .put("cas","64-17-5")
                .put("facts",new JSONArray().put(new JSONObject()
                    .put("key","fingerprint_reference")
                    .put("text","N09 test-only publisher state")
                    .put("source_id","HAZROB_RESEARCH")
                    .put("source_url","https://github.com/HazRob/HazRob-Tank")
                    .put("library_status","QUALIFIED")));
            byte[] aliasBytes=aliasPublication.toString().getBytes(
                java.nio.charset.StandardCharsets.UTF_8);
            h=java.security.MessageDigest.getInstance("SHA-256").digest(aliasBytes);
            hex=new StringBuilder();
            for(byte b:h)hex.append(String.format(java.util.Locale.US,"%02x",b&0xff));
            ChemicalReferenceSyncV6.applyVerifiedRecord(
                db,aliasBytes,hex.toString(),"N09-CANONICAL-ETHANOL-SOURCE");
            check(readiness.status(unifiedRepo.byUid(104)).publishedFingerprintQualified &&
                readiness.status(unifiedRepo.byUid(499)).publishedFingerprintQualified,
                "verified historical NIOSH alias sees central published canonical capabilities");
            check(unifiedRepo.scientificProfile(104)
                .getJSONArray("queried_reference_updates").length()==1 &&
                unifiedRepo.scientificProfile(104)
                .getJSONArray("queried_reference_updates").getJSONObject(0)
                .optLong("published_for_uid")==499,
                "historical ethanol material details show original canonical NIOSH source UID");
            check(num(sql,"SELECT COUNT(*) FROM chemicals")==734,
                "synonym status propagation never creates duplicate master records");
            out.put("central_published_capability_sync_e2e",
                "PASS_FOUR_CAPABILITIES_TWO_SOURCES_WITHDRAW_ALL_NO_REQUALIFICATION");
            out.put("material_field_coverage_audit",coverage);
            out.put("status","PASS");
            out.put("sqlite_version",14).put("legacy_rows",original)
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
                .put("ghs_pdf_page_count",1)
                .put("v14_unified_columns",v14Columns)
                .put("external_peer_r0_rs_observations",peerR0)
                .put("external_peer_r0_rs_disputed",disputedPeerR0)
                .put("v14_master_uid_rows",v14Rows)
                .put("v14_linked_source_curves",mappedCurves)
                .put("v14_unverified_source_r0_curves",unverifiedR0)
                .put("v14_actual_tank_r0_samples",0)
                .put("v14_simulator_only_r0_channels",simulatedBaselineRows)
                .put("v14_simulator_only_v6_three_window_r0_channels",threeWindowRows)
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
