package com.hazrob.tank.chemcore.next;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** P04.3 deterministic synthetic series and failure-injection G1 tests. */
public final class TemporalEvidenceExpertSmoke {
    private static int checks;
    private static void ok(boolean b,String why) {
        checks++;if(!b)throw new AssertionError("P04.3 #"+checks+": "+why);
    }
    private static TemporalEvidenceExpert.Protocol protocol() {
        return new TemporalEvidenceExpert.Protocol("ORIGINAL_REFERENCE_ROOT","RAW_AIR_RATIO_TEMPORAL_4D",
            "MQ4","CONTINUOUS",new long[][]{{0,2000},{10000,12000},{20000,22000},{30000,32000}},2,1500);
    }
    private static CoreContracts.SensorInstance sensor() {
        return new CoreContracts.SensorInstance("sensor-MQ4-uid","MQ4","board-A","CONTINUOUS");
    }
    private static CoreContracts.Snapshot sample(String id,long t,double ratio,boolean relay,
            CoreContracts.SampleQuality quality,String manifest,String device,String heater) {
        CoreContracts.Reading r=new CoreContracts.Reading("sensor-MQ4-uid",null,null,
            ratio*10.0,10.0,null,CoreContracts.CalibrationReference.AIR_RS_MEAN,heater,quality);
        return new CoreContracts.Snapshot(id,device,CoreContracts.Mode.FIELD_SESSION,
            t,25.0,40.0,relay,Arrays.asList(r),manifest);
    }
    private static List<CoreContracts.Snapshot> series() {
        List<CoreContracts.Snapshot> all=new ArrayList<>();
        long[] ts={0,1000,10000,11000,20000,21000,30000,31000};
        double[] rs={1,1,.8,.8,.6,.6,.5,.5};
        for(int i=0;i<ts.length;i++)all.add(sample("S-"+i,ts[i],rs[i],true,
            CoreContracts.SampleQuality.VALID,"sha256-stable-source","device-A","CONTINUOUS"));
        return all;
    }
    private static TemporalEvidenceExpert.Qualification verified() {
        return new TemporalEvidenceExpert.Qualification(true,true,true,true);
    }
    private static TemporalEvidenceExpert.Output run(List<CoreContracts.Snapshot> l) {
        return TemporalEvidenceExpert.analyze(protocol(),verified(),sensor(),"device-A",
            "exposure-001",0,l);
    }
    private static void expect(List<CoreContracts.Snapshot> l,TemporalEvidenceExpert.Status s) {
        TemporalEvidenceExpert.Output result=run(l);
        ok(result.status==s,"expected "+s+" actual "+result.status);
        ok(result.fourResearchFeatures==null && !result.hasFeatures(),
           "invalid source never receives fabricated temporal features");
    }
    private static void close(double a,double b,String reason) {
        ok(Math.abs(a-b)<1e-12,reason+" value "+a);
    }
    public static void main(String[] args) {
        List<CoreContracts.Snapshot> real=series();
        TemporalEvidenceExpert.Output v=run(real);
        ok(v.status==TemporalEvidenceExpert.Status.RESEARCH_FEATURES_ONLY,"full time source");
        close(v.baselineRatio,1.0,"baseline");
        double[] f=v.featureCopy();
        close(f[0],-0.2,"early");close(f[1],-0.4,"middle");close(f[2],-0.5,"late");
        close(f[3],-0.3,"derived late minus early");
        ok(v.samplesPerWindow[0]==2&&v.samplesPerWindow[3]==2,"real count per window");
        ok(!v.gasIdentityConfirmed&&!v.ppmAuthorized&&!v.g3Qualified,"research only");
        ok(v.sourceRoot.equals("ORIGINAL_REFERENCE_ROOT"),"one source root, not four votes");
        f[0]=123;close(v.featureCopy()[0],-0.2,"immutable array");
        ok(TemporalEvidenceExpert.analyze(protocol(),TemporalEvidenceExpert.Qualification.unverified(),
            sensor(),"device-A","exposure-001",0,real).status
            ==TemporalEvidenceExpert.Status.UNQUALIFIED_PROTOCOL,"source not validated must abstain");
        ok(TemporalEvidenceExpert.analyze(protocol(),verified(),
            new CoreContracts.SensorInstance("sensor-MQ4-uid","MQ8","board","CONTINUOUS"),
            "device-A","exposure-001",0,real).status
            ==TemporalEvidenceExpert.Status.SENSOR_MISMATCH,"MQ8 never substitutes MQ4");
        expect(new ArrayList<>(),TemporalEvidenceExpert.Status.INVALID_SERIES);

        List<CoreContracts.Snapshot> changed=series();
        changed.set(4,sample("S-4",20000,.6,true,CoreContracts.SampleQuality.STALE,
            "sha256-stable-source","device-A","CONTINUOUS"));
        expect(changed,TemporalEvidenceExpert.Status.QUALITY_OR_HEATER_INVALID);
        changed=series();changed.set(4,sample("S-4",20000,.6,false,CoreContracts.SampleQuality.VALID,
            "sha256-stable-source","device-A","CONTINUOUS"));
        expect(changed,TemporalEvidenceExpert.Status.QUALITY_OR_HEATER_INVALID);
        changed=series();changed.set(4,sample("S-4",20000,.6,true,CoreContracts.SampleQuality.VALID,
            "sha256-stable-source","device-B","CONTINUOUS"));
        expect(changed,TemporalEvidenceExpert.Status.TIMEBASE_OR_DEVICE_MISMATCH);
        changed=series();changed.set(4,sample("S-4",20000,.6,true,CoreContracts.SampleQuality.VALID,
            "changed-stable-manifest","device-A","CONTINUOUS"));
        expect(changed,TemporalEvidenceExpert.Status.TIMEBASE_OR_DEVICE_MISMATCH);
        changed=series();changed.set(4,sample("S-4",20000,.6,true,CoreContracts.SampleQuality.VALID,
            "sha256-stable-source","device-A","LOW"));
        expect(changed,TemporalEvidenceExpert.Status.QUALITY_OR_HEATER_INVALID);
        changed=series();changed.remove(6);
        expect(changed,TemporalEvidenceExpert.Status.MISSING_OR_SHORT_WINDOW);
        changed=series();changed.set(3,sample("S-3",10000,.8,true,CoreContracts.SampleQuality.VALID,
            "sha256-stable-source","device-A","CONTINUOUS"));
        expect(changed,TemporalEvidenceExpert.Status.TIMEBASE_OR_DEVICE_MISMATCH);
        changed=series();changed.set(1,sample("S-0",1000,1,true,CoreContracts.SampleQuality.VALID,
            "sha256-stable-source","device-A","CONTINUOUS"));
        expect(changed,TemporalEvidenceExpert.Status.TIMEBASE_OR_DEVICE_MISMATCH);
        changed=series();changed.set(3,sample("S-3",13000,.8,true,CoreContracts.SampleQuality.VALID,
            "sha256-stable-source","device-A","CONTINUOUS"));
        expect(changed,TemporalEvidenceExpert.Status.MISSING_OR_SHORT_WINDOW);
        changed=series();changed.set(7,sample("S-7",31600,.5,true,CoreContracts.SampleQuality.VALID,
            "sha256-stable-source","device-A","CONTINUOUS"));
        expect(changed,TemporalEvidenceExpert.Status.WINDOW_GAP_TOO_LARGE);
        ok(TemporalEvidenceExpert.analyze(protocol(),verified(),sensor(),"device-A","",0,real).status
            ==TemporalEvidenceExpert.Status.INVALID_SERIES,"exposure identifier needed");
        System.out.println("CHEMCORE_NEXT_P04_3_TEMPORAL_PASS assertions="+checks+
            " 4D_from_one_source=true time_series_required=true G3=0");
    }
}
