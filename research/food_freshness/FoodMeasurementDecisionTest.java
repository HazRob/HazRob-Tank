package com.hazrob.tank.foodfreshness;
import com.hazrob.tank.foodfreshness.meat.BeefResearchCatalog;
import java.util.LinkedHashMap;
import java.util.Map;

/** Genuine source-reference projection tests; never physical food validation. */
public final class FoodMeasurementDecisionTest {
  static int checks=0;
  static void check(boolean b,String msg){checks++;if(!b)throw new AssertionError(msg);}
  static Map<String,Double> fruitInput(double[] v,double gain){
    Map<String,Double> x=new LinkedHashMap<>();
    for(int i=0;i<v.length;i++)
      x.put(FoodReferenceCatalog.SENSOR_ORDER.get(i),v[i]*gain);
    return x;
  }
  public static void main(String[] args){
    int count=0,dayCount=0;
    for(FoodReferenceCatalog.Pattern profile:FoodReferenceCatalog.patterns()){
      if(FoodFreshnessEngine.family(profile.label).equals("mushroom"))continue;
      String family=FoodFreshnessEngine.family(profile.label);
      FoodMeasurementDecision.Decision r=FoodMeasurementDecision.fruit(fruitInput(profile.vector(),2678.0),family);
      check(r.channels==8,"all MQ inputs kept");
      check(!r.validatedOnTank&&!r.measuresMicrobiology&&!r.safeToEat,"food safety never certified");
      check(r.referenceDistance<1e-9,"source profile itself must be nearest");
      int day=FoodFreshnessEngine.day(profile.label);
      if(day>0){
        check(Double.isFinite(r.fittedStudyPosition),"study five-day family has day-curve");
        check(Math.abs(r.fittedStudyPosition-day)<1e-6,"source day curve interpolation identity: "+profile.label+" "+r.fittedStudyPosition);
        dayCount++;
      }else check(Double.isNaN(r.fittedStudyPosition),"undated mixture cannot have fabricated day");
      count++;
    }
    check(count==54,"exact 54 fruit references, no mushroom");
    check(dayCount==45,"nine original five-day trajectories");
    Map<String,Double> poor=new LinkedHashMap<>();
    for(int i=0;i<5;i++) poor.put(FoodReferenceCatalog.SENSOR_ORDER.get(i),2.0+i);
    check(FoodMeasurementDecision.fruit(poor,"banana").status.equals("INSUFFICIENT_EVIDENCE"),
       "reject fewer than six active fruit channels");
    poor.clear();
    poor.put("MQ2",3.0);
    check(FoodMeasurementDecision.meat(poor).status.equals("INSUFFICIENT_EVIDENCE"),
       "reject sparse beef input");
    for(BeefResearchCatalog.Template p:BeefResearchCatalog.classes()){
      Map<String,Double> source=new LinkedHashMap<>();
      double[] v=p.logShape();
      for(int i=0;i<v.length;i++)
        source.put(BeefResearchCatalog.SENSOR_ORDER.get(i),Math.pow(10,v[i])*932.0);
      var result=FoodMeasurementDecision.meat(source);
      check(result.referenceId.equals(p.label),"seven-sensor beef source exemplar matches class");
      check(result.referenceDistance<1e-9,"zero source-template distance");
      check(!result.safeToEat&&!result.measuresMicrobiology,"no microbiology interpretation");
    }
    System.out.println("PASS food measurement interpretation: "+checks+
      " assertions, "+count+" fruit, "+dayCount+" day curve endpoints, four beef source templates");
  }
}