package com.hazrob.tank.chemcore.v7;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Research-only, algebraic port of the MQSpaceData 7.0.0 3D/4D SURFACE model.
 *
 * Upstream: abcdaaaaaaaaa/MQDataScience @
 * 99c7262d8ce0fbd5f3aaee096cb0b469d2283637
 * Original methods: 3DSurface.py (CorrectionCoefficient, ppm, Sensorppm),
 * 4DCurve.py (sensor percentile, temperature, humidity, time coordinates).
 * Upstream is MIT licensed; license retained with the vendored research scripts.
 *
 * Axes: 3D=(temperature Celsius, RH %, source-derived gas value),
 * 4D=3D path plus time seconds. This is NOT the superseded
 * MQ sensor A/MQ sensor B/time path. Never physical Tank ppm or gas identity.
 */
public final class MQSpaceDataSurfaceV7 {
    public static final String SCHEMA="hazrob-chemcore-v7-mqspacedata-surface/1";
    public static final String UPSTREAM_COMMIT=
        "99c7262d8ce0fbd5f3aaee096cb0b469d2283637";
    public static final String EVIDENCE="SOURCE_EQUATION_RESEARCH_ONLY";
    private MQSpaceDataSurfaceV7(){}

    private static boolean finite(double x){return Double.isFinite(x);}
    private static boolean positive(double x){return finite(x)&&x>0;}
    private static void check(boolean ok,String message){
        if(!ok)throw new IllegalArgumentException(message);
    }
    private static double clamp(double x,double lo,double hi){
        return Math.max(lo,Math.min(hi,x));
    }
    private static double interpolate(double input,double x1,double x2,double y1,double y2){
        return y1+(input-x1)*(y2-y1)/(x2-x1);
    }
    public enum RatioMode { RS_OVER_RO, RO_OVER_RS, RS_OVER_RS }
    public enum ReadingMode { SOURCE_NATIVE_RATIO, SOURCE_SENSOR_PERCENT }
    public enum Provenance {
        ORIGINAL_MQSPACEDATA_PRIOR,
        DOCUMENTED_SOURCE_EXPOSURE,
        TANK_RS_OVER_DEVICE_CLEAN_AIR_R0_UNVERIFIED_EQUIVALENCE
    }

    public static final class RhAnchor {
        public final double rh,a,b,c;
        public RhAnchor(double humidity,double a,double b,double c){
            check(finite(humidity)&&humidity>=0&&humidity<=100&&
                finite(a)&&finite(b)&&finite(c),"Finite RH correction parameters");
            rh=humidity;this.a=a;this.b=b;this.c=c;
        }
    }
    public static final class Correction {
        private final List<RhAnchor> anchors;
        public final double lowestRh,highestRh;
        public Correction(RhAnchor... points){
            check(points!=null&&(points.length==2||points.length==3),
                "Upstream RH correction requires two or three source anchors");
            ArrayList<RhAnchor> copy=new ArrayList<>();
            for(RhAnchor p:points){
                check(p!=null,"Null source anchor");
                if(!copy.isEmpty())check(p.rh>copy.get(copy.size()-1).rh,
                    "RH anchors must increase");
                copy.add(p);
            }
            anchors=Collections.unmodifiableList(copy);
            lowestRh=anchors.get(0).rh;
            highestRh=anchors.get(anchors.size()-1).rh;
        }
        /** No silent extrapolation beyond the source temperature/RH region. */
        public double coefficient(double tC,double humidity){
            check(finite(tC)&&tC>=-10&&tC<=50&&finite(humidity)&&
                humidity>=lowestRh&&humidity<=highestRh,
                "Temperature/RH outside source correction anchors or range");
            RhAnchor lo=anchors.get(0),hi=anchors.get(anchors.size()-1);
            for(int i=1;i<anchors.size();i++)
                if(humidity<=anchors.get(i).rh){
                    lo=anchors.get(i-1);hi=anchors.get(i);break;
                }
            double a=interpolate(humidity,lo.rh,hi.rh,lo.a,hi.a);
            double b=interpolate(humidity,lo.rh,hi.rh,lo.b,hi.b);
            double c=interpolate(humidity,lo.rh,hi.rh,lo.c,hi.c);
            double k=a+c*Math.exp(b*tC);
            check(positive(k),"Non-physical source correction coefficient");
            return k;
        }
    }

