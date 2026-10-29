package com.hazrob.tank.chemcore.v6;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Clean-slate research coordinator for HazRob Tank ChemCore V6.
 * Does NOT depend on the predecessor ChemCore, Android UI, food engine, SQLite
 * or hardware control. Scientific source equations and fingerprint patterns
 * are independent branches; scores are NEVER mixed into a fake probability.
 *
 * Every finite source fit and fingerprint hypothesis remains accessible.
 * The ledger indexes source provenance, it is NOT a gas-certification verdict.
 */
public final class ChemCoreTankV6 {
    public static final String SCHEMA="hazrob-tank-independent-chemcore-v6/2";
    private ChemCoreTankV6(){}

    public static final class Request {
        public final List<Engine.Channel> channels;
        public final List<FingerprintEngineV6.Input> featureInputs;
        public final double ambientTemperatureC,relativeHumidityPct,elapsedSeconds;
        public Request(List<Engine.Channel> values,
                       List<FingerprintEngineV6.Input> fingerprints,
                       double temperatureC,double humidityPct,double seconds){
            channels=Collections.unmodifiableList(new ArrayList<>(
                values==null?Collections.<Engine.Channel>emptyList():values));
            featureInputs=Collections.unmodifiableList(new ArrayList<>(
                fingerprints==null?Collections.<FingerprintEngineV6.Input>emptyList():fingerprints));
            ambientTemperatureC=temperatureC;
            relativeHumidityPct=humidityPct;
            elapsedSeconds=seconds;
        }
    }

    public static final class ChemicalEvidence {
        public final long chemicalId;
        public final List<Engine.SourceFit> curveFits;
        public final List<FingerprintEngineV6.Hypothesis> fingerprintMatches;
        public final List<String> independentSourceRoots,distinctSensorInstances;
        public final int sourceCurveCount,featurePrototypeCount,sourceRootCount,
                         actualPhysicalSensorCount;
        public final boolean hasMeasuredReferencePpm,verifiedTankGasIdentity;
        private ChemicalEvidence(long id,List<Engine.SourceFit> fits,
                                 List<FingerprintEngineV6.Hypothesis> shapes,
                                 Set<String> roots,Set<String> physicalSensors){
            chemicalId=id;
            curveFits=Collections.unmodifiableList(new ArrayList<>(fits));
            fingerprintMatches=Collections.unmodifiableList(new ArrayList<>(shapes));
            independentSourceRoots=Collections.unmodifiableList(new ArrayList<>(roots));
            distinctSensorInstances=Collections.unmodifiableList(new ArrayList<>(physicalSensors));
            sourceCurveCount=fits.size();featurePrototypeCount=shapes.size();
            sourceRootCount=roots.size();actualPhysicalSensorCount=physicalSensors.size();
            hasMeasuredReferencePpm=false;verifiedTankGasIdentity=false;
        }
    }
    public static final class Result {
        public final String schema;
        public final Engine.Result responseCurves;
        public final EvidenceConflictV6.Result sourceCurveDisagreements;
        public final List<FingerprintEngineV6.Result> fingerprintSpaces;
        public final List<ChemicalEvidence> chemicalEvidence;
        public final List<FingerprintEngineV6.Hypothesis> operationalClasses;
        public final double temperatureC,humidityPct,elapsedSeconds;
        public final boolean physicalCalibrationProven,reportablePpm,foodModelAttached;
        private Result(Engine.Result responses,
                       List<FingerprintEngineV6.Result> inputSpaces,
                       List<ChemicalEvidence> chemicals,
                       List<FingerprintEngineV6.Hypothesis> classes,Request request){
            schema=SCHEMA;responseCurves=responses;
            sourceCurveDisagreements=EvidenceConflictV6.compare(responses);
            fingerprintSpaces=Collections.unmodifiableList(new ArrayList<>(inputSpaces));
            chemicalEvidence=Collections.unmodifiableList(new ArrayList<>(chemicals));
            operationalClasses=Collections.unmodifiableList(new ArrayList<>(classes));
            temperatureC=request.ambientTemperatureC;
            humidityPct=request.relativeHumidityPct;
            elapsedSeconds=request.elapsedSeconds;
            physicalCalibrationProven=false;reportablePpm=false;foodModelAttached=false;
        }
    }
    private static final class Builder {
        final long id;
        final List<Engine.SourceFit> curves=new ArrayList<>();
        final List<FingerprintEngineV6.Hypothesis> fingerprints=new ArrayList<>();
        final Set<String> roots=new LinkedHashSet<>();
        final Set<String> sensors=new LinkedHashSet<>();
        Builder(long uid){id=uid;}
        void curve(Engine.SourceFit value){
            curves.add(value);roots.add(value.sourceRoot);
            sensors.add(value.sensorInstance);
        }
        void fingerprint(FingerprintEngineV6.Hypothesis value){
            fingerprints.add(value);roots.add(value.sourceRoot);
        }
        ChemicalEvidence build(){
            return new ChemicalEvidence(id,curves,fingerprints,roots,sensors);
        }
    }

    /** All source hypotheses are preserved and ledgered by distinct origin. */
    public static Result analyze(Request request){
        if(request==null)request=new Request(null,null,Double.NaN,Double.NaN,Double.NaN);
        Engine.Result ratio=Engine.analyze(request.channels);
        List<FingerprintEngineV6.Result> spaces=new ArrayList<>();
        List<FingerprintEngineV6.Hypothesis> operational=new ArrayList<>();
        Map<Long,Builder> evidence=new LinkedHashMap<>();

        for(Engine.SourceFit fit:ratio.allSourceFits)
            if(fit.chemicalId>0)
                evidence.computeIfAbsent(fit.chemicalId,Builder::new).curve(fit);

        for(FingerprintEngineV6.Input feature:request.featureInputs){
            FingerprintEngineV6.Result result=FingerprintEngineV6.match(feature);
            spaces.add(result);
            for(FingerprintEngineV6.Hypothesis h:result.hypotheses){
                if(h.chemicalId>0)
                    evidence.computeIfAbsent(h.chemicalId,Builder::new).fingerprint(h);
                else operational.add(h);
            }
        }
        List<ChemicalEvidence> chemicals=new ArrayList<>();
        for(Builder group:evidence.values())chemicals.add(group.build());
        // Not a probability ranking: ordering only by physical-channel presence,
        // then number of documented distinct data source roots and stable source ID.
        chemicals.sort(Comparator
            .comparingInt((ChemicalEvidence e)->e.actualPhysicalSensorCount).reversed()
            .thenComparing(Comparator.comparingInt(
                (ChemicalEvidence e)->e.sourceRootCount).reversed())
            .thenComparingLong(e->e.chemicalId));
        return new Result(ratio,spaces,chemicals,operational,request);
    }
}
