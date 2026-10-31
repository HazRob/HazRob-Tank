package com.hazrob.tank.chemcore.v7;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ChemCore V7: research-only, synchronized 3D/4D MQ trajectory.
 *
 * 3D: X=log10(RsA/R0A), Y=log10(RsB/R0B), Z=elapsed fraction.
 * 4D: the same 3D path plus ONE declared environmental coordinate:
 *      (RH-RH0)/100 or (T-T0)/40. Temperature and RH are observed,
 *      NEVER compensated without a separately trained and validated model.
 *
 * This is a geometric descriptor, not molecular identity, gas ppm,
 * a manufacturer's 3D curve, or a Tank G3 validated measurement.
 */
public final class Trajectory3D4DV7 {
    public static final String SCHEMA="hazrob-tank-chemcore-v7-3d4d/1";
    public static final int MIN_INDEPENDENT_CYCLES=10;
    public static final int MAX_FRAMES=10000;
    private Trajectory3D4DV7(){}

    public enum EnvironmentAxis { NONE, HUMIDITY_RH, TEMPERATURE_C }

    public static final class Sensor {
        public final double rsKohm,r0Kohm;
        public final String r0Source,r0DeviceId,definition;
        public final boolean ok,powered,provenanceComplete;
        public final int r0Samples;
        public Sensor(double rs,double r0,String source,String device,String definition,
                      boolean ok,boolean powered,boolean provenance,int samples){
            this.rsKohm=rs;this.r0Kohm=r0;
            this.r0Source=source==null?"":source;
            this.r0DeviceId=device==null?"":device;
            this.definition=definition==null?"":definition;
            this.ok=ok;this.powered=powered;this.provenanceComplete=provenance;
            this.r0Samples=samples;
        }
        public boolean recordedDeviceRatioAvailable(String device){
            boolean known="firmware_clean_air_r0".equals(r0Source)||
                "manual_device_r0".equals(r0Source)||
                "automatic_clean_air_field_r0".equals(r0Source);
            if("firmware_clean_air_r0".equals(r0Source)&&
                (!"DEVICE_CLEAN_AIR_RS_MEAN".equals(definition)||r0Samples<10))
                return false;
            return known&&ok&&powered&&provenanceComplete&&
                !r0DeviceId.isEmpty()&&r0DeviceId.equals(device)&&
                Double.isFinite(rsKohm)&&rsKohm>0&&
                Double.isFinite(r0Kohm)&&r0Kohm>0;
        }
    }

    public static final class Frame {
        public final String sessionId,deviceId;
        public final long scanCycle,timeMs;
        public final Map<String,Sensor> sensors;
        public final double temperatureC,humidityRh;
        public Frame(String session,String device,long cycle,long elapsedMs,
                     Map<String,Sensor> mq,double temperature,double rh){
            sessionId=session==null?"":session.trim();
            deviceId=device==null?"":device.trim();
            scanCycle=cycle;timeMs=elapsedMs;
            sensors=Collections.unmodifiableMap(new LinkedHashMap<>(
                mq==null?Collections.<String,Sensor>emptyMap():mq));
            temperatureC=temperature;humidityRh=rh;
        }
    }

    public static final class Point {
        public final long scanCycle,timeMs;
        public final double xLogRatio,yLogRatio,elapsedFraction;
        public final double environmentCoordinate,environmentRaw;
        private Point(long cycle,long ms,double x,double y,double z,double e,double raw){
            scanCycle=cycle;timeMs=ms;xLogRatio=x;yLogRatio=y;elapsedFraction=z;
            environmentCoordinate=e;environmentRaw=raw;
        }
    }

    public static final class Result {
        public final String schema,sessionId,deviceId,sensorA,sensorB,status;
        public final EnvironmentAxis environmentAxis;
        public final List<Point> points;
        public final int submittedFrames,distinctValidScanCycles,rejectedFrames,duplicateCycles;
        public final boolean available3D,available4D,environmentCompensated,
                             identifiableGas,reportablePpm,tankG3Validated;
        public final double pathLength3D,pathLength4D,turningAngle3DRad,
                            responseAChangeLog10,responseBChangeLog10;
        private Result(String session,String device,String a,String b,EnvironmentAxis axis,
                String state,List<Point> pts,int submitted,int rejected,int duplicates,
                boolean three,boolean four,double len3,double len4,double turning,
                double dA,double dB){
            schema=SCHEMA;sessionId=session;deviceId=device;sensorA=a;sensorB=b;
            environmentAxis=axis;status=state;
            points=Collections.unmodifiableList(new ArrayList<>(pts));
            submittedFrames=submitted;distinctValidScanCycles=pts.size();
            rejectedFrames=rejected;duplicateCycles=duplicates;
            available3D=three;available4D=four;
            environmentCompensated=false;identifiableGas=false;
            reportablePpm=false;tankG3Validated=false;
            pathLength3D=len3;pathLength4D=len4;turningAngle3DRad=turning;
            responseAChangeLog10=dA;responseBChangeLog10=dB;
        }
    }

