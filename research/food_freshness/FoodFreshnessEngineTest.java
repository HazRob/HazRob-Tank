package com.hazrob.tank.foodfreshness;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Standalone source-derived test fixtures, never real food safety claims. */
public final class FoodFreshnessEngineTest {
    private static int checks=0;
    static void ok(boolean good,String message){
        checks++;if(!good)throw new AssertionError(checks+": "+message);
    }
    public static void main(String[] ignored){
        ok(FoodReferenceCatalog.patterns().size()==59,"all original reference sessions");
        ok(FoodReferenceCatalog.SOURCE_BLOB.equals(
            "959aae7574307a0030d218d4b86eb6618a6f72b0"),"source fingerprint");
        ok(FoodReferenceCatalog.SENSOR_ORDER.size()==8,"all eight MQ models");
        ok(FoodFreshnessEngine.day("Blueberry D5")==5,"study day");
        ok(FoodFreshnessEngine.day("AppleBanana")==0,"undated combination");
        ok(FoodFreshnessEngine.family("Green Apple D2").equals("green apple"),
           "case-insensitive family");
        int count=0;
        for(FoodReferenceCatalog.Pattern p:FoodReferenceCatalog.patterns()){
            List<FoodFreshnessEngine.Reading> readings=new ArrayList<>();
            double[] raw=p.vector();
            ok(raw.length==8,"source vector always 8");
            for(int i=0;i<8;i++)readings.add(new FoodFreshnessEngine.Reading(
                FoodReferenceCatalog.SENSOR_ORDER.get(i),raw[i]*9234.5));
            var r=FoodFreshnessEngine.analyze(readings,FoodFreshnessEngine.family(p.label));
            ok(r.usableSensorCount==8,"eight source-profile readings retained");
            ok(r.rankedTemplates.size()>0,"study exemplar must match a reference");
            ok(r.rankedTemplates.get(0).logShapeDistance<1e-10,
               "same geomean-normalized source pattern compares as zero");
            ok(!r.foodSafetyCertification&&!r.microbiologyMeasured&&!r.gasIdentity,
               "food research similarities never become microbiology or gas identification");
            count++;
        }
        var banana=FoodFreshnessEngine.analyze(Arrays.asList(
           new FoodFreshnessEngine.Reading("MQ2",1.0),
           new FoodFreshnessEngine.Reading("MQ3",2.0)), "banana");
        ok(banana.usableSensorCount==2,"partially observed sensor collection retained");
        ok(banana.rankedTemplates.size()==5,"five banana study days");
        ok(banana.selectedFoodTrend.size()==5,"five original day labels for banana graph");
        ok(banana.rankedTemplates.get(0).advisories.contains("PARTIAL_SENSOR_OVERLAP"),
           "no fabricated missing sensor values");
        var invalid=FoodFreshnessEngine.analyze(Arrays.asList(
           new FoodFreshnessEngine.Reading("MQ2",Double.NaN),
           new FoodFreshnessEngine.Reading("MQ3",-1),
           new FoodFreshnessEngine.Reading("MQ5",0)
        ),"banana");
        ok(invalid.usableSensorCount==0,"invalid numeric channels do not become readings");
        ok(invalid.unusableSensorCount==3,"bad readings counted");
        ok(invalid.rankedTemplates.isEmpty(),"no invented headspace evidence");
        var all=FoodFreshnessEngine.analyze(Collections.singletonList(
           new FoodFreshnessEngine.Reading("MQ2",500)),"");
        ok(all.rankedTemplates.size()==59,"all food exemplars, none filtered by confidence");
        var fruit=FoodFreshnessEngine.analyzeFruit(Collections.singletonList(
            new FoodFreshnessEngine.Reading("MQ2",500)),"");
        ok(fruit.rankedTemplates.size()==54,
           "fruit UI uses 54 fruit samples and preserves 5 mushroom reference records");
        ok(FoodFreshnessEngine.fruitFamilies().size()==18,
           "fruit selector excludes only mushroom category");
        ok(!FoodFreshnessEngine.fruitFamilies().contains("mushroom"),
           "mushroom not mislabeled a fruit");
        ok(all.rankedTemplates.get(0).overlappingSensors==1,"limited evidence annotated");
        ok(all.rankedTemplates.get(0).similarityIndex<=100,"bounded similarity index");
        ok(!FoodFreshnessEngine.foodFamilies().isEmpty(),"food selection available");
        try{all.rankedTemplates.clear();throw new AssertionError("mutable result");}
        catch(UnsupportedOperationException expected){checks++;}
        System.out.println("PASS FoodFreshnessEngine: "+checks+
                           " assertions, "+count+" source exemplars, no gas ChemCore dependency");
    }
}
