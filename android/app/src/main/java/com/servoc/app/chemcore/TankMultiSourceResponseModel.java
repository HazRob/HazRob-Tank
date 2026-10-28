package com.servoc.app.chemcore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * One MQ channel, all sourced response relations.
 *
 * A published MQ sensor appears ONLY ONCE in a joint 8-channel inverse fit:
 * multiple papers/manufacturer curves for that sensor are ALTERNATIVES,
 * not independent physical observations. All are retained and evaluated
 * within their actual domain. The best eligible evidence is selected per
 * tested concentration; no M4 condition hides research-source calculations.
 */
public final class TankMultiSourceResponseModel implements ChemCoreResponseModel {
    private final String sensor;
    private final List<ChemCoreResponseModel> sources;
    private final double min,max;
    private final ChemCoreDecision.EvidenceTier conservativeTier;

    public TankMultiSourceResponseModel(String key,List<ChemCoreResponseModel> models){
        if(key==null||models==null||models.isEmpty())
            throw new IllegalArgumentException("Source models required");
        sensor=key.toLowerCase(Locale.US);
        List<ChemCoreResponseModel> valid=new ArrayList<>();
        double lo=Double.POSITIVE_INFINITY,hi=Double.NEGATIVE_INFINITY;
        ChemCoreDecision.EvidenceTier minTier=null;
        for(ChemCoreResponseModel m:models){
            if(m==null||!sensor.equalsIgnoreCase(m.sensorKey())||
               !(m.minPpm()>0)||!(m.maxPpm()>m.minPpm()))continue;
            valid.add(m);
            lo=Math.min(lo,m.minPpm());hi=Math.max(hi,m.maxPpm());
            if(minTier==null||m.evidenceTier().ordinal()<minTier.ordinal())
                minTier=m.evidenceTier();
        }
        if(valid.isEmpty())throw new IllegalArgumentException("No usable models");
        sources=List.copyOf(valid);
        min=lo;max=hi;conservativeTier=minTier;
    }

    public List<ChemCoreResponseModel> sources(){return sources;}
    @Override public String sensorKey(){return sensor;}
    @Override public String version(){return "tank-v5-multisource/1";}
    @Override public String sourceId(){return "MULTISOURCE:"+sensor;}
    @Override public String modelId(){return version()+":"+sensor;}
    @Override public String evidenceFamilyId(){return "MULTISOURCE_SENSOR_ALTERNATIVES";}
    /** Conservatively do not promote identity using a stronger tier from an unselected alternative. */
    @Override public ChemCoreDecision.EvidenceTier evidenceTier(){return conservativeTier;}
    @Override public ChemCoreDecision.EvidenceTier evidenceTierFor(Estimate prediction){
        if(prediction!=null){
            for(ChemCoreResponseModel source:sources)
                if(source.modelId().equals(prediction.modelId)&&
                    source.sourceId().equals(prediction.sourceId))
                    return source.evidenceTierFor(prediction);
        }
        return conservativeTier; // never infer a higher tier from a missing source mapping
    }
    @Override public double minPpm(){return min;}
    @Override public double maxPpm(){return max;}
    @Override public boolean reportableQuantitative(){return false;}

    @Override public Estimate predict(double ppm,double t,double rh,SamplingProfile profile){
        if(!Double.isFinite(ppm)||ppm<=0)
            return new Estimate(false,Double.NaN,1.0,false,"invalid ppm");
        Estimate best=null;ChemCoreResponseModel chosen=null;
        for(ChemCoreResponseModel source:sources){
            if(ppm<source.minPpm()||ppm>source.maxPpm())continue;
            Estimate e=source.predict(ppm,t,rh,profile);
            if(e==null||!e.available||!Double.isFinite(e.meanLog10Ratio))continue;
            if(best==null||better(source,e,chosen,best)){
                best=e;chosen=source;
            }
        }
        if(best==null)return new Estimate(false,Double.NaN,1.0,false,
            "No source relation supports this ppm; retained all original model domains");
        String source=best.sourceId.isEmpty()?chosen.sourceId():best.sourceId;
        String model=best.modelId.isEmpty()?chosen.modelId():best.modelId;
        String family=best.familyId.isEmpty()?chosen.evidenceFamilyId():best.familyId;
        String independent=best.independenceGroup.isEmpty()?chosen.independenceGroup():best.independenceGroup;
        return new Estimate(true,best.meanLog10Ratio,best.sigmaLog10,best.insideDomain,
            "Selected 1 of "+sources.size()+" sourced alternatives for "+sensor+
            "; source="+source+"; tier="+chosen.evidenceTier()+"; "+best.note,
            best.evidenceType==null?chosen.evidenceType():best.evidenceType,
            family,model,source,independent);
    }
    private static boolean better(ChemCoreResponseModel next,Estimate estimate,
                                  ChemCoreResponseModel current,Estimate prior){
        if(estimate.insideDomain!=prior.insideDomain)return estimate.insideDomain;
        int a=next.evidenceTier().ordinal(),b=current.evidenceTier().ordinal();
        if(a!=b)return a>b;
        double sa=estimate.sigmaLog10,sb=prior.sigmaLog10;
        if(Double.isFinite(sa)&&Double.isFinite(sb)&&sa!=sb)return sa<sb;
        return false; // deterministic pinned source ordering
    }
}
