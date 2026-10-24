package com.servoc.app.chemcore;

import android.content.Context;
import android.database.Cursor;

import com.servoc.app.chemcore.library.ChemCoreLibraryStore;
import com.servoc.app.data.ExperimentsDb;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * V4 Tank-only bridge: turn the ACTIVE, content-verified ChemCore Stable LOG10_LINEAR
 * reference priors into actual screening models. Stable M1/M2 curves produce candidate
 * fitted ppm without any M4 requirement, but NEVER confer device validation, a
 * reportable occupational ppm, or permission to change the regulatory compliance gate.
 *
 * Reconcile legacy_id against CURRENT SQLite uid (never assume uid=legacy_id+1).
 * Prevent duplicate contributions by replacing older built-in models for stable sensors.
 * Two stable priors for one sensor are permitted only with DISJOINT ppm domains.
 * A corrupt/unsupported/unavailable library falls back to existing source-backed models,
 * with an explicit provenance status in the resulting instant analysis.
 */
public final class TankStableScreeningModelRegistry {
    private static final int MAX_BYTES=16*1024*1024,MAX_PRIORS=4000;
    private static final Set<String> MQ=Set.of("mq2","mq3","mq4","mq5","mq7","mq8","mq9","mq135");
    private TankStableScreeningModelRegistry(){}

    public static final class LoadResult {
        public final List<InverseMolecularSolver.Candidate> candidates;
        public final String status,version,sha256;
        public final int importedModels,rejectedModels;
        LoadResult(List<InverseMolecularSolver.Candidate> items,String status,String version,String hash,int accepted,int rejected){
            this.candidates=items;this.status=status;this.version=version;this.sha256=hash;
            this.importedModels=accepted;this.rejectedModels=rejected;
        }
    }

    private static final class StableModel implements ChemCoreResponseModel {
        final String sensor,version,source,note;
        final double intercept,slope,sigma,min,max;
        final ChemCoreDecision.EvidenceTier tier;
        StableModel(JSONObject q){
            sensor=q.optString("sensor_key","").trim().toLowerCase(Locale.US);
            version=q.optString("model_version","stable-prior");
            source=q.optString("source_id","").trim();
            note=q.optString("note","");
            intercept=q.optDouble("intercept",Double.NaN);slope=q.optDouble("slope",Double.NaN);
            sigma=q.optDouble("sigma_log10",Double.NaN);
            min=q.optDouble("min_ppm",Double.NaN);max=q.optDouble("max_ppm",Double.NaN);
            String level=q.optString("evidence_tier","");
            tier="M2".equals(level)?ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED:
                 ChemCoreDecision.EvidenceTier.M1_MOLECULARLY_INFERRED;
        }
        boolean valid(JSONObject q){
            String e=q.optString("evidence_tier","");
            return MQ.contains(sensor)&&("M1".equals(e)||"M2".equals(e))&&
                "LOG10_LINEAR".equals(q.optString("form",""))&&!q.optBoolean("device_validated",false)&&
                !source.isEmpty()&&Double.isFinite(intercept)&&Double.isFinite(slope)&&Math.abs(slope)>1e-9&&
                Double.isFinite(sigma)&&sigma>0&&Double.isFinite(min)&&min>0&&
                Double.isFinite(max)&&max>min;
        }
        @Override public String sensorKey(){return sensor;}
        @Override public String version(){return "tank-stable-reference/1:"+version;}
        @Override public String sourceId(){return source;}
        @Override public String modelId(){return version()+":"+sensor+":"+source;}
        @Override public String evidenceFamilyId(){return "STABLE_NON_M4_REFERENCE_PRIOR";}
        @Override public ChemCoreDecision.EvidenceTier evidenceTier(){return tier;}
        @Override public double minPpm(){return min;}
        @Override public double maxPpm(){return max;}
        @Override public boolean reportableQuantitative(){return false;}
        @Override public ChemCoreEvidenceObservation.Type evidenceType(){
            return tier==ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED
                ?ChemCoreEvidenceObservation.Type.LITERATURE_CURVE:ChemCoreEvidenceObservation.Type.RESEARCH_CURVE;
        }
        @Override public Estimate predict(double ppm,double temp,double rh,SamplingProfile profile){
            if(!Double.isFinite(ppm)||ppm<min||ppm>max)
                return new Estimate(false,Double.NaN,sigma,false,"outside declared Stable model ppm domain");
            double logRatio=intercept+slope*Math.log10(ppm);
            if(!Double.isFinite(logRatio))return new Estimate(false,Double.NaN,sigma,false,"invalid Stable response");
            return new Estimate(true,logRatio,sigma,true,
                "Verified Stable source "+source+"; device-specific calibration NOT claimed; "+note,
                evidenceType(),evidenceFamilyId(),modelId(),sourceId(),independenceGroup());
        }
    }

