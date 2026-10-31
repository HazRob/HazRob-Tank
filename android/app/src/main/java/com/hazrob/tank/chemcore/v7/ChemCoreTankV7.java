package com.hazrob.tank.chemcore.v7;

import com.hazrob.tank.chemcore.v6.ChemCoreTankV6;

import java.util.List;

/**
 * Additive V7 research coordinator. Preserves V6 chemical evidence results
 * verbatim, and attaches a separately provenance-tagged MQSpaceData 3D/4D
 * reference result. No model is allowed to override G3/ppm/identity gates.
 *
 * This class is intentionally not wired into production alarm / hazard / ppm
 * routes until an Android measurement bridge and independent G3 are approved.
 */
public final class ChemCoreTankV7 {
    public static final String SCHEMA="hazrob-tank-independent-chemcore-v7-research/1";
    private ChemCoreTankV7(){}

    public static final class Request {
        public final ChemCoreTankV6.Request v6;
        public final MQSpaceDataSurfaceV7.SourceGas sourceGas;
        public final MQSpaceDataSurfaceV7.ReadingMode sourceReadingSpace;
        public final List<MQSpaceDataSurfaceV7.TimedReading> sourceHistory,sourceForecast;
        public final String externalModelId;
        public final MQSpaceDataSurfaceV7.Provenance sourceProvenance;
        public Request(ChemCoreTankV6.Request baseline,
                MQSpaceDataSurfaceV7.SourceGas reference,
                MQSpaceDataSurfaceV7.ReadingMode reading,
                List<MQSpaceDataSurfaceV7.TimedReading> history,
                List<MQSpaceDataSurfaceV7.TimedReading> forecast,
                String namedModel,MQSpaceDataSurfaceV7.Provenance sourceProvenance){
            v6=baseline;sourceGas=reference;sourceReadingSpace=reading;
            sourceHistory=history;sourceForecast=forecast;
            externalModelId=namedModel;this.sourceProvenance=sourceProvenance;
        }
    }

    public static final class Result {
        public final String schema;
        public final ChemCoreTankV6.Result v6Evidence;
        public final MQSpaceDataSurfaceV7.Result mqSpaceResearch;
        public final boolean independentTankG3,verifiedGasIdentity,reportablePpm;
        public final String evidencePolicy;
        private Result(ChemCoreTankV6.Result baseline,MQSpaceDataSurfaceV7.Result source){
            schema=SCHEMA;v6Evidence=baseline;mqSpaceResearch=source;
            independentTankG3=false;verifiedGasIdentity=false;reportablePpm=false;
            evidencePolicy="MQSPACEDATA_SOURCE_3D4D_NEVER_PROMOTES_V6_G3";
        }
    }

    public static Result analyze(Request request){
        ChemCoreTankV6.Result baseline=ChemCoreTankV6.analyze(
            request==null?null:request.v6);
        MQSpaceDataSurfaceV7.Result reference=null;
        if(request!=null&&request.sourceGas!=null&&
           request.sourceHistory!=null&&!request.sourceHistory.isEmpty()){
            reference=MQSpaceDataSurfaceV7.sourceTimePath(
                request.sourceGas,request.sourceReadingSpace,request.sourceHistory,
                request.sourceForecast,request.externalModelId,request.sourceProvenance);
        }
        return new Result(baseline,reference);
    }
}
