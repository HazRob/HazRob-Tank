package com.hazrob.tank.foodfreshness.meat;
import java.util.Map;
import java.util.LinkedHashMap;
public final class BeefGaussianResearchEngineTest {
    static int checks=0;
    static void yes(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    public static void main(String[] args){
        yes(BeefGaussianResearchEngine.classMean("excellent").length==7,"seven source MQ mean");
        yes(Math.abs(BeefGaussianResearchEngine.classMean("excellent")[0]
            -(-0.226490239769053))<1e-12,"source TS1-TS4 fitted mean pinned");
        yes(Math.abs(BeefGaussianResearchEngine.classVariance("spoiled")[3]
            -0.312412865497274)<1e-12,"source variance pinned");
        yes(BeefGaussianResearchEngine.classMean("missing").length==0,"no invented unknown class");
        for(BeefResearchCatalog.Template src:BeefResearchCatalog.classes()){
            Map<String,Double> x=new LinkedHashMap<>();
            double[] v=src.logShape();
            for(int i=0;i<7;i++)x.put(BeefResearchCatalog.SENSOR_ORDER.get(i),
                 1200*Math.pow(10,v[i]));
            BeefGaussianResearchEngine.Result result=BeefGaussianResearchEngine.analyze(x);
            yes(result.ranked.size()==4,"all four source class scores visible");
            yes(result.status.equals("SOURCE_DIAGNOSTIC_ONLY"),"research-only state");
            yes(!result.certifiedForFoodSafety&&!result.validatedOnTank,
                 "no false edibility or Tank test");
            yes(result.ranked.get(0).gaussianCost<=result.ranked.get(1).gaussianCost,
                 "sorted relative diagnostic score");
            x.remove("MQ9");
            yes(BeefGaussianResearchEngine.analyze(x).status.startsWith("NOT_RUN"),
                 "no missing MQ profile fabricated");
        }
        yes(BeefGaussianResearchEngine.SOURCE_FIVE_CUT_MEAN_BALANCED_ACCURACY<.7,
             "performance limited, not assumed certified");
        System.out.println("PASS seven-MQ independent Gaussian beef research model: "+checks);
    }
}