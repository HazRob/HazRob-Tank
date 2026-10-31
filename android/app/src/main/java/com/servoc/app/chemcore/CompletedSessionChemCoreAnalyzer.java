package com.servoc.app.chemcore;

import android.content.Context;
import android.database.Cursor;
import com.servoc.app.calibration.AcceptedR0Normalizer;
import com.servoc.app.chemcore.library.ChemCoreLibraryStore;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.hardware.HazRobHardwareProfile;

import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts one completed ordinary HR069 measurement into a persisted ChemCore screening decision.
 *
 * This bridge deliberately keeps candidate ranking separate from identity acceptance. A normal
 * field measurement may produce ranked screening hypotheses, but it has no explicit open-set/OOD
 * acceptance and no M4 reference-gas validation. Therefore this class never manufactures a named
 * identity or reportable ppm merely because the MQ array responded strongly.
 */
public final class CompletedSessionChemCoreAnalyzer {
    public static final String MODEL_VERSION="chemcore-completed-session-screening/2-stable";
    private CompletedSessionChemCoreAnalyzer(){}

    public static final class Outcome {
        public final long decisionRowId;
        public final int observedChannels,candidateCount;
        public final ChemCoreResultEnvelope result;
        Outcome(long row,int observed,int candidates,ChemCoreResultEnvelope result){
            this.decisionRowId=row;this.observedChannels=observed;this.candidateCount=candidates;this.result=result;
        }
        public boolean persisted(){return decisionRowId>=0;}
    }

    /** Analyze and persist a completed firmware measurement session. */
    public static Outcome analyzeAndPersist(ExperimentsDb db,String sessionId,JSONObject terminalSnapshot,long tMs){
        return analyzeAndPersist(null,db,sessionId,terminalSnapshot,tMs);
    }
    /** Use the installed, SHA-verified Stable payload if Android context is available. */
    public static Outcome analyzeAndPersist(Context appContext,ExperimentsDb db,String sessionId,
                                            JSONObject terminalSnapshot,long tMs){
        return analyzeAndPersist(appContext,db,sessionId,terminalSnapshot,tMs,false);
    }

