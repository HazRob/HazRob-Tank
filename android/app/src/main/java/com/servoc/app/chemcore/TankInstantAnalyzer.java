package com.servoc.app.chemcore;

import com.servoc.app.analytics.AnalyticsSnapshot;
import com.servoc.app.calibration.AcceptedR0Normalizer;
import com.servoc.app.calibration.SqliteCalibrationProvider;
import com.servoc.app.chemical.ChemicalRepository;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.hardware.HazRobHardwareProfile;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One immutable in-memory Tank measurement operation, NEVER a field-site session. */
public final class TankInstantAnalyzer {
    private TankInstantAnalyzer(){}
    public static final class Result {
        public final String id;
        public final JSONObject original,analysis;
        public final long capturedAtMs;
        public final int channels;
        Result(String id,long at,JSONObject source,JSONObject computed,int channels){
            this.id=id;this.capturedAtMs=at;this.original=source;this.analysis=computed;this.channels=channels;
        }
    }
    public static Result analyze(ExperimentsDb db,JSONObject live,long timestamp)throws Exception{
        if(db==null||live==null)throw new IllegalArgumentException("Live reading required");
        // Copy first: subsequent telemetry and new saved R0 must never alter this operation.
        JSONObject frozen=new JSONObject(live.toString());
        JSONObject mq=frozen.optJSONObject("mq");
        Map<String,Double> ratios=new LinkedHashMap<>();
        for(HazRobHardwareProfile.Channel channel:HazRobHardwareProfile.MQ){
            JSONObject value=mq==null?null:mq.optJSONObject(channel.key);
            if(value==null||!value.optBoolean("ok",value.optBoolean("valid",false))||
                    value.has("powered")&&!value.optBoolean("powered",false))continue;
            double normalized=AcceptedR0Normalizer.recordedRatio(channel.key,value);
            if(Double.isFinite(normalized)&&normalized>0)ratios.put(channel.key,normalized);
        }
        if(ratios.isEmpty())throw new IllegalStateException("No valid measured MQ/R0 ratios");
        final String id="INSTANT-"+Math.max(1,timestamp);
        JSONObject output;
        try{
            JSONObject env=frozen.optJSONObject("env");
            double temp=env==null?Double.NaN:env.optDouble("temperature",Double.NaN);
            double rh=env==null?Double.NaN:env.optDouble("humidity",Double.NaN);
            SamplingProfile profile=SamplingProfile.hazRobStandardChamberV2();
            ChemCoreInferenceEngine.Context context=new ChemCoreInferenceEngine.Context(
                id,profile.profileId,"tank-v4-instant-screening/1",temp,rh,null,false);
            List<InverseMolecularSolver.Candidate> refs=ChemCoreLegacyCandidateRegistry.acceptedReferenceCandidates();
            ChemCoreResultEnvelope inferred=ChemCoreInferenceEngine.analyze(ratios,refs,profile,context);
            output=inferred==null?new JSONObject():inferred.toJson();
        }catch(Exception ex){
            // A solver failure should be visible, while still allowing other scientific methods
            // to return evidence. A failed solver is never presented as a successful inference.
            output=new JSONObject().put("solver_error",ex.getClass().getSimpleName());
        }
        // This existing independent fingerprint/curve evidence path is retained.
        JSONObject legacy=new JSONObject(AnalyticsSnapshot.toJson(frozen,
            new ChemicalRepository(db),new SqliteCalibrationProvider(db),false));
        output.put("legacy_fingerprint_curve_analysis",legacy);
        JSONArray primary=output.optJSONArray("candidates");
        if(primary==null||primary.length()==0){
            JSONArray alternatives=new JSONArray(),raw=legacy.optJSONArray("results");
            if(raw!=null)for(int i=0;i<Math.min(20,raw.length());i++){
                JSONObject r=raw.optJSONObject(i);if(r==null)continue;
                JSONObject a=new JSONObject();
                a.put("rank",alternatives.length()+1);
                a.put("chemical_uid",r.optLong("chemical_uid",-1));
                a.put("label",r.optString("name_ar",r.optString("name","—")));
                double vcandidatescorepct=r.optDouble("candidate_score_pct",Double.NaN);
                if(Double.isFinite(vcandidatescorepct))a.put("candidate_score_pct",vcandidatescorepct);
                double vcoveragepct=r.optDouble("coverage_pct",Double.NaN);
                if(Double.isFinite(vcoveragepct))a.put("coverage_pct",vcoveragepct);
                double vagreementpct=r.optDouble("agreement",Double.NaN);
                if(Double.isFinite(vagreementpct))a.put("agreement_pct",vagreementpct);
                double vscreeningfitppm=r.optDouble("ppm",Double.NaN);
                if(Double.isFinite(vscreeningfitppm))a.put("screening_fit_ppm",vscreeningfitppm);
                a.put("evidence_tier","FINGERPRINT_CURVE_SCREENING");
                alternatives.put(a);
            }
            output.put("candidates",alternatives);
            output.put("candidate_count",alternatives.length());
            output.put("evidence_tier","SCREENING_ONLY");
        }
        // Snapshot local authoritative OSHA/NIOSH/etc. reference facts for ranked hypotheses.
        // Display does not depend on M4 and never constitutes an exposure-limit exceedance.
        output.put(TankExposureReferenceCatalog.KEY,
            TankExposureReferenceCatalog.forAnalysis(db,output));
        output.put("session_id",id);
        output.put("source_kind","ONE_SHOT_IN_MEMORY_NO_HISTORY");
        output.put("captured_utc_ms",timestamp);
        output.put("observed_channels",ratios.size());
        output.put("single_snapshot_no_temporal_series",true);
        output.put("ppm_reportable",false); // one snapshot is never a certified exposure value
        output.put("operator_approved_field_session",false);
        return new Result(id,timestamp,frozen,output,ratios.size());
    }
}
