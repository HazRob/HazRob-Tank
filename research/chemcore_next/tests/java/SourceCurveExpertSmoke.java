package com.hazrob.tank.chemcore.next;

import java.util.ArrayList;
import java.util.List;

/** P04.1 independently reproducible G1 curve algebra and fail-closed proof. */
public final class SourceCurveExpertSmoke {
    private static int tests=0;
    private static void ok(boolean yes,String why) {
        tests++;
        if(!yes) throw new AssertionError("P04.1 #"+tests+": "+why);
    }
    private static void close(double a,double b,String why) {
        ok(Double.isFinite(a)&&Double.isFinite(b)
            &&Math.abs(a-b)<=1e-10*Math.max(1.0,Math.abs(b)),why);
    }
    private static void reject(Runnable task,String why) {
        boolean did=false;
        try{task.run();}catch(IllegalArgumentException expected){did=true;}
        ok(did,why);
    }
    private static CoreContracts.SensorInstance sensor(String model,String heater) {
        return new CoreContracts.SensorInstance("unit-mq2",model,"ADS1",""+heater);
    }
    private static CoreContracts.Reading reading(Double resistance,Double r0,String heater,
                                                  CoreContracts.SampleQuality quality) {
        return new CoreContracts.Reading("unit-mq2",10000,1.8,resistance,100.0,r0,
            CoreContracts.CalibrationReference.SOURCE_R0,heater,quality);
    }
    private static SourceCurveExpert.Curve mqds(Double scale,Double min,Double max,String heater) {
        return new SourceCurveExpert.Curve("MQDS-SRC-001",
            "MQDATASCIENCE_ORIGINAL_GAS_SENSOR_CURVE_FAMILY","MQ2","LPG",136L,
            SourceCurveExpert.Equation.RATIO_EQUALS_A_TIMES_PPM_OVER_SCALE_POWER_B,
            17.6135,-0.4539,scale,min,max,heater);
    }
    public static void main(String[] ignored) {
        SourceCurveExpert.Curve source=mqds(1.,200.,10000.,"HEATER_STEADY");
        for(double concentration:new double[]{200.,250.,500.,1000.,1500.,3000.,10000.}) {
            double ratio=SourceCurveExpert.forwardSourceRatio(source,concentration);
            double inverse=SourceCurveExpert.inverseSourcePpm(source,ratio);
            close(inverse,concentration,"MQDataScience direct/inverse algebra parity");
        }
        SourceCurveExpert.Curve mqsl=new SourceCurveExpert.Curve(
            "MQSL-EX-001","MQSENSORSLIB_MQ135_ALL_EXAMPLE","MQ135",
            "CO",21L,SourceCurveExpert.Equation.PPM_EQUALS_A_TIMES_RATIO_POWER_B,
            605.18,-3.937,null,1.,5000.,"MQ135_HEATER");
        for(double concentration:new double[]{1.,2.,5.,10.,30.,100.,500.}) {
            double ratio=SourceCurveExpert.forwardSourceRatio(mqsl,concentration);
            close(SourceCurveExpert.inverseSourcePpm(mqsl,ratio),concentration,
                "MQSensorsLib inverse/forward parity must use alternate form");
        }
        SourceCurveExpert.Curve withoutScale=mqds(null,200.,10000.,"HEATER_STEADY");
        reject(()->SourceCurveExpert.forwardSourceRatio(withoutScale,500.),
            "MQDataScience source ppm scale omitted; no accidental scale=1");
        reject(()->SourceCurveExpert.inverseSourcePpm(withoutScale,2.0),
            "inverse with unknown scale forbidden");
        reject(()->SourceCurveExpert.forwardSourceRatio(source,0.0),
            "no zero ppm under power law");
        reject(()->SourceCurveExpert.inverseSourcePpm(source,-1.0),
            "no negative source ratio");
        reject(()->SourceCurveExpert.forwardSourceRatio(source,Double.NaN),
            "nonfinite input rejected");
        reject(()->SourceCurveExpert.inverseSourcePpm(mqsl,0.0),
            "no zero ratio");
        reject(()->new SourceCurveExpert.Curve("x","root","MQ2","LPG",null,
            SourceCurveExpert.Equation.PPM_EQUALS_A_TIMES_RATIO_POWER_B,
            0,-1,null,1.,2.,"H"),"invalid A rejected");
        reject(()->new SourceCurveExpert.Curve("x","root","MQ2","LPG",null,
            SourceCurveExpert.Equation.PPM_EQUALS_A_TIMES_RATIO_POWER_B,
            1,0,null,1.,2.,"H"),"invalid B rejected");
        reject(()->new SourceCurveExpert.Curve("x","root","MQ2","LPG",null,
            SourceCurveExpert.Equation.PPM_EQUALS_A_TIMES_RATIO_POWER_B,
            1,-1,null,300.,100.,"H"),"reversed source domain forbidden");
        ok(new SourceCurveExpert.Curve("x","root","MQ2","CO",null,
            SourceCurveExpert.Equation.PPM_EQUALS_A_TIMES_RATIO_POWER_B,
            1,-1,null,null,null,"H").sourcePpmMin==null,
            "unknown source domain may be retained as metadata but never activated");

        SourceCurveExpert.Qualification unverified=SourceCurveExpert.Qualification.unverified();
        SourceCurveExpert.Qualification qualified=new SourceCurveExpert.Qualification(true,true,true,true);
        CoreContracts.SensorInstance instance=sensor("MQ2","HEATER_STEADY");
        double ratio500=SourceCurveExpert.forwardSourceRatio(source,500);
        CoreContracts.Reading valid=reading(ratio500*10.0,10.0,
            "HEATER_STEADY",CoreContracts.SampleQuality.VALID);
        SourceCurveExpert.Output refusal=SourceCurveExpert.evaluate(source,instance,valid,unverified);
        ok(refusal.decision==SourceCurveExpert.Decision.NOT_EVALUABLE,
            "real device source R0 unqualified");
        ok(refusal.abstention==SourceCurveExpert.AbstentionReason.SOURCE_R0_NOT_PROVEN_FOR_DEVICE,
            "unqualified source R0 reason preserved");
        ok(refusal.researchOnlyPpm==null,"no inferred ppm without metadata");
        SourceCurveExpert.Output mismatch=SourceCurveExpert.evaluate(source,
            sensor("MQ7","HEATER_STEADY"),valid,qualified);
        ok(mismatch.abstention==SourceCurveExpert.AbstentionReason.SENSOR_MODEL_MISMATCH,
            "MQ7 cannot apply MQ2 curve");
        SourceCurveExpert.Output badHeater=SourceCurveExpert.evaluate(source,instance,
            reading(ratio500*10,10,"HEATER_CYCLE_LOW",CoreContracts.SampleQuality.VALID),
            qualified);
        ok(badHeater.abstention==SourceCurveExpert.AbstentionReason.HEATER_PHASE_UNQUALIFIED,
            "heater profile mismatch blocks source model");
        SourceCurveExpert.Output missingScale=SourceCurveExpert.evaluate(withoutScale,instance,
            valid,qualified);
        ok(missingScale.abstention==SourceCurveExpert.AbstentionReason.SOURCE_SCALE_UNVERIFIED,
            "missing source scale blocks quantitative estimate");
        SourceCurveExpert.Output missingDomain=SourceCurveExpert.evaluate(
            mqds(1.,null,null,"HEATER_STEADY"),instance,valid,qualified);
        ok(missingDomain.abstention==SourceCurveExpert.AbstentionReason.SOURCE_DOMAIN_UNVERIFIED,
            "no verified source ppm range blocks estimate");
        SourceCurveExpert.Qualification wrongScale=new SourceCurveExpert.Qualification(
            false,true,true,true);
        ok(SourceCurveExpert.evaluate(source,instance,valid,wrongScale).abstention
           ==SourceCurveExpert.AbstentionReason.SOURCE_SCALE_UNVERIFIED,
           "source scale must carry qualification metadata");
        SourceCurveExpert.Qualification unknownHeater=new SourceCurveExpert.Qualification(
            true,true,false,true);
        ok(SourceCurveExpert.evaluate(source,instance,valid,unknownHeater).abstention
           ==SourceCurveExpert.AbstentionReason.HEATER_PHASE_UNQUALIFIED,
           "heater must be independently qualified");
        SourceCurveExpert.Qualification unknownDomain=new SourceCurveExpert.Qualification(
            true,true,true,false);
        ok(SourceCurveExpert.evaluate(source,instance,valid,unknownDomain).abstention
           ==SourceCurveExpert.AbstentionReason.SOURCE_DOMAIN_UNVERIFIED,
           "range qualification gate");
        SourceCurveExpert.Output synthetic=SourceCurveExpert.evaluate(source,instance,valid,qualified);
        close(synthetic.researchOnlyPpm,500.,"fully supplied synthetic fixture can algebraically recover ppm");
        ok(synthetic.decision==SourceCurveExpert.Decision.RESEARCH_SOURCE_QUANTITATIVE_ESTIMATE,
            "explicit research only state");
        ok(!synthetic.confirmedGas&&!synthetic.fieldPpmAuthorized
           &&!synthetic.safetyAlarmAuthorized&&!synthetic.g3Qualified,
           "even all external flags cannot manufacture Tank G3");
        ok(synthetic.canonicalChemicalUid==136L,"canonical source UID does not confirm identity");
        ok(synthetic.sourceRoot.equals(source.sourceRoot),"source-root provenance preserved");
        double ratioOutside=SourceCurveExpert.forwardSourceRatio(source,15000);
        SourceCurveExpert.Output outside=SourceCurveExpert.evaluate(source,instance,
            reading(ratioOutside*10,10,"HEATER_STEADY",
                CoreContracts.SampleQuality.VALID),qualified);
        ok(outside.decision==SourceCurveExpert.Decision.OUTSIDE_SOURCE_DOMAIN,
            "source extrapolation blocked");
        ok(outside.researchOnlyPpm==null,"no extrapolated ppm output");
        CoreContracts.Reading stale=reading(ratio500*10,10,"HEATER_STEADY",
            CoreContracts.SampleQuality.STALE);
        ok(SourceCurveExpert.evaluate(source,instance,stale,qualified).abstention
           ==SourceCurveExpert.AbstentionReason.READING_INVALID_OR_MISSING,
           "stale I2C sample blocked");
        ok(SourceCurveExpert.evaluate(source,instance,null,qualified).decision
           ==SourceCurveExpert.Decision.NOT_EVALUABLE,"missing reading");
        SourceCurveExpert.Curve unknownLabel=new SourceCurveExpert.Curve(
            "MQDS-UNKNOWN","same-root","MQ2","NH4",null,
            SourceCurveExpert.Equation.RATIO_EQUALS_A_TIMES_PPM_OVER_SCALE_POWER_B,
            6.2968,-0.3892,1.0,1.0,10000.0,"HEATER_STEADY");
        ok(SourceCurveExpert.evaluate(unknownLabel,instance,reading(
            SourceCurveExpert.forwardSourceRatio(unknownLabel,100)*10,10,
            "HEATER_STEADY",CoreContracts.SampleQuality.VALID),qualified)
            .canonicalChemicalUid==null,
            "NH4 source label never assigned molecular UID");
        System.out.println("CHEMCORE_NEXT_P04_1_SOURCE_CURVE_PASS assertions="+tests
            +" original_curve_forms=2 source_ppm_algebra_only=true physical_G3=false");
    }
}