    /** A documented field capture derives its hypothesis from ALL archived 60-second samples. */
    public static Outcome analyzeAndPersist(Context appContext,ExperimentsDb db,String sessionId,
                                            JSONObject terminalSnapshot,long tMs,boolean fieldCapture){
        if(db==null||sessionId==null||sessionId.trim().isEmpty()||terminalSnapshot==null)
            return new Outcome(-1,0,0,null);
        String id=sessionId.trim();
        ChemCoreDecisionRepository decisions=new ChemCoreDecisionRepository(db);
        JSONObject existing=decisions.latestPayloadForSession(id);
        if(existing!=null){
            int count=existing.optInt("candidate_count",existing.optJSONArray("candidates")==null?0:existing.optJSONArray("candidates").length());
            return new Outcome(-2,0,count,null);
        }

        Map<String,Double> ratios=fieldCapture?
            observedRatiosAcrossFieldSamples(db,id):observedRatios(terminalSnapshot);
        // Field sessions must never silently fall back to a single terminal snapshot.
        // A REAL raw-MQ field capture remains a valid documented observation even
        // when clean-air R0 has not yet been calibrated. Persist an honest M0
        // INSUFFICIENT_EVIDENCE decision: no identity, candidates, or ppm. The
        // independent FieldCycleRepository still validates real sensor samples,
        // duration, freshness and approval before accepting the measurement.
        if(ratios.isEmpty()&&fieldCapture){
            ChemCoreResultEnvelope insufficient=new ChemCoreResultEnvelope(
                id,ChemCoreDecision.Outcome.INSUFFICIENT_EVIDENCE,
                ChemCoreDecision.EvidenceTier.M0_CATALOG_ONLY,
                null,"",Double.NaN,Double.NaN,Double.NaN,Double.NaN,
                Double.NaN,Double.NaN,
                SamplingProfile.hazRobStandardChamberV2().profileId,
                ChemCoreFeaturePipeline.SCHEMA_VERSION,MODEL_VERSION,
                "NO_VALID_RECORDED_R0_RATIOS; preserved raw MQ source only; no gas identity or ppm",
                Collections.emptyList());
            try{
                JSONObject quality=new JSONObject();
                quality.put("state","RAW_MQ_CAPTURE_NO_VALID_R0");
                quality.put("valid_r0_normalized_channels",0);
                quality.put("field_60s_capture",true);
                quality.put("candidate_ranking_permitted",false);
                quality.put("named_identity_permitted",false);
                quality.put("ppm_reportable",false);
                quality.put("requires_r0_before_gas_analysis",true);
                insufficient=insufficient.withLabQuality(quality);
            }catch(Exception ignored){}
            long row=decisions.save(Math.max(0,tMs),insufficient);
            return new Outcome(row,0,0,insufficient);
        }
        if(ratios.isEmpty())return new Outcome(-1,0,0,null);

        ChemCoreSessionFeatureAdapter.Extraction extraction=null;
        try{extraction=ChemCoreSessionFeatureAdapter.extract(db,id);}catch(Exception ignored){}

        JSONObject env=terminalSnapshot.optJSONObject("env");
        double temperature=env==null?Double.NaN:finite(env,"temperature","temperature_c","temp_c");
        double rh=env==null?Double.NaN:finite(env,"humidity","humidity_rh","rh");
        if(extraction!=null){
            if(!Double.isFinite(temperature)&&Double.isFinite(extraction.meanTemperatureC))temperature=extraction.meanTemperatureC;
            if(!Double.isFinite(rh)&&Double.isFinite(extraction.meanRh))rh=extraction.meanRh;
        }

        SamplingProfile profile=SamplingProfile.hazRobStandardChamberV2();
        TankStableScreeningModelRegistry.LoadResult loaded=
            TankStableRuntimeCatalog.responseModels(appContext,db);
        String modelVersion=MODEL_VERSION+
            (loaded.status.startsWith("ACTIVE_STABLE")?":"+loaded.version:":builtin");
        ChemCoreInferenceEngine.Context inferenceContext=new ChemCoreInferenceEngine.Context(
                id,profile.profileId,modelVersion,temperature,rh,null,false);
        List<InverseMolecularSolver.Candidate> library=loaded.candidates;
        ChemCoreResultEnvelope result=ChemCoreInferenceEngine.analyze(
            ratios,library,profile,inferenceContext);
        if(loaded.status.startsWith("ACTIVE_STABLE")&&appContext!=null){
            ChemCoreLibraryStore store=new ChemCoreLibraryStore(appContext.getApplicationContext());
            result=result.withLibraryProvenance(loaded.version,loaded.sha256,store.activeSourceCommit());
        }

        try{
            JSONObject quality=new JSONObject();quality.put("state","FIELD_SCREENING");
            quality.put("valid_mq_channels",ratios.size());quality.put("r0_normalized_channels",ratios.size());
            quality.put("reference_validated_channels",0);quality.put("m4_quantitative_gate",false);
            quality.put("ratio_aggregation_method",fieldCapture?
                "60S_MEDIAN_DISTINCT_HARDWARE_SCAN_CYCLE":"TERMINAL_FIRMWARE_AVERAGE_OR_LIVE");
            quality.put("field_60s_capture",fieldCapture);
            quality.put("candidate_ranking_permitted",true);quality.put("named_identity_requires_open_set_acceptance",true);
            quality.put(TankFingerprintScreening.KEY,TankFingerprintScreening.evaluate(ratios));
            quality.put("active_stable_status",loaded.status);
            quality.put("stable_imported_response_models",loaded.importedModels);
            quality.put("stable_rejected_response_models",loaded.rejectedModels);
            quality.put("stable_sha256",loaded.sha256);
            quality.put("stable_version",loaded.version);
            quality.put("research_log_curves_enabled",loaded.importedModels>0);
            quality.put("concentration_estimate_is_device_validated",false);
            result=result.withLabQuality(quality);
        }catch(Exception ignored){}

        long row=decisions.save(Math.max(0,tMs),result);
        return new Outcome(row,ratios.size(),result==null?0:result.candidates.size(),result);
    }