    public static LoadResult load(Context context,ExperimentsDb db,List<InverseMolecularSolver.Candidate> builtIn){
        List<InverseMolecularSolver.Candidate> base=builtIn==null?new ArrayList<>():new ArrayList<>(builtIn);
        if(context==null||db==null)return new LoadResult(base,"STABLE_CONTEXT_UNAVAILABLE_USE_BUILTIN","","",0,0);
        String version="",hash="";
        try{
            ChemCoreLibraryStore store=new ChemCoreLibraryStore(context);
            File snapshot=store.activePayload();
            version=store.activeVersion();hash=store.activeSha256();
            if(snapshot==null||version.isEmpty())return new LoadResult(base,"NO_ACTIVE_STABLE_USE_BUILTIN",version,hash,0,0);
            if(hash.length()!=64||snapshot.length()<1||snapshot.length()>MAX_BYTES)
                return new LoadResult(base,"STABLE_SIZE_OR_HASH_UNTRUSTED_USE_BUILTIN",version,hash,0,0);
            byte[] raw=readBounded(snapshot);
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            StringBuilder hex=new StringBuilder();for(byte k:digest.digest(raw))hex.append(String.format(Locale.US,"%02x",k&0xff));
            if(!hash.equalsIgnoreCase(hex.toString()))
                return new LoadResult(base,"STABLE_SHA256_MISMATCH_USE_BUILTIN",version,hash,0,0);
            JSONObject root=new JSONObject(new String(raw,StandardCharsets.UTF_8));
            if(!"chemcore-stable-snapshot-v1".equals(root.optString("schema","")))
                return new LoadResult(base,"UNSUPPORTED_STABLE_SCHEMA_USE_BUILTIN",version,hash,0,0);
            JSONObject knowledge=root.optJSONObject("knowledge");
            JSONObject priors=knowledge==null?null:knowledge.optJSONObject("response_priors");
            JSONArray records=priors==null?null:priors.optJSONArray("records");
            JSONObject counts=root.optJSONObject("content_counts");
            if(records==null||counts==null||records.length()<1||records.length()>MAX_PRIORS||
                records.length()!=counts.optInt("response_priors",-1)||
                records.length()!=priors.optInt("record_count",-1))
                return new LoadResult(base,"STABLE_PRIORS_INCOMPLETE_USE_BUILTIN",version,hash,0,0);

            Map<Integer,Long> byLegacy=canonicalUidMap(db);
            Map<Long,List<ChemCoreResponseModel>> stableModels=new LinkedHashMap<>();
            Map<Long,Set<String>> stableSensorKeys=new HashMap<>();
            Map<Long,String> labels=new HashMap<>();
            Map<Long,Integer> minimums=new HashMap<>();
            int accepted=0,rejected=0;
            for(int i=0;i<records.length();i++){
                JSONObject rec=records.optJSONObject(i);
                if(rec==null){rejected++;continue;}
                long legacy=rec.optLong("legacy_id",-1);
                Long uid=legacy>=0&&legacy<Integer.MAX_VALUE?byLegacy.get((int)legacy):null;
                StableModel model=new StableModel(rec);
                if(uid==null||!model.valid(rec)){rejected++;continue;}
                List<ChemCoreResponseModel> items=stableModels.computeIfAbsent(uid,k->new ArrayList<>());
                boolean overlap=false;
                for(ChemCoreResponseModel prior:items)if(prior.sensorKey().equals(model.sensor)&&
                    model.minPpm()<prior.maxPpm()&&prior.minPpm()<model.maxPpm()){overlap=true;break;}
                if(overlap){rejected++;continue;}
                items.add(model);
                stableSensorKeys.computeIfAbsent(uid,k->new HashSet<>()).add(model.sensor);
                labels.putIfAbsent(uid,rec.optString("label",""));
                minimums.put(uid,Math.max(minimums.getOrDefault(uid,1),Math.max(1,rec.optInt("minimum_contributors",1))));
                accepted++;
            }
            if(accepted==0)return new LoadResult(base,"NO_VALID_STABLE_PRIORS_USE_BUILTIN",version,hash,0,rejected);
            Map<Long,InverseMolecularSolver.Candidate> merged=new LinkedHashMap<>();
            for(InverseMolecularSolver.Candidate candidate:base)if(candidate!=null)merged.put(candidate.chemicalUid,candidate);
            for(Map.Entry<Long,List<ChemCoreResponseModel>> item:stableModels.entrySet()){
                long uid=item.getKey();
                InverseMolecularSolver.Candidate old=merged.get(uid);
                Set<String> replacement=stableSensorKeys.get(uid);
                List<ChemCoreResponseModel> models=new ArrayList<>();
                if(old!=null)for(ChemCoreResponseModel model:old.models)
                    if(model!=null&&!replacement.contains(model.sensorKey()))models.add(model);
                models.addAll(item.getValue());
                String name=old!=null?old.label:labels.getOrDefault(uid,"");
                int minimum=Math.max(old==null?1:old.minimumContributors,minimums.getOrDefault(uid,1));
                merged.put(uid,new InverseMolecularSolver.Candidate(uid,name,models,
                    old==null?-0.5:old.priorLogWeight,minimum));
            }
            return new LoadResult(new ArrayList<>(merged.values()),
                rejected>0?"ACTIVE_STABLE_PARTIAL_PRIORS":"ACTIVE_STABLE_PRIORS",version,hash,accepted,rejected);
        }catch(Exception ex){
            return new LoadResult(base,"STABLE_LOAD_FAILED_USE_BUILTIN:"+ex.getClass().getSimpleName(),version,hash,0,0);
        }
    }

    private static Map<Integer,Long> canonicalUidMap(ExperimentsDb db){
        Map<Integer,Long> out=new HashMap<>();
        try(Cursor c=db.getReadableDatabase().rawQuery(
            "SELECT legacy_id,uid FROM chemicals WHERE legacy_id IS NOT NULL AND active=1",null)){
            while(c.moveToNext())out.put(c.getInt(0),c.getLong(1));
        }
        return out;
    }
    private static byte[] readBounded(File file)throws Exception{
        try(FileInputStream in=new FileInputStream(file);
            ByteArrayOutputStream out=new ByteArrayOutputStream((int)Math.min(file.length(),MAX_BYTES))){
            byte[] buffer=new byte[8192];
            for(int n;(n=in.read(buffer))!=-1;){
                if(n==0)continue;
                if(out.size()+n>MAX_BYTES)throw new IllegalArgumentException("Stable payload too large");
                out.write(buffer,0,n);
            }
            return out.toByteArray();
        }
    }
}
