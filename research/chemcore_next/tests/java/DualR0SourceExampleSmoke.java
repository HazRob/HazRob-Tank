package com.hazrob.tank.chemcore.next;

import java.util.HashSet;
import java.util.Set;

/**
 * P11.2 G1 JVM regression. Tests pinned coefficients and both denominators,
 * not a real gas, quantitative Tank accuracy, or G3 promotion.
 */
public final class DualR0SourceExampleSmoke {
    static int assertions=0;
    private static void check(boolean ok,String why){
        assertions++;
        if(!ok)throw new AssertionError("P11.2 assertion "+assertions+": "+why);
    }
    private static void near(double a,double b,String why){
        check(Double.isFinite(a)&&Double.isFinite(b)&&
              Math.abs(a-b)<=1e-12*Math.max(1.0,Math.abs(b)),why);
    }
    public static void main(String[] args){
        var all=MQSensorsLibExampleCatalog.all();
        var selected=MQSensorsLibExampleCatalog.installedModels();
        check(all.size()==14,"all original source equations retained");
        check(selected.size()==13,"13 installed-model source equations selected");
        Set<String> ids=new HashSet<>(),models=new HashSet<>();
        int foundMQ6=0;
        for(var original:all){
            check(ids.add(original.id),"source curves require unique source IDs");
            if(!original.installedOnTank){
                check(original.model.equals("MQ6"),"only MQ6 excluded from Tank");
                foundMQ6++;
            }
            check(original.sourceBlob.length()==40,"original blob SHA1 preserved");
            check(original.sourceRoot.equals(MQSensorsLibExampleCatalog.ROOT_MQ135)
                  || original.sourceRoot.equals(MQSensorsLibExampleCatalog.ROOT_BOARD),
                  "two source-root families, not 13 independent lab votes");
        }
        check(foundMQ6==1,"single source equation for uninstalled model");
        double[] qValues={0.05,0.2,0.5,0.8,1.0,1.25,2,8};
        for(var m:selected){
            models.add(m.model);
            double air=98.3;
            R0ReferenceMath.Reference reference=new R0ReferenceMath.Reference(
                "tank-uid:"+m.model,m.model,"TANK_HEATER_UNVERIFIED",
                R0ReferenceMath.FIRMWARE_DEVICE_AIR,air);
            R0ReferenceMath.SourceK source=new R0ReferenceMath.SourceK(
                m.sourceRoot+"@"+m.sourceBlob,m.model,
                "EXAMPLE_HEATER_NOT_QUALIFIED",m.cleanAirK,false);
            for(double q:qValues){
                double rs=air*q;
                R0ReferenceMath.Comparison c=
                    R0ReferenceMath.researchTransform(reference,rs,source);
                check(c.status==R0ReferenceMath.Status.SOURCE_K_RESEARCH_ONLY,
                    "math path is never qualified");
                check(!c.sourceCurveUseQualified,"source K never grants G3");
                near(c.measuredAirRsKohm,air,"measured clean air kept immutable");
                near(c.sourceR0Kohm,air/m.cleanAirK,"reference R0 transport");
                near(c.rsOverAir,q,"air ratio unchanged");
                near(c.rsOverSourceR0,q*m.cleanAirK,"source ratio transformation");
                double original=m.a*Math.pow(rs/c.sourceR0Kohm,m.b);
                double transformed=m.a*Math.pow(m.cleanAirK*c.rsOverAir,m.b);
                double coefficient=(m.a*Math.pow(m.cleanAirK,m.b))*
                    Math.pow(c.rsOverAir,m.b);
                near(original,transformed,"original reference equals transformed ratio");
                near(original,coefficient,"original reference equals transformed A");
                check(Double.isFinite(original)&&original>0,
                    "original source relation finite for math-only test");
            }
        }
        check(models.size()==8,"all 8 physical installed MQ models present");
        R0ReferenceMath.Reference r=new R0ReferenceMath.Reference(
            "tank:MQ2","MQ2","HEATER_STEADY",
            R0ReferenceMath.FIRMWARE_DEVICE_AIR,98.3);
        R0ReferenceMath.SourceK k=new R0ReferenceMath.SourceK(
            "source","MQ2","UNMATCHED_SOURCE_HEATER",9.83,false);
        check(R0ReferenceMath.compare(r,49.15,k).status==
              R0ReferenceMath.Status.SOURCE_HEATER_MISMATCH,
              "strict qualification rejects unmatched heater");
        check(R0ReferenceMath.researchTransform(r,49.15,k).status==
              R0ReferenceMath.Status.SOURCE_K_RESEARCH_ONLY,
              "separate conditional math still inspectable");
        check(R0ReferenceMath.researchTransform(r,0.0,k).status==
              R0ReferenceMath.Status.INVALID_REFERENCE,"zero Rs rejected");
        check(R0ReferenceMath.researchTransform(r,Double.NaN,k).status==
              R0ReferenceMath.Status.INVALID_REFERENCE,"NaN Rs rejected");
        R0ReferenceMath.Reference legacy=new R0ReferenceMath.Reference(
            "tank:MQ2","MQ2","HEATER_STEADY",
            R0ReferenceMath.FIRMWARE_LEGACY_SCALED,10.0);
        check(R0ReferenceMath.researchTransform(legacy,49.15,k).status==
              R0ReferenceMath.Status.INVALID_REFERENCE,
              "legacy factor-scaled R0 must not be re-used as measured R_air");
        R0ReferenceMath.SourceK wrong=new R0ReferenceMath.SourceK(
            "wrong","MQ4","HEATER_STEADY",4.4,false);
        check(R0ReferenceMath.researchTransform(r,49.15,wrong).status==
              R0ReferenceMath.Status.SOURCE_MODEL_MISMATCH,
              "cross-model K substitution rejected");
        R0ReferenceMath.SourceK fakeVerified=new R0ReferenceMath.SourceK(
            "fake boolean is insufficient","MQ2","HEATER_STEADY",9.83,true);
        check(!R0ReferenceMath.researchTransform(r,49.15,fakeVerified)
            .sourceCurveUseQualified,
              "algebraic-only transport cannot be G3 qualified by a boolean");
        check(r.rAirKohm==98.3,"immutable physical air reference");
        System.out.println("P11_DUAL_R0_SOURCE_JAVA17_PASS assertions="+assertions+
            " models="+selected.size()+" original="+all.size()+
            " no_g3=true no_storage_mutation=true");
    }
}
