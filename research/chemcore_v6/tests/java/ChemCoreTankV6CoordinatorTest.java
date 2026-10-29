package com.hazrob.tank.chemcore.v6;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** New V6 integration contract; all inputs are source-native synthetic fixtures. */
public final class ChemCoreTankV6CoordinatorTest {
    static int tests=0;
    static void ok(boolean b,String message){tests++;if(!b)throw new AssertionError(
        "assertion "+tests+": "+message);}
    private static List<Engine.Channel> coAt(double concentration){
        List<Engine.Channel> observations=new ArrayList<>();
        for(SourceCatalog.Curve curve:SourceCatalog.curves()){
            if(curve.legacyChemicalId!=20L)continue;
            double rs=10000*Math.pow(10,curve.log10Prediction(Math.log10(concentration)));
            observations.add(new Engine.Channel(curve.sensorType,
                                    curve.sensorType+"-real-instance-fixture",rs,10000));
        }
        return observations;
    }
    private static FingerprintEngineV6.Input nativeInput(
            String space,int signatureIndex){
        GasFingerprintCatalogV6.Signature sig=
            GasFingerprintCatalogV6.inSpace(space).get(signatureIndex);
        return new FingerprintEngineV6.Input(space,sig.vector(),
            "SOURCE_NATIVE_FEATURES","PINNED_SOURCE_ALGEBRA_FIXTURE");
    }
    public static void main(String[] args){
        List<FingerprintEngineV6.Input> data=Arrays.asList(
            nativeInput(FingerprintEngineV6.ADC,0),
            nativeInput(FingerprintEngineV6.OPERATIONAL,0),
            nativeInput(FingerprintEngineV6.TEMPORAL,0));
        var result=ChemCoreTankV6.analyze(
            new ChemCoreTankV6.Request(coAt(200),data,26.5,54.0,180.0));
        ok(result.responseCurves.allSourceFits.size()==49,"49 response curves retained");
        ok(result.fingerprintSpaces.size()==3,"independent feature-space outputs separate");
        ok(result.fingerprintSpaces.get(0).hypotheses.size()==4,"ADC labels");
        ok(result.fingerprintSpaces.get(1).hypotheses.size()==2,"operational labels");
        ok(result.fingerprintSpaces.get(2).hypotheses.size()==18,"UCI source conditions");
        ok(result.operationalClasses.size()==6,"all six class matches are not molecular IDs");
        ok(result.responseCurves.observedPhysicalChannels==8,"eight measured channels");
        ok(result.temperatureC==26.5&&result.humidityPct==54&&result.elapsedSeconds==180,
           "ambient context retained");
        ok(!result.physicalCalibrationProven&&!result.reportablePpm&&!result.foodModelAttached,
           "numerical screening is neither gas certification nor food science");
        ok(result.chemicalEvidence.size()>=13,
           "at least thirteen original chemical hypothesis families");
        ChemCoreTankV6.ChemicalEvidence carbonMonoxide=null,acetone=null;
        int sumFits=0,sumShapes=0;
        for(var entry:result.chemicalEvidence){
            sumFits+=entry.sourceCurveCount;sumShapes+=entry.featurePrototypeCount;
            ok(entry.sourceRootCount==entry.independentSourceRoots.size(),
                "distinct source roots, not curve counts");
            ok(!entry.verifiedTankGasIdentity&&!entry.hasMeasuredReferencePpm,
                "no identity promoted from reference features");
            if(entry.chemicalId==20L)carbonMonoxide=entry;
            if(entry.chemicalId==110L)acetone=entry;
        }
        ok(sumFits==49,"none of 49 curves silently hidden");
        ok(sumShapes==18,"all molecular UCI fingerprints retained");
        ok(carbonMonoxide!=null&&carbonMonoxide.sourceCurveCount==9,
           "CO includes eight original curves plus one alternate MQ135");
        ok(carbonMonoxide.actualPhysicalSensorCount==8,
           "two CO source curves on MQ135 do not pretend to be two sensors");
        ok(carbonMonoxide.sourceRootCount==2,
           "CO two independent research source families");
        ok(acetone!=null&&acetone.featurePrototypeCount==3,
           "all three source concentrations for acetone retained");
        ok(acetone.independentSourceRoots.contains("UCI1081_MQ135_TEMPORAL"),
           "temporal source visibly separate");
        var duplicated=new ArrayList<FingerprintEngineV6.Input>(data);
        duplicated.add(nativeInput(FingerprintEngineV6.TEMPORAL,0));
        var repeated=ChemCoreTankV6.analyze(new ChemCoreTankV6.Request(
            coAt(200),duplicated,Double.NaN,Double.NaN,Double.NaN));
        for(var candidate:repeated.chemicalEvidence)if(candidate.chemicalId==110L){
            ok(candidate.sourceRootCount==acetone.sourceRootCount,
               "identical source groups never inflated by repeated observations");
            ok(candidate.featurePrototypeCount==6,
               "all repeated research observations remain visible");
        }
        var noR0=ChemCoreTankV6.analyze(new ChemCoreTankV6.Request(
            Collections.singletonList(new Engine.Channel("MQ8","device-1",1000,Double.NaN)),
            Collections.singletonList(nativeInput(FingerprintEngineV6.TEMPORAL,0)),
            25,50,100));
        ok(noR0.responseCurves.retainedRawSensorRecords.size()==1,
           "raw MQ reading not lost without calibration");
        ok(noR0.responseCurves.allSourceFits.isEmpty(),"no invented Rs/R0");
        ok(noR0.chemicalEvidence.size()==6,
           "named UCI research candidates available independent of R0");
        ok(noR0.operationalClasses.isEmpty(),"no invented operational class");
        var empty=ChemCoreTankV6.analyze(null);
        ok(empty.responseCurves.allSourceFits.isEmpty()&&
           empty.chemicalEvidence.isEmpty(),"no fabricated data with empty request");
        try{result.fingerprintSpaces.clear();throw new AssertionError("mutable output");}
        catch(UnsupportedOperationException expected){tests++;}
        System.out.println("PASS ChemCoreTankV6Coordinator: "+tests+
           " assertions; 49 source equations + 24 signatures, independent ledgers");
    }
}
