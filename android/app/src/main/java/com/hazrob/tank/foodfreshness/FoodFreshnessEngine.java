package com.hazrob.tank.foodfreshness;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Independent food-headspace research prototype. Uses only food reference shapes.
 * NO imports or shared model IDs with chemical/gas ChemCore inference.
 *
 * Comparison: normalized log10 shape of a simultaneous 8-MQ observation vs
 * source-provided geomean-normalized 8-MQ food reference. The database is
 * descriptive (food type/day), not a calibrated microbial/TVC regression.
 *
 * No hard thresholds on similarity; invalid arithmetic is the only exclusion.
 */
public final class FoodFreshnessEngine {
    public static final String SCHEMA="hazrob-tank-food-headspace/1";
    private FoodFreshnessEngine(){}
    public static final class Reading {
        public final String sensorType;
        public final double value;
        public Reading(String sensor,double raw){
            sensorType=sensor==null?"":sensor.trim().toUpperCase(Locale.ROOT);
            value=raw;
        }
    }
    public static final class Match {
        public final String referenceLabel,foodFamily;
        public final int studyDay;
        public final double logShapeDistance,similarityIndex;
        public final int overlappingSensors;
        public final List<String> advisories;
        private Match(String label,String family,int day,double error,double score,int matched,
                      List<String> warnings){
            referenceLabel=label;foodFamily=family;studyDay=day;
            logShapeDistance=error;similarityIndex=score;overlappingSensors=matched;
            advisories=Collections.unmodifiableList(new ArrayList<>(warnings));
        }
    }
    public static final class Result {
        public final List<Match> rankedTemplates,selectedFoodTrend;
        public final String foodSelection;
        public final int usableSensorCount,unusableSensorCount;
        public final boolean studyEquivalentProtocol,foodSafetyCertification,
                             microbiologyMeasured,gasIdentity;
        private Result(List<Match> all,List<Match> trend,String food,int usable,int invalid){
            rankedTemplates=Collections.unmodifiableList(new ArrayList<>(all));
            selectedFoodTrend=Collections.unmodifiableList(new ArrayList<>(trend));
            foodSelection=food;usableSensorCount=usable;unusableSensorCount=invalid;
            studyEquivalentProtocol=false;foodSafetyCertification=false;
            microbiologyMeasured=false;gasIdentity=false;
        }
    }
    public static String family(String label){
        if(label==null)return "";
        return label.trim().replaceAll("(?i)\\s+D[1-9][0-9]*$", "").trim()
            .toLowerCase(Locale.ROOT);
    }
    public static int day(String label){
        if(label==null)return 0;
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?i)\\s+D([1-9][0-9]*)$")
            .matcher(label.trim());
        return m.find()?Integer.parseInt(m.group(1)):0;
    }
    public static List<String> foodFamilies(){
        Set<String> unique=new LinkedHashSet<>();
        for(FoodReferenceCatalog.Pattern p:FoodReferenceCatalog.patterns())
            unique.add(family(p.label));
        return Collections.unmodifiableList(new ArrayList<>(unique));
    }
    private static boolean valid(double x){return x>0&&Double.isFinite(x);}
    public static Result analyze(List<Reading> readings,String selectedFood){
        Map<String,Double> observed=new LinkedHashMap<>();
        int bad=0;
        if(readings!=null)for(Reading r:readings){
            if(r==null || !valid(r.value)){bad++;continue;}
            if(!FoodReferenceCatalog.SENSOR_ORDER.contains(r.sensorType))continue;
            observed.put(r.sensorType,r.value);
        }
        String requested=family(selectedFood);
        List<Match> matches=new ArrayList<>();
        for(FoodReferenceCatalog.Pattern profile:FoodReferenceCatalog.patterns()){
            String f=family(profile.label);
            if(!requested.isEmpty()&&!requested.equals(f))continue;
            double[] v=profile.vector();
            // Only overlapping sensor channels are compared; never fabricate a
            // missing MQ reading. Re-normalize both to the same visible subspace.
            List<Double> obs=new ArrayList<>(),reference=new ArrayList<>();
            for(int i=0;i<FoodReferenceCatalog.SENSOR_ORDER.size();i++){
                Double x=observed.get(FoodReferenceCatalog.SENSOR_ORDER.get(i));
                if(x==null || !valid(v[i]))continue;
                obs.add(Math.log10(x));reference.add(Math.log10(v[i]));
            }
            if(obs.isEmpty())continue;
            double avgO=0,avgR=0;
            for(int k=0;k<obs.size();k++){avgO+=obs.get(k);avgR+=reference.get(k);}
            avgO/=obs.size();avgR/=reference.size();
            double error=0;
            for(int k=0;k<obs.size();k++){
                double d=(obs.get(k)-avgO)-(reference.get(k)-avgR);
                error+=d*d;
            }
            error=Math.sqrt(error/obs.size());
            if(!Double.isFinite(error))continue;
            // A similarity index is monotonic with distance and NOT probability.
            double index=100/(1+error);
            List<String> warnings=new ArrayList<>();
            warnings.add("RESEARCH_SHAPE_SIMILARITY_NOT_EDIBILITY");
            warnings.add("SOURCE_INSTRUMENT_TRANSFER_NOT_VALIDATED_ON_TANK");
            if(obs.size()<8)warnings.add("PARTIAL_SENSOR_OVERLAP");
            if(day(profile.label)==0)warnings.add("COMBINED_FOOD_OR_UNDATED_REFERENCE");
            matches.add(new Match(profile.label,f,day(profile.label),error,index,
                                  obs.size(),warnings));
        }
        matches.sort(Comparator.comparingDouble((Match m)->m.logShapeDistance)
                               .thenComparing(m->m.referenceLabel));
        List<Match> trend=new ArrayList<>(matches);
        trend.removeIf(m->m.studyDay<=0);
        trend.sort(Comparator.comparingInt((Match m)->m.studyDay)
                              .thenComparing(m->m.referenceLabel));
        return new Result(matches,trend,requested,observed.size(),bad);
    }
}
