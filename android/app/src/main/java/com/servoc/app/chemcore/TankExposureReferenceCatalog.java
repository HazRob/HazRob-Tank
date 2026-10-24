package com.servoc.app.chemcore;

import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.exposure.ExposureRepository;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Immutable reference inventory for the CURRENT Tank one-shot analysis.
 *
 * Published OSHA/NIOSH and other recorded limits are scientific library facts:
 * the operator can inspect them without performing a reference-gas R0/PPM calibration.
 * Listing a reference MUST NOT imply a detected chemical, a measured concentration,
 * an exceedance, or TWA/STEL compliance. These require separate qualified evidence.
 */
public final class TankExposureReferenceCatalog {
    public static final String KEY="exposure_reference_inventory";
    public static final String SCHEMA="tank-exposure-reference-inventory/1";
    private static final int MAX_CANDIDATES=8,MAX_LIMITS_PER_CANDIDATE=32;
    private TankExposureReferenceCatalog(){}

    public static JSONObject forAnalysis(ExperimentsDb db,JSONObject analysis){
        JSONObject out=new JSONObject();
        try{
            out.put("schema",SCHEMA);
            out.put("source","local_authority_preserving_exposure_limits");
            out.put("calibration_required_to_display",false);
            out.put("comparison_with_estimated_ppm_permitted",false);
            out.put("occupational_compliance_determination","NOT_MADE");
            out.put("reference_is_not_detection",true);
            JSONArray candidates=new JSONArray();out.put("candidates",candidates);
            if(db==null){out.put("status","DATABASE_UNAVAILABLE");return out;}
            if(analysis==null){out.put("status","NO_ANALYSIS");return out;}
            JSONArray source=analysis.optJSONArray("candidates");
            if(source==null||source.length()==0){out.put("status","NO_CANDIDATES");return out;}
            ExposureRepository repo=new ExposureRepository(db);
            Set<Long> seen=new HashSet<>();
            for(int i=0;i<source.length()&&candidates.length()<MAX_CANDIDATES;i++){
                JSONObject q=source.optJSONObject(i);if(q==null)continue;
                long uid=q.optLong("chemical_uid",-1);
                if(uid<0||!seen.add(uid))continue;
                JSONObject item=new JSONObject();
                item.put("chemical_uid",uid);item.put("label",q.optString("label","—"));
                item.put("candidate_rank",q.optInt("rank",i+1));
                JSONArray rows=new JSONArray();item.put("limits",rows);
                try{
                    List<ExposureRepository.Limit> limits=repo.forChemical(uid);
                    item.put("source_limit_count",limits.size());
                    for(int j=0;j<limits.size()&&j<MAX_LIMITS_PER_CANDIDATE;j++){
                        ExposureRepository.Limit v=limits.get(j);
                        JSONObject limit=new JSONObject();
                        limit.put("authority",v.authority==null?"":v.authority);
                        limit.put("jurisdiction",v.jurisdiction==null?"":v.jurisdiction);
                        limit.put("limit_type",v.type==null?"":v.type);
                        if(v.numeric())limit.put("value",v.value);else limit.put("value",JSONObject.NULL);
                        limit.put("unit",v.unit==null?"":v.unit);
                        if(v.durationMin!=null&&Double.isFinite(v.durationMin)&&v.durationMin>0)limit.put("duration_min",v.durationMin);
                        limit.put("notation",v.notation==null?"":v.notation);
                        limit.put("source_id",v.sourceId==null?"":v.sourceId);
                        limit.put("revision",v.revision==null?"":v.revision);
                        rows.put(limit);
                    }
                    item.put("status",limits.isEmpty()?"NO_REFERENCES":"AVAILABLE");
                    item.put("truncated",limits.size()>MAX_LIMITS_PER_CANDIDATE);
                }catch(RuntimeException ex){
                    item.put("status","REFERENCE_QUERY_FAILED");
                    item.put("source_limit_count",0);
                }
                candidates.put(item);
            }
            out.put("status",candidates.length()>0?"READY":"NO_MATCHING_LIBRARY_IDS");
            out.put("candidate_count",candidates.length());
        }catch(Exception ignored){}
        return out;
    }
}
