package com.hazrob.tank.chemcore.v6;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Ten new V6 source- and algebra-based techniques, not device G3 calibration. */
public final class V6BatchTenDiagnosticsTest {
    private static int asserts=0;
    private static void check(boolean x,String message){
        asserts++;if(!x)throw new AssertionError("check "+asserts+": "+message);
    }
    private static void near(double x,double expected,double tolerance,String why){
        check(Double.isFinite(x)&&Math.abs(x-expected)<=tolerance,
              why+" actual="+x+" expected="+expected);
    }
    public static void main(String[] args){
        // 01.1 — independent ADC-to-Rs circuit law (Rs high-side/RL low-side).
        var circuit=MeasurementDiagnosticsV6.adcToRs(2048,4096,3.3,2,5,10000);
        check(circuit.numericallyDefined,"valid circuit");
        near(circuit.vAdc,1.65,1e-10,"ADC pin voltage");
        near(circuit.vSensor,3.3,1e-10,"physical divider sensor output");
        near(circuit.rsOhm,10000*(5/3.3-1),1e-7,"Ohm law");
        check(!MeasurementDiagnosticsV6.adcToRs(0,4096,3.3,2,5,10000)
              .numericallyDefined,"zero divider voltage impossible Rs");
        check(!MeasurementDiagnosticsV6.adcToRs(4096,4096,3.3,2,5,10000)
              .numericallyDefined,"Vout>Vc impossible for specified circuit");
        check(!MeasurementDiagnosticsV6.adcToRs(Double.NaN,4096,3.3,2,5,10000)
              .numericallyDefined,"NaN ADC no numeric result");
        check(!MeasurementDiagnosticsV6.adcToRs(3500,4096,3.3,0,5,10000)
              .numericallyDefined,"zero divider gain undefined");

        // 01.2 — clean-air reference ratio, source *definition* explicit.
        var air=MeasurementDiagnosticsV6.cleanAirR0(
            new double[]{9830,19660,29490,Double.NaN,-1},9.83);
        check(air.received==5&&air.finiteAirSamples==3&&air.invalidSamples==2,
              "all original samples accounted");
        near(air.medianAirRs,19660,1e-9,"clean-air median");
        near(air.r0Ohm,2000,1e-9,"source air-Rs to R0 factor");
        check(!air.physicalDeviceCalibrationProven,
              "source-derived R0 never claims proven clean-air experiment");
        var badAir=MeasurementDiagnosticsV6.cleanAirR0(
            new double[]{100,200},0);
        check(!badAir.numericallyDefined&&Double.isNaN(badAir.r0Ohm),
              "zero source factor not invented");
        check(!MeasurementDiagnosticsV6.cleanAirR0(null,9.83).numericallyDefined,
              "missing clean-air measurements");

        // 01.6 — full signal-quality audit without threshold rejection.
        List<SignalFeaturesV6.Sample> trace=Arrays.asList(
            new SignalFeaturesV6.Sample(3000,200),
            new SignalFeaturesV6.Sample(0,100),
            new SignalFeaturesV6.Sample(1000,100),
            new SignalFeaturesV6.Sample(3000,300),
            new SignalFeaturesV6.Sample(-1,150),
            new SignalFeaturesV6.Sample(4000,Double.NaN));
        var q=MeasurementDiagnosticsV6.signalQuality(trace);
        check(q.total==6&&q.validResistance==4&&q.invalidResistance==2,
              "only mathematically unusable samples removed");
        check(q.duplicateTimestamps==1,"duplicate timestamp recorded, still used");
        near(q.medianResistance,150,1e-10,"MAD centre");
        near(q.robustMad,50,1e-10,"median absolute deviation");
        near(q.timeSpanSeconds,3,1e-10,"time span");
        check(q.coefficientOfVariation>0&&Double.isFinite(q.coefficientOfVariation),
              "coefficient of variation");
        check(q.notes.contains("DUPLICATE_SAMPLE_TIMESTAMPS"),
              "duplicate label advisory not gate");

        // 01.7 — two-window drift *indicator*, not chemical/hardware certainty.
        var drift=MeasurementDiagnosticsV6.windowDrift(Arrays.asList(
            new SignalFeaturesV6.Sample(3000,200),
            new SignalFeaturesV6.Sample(0,100),
            new SignalFeaturesV6.Sample(1000,100),
            new SignalFeaturesV6.Sample(2000,200)),2);
        near(drift.earlyMedianRs,100,1e-10,"early median");
        near(drift.lateMedianRs,200,1e-10,"late median");
        near(drift.relativeDifference,1,1e-10,"relative drift indicator");
        near(drift.log10Difference,Math.log10(2),1e-12,"log drift indicator");
        check(!drift.deviceDriftProven,"drift is not physically attributed");
        check(Double.isNaN(MeasurementDiagnosticsV6.windowDrift(
            Collections.singletonList(new SignalFeaturesV6.Sample(0,100)),2)
            .relativeDifference),"single sample cannot invent drift");
        try{
            MeasurementDiagnosticsV6.windowDrift(Collections.emptyList(),0);
            throw new AssertionError("invalid window accepted");
        }catch(IllegalArgumentException expected){asserts++;}

        // 02.6 — true named/context-labelled 4D output, no unit merging.
        var four=MeasurementDiagnosticsV6.physical4d(2000,1000,25,50,180);
        check(four.names.length==4&&four.usableDimensions==4,"4 coordinates retained");
        near(four.values[0],Math.log10(2),1e-12,"log Rs/R0");
        near(four.values[1],25,1e-12,"temperature C");
        near(four.values[2],50,1e-12,"humidity percent");
        near(four.values[3],180,1e-12,"time seconds");
        check(!four.quantitativeGasCalibration,"context not a gas calibration");
        var missing=MeasurementDiagnosticsV6.physical4d(1500,Double.NaN,23,55,70);
        check(missing.usableDimensions==3&&Double.isNaN(missing.values[0]),
              "missing R0 does not destroy available context");
        var clone=four.copyValues();clone[0]=999;
        near(four.values[0],Math.log10(2),1e-12,"vector array defensive clone");

        // 02.7 — rank all usable source dimensions, no low-variance gating.
        var ranked=ResearchDiagnosticsV6.featureVarianceRanking(new double[][]{
            {1,5,100},{2,5,200},{3,5,300},{Double.NaN,Double.NaN,Double.NaN}});
        check(ranked.size()==3,"all 3 finite-coordinate features returned");
        check(ranked.get(0).featureIndex==2&&ranked.get(1).featureIndex==0
                &&ranked.get(2).featureIndex==1,"variance ordering");
        near(ranked.get(0).variance,20000d/3,1e-7,"variance col2");
        near(ranked.get(2).variance,0,1e-12,"zero variance remains");
        check(ResearchDiagnosticsV6.featureVarianceRanking(new double[][]{
                {Double.NaN,Double.NaN}}).isEmpty(),
              "no fabricated variance from nonfinite data");

        // 03.3 — source-prototype overlap strictly within compatible spaces.
        var adcPairs=ResearchDiagnosticsV6.classOverlap(FingerprintEngineV6.ADC);
        var opsPairs=ResearchDiagnosticsV6.classOverlap(FingerprintEngineV6.OPERATIONAL);
        var uciPairs=ResearchDiagnosticsV6.classOverlap(FingerprintEngineV6.TEMPORAL);
        check(adcPairs.size()==6,"4 ADC classes => six unique comparisons");
        check(opsPairs.size()==1,"2 operational classes => one pair");
        check(uciPairs.size()==153,"18 UCI source prototypes => 153 pairs");
        check(uciPairs.stream().anyMatch(p->p.sameClass),
              "source same-gas across concentration comparisons retained");
        check(uciPairs.stream().anyMatch(p->!p.sameClass),
              "different gases also compared");
        for(var p:uciPairs){
            check(p.sharedDimensions==3,"UCI three-feature vector");
            check(Double.isFinite(p.symmetricMeanDistance)&&p.symmetricMeanDistance>=0,
                  "defined pairwise distance");
        }
        check(ResearchDiagnosticsV6.classOverlap("NO_SUCH_SPACE").isEmpty(),
              "no cross-space comparisons made up");

        // 03.5 — descriptive leave-one-channel-out margin contributions.
        var a=GasFingerprintCatalogV6.inSpace(FingerprintEngineV6.ADC);
        var sourceInput=new FingerprintEngineV6.Input(
            FingerprintEngineV6.ADC,a.get(0).vector(),"SOURCE_NATIVE_FEATURES","reference");
        var importance=ResearchDiagnosticsV6.leaveOneFeatureOut(
            sourceInput,a.get(0).id,a.get(1).id);
        check(importance.size()==6,"all six observed ADC coordinates examined");
        for(var feature:importance){
            check(feature.numericallyDefined,"leave-one-out margin remains defined");
            check(feature.featureIndex!=2&&feature.featureIndex!=6,
                  "no importance assigned to absent MQ4/MQ9 source channels");
        }
        check(ResearchDiagnosticsV6.leaveOneFeatureOut(null,"a","b").isEmpty(),
              "no attribution for absent input");

        // 03.7 — 6 gases x3 levels UCI source confounding; never classifier accuracy.
        var confounding=ResearchDiagnosticsV6.uciConcentrationConfounding();
        check(confounding.size()==6,"six original source gases");
        for(var gas:confounding){
            check(gas.intraPairs==6&&gas.interPairs==15,
                  "all three within-gas levels and five other same-ppm gases");
            check(Double.isFinite(gas.meanWithinGasAcrossLevels)&&
                  Double.isFinite(gas.meanBetweenGasesAtSameLevel),
                  "all source comparison diagnostics defined");
            check(!gas.independentlyValidatedDiscrimination,
                  "do not promote source-shape distances to classifier accuracy");
        }

        // 03.8 — one MQ135 Rs/R0 has 12 alternative curves but 6 reference
        // chemical hypotheses, not uniquely identifying any of those gases.
        Engine.Result fits=Engine.analyze(Collections.singletonList(
            new Engine.Channel("MQ135","physical-mq135-1",42000,10000)));
        var ident=ResearchDiagnosticsV6.sourceIdentifiability(fits);
        check(ident.size()==1,"one physical MQ135 instance");
        check(ident.get(0).alternativeSourceCurves==12,
              "all 12 MQ135 research equations included");
        check(ident.get(0).candidateChemicalIds==6,
              "six ambiguously compatible gas identities");
        check(ident.get(0).sourceRoots==2,
              "two provenance roots, not 12 experiments");
        check(!ident.get(0).uniqueIdentityEstablished,
              "ambiguous source curves do not prove identity");
        check(ResearchDiagnosticsV6.sourceIdentifiability(null).isEmpty(),
              "absence of data not an artificial chemical");
        System.out.println("PASS V6BatchTenDiagnostics: "+asserts+
            " assertions, 10 independent research-stage technique operations");
    }
}
