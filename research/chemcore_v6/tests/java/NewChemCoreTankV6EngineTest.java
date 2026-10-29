package com.hazrob.tank.chemcore.v6;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Pure JDK implementation tests. All sample concentrations here are synthetic. */
public final class NewChemCoreTankV6EngineTest {
    static int checks=0;
    static void assertThat(boolean ok,String msg){
        checks++;if(!ok)throw new AssertionError("assertion "+checks+": "+msg);
    }

    static List<Engine.Channel> sourceCoExample(double ppm){
        // Generate one hypothetical Rs/R0 for each sensor directly from original CO
        // curves. Not a real exposure or manufacturer/device calibration.
        Map<String,Double> ratios=new HashMap<>();
        for(SourceCatalog.Curve src:SourceCatalog.curves()){
            if(src.legacyChemicalId!=20L)continue;
            ratios.put(src.sensorType,Math.pow(10,src.log10Prediction(Math.log10(ppm))));
        }
        List<Engine.Channel> v=new ArrayList<>();
        for(Map.Entry<String,Double> e:ratios.entrySet()){
            v.add(new Engine.Channel(e.getKey(),e.getKey()+"-fixture-01",10000*e.getValue(),10000));
        }
        return v;
    }

    public static void main(String[] args){
        assertThat(SourceCatalog.curves().size()==43,"43 curves in independent catalog");
        var co=sourceCoExample(200);
        assertThat(co.size()==8,"CO source has all 8 MQ types");
        var result=Engine.analyze(co);
        assertThat(result.allSourceFits.size()==43,"every finite available source fit included");
        assertThat(result.rankedCandidates.size()==13,"all source chemical identities represented");
        assertThat(result.observedPhysicalChannels==8,"8 unique physical sensors");
        assertThat(result.rejectedNumericalChannels==0,"no invalid numeric fixtures");
        assertThat(!result.actualTankCalibrationVerified&&!result.ppmReportable,
                   "synthetic inputs never become calibrated gas statements");
        assertThat(result.rankingSemantics.contains("not_calibrated_probability"),"ranking is not probability");
        Engine.Candidate carbonMonoxide=null;
        for(Engine.Candidate c:result.rankedCandidates){
            assertThat(c.jointResearchPpm>0&&Double.isFinite(c.jointResearchPpm),
                       "each candidate joint numeric fit valid");
            assertThat(c.contributingSourceCurves>=1,"candidate source retained");
            if(c.chemicalId==20L)carbonMonoxide=c;
        }
        assertThat(carbonMonoxide!=null,"CO remains visible");
        assertThat(carbonMonoxide.supportingSensorInstances==8,"eight CO evidence channels");
        assertThat(Math.abs(carbonMonoxide.jointResearchPpm-200)<1e-7,
                   "joint log least squares exactly fits same-source artificial response");
        assertThat(carbonMonoxide.jointResidualRmseLog10<1e-10,
                   "synthetic joint residual near zero");
        assertThat(result.rankedCandidates.get(0).chemicalId==20L,
                   "eight sensor source CO hypothesis ranks first by breadth");
        var only135=Engine.analyze(Collections.singletonList(
          new Engine.Channel("mq135","single-mq135",42000,10000)));
        assertThat(only135.allSourceFits.size()==6,"single channel retains all 6 MQ135 curves");
        assertThat(only135.rankedCandidates.size()==6,"no artificial two-sensor exclusion");
        assertThat(only135.rankedCandidates.get(0).supportingSensorInstances==1,
                   "single-sensor research fit still visible");
        var bad=Engine.analyze(Arrays.asList(
          new Engine.Channel("MQ2","b",Double.NaN,10000),
          new Engine.Channel("MQ3","c",1000,0),
          new Engine.Channel("MQ8","d",Double.POSITIVE_INFINITY,8000),
          new Engine.Channel("MQ135","e",10000,-1)));
        assertThat(bad.rejectedNumericalChannels==2,"only broken raw Rs observations rejected");
        assertThat(bad.rawChannelsAwaitingR0==2,"raw Rs with invalid R0 preserved for non-ratio methods");
        assertThat(bad.observedPhysicalChannels==2,"all valid raw observations retained");
        assertThat(bad.ratioReadyChannels==0,"ratio math not fabricated with missing R0");
        assertThat(bad.retainedRawSensorRecords.size()==2,"raw samples accessible to future techniques");
        assertThat(bad.allSourceFits.isEmpty(),"never invent ratio without a reference");
        var noR0=Engine.analyze(Collections.singletonList(
          new Engine.Channel("MQ2","not_calibrated",15000,Double.NaN)));
        assertThat(noR0.observedPhysicalChannels==1,"uncalibrated MQ raw Rs kept");
        assertThat(noR0.rawChannelsAwaitingR0==1,"missing R0 explicitly counted");
        assertThat(noR0.rejectedNumericalChannels==0,"missing reference is not corrupt raw data");
        assertThat(noR0.allSourceFits.isEmpty(),"no artificial ppm without source ratio");
        assertThat(Engine.analyze(null).rankedCandidates.isEmpty(),"no readings means empty results");
        var duplicate=sourceCoExample(200);
        duplicate.add(duplicate.get(0));
        var twice=Engine.analyze(duplicate);
        assertThat(twice.observedPhysicalChannels==8,"duplicate instance does not inflate sensor breadth");
        assertThat(twice.allSourceFits.size()==43,"duplicate source rows do not duplicate evidence");
        var extra=Engine.analyze(Collections.singletonList(
           new Engine.Channel("MQ2","unknown_calibration",1000000000,0.001)));
        assertThat(extra.allSourceFits.size()>0,"valid extrapolation not censored");
        assertThat(extra.allSourceFits.stream().anyMatch(s->s.outsideSourceRange),
                  "extrapolation annotated as research uncertainty");
        assertThat(extra.allSourceFits.stream().anyMatch(s->!s.advisories.isEmpty()),
                  "research provenance caveats retained");
        var stableSource=SourceCatalog.curves();
        try{stableSource.clear();throw new AssertionError("source mutable");}
        catch(UnsupportedOperationException expected){checks++;}
        System.out.println("PASS IndependentNewChemCoreTankV6: "+checks+
           " assertions; all 43 source curves, 13 candidate groups, no predecessor engine");
    }
}