    /** Source coefficients; never substitute a Tank's NVS clean-air Rs into
     * calibration-reference normalization without independent equivalence.
     */
    public static final class SourceGas {
        public final String sensor,gas,sourceRatioLabel,sourceOutputUnit;
        public final RatioMode mode;
        public final double a,b,sourceMin,sourceMax,
            airReference,calibrationReference,loadResistorScale,outputScale;
        public final Correction correction;
        public SourceGas(String sensor,String gas,RatioMode mode,double a,double b,
                         double sourceMin,double sourceMax,double airReference,
                         double calibrationReference,double loadResistorScale,
                         double outputScale,String outputUnit,Correction correction){
            check(sensor!=null&&!sensor.trim().isEmpty()&&
                gas!=null&&!gas.trim().isEmpty(),"Source sensor/gas");
            check(mode!=null&&positive(a)&&finite(b)&&Math.abs(b)>1e-12&&
                positive(sourceMin)&&sourceMax>sourceMin&&
                positive(airReference)&&positive(calibrationReference)&&
                positive(loadResistorScale)&&positive(outputScale)&&correction!=null,
                "Finite source coefficient / calibration / range required");
            this.sensor=sensor;this.gas=gas;this.mode=mode;
            this.a=a;this.b=b;this.sourceMin=sourceMin;this.sourceMax=sourceMax;
            this.airReference=airReference;this.calibrationReference=calibrationReference;
            this.loadResistorScale=loadResistorScale;this.outputScale=outputScale;
            this.sourceOutputUnit=outputUnit==null?"UNSPECIFIED":outputUnit;
            this.sourceRatioLabel=mode==RatioMode.RS_OVER_RO?"SOURCE_Rs/Ro":
                mode==RatioMode.RO_OVER_RS?"SOURCE_Ro/Rs":"SOURCE_Rs/Rs";
        }

        /**
         * Source power law ratio = a*(c^b)*K for Rs/Ro and Rs/Rs;
         * for Ro/Rs MQSpaceData changes exponents first as in 3DSurface.py.
         */
        private double invert(double ratio,double coefficient){
            double aa=a,bb=b;
            if(mode==RatioMode.RO_OVER_RS){
                aa=1.0/Math.pow(a,1.0/b);
                bb=1.0/b;
            }
            double value=Math.pow((ratio/coefficient)/aa,1.0/bb);
            check(finite(value)&&value>=0,"Nonfinite source inversion");
            return value;
        }

        private double sourceCalibrationFraction(){
            double aa=a,bb=b;
            if(mode==RatioMode.RO_OVER_RS){
                aa=1.0/Math.pow(a,1.0/b);bb=1.0/b;
            }
            double calAir=Math.pow(calibrationReference/aa,1.0/bb);
            check(positive(calAir),"Invalid source gas calibration reference");
            double fraction=(Math.log10(calAir)-Math.log10(sourceMin))/
                (Math.log10(sourceMax)-Math.log10(sourceMin));
            return clamp(fraction,0.01,0.99);
        }

        public double researchGasCoordinate(double measuredSourceValue,ReadingMode reading,
                                             double tC,double humidity){
            check(reading!=null&&positive(measuredSourceValue),
                "Source ratio/percent must be positive");
            double correctionFactor=correction.coefficient(tC,humidity);
            double ratio=measuredSourceValue;
            if(reading==ReadingMode.SOURCE_SENSOR_PERCENT){
                check(measuredSourceValue<1.0,"Upstream sensor percent fraction must be <1");
                double cal=sourceCalibrationFraction();
                ratio=airReference*loadResistorScale*
                    (cal*(measuredSourceValue-1))/(measuredSourceValue*(cal-1));
                check(positive(ratio),"Invalid source sensor-fraction transform");
            }
            double raw=invert(ratio,correctionFactor)*outputScale;
            double upper=sourceMax*outputScale*correctionFactor;
            return clamp(raw,0,upper);
        }
    }

