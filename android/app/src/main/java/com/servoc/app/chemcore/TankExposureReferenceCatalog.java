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
    /** Human-readable catalogue: do not compare numbers against estimated sensor PPM. */
    public static String summary(JSONObject analysis,int maxCandidates,int maxLimitsPerCandidate){
        if(analysis==null)return "حدود التعرض: لا يوجد تحليل حالي.";
        JSONObject inventory=analysis.optJSONObject(KEY);
        if(inventory==null)return "حدود التعرض: لم تُرفق المراجع مع هذا التحليل.";
        JSONArray entries=inventory.optJSONArray("candidates");
        if(entries==null||entries.length()==0)return "حدود التعرض: لا توجد مادة مرشحة لها معرّف في المكتبة.";
        StringBuilder out=new StringBuilder("حدود التعرض المرجعية — مستقلة عن معايرة R0");
        out.append("\nهذه حدود منشورة لمواد محتملة، وليست إثبات رصد أو تجاوز أو حكم امتثال.");
        int limitCount=0;
        for(int i=0;i<entries.length()&&i<Math.max(1,maxCandidates);i++){
            JSONObject candidate=entries.optJSONObject(i);if(candidate==null)continue;
            out.append("\n\n#").append(candidate.optInt("candidate_rank",i+1)).append(" ")
                .append(candidate.optString("label","—"));
            JSONArray limits=candidate.optJSONArray("limits");
            if(limits==null||limits.length()==0){out.append("\nلا يوجد حد موثّق في قاعدة البيانات لهذه المادة.");continue;}
            for(int j=0;j<limits.length()&&j<Math.max(1,maxLimitsPerCandidate);j++){
                JSONObject row=limits.optJSONObject(j);if(row==null)continue;
                out.append("\n").append(row.optString("authority","—"))
                    .append(" ").append(row.optString("limit_type","—")).append(": ");
                double n=row.optDouble("value",Double.NaN);
                out.append(Double.isFinite(n)?String.format(java.util.Locale.US,"%.4g",n):"غير رقمي");
                out.append(" ").append(row.optString("unit",""));
                double duration=row.optDouble("duration_min",Double.NaN);
                if(Double.isFinite(duration)&&duration>0)
                    out.append(" / ").append(String.format(java.util.Locale.US,"%.0f",duration)).append(" min");
                String note=row.optString("notation","");
                if(!note.isEmpty())out.append(" • ").append(note);
                String source=row.optString("source_id","");
                if(!source.isEmpty())out.append(" • ").append(source);
                limitCount++;
            }
            if(candidate.optBoolean("truncated",false))out.append("\n(بقية المراجع محفوظة في المكتبة)");
        }
        if(limitCount==0)out.append("\nلم تتوفر حدود رقمية من القاعدة المحلية.");
        return out.toString();
    }

}
