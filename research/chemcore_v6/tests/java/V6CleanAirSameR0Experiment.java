package com.hazrob.tank.chemcore.v6;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Research replay ONLY. Rs=R0 is same-air baseline, not certified absence of gases. */
public final class V6CleanAirSameR0Experiment {
  private static final String[] MODELS={"MQ2","MQ3","MQ4","MQ5","MQ7","MQ8","MQ9","MQ135"};
  private static final double[] R0={11.0,15.0,18.0,13.0,27.0,22.0,19.0,31.0};
  private static int assertions=0;
  private static void check(boolean b,String message){
    assertions++;if(!b)throw new AssertionError("clean-air "+assertions+" "+message);
  }
  private static List<Engine.Channel> scenario(double[] scale){
    List<Engine.Channel> out=new ArrayList<>();
    for(int i=0;i<MODELS.length;i++)
      out.add(new Engine.Channel(MODELS[i],MODELS[i]+"-TANK-PHYSICAL-"+i,R0[i]*scale[i],R0[i]));
    return out;
  }
  private static void run(String label,double[] scale){
    Engine.Result result=Engine.analyze(scenario(scale));
    ChemCoreTankV6.Result coordinated=ChemCoreTankV6.analyze(
      new ChemCoreTankV6.Request(scenario(scale),Collections.emptyList(),25.0,48.0,120.0));
    check(result.observedPhysicalChannels==8,label+" raw MQ readings");
    check(result.ratioReadyChannels==8,label+" valid Rs/R0 channels");
    check(result.allSourceFits.size()==49,label+" all 49 research curves still inverted");
    check(result.rankedCandidates.size()==13,label+" all 13 chemical reference groups retained");
    check(!result.ppmReportable&&!result.actualTankCalibrationVerified,
      label+" no physical calibration or reportable PPM");
    check(!coordinated.reportablePpm&&!coordinated.physicalCalibrationProven,
      label+" no coordinator quantitative certification");
    check(coordinated.fingerprintSpaces.isEmpty(),label+" snapshot cannot fabricate temporal fingerprints");
    check(coordinated.chemicalEvidence.size()==13,label+" source candidates NOT confirmed gas identification");
    System.out.println("SCENARIO,"+label+",RAW,"+result.observedPhysicalChannels+
      ",RATIO_READY,"+result.ratioReadyChannels+",CURVES,"+result.allSourceFits.size()+
      ",GAS_GROUPS,"+result.rankedCandidates.size()+",IDENTITY,UNKNOWN,PPM_REPORTABLE,false");
    for(int i=0;i<Math.min(6,result.rankedCandidates.size());i++){
      Engine.Candidate c=result.rankedCandidates.get(i);
      System.out.printf(Locale.ROOT,
        "RANK,%s,%d,%s,chemical_id=%d,sensors=%d,joint_ppm=%.9g,RMSE_log10=%.9g,extrapolation=%s%n",
        label,i+1,c.primaryLabel,c.chemicalId,c.supportingSensorInstances,c.jointResearchPpm,
        c.jointResidualRmseLog10,Boolean.toString(c.anySourceDomainExtrapolated));
    }
    long outside=0;
    for(Engine.SourceFit fit:result.allSourceFits){
      check(Double.isFinite(fit.fitPpm)&&fit.fitPpm>0,"finite exploratory inversion");
      if(fit.outsideSourceRange)outside++;
      if("EXACT_RS_EQUALS_R0".equals(label))
        System.out.printf(Locale.ROOT,"CURVE,%s,%s,%s,Rs_R0=%.8f,ppm_research=%.9g,outside=%s%n",
          label,fit.id,fit.gasLabel,fit.observedRatio,fit.fitPpm,Boolean.toString(fit.outsideSourceRange));
    }
    System.out.println("SUMMARY,"+label+",OUTSIDE_SOURCE_CURVES,"+outside+",TOTAL_SOURCE_CURVES,"+
      result.allSourceFits.size());
  }
  public static void main(String[] args){
    double[] eq=new double[8],plus=new double[8],minus=new double[8],mixed=new double[8];
    Arrays.fill(eq,1.0);Arrays.fill(plus,1.02);Arrays.fill(minus,0.98);
    for(int i=0;i<mixed.length;i++)mixed[i]=(i%2==0?1.02:0.98);
    run("EXACT_RS_EQUALS_R0",eq);
    run("SAME_AIR_PLUS_2PCT",plus);
    run("SAME_AIR_MINUS_2PCT",minus);
    run("SAME_AIR_MIXED_2PCT",mixed);
    Engine.Result noR0=Engine.analyze(Arrays.asList(
      new Engine.Channel("MQ2","TEST",11,Double.NaN),
      new Engine.Channel("MQ3","TEST",15,Double.NaN)));
    check(noR0.observedPhysicalChannels==2&&noR0.ratioReadyChannels==0,
      "unavailable R0 never fabricated");
    check(noR0.allSourceFits.isEmpty()&&noR0.rankedCandidates.isEmpty(),
      "without R0 no concentration inversions");
    System.out.println("PASS,V6CleanAirSameR0Experiment,assertions="+assertions+",G3=0");
  }
}