    public static final class Point {
        public final double timeSeconds,temperatureC,humidityPct,sourceGasCoordinate;
        public final double inputValue,correctionCoefficient;
        public final boolean measuredSourceInput,futureForecast;
        private Point(double time,double t,double rh,double input,double k,double output,
                      boolean measured,boolean future){
            timeSeconds=time;temperatureC=t;humidityPct=rh;
            inputValue=input;correctionCoefficient=k;
            sourceGasCoordinate=output;
            measuredSourceInput=measured;futureForecast=future;
        }
    }
    public static final class TimedReading {
        public final double timeSeconds,temperatureC,humidityPct,sourceRatioOrPercent;
        public TimedReading(double sec,double c,double rh,double sourceValue){
            check(finite(sec)&&sec>=0&&finite(c)&&finite(rh)&&
                positive(sourceValue),"Finite source time/env/reading required");
            timeSeconds=sec;temperatureC=c;humidityPct=rh;
            sourceRatioOrPercent=sourceValue;
        }
    }
    public static final class Result {
        public final String schema,upstreamCommit,sourceSensor,sourceGas,
            sourceCoordinateUnit,modelId,sourceEvidenceTier;
        public final Provenance provenance;
        public final List<Point> points;
        public final int physicalTankQualifiedExposures;
        public final boolean reference3D,reference4D,sourcePredictionProvided,
            physicalCalibrationProven,reportableTankPpm,identifiableGas;
        private Result(SourceGas model,List<Point> pts,Provenance prov,String forecast,
                       boolean forecastIncluded){
            schema=SCHEMA;upstreamCommit=UPSTREAM_COMMIT;
            sourceSensor=model.sensor;sourceGas=model.gas;
            sourceCoordinateUnit=model.sourceOutputUnit;
            sourceEvidenceTier=EVIDENCE;provenance=prov;
            modelId=forecast==null?"":forecast;
            points=Collections.unmodifiableList(new ArrayList<>(pts));
            reference3D=!pts.isEmpty();reference4D=pts.size()>=2;
            sourcePredictionProvided=forecastIncluded;
            physicalTankQualifiedExposures=0;physicalCalibrationProven=false;
            reportableTankPpm=false;identifiableGas=false;
        }
    }

    /** Source-simulated 3D surface; returned Z is NOT a Tank gas measurement. */
    public static Result sourceSurface(SourceGas source,ReadingMode mode,double sourceValue,
                                       double[] temperatures,double[] humidities){
        check(source!=null&&mode!=null&&temperatures!=null&&humidities!=null&&
            temperatures.length>0&&humidities.length>0&&
            (long)temperatures.length*humidities.length<=10000,
            "3D source surface size 1..10000");
        List<Point> points=new ArrayList<>();
        for(double t:temperatures)for(double rh:humidities){
            double k=source.correction.coefficient(t,rh);
            points.add(new Point(0,t,rh,sourceValue,k,
                source.researchGasCoordinate(sourceValue,mode,t,rh),false,false));
        }
        return new Result(source,points,Provenance.ORIGINAL_MQSPACEDATA_PRIOR,"",false);
    }

    /**
     * Source 4D=(T,RH,source-derived gas coordinate,time) in the original
     * MQSpaceData interpretation. Future predictions must arrive from a named
     * external trained model; this method NEVER fabricates a forecast.
     */
    public static Result sourceTimePath(SourceGas source,ReadingMode reading,
                                        List<TimedReading> historical,
                                        List<TimedReading> forecast,
                                        String externalModelId,
                                        Provenance provenance){
        check(source!=null&&reading!=null&&historical!=null&&
            !historical.isEmpty()&&historical.size()<=10000,
            "At least one original source reading");
        check(provenance!=null,"Provenance must be explicit");
        boolean predicted=forecast!=null&&!forecast.isEmpty();
        if(predicted)check(externalModelId!=null&&!externalModelId.trim().isEmpty(),
            "Forecast requires external model identity and provenance");
        int size=historical.size()+(forecast==null?0:forecast.size());
        check(size<=10000,"Time path capped at 10000 frames");
        List<Point> points=new ArrayList<>();
        double previous=-1;
        for(int part=0;part<2;part++){
            List<TimedReading> inputs=part==0?historical:forecast;
            if(inputs==null)continue;
            for(TimedReading frame:inputs){
                check(frame!=null&&frame.timeSeconds>previous,
                    "Time must be strictly increasing across history and forecast");
                previous=frame.timeSeconds;
                double k=source.correction.coefficient(frame.temperatureC,frame.humidityPct);
                points.add(new Point(frame.timeSeconds,frame.temperatureC,frame.humidityPct,
                    frame.sourceRatioOrPercent,k,
                    source.researchGasCoordinate(frame.sourceRatioOrPercent,reading,
                        frame.temperatureC,frame.humidityPct),
                    part==0,part==1));
            }
        }
        return new Result(source,points,provenance,externalModelId,predicted);
    }
}
