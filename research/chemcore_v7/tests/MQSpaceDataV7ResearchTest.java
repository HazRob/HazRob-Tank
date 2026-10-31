import com.hazrob.tank.chemcore.v7.MQSpaceDataSurfaceV7;
import com.hazrob.tank.chemcore.v7.ChemCoreTankV7;
import com.hazrob.tank.chemcore.v6.ChemCoreTankV6;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Pure JVM source-equation research regression. Synthetic values are not gas evidence. */
public final class MQSpaceDataV7ResearchTest {
    private static int checks=0;
    private static void ok(boolean value,String why){
        checks++;if(!value)throw new AssertionError(checks+": "+why);
    }
    private static void near(double actual,double expected,double tolerance,String why){
        ok(Double.isFinite(actual)&&Math.abs(actual-expected)<=tolerance,
            why+" actual="+actual+" expected="+expected);
    }
    private static void rejected(Runnable f,String why){
        boolean did=false;
        try{f.run();}catch(IllegalArgumentException expected){did=true;}
        ok(did,why);
    }
    private static MQSpaceDataSurfaceV7.SourceGas mq8(){
        MQSpaceDataSurfaceV7.Correction correction=new MQSpaceDataSurfaceV7.Correction(
            new MQSpaceDataSurfaceV7.RhAnchor(33,.8559,-.0611,.1673),
            new MQSpaceDataSurfaceV7.RhAnchor(85,.8201,-.0606,.1492));
        return new MQSpaceDataSurfaceV7.SourceGas("MQ8","H2",
            MQSpaceDataSurfaceV7.RatioMode.RS_OVER_RO,
            18391.5667,-1.4494,200,10000,70,1,1,1,"source-reference-ppm",correction);
    }
    private static MQSpaceDataSurfaceV7.SourceGas mq9(){
        MQSpaceDataSurfaceV7.Correction correction=new MQSpaceDataSurfaceV7.Correction(
            new MQSpaceDataSurfaceV7.RhAnchor(30,-2.5404,-.0037,4.1019),
            new MQSpaceDataSurfaceV7.RhAnchor(60,-1.6874,-.0043,3.0105),
            new MQSpaceDataSurfaceV7.RhAnchor(85,-1.2399,-.0047,2.3914));
        return new MQSpaceDataSurfaceV7.SourceGas("MQ9","CO",
            MQSpaceDataSurfaceV7.RatioMode.RS_OVER_RO,
            16.8797,-.4445,200,1000,9.7,1,.5,1,"source-reference-ppm",correction);
    }
    public static void main(String[] args){
        var m=mq8();double temp=20,rh=60;
        double k=m.correction.coefficient(temp,rh);
        double expectedK=(.8559+(.8201-.8559)*((rh-33)/52.0))+
            (.1673+(.1492-.1673)*((rh-33)/52.0))*
            Math.exp((-.0611+(-.0606+.0611)*((rh-33)/52.0))*temp);
        near(k,expectedK,1e-12,"3D coefficient source interpolation");
        double sourceRatio=18391.5667*Math.pow(1000,-1.4494)*k;
        var surface=MQSpaceDataSurfaceV7.sourceSurface(m,
            MQSpaceDataSurfaceV7.ReadingMode.SOURCE_NATIVE_RATIO,
            sourceRatio,new double[]{20},new double[]{60});
        near(surface.points.get(0).sourceGasCoordinate,1000,1e-8,
            "Source Rs/Ro inverse power law");
        ok(surface.reference3D&&!surface.reference4D,"one source 3D point");
        ok(!surface.reportableTankPpm&&!surface.identifiableGas&&
            !surface.physicalCalibrationProven,"reference equation is not device calibration");
        var sweep=MQSpaceDataSurfaceV7.sourceSurface(m,
            MQSpaceDataSurfaceV7.ReadingMode.SOURCE_SENSOR_PERCENT,
            .5,new double[]{10,20,30},new double[]{33,60,85});
        ok(sweep.points.size()==9,"3x3 environmental source grid");
        for(var p:sweep.points)
            ok(Double.isFinite(p.sourceGasCoordinate)&&p.sourceGasCoordinate>=0,
                "finite source 3D grid without fictitious gas alarm");
        var tri=mq9();
        double coK=tri.correction.coefficient(20,60);
        near(coK,-1.6874+3.0105*Math.exp(-.0043*20),1e-12,
            "MQ9 three-anchor correction at 60% RH");
        rejected(()->tri.correction.coefficient(20,29),
            "No unsupported RH extrapolation");
        rejected(()->m.correction.coefficient(60,50),
            "No unsupported temperature extrapolation");
        rejected(()->MQSpaceDataSurfaceV7.sourceSurface(m,
            MQSpaceDataSurfaceV7.ReadingMode.SOURCE_SENSOR_PERCENT,
            1.0,new double[]{20},new double[]{60}),
            "Do not divide source sensor percentile by zero at unity");

        List<MQSpaceDataSurfaceV7.TimedReading> history=Arrays.asList(
            new MQSpaceDataSurfaceV7.TimedReading(0,20,50,.45),
            new MQSpaceDataSurfaceV7.TimedReading(1,21,50,.48));
        List<MQSpaceDataSurfaceV7.TimedReading> future=Arrays.asList(
            new MQSpaceDataSurfaceV7.TimedReading(2,22,51,.49),
            new MQSpaceDataSurfaceV7.TimedReading(3,23,52,.51));
        var path=MQSpaceDataSurfaceV7.sourceTimePath(m,
            MQSpaceDataSurfaceV7.ReadingMode.SOURCE_SENSOR_PERCENT,
            history,future,"MQSpaceData-76-upstream-predictor",
            MQSpaceDataSurfaceV7.Provenance.ORIGINAL_MQSPACEDATA_PRIOR);
        ok(path.reference4D&&path.sourcePredictionProvided&&
            path.points.size()==4,"time-colored 4D source path");
        ok(path.points.get(0).measuredSourceInput&&
            !path.points.get(0).futureForecast&&
            path.points.get(3).futureForecast,
            "history and prediction separated");
        ok(!path.reportableTankPpm&&path.physicalTankQualifiedExposures==0,
            "model-selected source time path never makes device ppm");
        rejected(()->MQSpaceDataSurfaceV7.sourceTimePath(m,
            MQSpaceDataSurfaceV7.ReadingMode.SOURCE_SENSOR_PERCENT,
            history,future,null,
            MQSpaceDataSurfaceV7.Provenance.ORIGINAL_MQSPACEDATA_PRIOR),
            "Named model required for future predictions");
        rejected(()->MQSpaceDataSurfaceV7.sourceTimePath(m,
            MQSpaceDataSurfaceV7.ReadingMode.SOURCE_SENSOR_PERCENT,
            history,Collections.singletonList(
                new MQSpaceDataSurfaceV7.TimedReading(1,20,50,.45)),
            "sample",MQSpaceDataSurfaceV7.Provenance.DOCUMENTED_SOURCE_EXPOSURE),
            "No duplicated time across forecast boundary");
        var combined=ChemCoreTankV7.analyze(new ChemCoreTankV7.Request(
            new ChemCoreTankV6.Request(null,null,20,50,3),
            m,MQSpaceDataSurfaceV7.ReadingMode.SOURCE_SENSOR_PERCENT,
            history,future,"MQSpaceData-76-upstream-predictor",
            MQSpaceDataSurfaceV7.Provenance.ORIGINAL_MQSPACEDATA_PRIOR));
        ok(combined.v6Evidence!=null&&combined.mqSpaceResearch!=null,
            "V6 source evidence preserved with independent research branch");
        ok(!combined.reportablePpm&&!combined.independentTankG3&&
            !combined.verifiedGasIdentity,"G3/ppm/identity remain blocked");
        System.out.println("PASS MQSpaceData V7 source 3D/4D + V6 isolation assertions="+checks);
    }
}
