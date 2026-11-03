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
 * ONE immutable instant gas-analysis operation. V8 routes through real ChemCore Next.
 *
 * Does not replace Next candidate inference with predecessor ranking.
 * Independent reference-curve algebra is visible as source diagnostics.
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

    /** Real V8 operator path. No silent legacy candidate projection or source R0 transfer. */
    public static Result analyze(android.content.Context androidContext,ExperimentsDb db,
                                 JSONObject live,long timestamp)throws Exception {
        if(androidContext==null||db==null||live==null||timestamp<=0)
            throw new IllegalArgumentException("Android context and original MQ snapshot required");
        final JSONObject frozen=new JSONObject(live.toString());
        if(frozen.optJSONObject("mq")==null)
            throw new IllegalArgumentException("missing original MQ sensor readings");
        final String uid="INSTANT-"+timestamp;
        // Read exact canonical master UID, not a legacy 0..143 source index.
        ChemicalRepository master=new ChemicalRepository(db);
        java.util.List<com.hazrob.tank.chemcore.next.ResearchCandidateGenerator.Material> catalog=
            new java.util.ArrayList<>();
        for(ChemicalRepository.Chemical item:master.all(5000)){
            String kind=item.chemicalClass==null?"":item.chemicalClass.toLowerCase(java.util.Locale.ROOT);
            boolean generic=kind.contains("mixture")||kind.contains("generic")||
                kind.contains("general");
            catalog.add(new com.hazrob.tank.chemcore.next.ResearchCandidateGenerator.Material(
                item.uid,item.label(),generic?
                com.hazrob.tank.chemcore.next.ResearchCandidateGenerator.MaterialKind.GENERAL_CLASS:
                com.hazrob.tank.chemcore.next.ResearchCandidateGenerator.MaterialKind.MOLECULAR_CHEMICAL));
        }
        // Inspect original MQ research sources without claiming that their
        // reference R0/heater/range is equivalent to this Tank's measurements.
        // Qualification.unverified() makes these abstention diagnostics only.
        var researchCurves=com.servoc.app.chemcore.nextintegration
            .NextResearchSourceBindings.abstentionOnlyCurves(
                master,frozen.optString("device_id","").trim());
        com.servoc.app.chemcore.nextintegration.NextEngineRouter.Outcome next=
            com.servoc.app.chemcore.nextintegration.NextEngineRouter.analyzeAndPersist(
                androidContext,db,frozen,uid,uid,timestamp,
                com.hazrob.tank.chemcore.next.CoreContracts.Mode.INSTANT,
                catalog,researchCurves,java.util.List.of(),java.util.List.of(),12);
        JSONObject out=com.servoc.app.chemcore.nextintegration.NextDecisionEnvelope
            .fromReceipt(next.receipt);
        out.put("session_id",uid);
        out.put("source_kind","ONE_FROZEN_INSTANT_SNAPSHOT_NEXT_RESEARCH_ONLY");
        // Source-model estimates are a parallel, provenance-labelled set of
        // mathematical diagnostics, NOT a replacement for the Next decision.
        JSONObject scientific=com.servoc.app.chemcore.nextintegration
            .NextScientificDiagnostics.compute(frozen,master,next.receipt.sourceSnapshot);
        out.put("scientific_source_diagnostics",scientific);
        // Reference thresholds and GHS are sourced from the same unified
        // catalog. Calculated alerts remain conditional research comparisons.
        JSONObject screening=com.servoc.app.chemcore.nextintegration
            .NextReferenceRiskBridge.evaluate(db,scientific);
        out.put("scientific_reference_screening",screening);
        out.put("source_curve_hypotheses",scientific.getJSONArray("all_reference_models"));
        out.put("source_curve_research_diagnostic_ledger",
            scientific.getJSONArray("all_reference_models"));
        out.put("quantitative_source_methods","LOG10_INVERSE_POWER_CURVES_RESEARCH");
        out.put("numerical_source_ppm_available",scientific.optInt("computed_ppm_count",0));
        out.put("fingerprint_model_outputs",new JSONArray());
        out.put("fingerprint_runtime_status",
            "NOT_RUN_NO_SOURCE_BINDINGS; DO_NOT_INVENT_FINGERPRINT_SCORES");
        JSONArray nextExpertReasons=new JSONArray();
        for(String diag:next.next.sourceDiagnostics)nextExpertReasons.put(diag);
        out.put("next_algorithm_diagnostics",nextExpertReasons);
        out.put("next_models_inspected",next.next.modelsInspected);
        out.put("next_curve_qualification","ABSTENTION_ONLY_NO_SOURCE_R0_OR_G3");
        out.put("next_curve_source_count",researchCurves.size());
        out.put("next_positive_models",next.next.positiveModels);
        out.put("legacy_inference_fallback_used",false);
        out.put("candidate_order_source","PERSISTED_NEXT_DECISION_ONLY");
        out.put("source_model_linkage_status",
            "NEXT_IDENTITY_BINDINGS_PENDING; ALL_VALID_SOURCE_NUMERICS_VISIBLE_SEPARATELY");
        out.put("ratio_ready_mq_channels",next.original.validSourceMeasurements);
        out.put("raw_only_mq_channels",next.original.rawOnlyChannels);
        out.put("observed_channels",next.original.validSourceMeasurements);
        out.put("single_snapshot_no_temporal_series",true);
        out.put("ppm_reportable",false);
        out.put("scientific_ppm_values_visible",true);
        out.put("operator_approved_field_session",false);
        out.put("no_firmware_heater_changes",true);
        out.put("gas_food_isolated",true);
        return new Result(uid,timestamp,frozen,out,next.original.validSourceMeasurements);
    }

    /** V6 PREDECESSOR COMPARISON ONLY, never automatic fallback from Next. */
    public static Result analyzeV6Comparison(android.content.Context androidContext,
                                 ExperimentsDb db,JSONObject live,long timestamp)throws Exception {
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
        JSONObject out=TankV6TelemetryAdapter.toJson(analyzed,frozen);
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
