package com.hazrob.tank.foodfreshness.meat;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Food-only beef 2018 e-nose class references. Original 5 study cuts TS1–TS5:
 * TS1–TS4 training 8640 time rows, TS5 held out 2160 time rows.
 * Study measures MQ sensor resistance + humidity/temperature, NOT bacterial
 * viability on a new Android sample. This compact model is a derived
 * exploratory median-log-shape method, not the paper's exact trained SVM.
 */
public final class BeefResearchCatalog {
    public static final String STUDY_DOI="10.17632/mwmhh766fc.3";
    public static final String STUDY_PAPER="10.1016/j.dib.2018.11.075";
    public static final String MODEL="wijaya2018-5series-seven-overlap-median-centered-log-shape/1";
    public static final double HELDOUT_ACCURACY=0.4393518518518518;
    public static final double HELDOUT_BALANCED_ACCURACY=0.38878968253968255;
    public static final List<String> SENSOR_ORDER=Collections.unmodifiableList(
        Arrays.asList("MQ2","MQ3","MQ4","MQ5","MQ8","MQ9","MQ135"));
    public static final String[] GRADE_CODES=new String[]{"excellent","good","acceptable","spoiled"};

    private BeefResearchCatalog(){}
    public static final class Template {
        public final String label;
        public final int originalTrainingRows;
        private final double[] logProfile;
        Template(String value,int n,double[] vector){label=value;originalTrainingRows=n;logProfile=vector.clone();}
        public double[] logShape(){return logProfile.clone();}
    }
    private static final Template[] ORIGINAL={
        new Template("excellent",1320,new double[]{-0.22874039553091652,-0.07563906182913427,0.19800778303759403,0.21485237171951643,0.31675691447727283,-0.21475920170217228,-0.09027243289645726}),
        new Template("good",2280,new double[]{-0.3703318135558432,0.09821923966682489,0.15501727901613827,-0.1352554089206457,0.2368388306710052,-0.025031350502869698,-0.007759191595329784}),
        new Template("acceptable",1680,new double[]{-0.16490612734603166,0.18376703757403168,-0.3565854445975135,-0.04865652527760847,-0.13694646904117275,0.1237773938892423,0.09486705906887632}),
        new Template("spoiled",3360,new double[]{-0.08278315899345207,0.20274433821915255,-0.5068317418223839,-0.1088166396864989,-0.06073942291351336,0.24069875529608675,0.19679980780454645})
    };
    private static final List<Template> CLASSES;
    static{
        List<Template> list=new ArrayList<>(ORIGINAL.length);
        Collections.addAll(list,ORIGINAL);
        CLASSES=Collections.unmodifiableList(list);
    }
    public static List<Template> classes(){return CLASSES;}
}