    public static Result analyze(List<Frame> source,String sensorA,String sensorB,
                                 EnvironmentAxis dimension){
        if(sensorA==null||sensorB==null||sensorA.trim().isEmpty()||
           sensorB.trim().isEmpty()||sensorA.equals(sensorB))
            throw new IllegalArgumentException("Two distinct named physical MQ channels required");
        if(source!=null&&source.size()>MAX_FRAMES)
            throw new IllegalArgumentException("Excessive input frames; split by session");
        EnvironmentAxis axis=dimension==null?EnvironmentAxis.NONE:dimension;
        int submitted=source==null?0:source.size(),rejected=0,duplicates=0;
        List<Frame> valid=new ArrayList<>();
        Set<Long> seenCycles=new HashSet<>();
        String session="",device="";
        long latestTime=-1,latestCycle=-1;
        if(source!=null)for(Frame frame:source){
            if(frame==null||frame.sessionId.isEmpty()||frame.deviceId.isEmpty()||
               frame.scanCycle<0||frame.timeMs<0){rejected++;continue;}
            Sensor a=frame.sensors.get(sensorA),b=frame.sensors.get(sensorB);
            if(a==null||b==null||
               !a.recordedDeviceRatioAvailable(frame.deviceId)||
               !b.recordedDeviceRatioAvailable(frame.deviceId)){
                rejected++;continue;
            }
            if(!session.isEmpty()&&(!session.equals(frame.sessionId)||
               !device.equals(frame.deviceId))){rejected++;continue;}
            if(seenCycles.contains(frame.scanCycle)){duplicates++;continue;}
            if(frame.timeMs<=latestTime||frame.scanCycle<=latestCycle){
                rejected++;continue;
            }
            if(session.isEmpty()){session=frame.sessionId;device=frame.deviceId;}
            valid.add(frame);seenCycles.add(frame.scanCycle);
            latestTime=frame.timeMs;latestCycle=frame.scanCycle;
        }
        boolean enough=valid.size()>=MIN_INDEPENDENT_CYCLES&&
            valid.get(valid.size()-1).timeMs>valid.get(0).timeMs;
        boolean environmentComplete=axis!=EnvironmentAxis.NONE;
        double baseEnvironment=Double.NaN;
        if(!valid.isEmpty()){
            Frame first=valid.get(0);
            baseEnvironment=axis==EnvironmentAxis.HUMIDITY_RH?
                first.humidityRh:axis==EnvironmentAxis.TEMPERATURE_C?
                first.temperatureC:Double.NaN;
        }
        for(Frame f:valid){
            double e=axis==EnvironmentAxis.HUMIDITY_RH?f.humidityRh:f.temperatureC;
            if(!Double.isFinite(e)||axis==EnvironmentAxis.HUMIDITY_RH&&(e<0||e>100))
                environmentComplete=false;
        }
        List<Point> points=new ArrayList<>();
        if(!valid.isEmpty()){
            long first=valid.get(0).timeMs,last=valid.get(valid.size()-1).timeMs;
            for(Frame frame:valid){
                Sensor a=frame.sensors.get(sensorA),b=frame.sensors.get(sensorB);
                double x=Math.log10(a.rsKohm/a.r0Kohm);
                double y=Math.log10(b.rsKohm/b.r0Kohm);
                double t=last>first?(frame.timeMs-first)/(double)(last-first):0;
                double raw=axis==EnvironmentAxis.HUMIDITY_RH?
                    frame.humidityRh:axis==EnvironmentAxis.TEMPERATURE_C?
                    frame.temperatureC:Double.NaN;
                double env=environmentComplete?
                    (raw-baseEnvironment)/(axis==EnvironmentAxis.HUMIDITY_RH?100.0:40.0):
                    Double.NaN;
                points.add(new Point(frame.scanCycle,frame.timeMs,x,y,t,env,raw));
            }
        }
        double length3=Double.NaN,length4=Double.NaN,angle=Double.NaN;
        boolean four=enough&&environmentComplete;
        if(enough){
            length3=0;length4=four?0:Double.NaN;angle=0;
            for(int i=1;i<points.size();i++){
                Point p=points.get(i-1),q=points.get(i);
                double dx=q.xLogRatio-p.xLogRatio;
                double dy=q.yLogRatio-p.yLogRatio;
                double dz=q.elapsedFraction-p.elapsedFraction;
                length3+=Math.sqrt(dx*dx+dy*dy+dz*dz);
                if(four){
                    double de=q.environmentCoordinate-p.environmentCoordinate;
                    length4+=Math.sqrt(dx*dx+dy*dy+dz*dz+de*de);
                }
            }
            for(int i=2;i<points.size();i++){
                Point a=points.get(i-2),b=points.get(i-1),c=points.get(i);
                double ax=b.xLogRatio-a.xLogRatio,ay=b.yLogRatio-a.yLogRatio,
                    az=b.elapsedFraction-a.elapsedFraction;
                double bx=c.xLogRatio-b.xLogRatio,by=c.yLogRatio-b.yLogRatio,
                    bz=c.elapsedFraction-b.elapsedFraction;
                double na=Math.sqrt(ax*ax+ay*ay+az*az);
                double nb=Math.sqrt(bx*bx+by*by+bz*bz);
                if(na>1e-12&&nb>1e-12){
                    double cosine=(ax*bx+ay*by+az*bz)/(na*nb);
                    angle+=Math.acos(Math.max(-1,Math.min(1,cosine)));
                }
            }
        }
        double da=Double.NaN,db=Double.NaN;
        if(enough){
            da=points.get(points.size()-1).xLogRatio-points.get(0).xLogRatio;
            db=points.get(points.size()-1).yLogRatio-points.get(0).yLogRatio;
        }
        String status=!enough?"INSUFFICIENT_DISTINCT_SCAN_CYCLES":
            four?"RESEARCH_3D_AND_4D_UNCOMPENSATED":"RESEARCH_3D_ONLY";
        return new Result(session,device,sensorA,sensorB,axis,status,points,
            submitted,rejected,duplicates,enough,four,length3,length4,angle,da,db);
    }
}
