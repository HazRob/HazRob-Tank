package com.hazrob.tank.chemcore.v6;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Independent Tank ChemCore V6 research source catalog.
 *
 * Source: HazRob/build-harness/library/research/hr069/mq_research_curves_43.json
 * Immutable Git blob: bd597ffe9ecaee6a0ccfddc5ec1b89c1cccc0ba1.
 *
 * All 43 numeric relations are available for research, regardless of
 * missing reference R0/heater metadata or out-of-source-domain extrapolation.
 * These are response equations and do not certify actual Tank ppm.
 * No dependency on another ChemCore application or engine.
 */
public final class SourceCatalog {
    public static final String SOURCE_ROOT="MQDATASCIENCE_6_0_0";
    public static final String SOURCE_BLOB="bd597ffe9ecaee6a0ccfddc5ec1b89c1cccc0ba1";
    public static final String SOURCE_COMMIT="e88f876140587a708ee84a4d4d3acbd5720274ee";
    private SourceCatalog(){}

    public static final class Curve {
        public final int index;
        public final String sensorType, label;
        public final long legacyChemicalId;
        public final double a,b,minSourcePpm,maxSourcePpm,outputScale;
        private Curve(int idx,String sensor,long chemical,String name,
                      double a,double b,double min,double max,double scale){
            index=idx;sensorType=sensor;legacyChemicalId=chemical;label=name;
            this.a=a;this.b=b;minSourcePpm=min;maxSourcePpm=max;outputScale=scale;
        }
        public String id(){return String.format(Locale.ROOT,"MQDATA43-%03d",index);}
        public double log10Prediction(double log10Ppm) {
            return Math.log10(a)+b*(log10Ppm-Math.log10(outputScale));
        }
        public boolean numericalDefinitionValid(){
            return finitePositive(a)&&Double.isFinite(b)&&b!=0 &&
                   finitePositive(outputScale);
        }
    }
    private static boolean finitePositive(double x){return Double.isFinite(x)&&x>0;}
    private static final Curve[] SOURCES=new Curve[]{
        new Curve(1, "MQ2", 135L, "LPG", 17.6135, -0.4539, 200, 10000, 1),
        new Curve(2, "MQ2", 78L, "propane", 19.5575, -0.461, 200, 10000, 1),
        new Curve(3, "MQ2", 75L, "H2", 25.7473, -0.4731, 200, 10000, 1),
        new Curve(4, "MQ2", 103L, "alcohol", 19.2641, -0.3604, 200, 10000, 1),
        new Curve(5, "MQ2", 76L, "CH4", 20.7074, -0.36, 200, 10000, 1),
        new Curve(6, "MQ2", 139L, "smoke", 26.4698, -0.3876, 200, 10000, 1),
        new Curve(7, "MQ2", 20L, "CO", 28.024, -0.3182, 200, 10000, 1),
        new Curve(8, "MQ3", 103L, "Alcohol", 0.5565, -0.6333, 5.15, 500, 50),
        new Curve(9, "MQ3", 34L, "Benzene", 1.7957, -0.369, 5.15, 500, 50),
        new Curve(10, "MQ3", 84L, "Hexane", 22.9337, -0.358, 5.15, 500, 50),
        new Curve(11, "MQ3", 135L, "LPG", 27.8286, -0.2847, 5.15, 500, 50),
        new Curve(12, "MQ3", 20L, "CO", 31.9559, -0.2325, 5.15, 500, 50),
        new Curve(13, "MQ3", 76L, "CH4", 44.8389, -0.0476, 5.15, 500, 50),
        new Curve(14, "MQ4", 76L, "CH4", 11.5517, -0.3541, 200, 10000, 1),
        new Curve(15, "MQ4", 135L, "LPG", 14.1866, -0.3227, 200, 10000, 1),
        new Curve(16, "MQ4", 75L, "H2", 9.6432, -0.1765, 200, 10000, 1),
        new Curve(17, "MQ4", 139L, "smoke", 7.05, -0.1065, 200, 10000, 1),
        new Curve(18, "MQ4", 103L, "alcohol", 5.7994, -0.0696, 200, 10000, 1),
        new Curve(19, "MQ4", 20L, "CO", 5.49, -0.0495, 200, 10000, 1),
        new Curve(20, "MQ5", 135L, "LPG", 6.1067, -0.4102, 200, 10000, 1),
        new Curve(21, "MQ5", 76L, "CH4", 6.987, -0.3777, 200, 10000, 1),
        new Curve(22, "MQ5", 75L, "H2", 7.4265, -0.2847, 200, 10000, 1),
        new Curve(23, "MQ5", 103L, "alcohol", 10.8274, -0.2104, 200, 10000, 1),
        new Curve(24, "MQ5", 20L, "CO", 8.2236, -0.1492, 200, 10000, 1),
        new Curve(25, "MQ7", 75L, "H2", 21.1794, -0.7174, 50, 4000, 1),
        new Curve(26, "MQ7", 20L, "CO", 20.89, -0.6568, 50, 4000, 1),
        new Curve(27, "MQ7", 135L, "LPG", 14.9745, -0.1329, 50, 4000, 1),
        new Curve(28, "MQ7", 76L, "CH4", 19.8094, -0.09, 50, 4000, 1),
        new Curve(29, "MQ7", 103L, "Alcohol", 20.2524, -0.0655, 50, 4000, 1),
        new Curve(30, "MQ8", 75L, "H2", 18391.5667, -1.4494, 200, 10000, 1),
        new Curve(31, "MQ8", 103L, "alcohol", 468.8045, -0.5574, 200, 10000, 1),
        new Curve(32, "MQ8", 135L, "LPG", 152.1255, -0.2922, 200, 10000, 1),
        new Curve(33, "MQ8", 76L, "CH4", 117.9639, -0.1448, 200, 10000, 1),
        new Curve(34, "MQ8", 20L, "CO", 126.605, -0.1173, 200, 10000, 1),
        new Curve(35, "MQ9", 20L, "CO", 16.8797, -0.4445, 200, 1000, 1),
        new Curve(36, "MQ9", 135L, "LPG", 24.918, -0.4695, 200, 10000, 1),
        new Curve(37, "MQ9", 76L, "CH4", 21.8247, -0.3663, 200, 10000, 1),
        new Curve(38, "MQ135", 110L, "Acetone", 2.7988, -0.2913, 10, 200, 1),
        new Curve(39, "MQ135", 95L, "Toluene", 2.9788, -0.2892, 10, 200, 1),
        new Curve(40, "MQ135", 103L, "Alcohol", 3.8623, -0.3126, 10, 200, 1),
        new Curve(41, "MQ135", 23L, "CO2", 5.2278, -0.3585, 10, 200, 1),
        new Curve(42, "MQ135", 21L, "NH4/NH3", 6.2968, -0.3892, 10, 200, 1),
        new Curve(43, "MQ135", 20L, "CO", 5.0533, -0.2541, 10, 200, 1)
    };
    private static final List<Curve> ALL;
    static{
        List<Curve> v=new ArrayList<>(SOURCES.length);
        Collections.addAll(v,SOURCES);
        ALL=Collections.unmodifiableList(v);
    }
    public static List<Curve> curves(){return ALL;}
}