    /**
     * Calculates the per-sensor median of distinct firmware MQ scan cycles from
     * the documented field capture. A 200ms Android poll may read the SAME MQ
     * scan repeatedly; it must not create fake independent evidence.
     *
     * Uses R0/ratio FROZEN with each saved sample, never the user's current R0.
     * No single-UDP/HTTP snapshot fallback is allowed for field acquisition.
     */
    public static Map<String,Double> observedRatiosAcrossFieldSamples(
            ExperimentsDb db,String sessionId){
        Map<String,Double> out=new LinkedHashMap<>();
        if(db==null||sessionId==null||sessionId.trim().isEmpty())return out;
        Map<String,java.util.Map<Long,Double>> perSensor=new LinkedHashMap<>();
        for(HazRobHardwareProfile.Channel ch:HazRobHardwareProfile.MQ)
            perSensor.put(ch.key,new LinkedHashMap<>());
        try(Cursor c=db.getReadableDatabase().rawQuery(
                "SELECT mq_payload FROM telemetry_samples WHERE session_id=? ORDER BY t_ms,id",
                new String[]{sessionId.trim()})){
            while(c.moveToNext()){
                JSONObject mq;
                try{mq=new JSONObject(c.getString(0));}
                catch(Exception invalid){continue;}
                for(HazRobHardwareProfile.Channel ch:HazRobHardwareProfile.MQ){
                    JSONObject q=mq.optJSONObject(ch.key);
                    if(q==null||!q.optBoolean("ok",q.optBoolean("valid",false))||
                        !q.optBoolean("powered",false)||
                        !q.has("scan_cycle"))continue;
                    long scan=q.optLong("scan_cycle",-1);
                    if(scan<0)continue;
                    double ratio=AcceptedR0Normalizer.recordedRatio(ch.key,q);
                    if(Double.isFinite(ratio)&&ratio>0)
                        perSensor.get(ch.key).putIfAbsent(scan,ratio);
                }
            }
        }
        for(Map.Entry<String,java.util.Map<Long,Double>> entry:perSensor.entrySet()){
            java.util.List<Double> values=new ArrayList<>(entry.getValue().values());
            if(values.size()<10)continue; // no synthetic signal from repeated HTTP polls
            Collections.sort(values);
            int n=values.size();
            double median=n%2==1?values.get(n/2):
                (values.get(n/2-1)+values.get(n/2))/2.0;
            if(Double.isFinite(median)&&median>0)out.put(entry.getKey(),median);
        }
        return out;
    }

    /**
     * Prefer the completed firmware average; fall back channel-by-channel to the current live array.
     * Only ratios frozen with the completed operation enter the primary solver; a newer phone R0 must never rewrite past results.
     */
    public static Map<String,Double> observedRatios(JSONObject snapshot){
        Map<String,Double> out=new LinkedHashMap<>();if(snapshot==null)return out;
        JSONObject measurement=snapshot.optJSONObject("measurement");
        JSONObject averaged=measurement==null?null:measurement.optJSONObject("mq");
        JSONObject live=snapshot.optJSONObject("mq");
        for(HazRobHardwareProfile.Channel hw:HazRobHardwareProfile.MQ){
            JSONObject q=averaged==null?null:averaged.optJSONObject(hw.key);
            double ratio=AcceptedR0Normalizer.recordedRatio(hw.key,q);
            if(!finitePositive(ratio)){
                q=live==null?null:live.optJSONObject(hw.key);ratio=AcceptedR0Normalizer.recordedRatio(hw.key,q);
            }
            if(finitePositive(ratio))out.put(hw.key,ratio);
        }
        return out;
    }

    private static double finite(JSONObject o,String...keys){
        if(o==null)return Double.NaN;for(String key:keys){if(o.isNull(key))continue;double v=o.optDouble(key,Double.NaN);if(Double.isFinite(v))return v;}return Double.NaN;
    }
    private static boolean finitePositive(double v){return Double.isFinite(v)&&v>0;}
}
