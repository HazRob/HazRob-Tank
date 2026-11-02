package com.hazrob.tank.chemcore.next;

import java.util.List;

/** Source-defined synthetic environmental model G1 unit tests, never Tank G3. */
public final class EnvironmentEvidenceExpertSmoke {
    private static int checks;
    private static void ok(boolean yes,String why){
        checks++;if(!yes)throw new AssertionError("P04.4 #"+checks+": "+why);
    }
    private static EnvironmentEvidenceExpert.SourceModel model() {
        return new EnvironmentEvidenceExpert.SourceModel("SYNTHETIC-SOURCE-ROOT",
            "SOURCE-LOG-SLOPE-FIXTURE","MQ4","CONTINUOUS",25,40,.01,.005,10,45,10,90);
    }
    private static EnvironmentEvidenceExpert.Qualification q(){
        return new EnvironmentEvidenceExpert.Qualification(true,true,true,true);
    }
    private static CoreContracts.SensorInstance sensor(String model){
        return new CoreContracts.SensorInstance("physical-4",model,"test-board","CONTINUOUS");
    }
    private static CoreContracts.Snapshot snap(Double t,Double rh,Double ratio,
            boolean relay,String phase,CoreContracts.SampleQuality quality){
        CoreContracts.Reading r=new CoreContracts.Reading("physical-4",null,null,
            ratio==null?null:ratio*10.0,ratio==null?null:10.0,null,
            CoreContracts.CalibrationReference.AIR_RS_MEAN,phase,quality);
        return new CoreContracts.Snapshot("S-T-RH-1","device-A",CoreContracts.Mode.LAB,
            1000,t,rh,relay,List.of(r),"synthetic-test-stable");
    }
    private static EnvironmentEvidenceExpert.Output eval(CoreContracts.Snapshot s){
        return EnvironmentEvidenceExpert.evaluate(model(),q(),sensor("MQ4"),s);
    }
    private static void failed(CoreContracts.Snapshot s,EnvironmentEvidenceExpert.Status expected){
        EnvironmentEvidenceExpert.Output o=eval(s);
        ok(o.status==expected,"expected "+expected+" actual "+o.status);
        ok(o.correctedReferenceAirRatio==null&&o.vectorCopy()==null,
            "no valid transform from invalid measurement");
    }
    public static void main(String[] args){
        CoreContracts.Snapshot input=snap(30.0,50.0,.8,true,"CONTINUOUS",CoreContracts.SampleQuality.VALID);
        EnvironmentEvidenceExpert.Output v=eval(input);
        ok(v.status==EnvironmentEvidenceExpert.Status.RESEARCH_TRANSFORM_ONLY,
           "source model qualified for math, not ppm");
        double known=.8*Math.exp(-.1);
        ok(Math.abs(v.correctedReferenceAirRatio-known)<1e-12,"expected source log correction");
        double[] vector=v.vectorCopy();
        ok(vector.length==3 && vector[0]==.8&&vector[1]==30&&vector[2]==50,
           "3D observed signal temperature humidity");
        vector[0]=123;
        ok(v.vectorCopy()[0]==.8,"immutable 3D vector");
        ok(v.sourceRoot.equals("SYNTHETIC-SOURCE-ROOT"),"one environmental source root");
        ok(!v.gasIdentityConfirmed&&!v.concentrationAuthorized&&!v.g3Qualified,"G1 only");
        failed(snap(null,50.0,.8,true,"CONTINUOUS",CoreContracts.SampleQuality.VALID),
            EnvironmentEvidenceExpert.Status.INCOMPLETE_ENVIRONMENT);
        failed(snap(30.0,null,.8,true,"CONTINUOUS",CoreContracts.SampleQuality.VALID),
            EnvironmentEvidenceExpert.Status.INCOMPLETE_ENVIRONMENT);
        failed(snap(47.0,50.0,.8,true,"CONTINUOUS",CoreContracts.SampleQuality.VALID),
            EnvironmentEvidenceExpert.Status.OUTSIDE_SOURCE_DOMAIN);
        failed(snap(30.0,95.0,.8,true,"CONTINUOUS",CoreContracts.SampleQuality.VALID),
            EnvironmentEvidenceExpert.Status.OUTSIDE_SOURCE_DOMAIN);
        failed(snap(30.0,50.0,.8,false,"CONTINUOUS",CoreContracts.SampleQuality.VALID),
            EnvironmentEvidenceExpert.Status.INVALID_SAMPLE);
        failed(snap(30.0,50.0,.8,true,"LOW",CoreContracts.SampleQuality.VALID),
            EnvironmentEvidenceExpert.Status.INVALID_SAMPLE);
        failed(snap(30.0,50.0,.8,true,"CONTINUOUS",CoreContracts.SampleQuality.STALE),
            EnvironmentEvidenceExpert.Status.INVALID_SAMPLE);
        failed(snap(30.0,50.0,null,true,"CONTINUOUS",CoreContracts.SampleQuality.NO_SAMPLE),
            EnvironmentEvidenceExpert.Status.INVALID_SAMPLE);
        ok(EnvironmentEvidenceExpert.evaluate(model(),
           EnvironmentEvidenceExpert.Qualification.unverified(),sensor("MQ4"),input).status
           ==EnvironmentEvidenceExpert.Status.SOURCE_UNQUALIFIED,"unverified source must abstain");
        ok(EnvironmentEvidenceExpert.evaluate(model(),q(),sensor("MQ8"),input).status
           ==EnvironmentEvidenceExpert.Status.SENSOR_OR_PHASE_MISMATCH,"different MQ sensor");
        ok(EnvironmentEvidenceExpert.evaluate(model(),q(),sensor("MQ4"),null).status
           ==EnvironmentEvidenceExpert.Status.INVALID_SAMPLE,"missing snapshot");
        boolean rejected=false;
        try {new EnvironmentEvidenceExpert.SourceModel("root","model","MQ","HEAT",25,40,.1,.2,
            30,45,10,90);}catch(IllegalArgumentException ex){rejected=true;}
        ok(rejected,"source reference must fall inside verified domain");
        System.out.println("CHEMCORE_NEXT_P04_4_ENVIRONMENT_PASS assertions="+checks+
            " source_3D=true no_default_coefficients=true physical_G3=0");
    }
}
