package com.hazrob.tank.chemcore.v6;

/** N03-G: numerical, causality, topology, source grouping and fail-closed tests.
 * All synthetic test signals are NOT gas experiments or real Tank measurements.
 */
public final class V6SeventhNeuralTechniquesTest {
    private static int count=0;
    private static void ok(boolean b,String what){
        count++;if(!b)throw new AssertionError(count+": "+what);
    }
    private static void near(double a,double b,double tolerance,String what){
        ok(Double.isFinite(a)&&Math.abs(a-b)<=tolerance,
            what+": actual="+a+" expected="+b);
    }
    private static void reject(Runnable r,String reason){
        boolean failed=false;
        try{r.run();}catch(IllegalArgumentException expected){failed=true;}
        ok(failed,reason);
    }
    private static double[][] series(double value) {
        double[][] x=new double[12][2];
        for(int i=0;i<x.length;i++){
            x[i][0]=value*(1.0+i/12.0);
            x[i][1]=value*(.5+.15*i/12.0);
        }
        return x;
    }
    private static boolean[][] graph(){
        return new boolean[][]{{false,true},{true,false}};
    }
    public static void main(String[] args){
        var cnn=TemporalNeuralResearchV6.Kind.CNN1D_FROZEN_FILTERS;
        var tcn=TemporalNeuralResearchV6.Kind.TCN_CAUSAL_FROZEN_FILTERS;
        var gru=TemporalNeuralResearchV6.Kind.GRU_FROZEN_GATES;
        var gnn=TemporalNeuralResearchV6.Kind.GNN_FROZEN_MESSAGE_PASSING;

        double[][] p=series(1.5),n=series(-1.5);
        var c=TemporalNeuralResearchV6.encode(cnn,p,null);
        ok(c.length==8,"CNN four width-three filters times two source channels");
        ok(c[2]>0,"CNN short-window positive activation");
        var negCnn=TemporalNeuralResearchV6.encode(cnn,n,null);
        ok(c[2]>negCnn[2],"CNN detects sign-sensitive temporal mean");
        ok(TemporalNeuralResearchV6.encode(tcn,p,null).length==12,
           "TCN three dilations terminal+average times channels");
        ok(TemporalNeuralResearchV6.encode(gru,p,null).length==4,
           "GRU actual recurrent update and temporal mean");
        ok(TemporalNeuralResearchV6.encode(gnn,p,graph()).length==4,
           "GNN two explicit graph message passing hops");
        var noGraph=new boolean[][]{{false,false},{false,false}};
        var paired=TemporalNeuralResearchV6.encode(gnn,
            new double[][]{{2,0},{2,0},{2,0},{2,0},{2,0},{2,0},{2,0},{2,0}},graph());
        var isolated=TemporalNeuralResearchV6.encode(gnn,
            new double[][]{{2,0},{2,0},{2,0},{2,0},{2,0},{2,0},{2,0},{2,0}},noGraph);
        ok(Math.abs(paired[2]-isolated[2])>1e-6,
            "GNN output depends on actual declared graph edges");
        reject(()->TemporalNeuralResearchV6.encode(gnn,p,null),
            "GNN cannot fabricate a sensor adjacency graph");
        reject(()->TemporalNeuralResearchV6.encode(gnn,p,
            new boolean[][]{{false,true},{false,false}}),
            "non-symmetric graph refused");
        reject(()->TemporalNeuralResearchV6.encode(gnn,p,
            new boolean[][]{{true,false},{false,false}}),
            "diagonal self-edge declared twice rejected");

        var input=p.clone();input[0]=new double[]{Double.NaN,1};
        reject(()->TemporalNeuralResearchV6.encode(cnn,input,null),
            "no NaN imputation in source time windows");
        reject(()->TemporalNeuralResearchV6.encode(gru,
            new double[][]{{1,2},{3,4}},null),
            "must keep at least eight temporal records");
        reject(()->TemporalNeuralResearchV6.encode(tcn,
            new double[][]{{1,2},{1},{1,2},{1,2},{1,2},{1,2},{1,2},{1,2}},null),
            "ragged sampling axes are invalid");

        // Causality: manipulate future input without changing earlier prefix.
        var before=series(1.3),after=series(1.3);
        for(int i=8;i<after.length;i++){after[i][0]=1000;after[i][1]=-1000;}
        var prefix1=TemporalNeuralResearchV6.causalTcnPrefix(before,7);
        var prefix2=TemporalNeuralResearchV6.causalTcnPrefix(after,7);
        for(int i=0;i<prefix1.length;i++)
            near(prefix1[i],prefix2[i],0,"TCN causal prefix no future leakage feature "+i);
        var recurrent1=TemporalNeuralResearchV6.gruPrefix(before,7);
        var recurrent2=TemporalNeuralResearchV6.gruPrefix(after,7);
        for(int i=0;i<recurrent1.length;i++)
            near(recurrent1[i],recurrent2[i],0,
                "GRU recurrence prefix no future leakage "+i);
        ok(Math.abs(TemporalNeuralResearchV6.gruPrefix(before,11)[0]-
                    TemporalNeuralResearchV6.gruPrefix(after,11)[0])>.2,
           "future observations change final recurrent state but not earlier state");
        reject(()->TemporalNeuralResearchV6.causalTcnPrefix(p,-1),
            "TCN invalid causal index rejected");
        reject(()->TemporalNeuralResearchV6.gruPrefix(p,12),
            "GRU future index rejected");

        // Complete exposure is one training row: six negative and six positive.
        double[][][] rows=new double[12][][];
        int[] label=new int[12];
        for(int i=0;i<rows.length;i++){
            label[i]=i>=6?1:0;
            rows[i]=series((i>=6?1:-1)*(1.0+.08*(i%6)));
        }
        for(var kind:TemporalNeuralResearchV6.Kind.values()){
            var fit=TemporalNeuralResearchV6.fitBinary(
                kind,rows,label,kind==gnn?graph():null,500,.15,.001);
            ok(fit.sourceTrainingExposures==12,"whole-exposure source training count "+kind);
            ok(fit.featureCount>0&&fit.iterations==500,"actual fitted source head "+kind);
            double positive=fit.sourceRelativeScore(series(1.5));
            double negative=fit.sourceRelativeScore(series(-1.5));
            ok(positive>negative+.15,"source learner distinguishes distinct synthetic classes "+kind);
            ok(positive<=1&&positive>=0&&negative<=1&&negative>=0,
               "numerically bounded source score "+kind);
            ok(!fit.encoderWeightsTrained&&!fit.probabilitiesCalibrated&&
               !fit.physicalTankGasIdentityValidated,
               "FIXED encoder / uncalibrated source output / G3=0 "+kind);
        }
        reject(()->TemporalNeuralResearchV6.fitBinary(
            cnn,rows,new int[]{0,1},null,200,.1,.001),
            "no fragmented time rows or label misalignment");
        var allOne=new int[12];java.util.Arrays.fill(allOne,1);
        reject(()->TemporalNeuralResearchV6.fitBinary(
            gru,rows,allOne,null,100,.1,.001),
            "source training with only one class rejected");
        reject(()->TemporalNeuralResearchV6.fitBinary(
            gnn,rows,label,null,200,.1,.001),
            "GNN source adjacency missing must fail closed");
        System.out.println("PASS V6SeventhNeuralTechniques: "+count+
            " JDK assertions, 4 partial frozen-encoder CNN TCN GRU GNN research operations; G3=0.");
    }
}
