package com.servoc.app.chemcore;

import com.hazrob.tank.v6integration.TankV6TelemetryAdapter;
import com.hazrob.tank.chemcore.v6.ChemCoreTankV6;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.chemical.ChemicalRepository;
import com.servoc.app.chemical.GhsPictograms;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import com.servoc.app.hardware.HazRobHardwareProfile;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * ONE immutable instant gas-analysis operation. V6-only for new operations.
 *
 * Intentionally does NOT import/invoke predecessor inference, Stable legacy
 * screeners, food-freshness profiles, or prior AnalyticsSnapshot fallback.
 * All source fits are hypotheses; no Tank physical G3 calibrated identity/ppm.
 * Historical V5 session data and field approvals are not modified.
 */
public final class TankInstantAnalyzer {
    private TankInstantAnalyzer(){}

    public static final class Result {
        public final String id;
        public final JSONObject original,analysis;
        public final long capturedAtMs;
        public final int channels;
        Result(String id,long at,JSONObject source,JSONObject computed,int channels) {
            this.id=id;this.capturedAtMs=at;original=source;
            analysis=computed;this.channels=channels;
        }
    }

    public static Result analyze(ExperimentsDb db,JSONObject live,long timestamp)throws Exception {
        return analyze(null,db,live,timestamp);
    }

    public static Result analyze(android.content.Context androidContext,ExperimentsDb db,
                                 JSONObject live,long timestamp)throws Exception {
        if(live==null||db==null||timestamp<=0)
            throw new IllegalArgumentException("Immutable original gas snapshot required");
        // Freeze first: any later telemetry or R0 updates must not change this
        // operation, and must not create screenshot history or a field session.
        final JSONObject frozen=new JSONObject(live.toString());
        final JSONObject mq=frozen.optJSONObject("mq");
        if(mq==null)throw new IllegalArgumentException("Missing MQ source observations");
        int valid=0;
        for(HazRobHardwareProfile.Channel channel:HazRobHardwareProfile.MQ) {
            JSONObject observation=mq.optJSONObject(channel.key);
            if(observation==null)continue;
            boolean hardwareOk=observation.optBoolean("ok",
                                             observation.optBoolean("valid",false));
            boolean powered=!observation.has("powered")||
                              observation.optBoolean("powered",false);
            double rs=observation.optDouble("rs",Double.NaN);
            if(hardwareOk&&powered&&Double.isFinite(rs)&&rs>0)valid++;
        }
        if(valid==0)throw new IllegalStateException("No eligible original sensor Rs");
        // Sole chemical inference path. Exceptions intentionally propagate,
        // never replaced by old ChemCore/legacy analytics and never certified.
        ChemCoreTankV6.Result analyzed=TankV6TelemetryAdapter.analyzeSnapshot(frozen);
        JSONObject out=TankV6TelemetryAdapter.toJson(analyzed);
        // Candidate list is a *compatibility display projection* only, derived
        // from the independent new V6 source curve hypothesis ledger.
        JSONArray source=out.optJSONArray("source_curve_hypotheses");
        JSONArray candidates=new JSONArray();
        ChemicalRepository master=new ChemicalRepository(db);
        Map<Long,List<String>> ghs=GhsPictograms.allQualified(db);
        // The V6 source-catalog chemical_reference_id is a 0-based legacy
        // chemical index, NOT the SQLite chemical UID. Never compare/join them
        // as identical integers: resolve explicitly through legacy_id.
        //
        // Several curves of one candidate must not generate duplicate material
        // choices in tracking, candidate lists, lab, or downstream reports.
        Map<Long,JSONObject> distinct=new LinkedHashMap<>();
        if(source!=null)for(int i=0;i<source.length();i++){
            JSONObject curve=source.optJSONObject(i);
            if(curve==null)continue;
            long legacyId=curve.optLong("chemical_reference_id",-1);
            ChemicalRepository.Chemical chemical=legacyId>=0&&legacyId<144?
                master.byLegacyId((int)legacyId):null;
            // Unmapped class/source labels are still retained in the
            // source-curve research ledger, but never become fake master UIDs.
            if(chemical==null)continue;
            JSONObject item=distinct.get(chemical.uid);
            if(item==null){
                item=new JSONObject();
                item.put("rank",distinct.size()+1);
                item.put("chemical_uid",chemical.uid);
                item.put("legacy_id",legacyId);
                item.put("label",chemical.label());
                item.put("cas",chemical.cas==null?JSONObject.NULL:chemical.cas);
                // Source-qualified known-material hazard only, NEVER evidence
                // of identifying the gas from the current MQ snapshot.
                item.put("ghs_pictograms",GhsPictograms.asJson(ghs.get(chemical.uid)));
                item.put("ghs_pictogram_scope","CLASSIFICATION_IF_IDENTITY_CONFIRMED");
                item.put("ghs_is_detection_evidence",false);
                item.put("unified_catalog_identity_resolved",true);
                item.put("source_label_not_proof_of_gas_identity",true);
                item.put("research_only",true);
                item.put("reported_identity","UNKNOWN");
                item.put("evidence_tier","SOURCE_CURVE_RESEARCH_UNVALIDATED");
                item.put("source_curve_count",0);
                item.put("source_curve_ids",new JSONArray());
                item.put("source_roots",new JSONArray());
                distinct.put(chemical.uid,item);
            }
            item.put("source_curve_count",item.getInt("source_curve_count")+1);
            item.getJSONArray("source_curve_ids").put(
                curve.optString("source_curve_id",""));
            // Preserve traceability to source labels without silently
            // promoting a generic gas class to a certified molecule.
            item.getJSONArray("source_roots").put(curve.optString("source_root",""));
        }
        for(JSONObject candidate:distinct.values())candidates.put(candidate);
        out.put("unresolved_research_curves_preserved",
            Math.max(0,(source==null?0:source.length())-
                countLinkedSourceCurves(distinct)));
        String id="INSTANT-"+timestamp;
        out.put("candidates",candidates);
        out.put("candidate_count",candidates.length());
        out.put("source_kind","ONE_SHOT_IN_MEMORY_NO_HISTORY");
        out.put("session_id",id);
        out.put("captured_utc_ms",timestamp);
        out.put("observed_channels",valid);
        out.put("single_snapshot_no_temporal_series",true);
        out.put("reported_identity","UNKNOWN");
        out.put("open_set_status","N05H_SOURCE_UNKNOWN_GATE_FAILED_NO_TANK_G3");
        out.put("unknown_candidate_retained",true);
        out.put("evidence_tier","V6_SOURCE_ONLY_NOT_VALIDATED");
        out.put("model_version","ChemCore Tank V6 independent source engine");
        out.put("ppm_reportable",false);
        out.put("operator_approved_field_session",false);
        out.put("legacy_inference_fallback_used",false);
        out.put("report_research_only",true);
        out.put("no_firmware_heater_changes",true);
        out.put("gas_food_isolated",true);
        return new Result(id,timestamp,frozen,out,valid);
    }
    private static int countLinkedSourceCurves(Map<Long,JSONObject> rows){
        int count=0;
        for(JSONObject row:rows.values())
            count+=row.optInt("source_curve_count",0);
        return count;
    }

}
