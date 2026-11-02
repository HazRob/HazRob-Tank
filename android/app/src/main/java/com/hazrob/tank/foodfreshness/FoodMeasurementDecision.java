package com.hazrob.tank.foodfreshness;

import com.hazrob.tank.foodfreshness.meat.BeefResearchCatalog;
import com.hazrob.tank.foodfreshness.meat.BeefResearchEngine;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Food-only measurement interpretation. Does NOT depend on or publish to gas ChemCore.
 * Fruit D1-D5 are SOURCE REFERENCE POSITIONS, not physical age or ripeness measurements.
 * Beef source classes are exploratory prototypes (TS5 balanced accuracy 38.88%).
 * All thresholds below are diagnostics for numerical identifiability, NOT food-safety limits.
 */
public final class FoodMeasurementDecision {
    public static final String MODEL = "food-direct-measurement/1";
    private FoodMeasurementDecision(){}

    public static final class Decision {
        public final String mode, status, statement, referenceId, sourceDoi;
        public final double fittedStudyPosition, referenceDistance, relativeSeparation;
        public final int channels, sourceProfiles;
        public final boolean validatedOnTank, measuresMicrobiology, safeToEat;
        public final List<String> evidence;
        private Decision(String mode,String status,String statement,String ref,String doi,
                         double pos,double distance,double gap,int sensors,int profiles,
                         List<String> reasons){
            this.mode=mode;this.status=status;this.statement=statement;referenceId=ref;
            sourceDoi=doi;fittedStudyPosition=pos;referenceDistance=distance;
            relativeSeparation=gap;channels=sensors;sourceProfiles=profiles;
            validatedOnTank=false;measuresMicrobiology=false;safeToEat=false;
            evidence=java.util.Collections.unmodifiableList(new ArrayList<>(reasons));
        }
    }
    private static boolean valid(double n){return Double.isFinite(n)&&n>0;}
    private static double separation(double best,double second){
        if(!Double.isFinite(best)||!Double.isFinite(second)||second<=0)return Double.NaN;
        return Math.max(0,(second-best)/second);
    }
    public static Decision meat(Map<String,Double> rs){
        BeefResearchEngine.Result result=BeefResearchEngine.analyze(rs);
        List<String> notes=new ArrayList<>();
        notes.add("BEFF_2018_TS1_TS4_TRAIN_TS5_HOLDOUT");
        notes.add("HELDOUT_BALANCED_ACCURACY_0.3888_NOT_VALIDATED_FOR_TANK");
        notes.add("NO_TVC_MEASURED_NO_EDIBILITY_DECISION");
        int overlap=result.validChannels;
        if(overlap<6||result.rankedClasses.isEmpty()) {
            notes.add("AT_LEAST_SIX_VALID_MATCHING_MQ_CHANNELS_REQUIRED");
            return new Decision("MEAT","INSUFFICIENT_EVIDENCE","لا تكفي قراءات الحساسات لتفسير العينة",
                "",BeefResearchCatalog.STUDY_DOI,Double.NaN,Double.NaN,Double.NaN,
                overlap,4,notes);
        }
        BeefResearchEngine.Match first=result.rankedClasses.get(0);
        double gap=result.rankedClasses.size()>1?separation(first.distance,
            result.rankedClasses.get(1).distance):Double.NaN;
        String status=Double.isFinite(gap)&&gap<0.10?"AMBIGUOUS_REFERENCE":"RESEARCH_CLASS_ONLY";
        if("AMBIGUOUS_REFERENCE".equals(status)) notes.add("CLOSE_REFERENCE_CLASSES");
        String description=String.format(Locale.US,"أقرب فئة في دراسة اللحم البقري: %s؛ مطابقة بحثية لا تثبت سلامة الغذاء",
            first.classCode);
        return new Decision("MEAT",status,description,first.classCode,
            BeefResearchCatalog.STUDY_DOI,Double.NaN,first.distance,gap,overlap,4,notes);
    }
    /** Source day-curve projection in centered log10 MQ response space.
     * The selected food family must have at least two original dated source profiles.
     * It does NOT project a new date, infer microbial counts, or certify ripeness.
     */
    public static Decision fruit(Map<String,Double> rs,String selectedFruit){
        List<FoodFreshnessEngine.Reading> input=new ArrayList<>();
        if(rs!=null)for(Map.Entry<String,Double> row:rs.entrySet())
            input.add(new FoodFreshnessEngine.Reading(row.getKey(),row.getValue()));
        FoodFreshnessEngine.Result original=FoodFreshnessEngine.analyzeFruit(input,selectedFruit);
        List<String> notes=new ArrayList<>();
        notes.add("ZENODO_SOURCE_REFERENCE_DAY_NOT_SAMPLE_AGE");
        notes.add("NO_MICROBIAL_OR_EDIBILITY_OR_RIPENESS_CERTIFICATION");
        if(original.usableSensorCount<6||original.rankedTemplates.isEmpty()) {
            notes.add("AT_LEAST_SIX_VALID_MQ_CHANNELS_REQUIRED");
            return new Decision("FRUIT","INSUFFICIENT_EVIDENCE","لا تكفي قراءات الحساسات لتفسير العينة",
                "",FoodReferenceCatalog.SOURCE_DOI,Double.NaN,Double.NaN,Double.NaN,
                original.usableSensorCount,original.rankedTemplates.size(),notes);
        }
        FoodFreshnessEngine.Match best=original.rankedTemplates.get(0);
        double gap=original.rankedTemplates.size()>1?
            separation(best.logShapeDistance,original.rankedTemplates.get(1).logShapeDistance):Double.NaN;
        // In all-food mode, the closest source family determines the requested curve;
        // never interpolate incompatible fruit families.
        String family=best.foodFamily;
        Map<Integer,double[]> sourceByDay=new LinkedHashMap<>();
        for(FoodReferenceCatalog.Pattern p:FoodReferenceCatalog.patterns())
            if(FoodFreshnessEngine.family(p.label).equals(family)&&
               FoodFreshnessEngine.day(p.label)>0)
                sourceByDay.put(FoodFreshnessEngine.day(p.label),p.vector());
        double stage=Double.NaN;
        if(sourceByDay.size()>=2){
            double[] observation=new double[FoodReferenceCatalog.SENSOR_ORDER.size()];
            Arrays.fill(observation,Double.NaN);
            for(int i=0;i<observation.length;i++){
                Double v=rs==null?null:rs.get(FoodReferenceCatalog.SENSOR_ORDER.get(i));
                if(v!=null&&valid(v))observation[i]=Math.log10(v);
            }
            // Recenter identical common channels for observed and BOTH source endpoints.
            List<Integer> days=new ArrayList<>(sourceByDay.keySet());
            java.util.Collections.sort(days);
            double minError=Double.POSITIVE_INFINITY;
            for(int j=0;j+1<days.size();j++){
                int d0=days.get(j),d1=days.get(j+1);
                if(d1!=d0+1)continue; // no interpolation across missing reference days
                double[] a=sourceByDay.get(d0),b=sourceByDay.get(d1);
                double mo=0,ma=0,mb=0;int n=0;
                for(int i=0;i<observation.length;i++)
                    if(Double.isFinite(observation[i])&&valid(a[i])&&valid(b[i])){
                        mo+=observation[i];ma+=Math.log10(a[i]);mb+=Math.log10(b[i]);n++;
                    }
                if(n<6)continue;
                mo/=n;ma/=n;mb/=n;
                double dot=0,norm=0;
                for(int i=0;i<observation.length;i++)
                    if(Double.isFinite(observation[i])&&valid(a[i])&&valid(b[i])){
                        double x=(Math.log10(a[i])-ma),y=(Math.log10(b[i])-mb);
                        double v=(observation[i]-mo)-x;
                        dot+=v*(y-x);norm+=(y-x)*(y-x);
                    }
                double t=norm>1e-18?Math.max(0,Math.min(1,dot/norm)):0;
                double err=0;
                for(int i=0;i<observation.length;i++)
                    if(Double.isFinite(observation[i])&&valid(a[i])&&valid(b[i])){
                        double x=Math.log10(a[i])-ma,y=Math.log10(b[i])-mb;
                        double delta=(observation[i]-mo)-(x+t*(y-x));
                        err+=delta*delta;
                    }
                err=Math.sqrt(err/n);
                if(err<minError){minError=err;stage=d0+t;}
            }
        }else notes.add("UNDATED_OR_SINGLE_FOOD_REFERENCE_NO_DAY_CURVE");
        String state=Double.isFinite(stage)?"REFERENCE_STAGE_ONLY":"REFERENCE_FINGERPRINT_ONLY";
        if(Double.isFinite(gap)&&gap<0.10){
            state="AMBIGUOUS_REFERENCE";
            notes.add("CLOSE_REFERENCE_PROFILES");
        }
        String statement=Double.isFinite(stage)?
            String.format(Locale.US,"أقرب مسار دراسي: %s، الموضع المرجعي التقريبي D%.2f (ليس عمرًا فعليًا ولا حكم نضج)",family,stage):
            "أقرب نمط غذائي منشور: "+best.referenceLabel+" (لا يوجد منحنى أيام مؤهل لهذا النوع)";
        return new Decision("FRUIT",state,statement,best.referenceLabel,
            FoodReferenceCatalog.SOURCE_DOI,stage,best.logShapeDistance,gap,
            original.usableSensorCount,original.rankedTemplates.size(),notes);
    }
}