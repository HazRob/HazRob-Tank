package com.hazrob.tank.chemcore.next;

import java.util.ArrayList;
import java.util.List;

/**
 * P11.2: PINNED ORIGINAL MQSensorsLib source *example equations*, not a
 * new chemical database, Tank calibration, or certified gas fingerprints.
 *
 * MQSensorsLib v3.0.5 source commit:
 * 59a2e5852ec12f5da3cf508be68f5a9405314003
 * Original format (regressionMethod=1): ppm=A*(Rs/R0_example)^B.
 * MQUnifiedsensor::calibrate(K): R0_example=Rs_clean_air/K.
 *
 * The source's K requires independently matched sensor/heater/environment
 * evidence before concentrations can become physically meaningful. All
 * sample estimates below MUST remain research-only conditional numerics.
 *
 * MQ-Board's lines 131..137 incorrectly call MQ2.calibrate() for other
 * sensors. We reproduce its published *coefficients*, NOT this defect.
 * MQ135 CO2 example additionally prints CO2 + 400: do not silently add
 * that offset to these raw curve equations.
 */
public final class MQSensorsLibExampleCatalog {
    private MQSensorsLibExampleCatalog() {}
    public static final String COMMIT="59a2e5852ec12f5da3cf508be68f5a9405314003";
    public static final String ROOT_MQ135="MQSL_MQ135_EXAMPLE_SAME_AS_ADS1115_EXAMPLE";
    public static final String ROOT_BOARD="MQSL_MQ_BOARD_8_MODEL_EXAMPLE";
    public static final String BLOB_MQ135="3ffae99864cfc51c12941647290832dc39767bb2";
    public static final String BLOB_BOARD="ce78fce7d9f2af0465924d9bd317da1f7ba486b8";

    public static final class Model {
        public final String id,model,label,sourceRoot,sourceBlob,sourceFile;
        public final int sourceLine;
        public final double a,b,cleanAirK;
        public final boolean installedOnTank;
        private Model(int index,String sensor,String originalLabel,double a,double b,
                      double cleanAirK,boolean installed,int line,boolean is135){
            this.id=String.format(java.util.Locale.ROOT,"MQSL-EX-%03d",index);
            model=sensor;label=originalLabel;this.a=a;this.b=b;
            this.cleanAirK=cleanAirK;installedOnTank=installed;
            sourceLine=line;sourceRoot=is135?ROOT_MQ135:ROOT_BOARD;
            sourceBlob=is135?BLOB_MQ135:BLOB_BOARD;
            sourceFile=is135?"examples/MQ-135-ALL/MQ-135-ALL.ino":
                "examples/MQ-Board/MQ-Board.ino";
            if(!(Double.isFinite(a)&&a>0&&Double.isFinite(b)&&b!=0&&
                 Double.isFinite(cleanAirK)&&cleanAirK>0))
                throw new IllegalArgumentException("invalid pinned MQ source equation");
        }
    }
    private static Model mk(int i,String m,String label,double a,double b,
                            double k,boolean installed,int line,boolean is135){
        return new Model(i,m,label,a,b,k,installed,line,is135);
    }
    private static final List<Model> ALL=List.of(
        mk(1,"MQ135","CO",605.18,-3.937,3.6,true,83,true),
        mk(2,"MQ135","Alcohol",77.255,-3.18,3.6,true,86,true),
        mk(3,"MQ135","CO2",110.47,-2.862,3.6,true,89,true),
        mk(4,"MQ135","Toluene",44.947,-3.445,3.6,true,92,true),
        mk(5,"MQ135","NH4_UNRESOLVED_CLASS",102.2,-2.473,3.6,true,95,true),
        mk(6,"MQ135","Acetone",34.668,-3.369,3.6,true,98,true),
        mk(7,"MQ2","LPG",574.25,-2.222,9.83,true,64,false),
        mk(8,"MQ3","Alcohol",0.3934,-1.504,60.0,true,69,false),
        mk(9,"MQ4","CH4",1012.7,-2.786,4.4,true,74,false),
        mk(10,"MQ5","Alcohol",97124.0,-4.918,6.5,true,79,false),
        mk(11,"MQ6","CH4",2127.2,-2.526,10.0,false,84,false),
        mk(12,"MQ7","CO",99.042,-1.518,27.5,true,89,false),
        mk(13,"MQ8","H2",976.97,-0.688,70.0,true,94,false),
        mk(14,"MQ9","LPG",1000.5,-2.186,9.6,true,99,false)
    );
    public static List<Model> all(){return ALL;}
    public static List<Model> installedModels(){
        List<Model> installed=new ArrayList<>(13);
        for(Model m:ALL)if(m.installedOnTank)installed.add(m);
        return List.copyOf(installed);
    }
}
