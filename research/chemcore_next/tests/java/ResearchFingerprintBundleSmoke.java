package com.hazrob.tank.chemcore.next;
import java.util.*;
public final class ResearchFingerprintBundleSmoke {
 private static int checks;
 private static void test(boolean b,String why) {
    checks++;if(!b)throw new AssertionError("SOURCE_BUNDLE_"+checks+": "+why);
 }
 private static FingerprintExpert.Signature sig(String id,String sensor,String root){
   return new FingerprintExpert.Signature(id,root,"ACTUAL_SOURCE_FEATURE_SPACE",sensor,700L,
       List.of("source_feature"),List.of(0.2),List.of(0.1),1);
 }
 private static ResearchFingerprintBundle.Variant v(String technique,String sensor,String family,
                                                    String root,double f,boolean threshold){
   return new ResearchFingerprintBundle.Variant(
       sig(technique,sensor,root),
       new FingerprintExpert.Observation("ACTUAL_SOURCE_FEATURE_SPACE",sensor,
          "ORIGINAL_SOURCE_SAMPLE",Map.of("source_feature",f),true),
       new FingerprintExpert.Qualification(true,true),
       "TANK:"+sensor,family,technique,1.0,threshold,100.0);
 }
 private static CoreContracts.DeviceProfile device(){
    return new CoreContracts.DeviceProfile("owner-Tank","mq-board",
       "tank-v6",List.of(
       new CoreContracts.SensorInstance("TANK:MQ3","MQ3","slot3","READY"),
       new CoreContracts.SensorInstance("TANK:MQ4","MQ4","slot4","READY")));
 }
 private static CoreContracts.Snapshot snap(boolean with4,boolean relay) {
   List<CoreContracts.Reading> readings=new ArrayList<>();
   readings.add(new CoreContracts.Reading("TANK:MQ3",1000,null,25.,25.,null,
      CoreContracts.CalibrationReference.AIR_RS_MEAN,"READY",
      CoreContracts.SampleQuality.VALID));
   if(with4)readings.add(new CoreContracts.Reading("TANK:MQ4",1200,null,38.,38.,null,
      CoreContracts.CalibrationReference.AIR_RS_MEAN,"READY",
      CoreContracts.SampleQuality.VALID));
   return new CoreContracts.Snapshot("owner-source","owner-Tank",CoreContracts.Mode.LAB,60000,
      null,null,relay,readings,"stable-fingerprint-provenance");
 }
 private static ResearchFingerprintBundle.Bundle bundle(List<ResearchFingerprintBundle.Variant> list,
                                                        int n,int sensors,int families){
   return new ResearchFingerprintBundle.Bundle("SOURCE-BUNDLE","ORIGINAL-STUDY",700L,
      list,n,sensors,families);
 }
 public static void main(String[] args){
   var a=v("MQ3-STATIC","MQ3","STATIC","ORIGINAL-STUDY",.23,true);
   var b=v("MQ4-TEMPORAL","MQ4","TEMPORAL","ORIGINAL-STUDY",.22,true);
   var c=v("MQ4-MULTI-CONCENTRATION","MQ4","TEMPORAL","ORIGINAL-STUDY",.21,true);
   var two=bundle(List.of(a,b,c),2,2,2);
   var passed=ResearchFingerprintBundle.evaluate(two,device(),snap(true,true));
   test(passed.status==ResearchFingerprintBundle.Status.RESEARCH_SOURCE_BUNDLE_MATCH,
       "two different MQ channels and feature families");
   test(passed.matchedVariants==2,"third correlated concentration template cannot count independently");
   test(passed.distinctSensorChannels==2&&passed.distinctFeatureFamilies==2,
       "diverse channels and techniques verified");
   test(passed.diagnostics.stream().anyMatch(x->x.contains("CORRELATED")),
       "extra concentration fingerprint logged but not double-counted");
   test(!passed.gasIdentityConfirmed&&!passed.ppmCertified&&!passed.physicalTankG3
       &&!passed.fieldSafetyAlarmAuthorized,"source-research only no field qualification");
   var missing=ResearchFingerprintBundle.evaluate(two,device(),snap(false,true));
   test(missing.status==ResearchFingerprintBundle.Status.PARTIAL_BUNDLE,
       "missing MQ4 remains partial not imputed");
   test(missing.matchedVariants==1,"one MQ3 is one source family");
   var off=ResearchFingerprintBundle.evaluate(two,device(),snap(true,false));
   test(off.status==ResearchFingerprintBundle.Status.NOT_EVALUABLE,
       "relay off no source bundle");
   var noThreshold=ResearchFingerprintBundle.evaluate(
     bundle(List.of(v("NO-CAL","MQ3","STATIC","ORIGINAL-STUDY",.22,false)),1,1,1),
     device(),snap(true,true));
   test(noThreshold.status==ResearchFingerprintBundle.Status.NO_INDEPENDENT_CALIBRATION_THRESHOLD,
       "unqualified source cutoffs fail closed");
   var high=ResearchFingerprintBundle.evaluate(
     bundle(List.of(v("OUTLIER","MQ3","STATIC","ORIGINAL-STUDY",8.,true)),1,1,1),
     device(),snap(true,true));
   test(high.status==ResearchFingerprintBundle.Status.HIGH_SOURCE_RESIDUAL,
       "source outlier is NOT false molecular absence");
   boolean rejected=false;
   try{bundle(List.of(v("FOREIGN-ROOT","MQ3","STATIC","OTHER-ROOT",.2,true)),1,1,1);}
   catch(IllegalArgumentException ex){rejected=true;}
   test(rejected,"cannot smuggle foreign source root into one study bundle");
   rejected=false;
   try{bundle(List.of(a,a),2,1,1);}catch(IllegalArgumentException ex){rejected=true;}
   test(rejected,"cannot repeat same source technique");
   var one=ResearchFingerprintBundle.evaluate(bundle(List.of(a),1,1,1),device(),snap(true,true));
   test(one.isEligibleResearchHypothesis&&one.matchedVariants==1,
       "one sensor gracefully valid for a one-channel study");
   System.out.println("CHEMCORE_NEXT_DIVERSE_BUNDLE_G1_PASS assertions="+checks+
      " source_count_one=true concentration_dedup=true different_MQ=true G3=false");
 }
}
