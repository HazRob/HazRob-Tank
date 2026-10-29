package com.hazrob.tank.chemcore.v6;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * N05-J: ORIGINAL Minnesota MQ4 reference methane replay via the actual,
 * independent Java ChemCoreTankV6 coordinator, plus real-source V6 PLS1 research.
 *
 * Source: SHA-pinned 10ppm_data.csv / Furuta et al, 14,186 original minute rows,
 * three physical MQ4 instances, Li-Cor CH4 ppb, T °C, RH %.
 *
 * Critical limitation: original source DOES NOT supply per-unit Tank R0.
 * Feed raw RESISTANCE IN OHMS and R0=NaN to V6; never fabricate Rs/R0,
 * gas identity, ppm, or a G3 certification. Reference Li-Cor labels are
 * held separately and NEVER passed to coordinator.
 *
 * PLS1 source regression is a distinct research method. Whole-day
 * TRAIN 9 / CAL 1 / TEST 1 (11 folds), no leaked source day.
 */
public final class MinnesotaMq4JavaV6Replay {
    private MinnesotaMq4JavaV6Replay(){}
    private static final String[] TYPES={"MQ4_1","MQ4_2","MQ4_3"};
    private static final String[] MODELS={
        "SOURCE_TRAIN_MEDIAN","V6_PLS1_MQ4_UNIT1",
        "V6_PLS1_MQ4_THREE","V6_PLS1_MQ4_THREE_T_RH"
    };
    private static void require(boolean b,String what) {
        if(!b)throw new IllegalArgumentException(what);
    }
    private static final class Row {
        final int id;
        final String timestamp,day;
        final double methanePpb,temperature,rh;
        final double[] resistance;
        final double[][] input;
        Row(int id,String timestamp,double methanePpb,double t,double h,double[] r) {
            this.id=id;this.timestamp=timestamp;day=timestamp.substring(0,10);
            this.methanePpb=methanePpb;temperature=t;rh=h;
            resistance=r.clone();
            input=new double[][]{
                {Math.log(r[0])},
                {Math.log(r[0]),Math.log(r[1]),Math.log(r[2])},
                {Math.log(r[0]),Math.log(r[1]),Math.log(r[2]),t,h}
            };
        }
        double ppm(){return methanePpb/1000.0;}
    }
    private static double decimal(String s,String col) {
        double v=Double.parseDouble(s.trim());
        require(Double.isFinite(v),"nonfinite original "+col);
        return v;
    }
    private static List<Row> read(Path source)throws Exception {
        List<Row> records=new ArrayList<>();
        try(BufferedReader reader=Files.newBufferedReader(source,StandardCharsets.UTF_8)) {
            String header=reader.readLine();
            require(header!=null&&header.equals(
                "datetime,licor_ch4,licor_h2o,t,rh,2600_1,2600_2,2600_3,"+
                "2602_1,2602_2,2602_3,2611C_1,2611C_2,2611C_3,"+
                "2611E_1,2611E_2,2611E_3,MQ4_1,MQ4_2,MQ4_3"),
                "original 20-column Li-Cor/MQ4 source schema required");
            String line,previous=null;
            while((line=reader.readLine())!=null) {
                String[] cells=line.split(",",-1);
                require(cells.length==20,"exactly 20 original source columns");
                String time=cells[0].trim();
                require(time.length()>=16&&time.charAt(4)=='-'&&
                    time.charAt(7)=='-'&&time.charAt(10)==' ',
                    "original UTC/local source time axis must be retained");
                require(previous==null||time.compareTo(previous)>0,
                    "original source times must strictly increase");
                previous=time;
                double ref=decimal(cells[1],"licor_ch4");
                double temp=decimal(cells[3],"t");
                double rh=decimal(cells[4],"rh");
                double[] rs={decimal(cells[17],"MQ4_1"),
                            decimal(cells[18],"MQ4_2"),
                            decimal(cells[19],"MQ4_3")};
                require(ref>0&&rs[0]>0&&rs[1]>0&&rs[2]>0&&
                        rh>=0&&rh<=100,"physically valid original source rows");
                records.add(new Row(records.size(),time,ref,temp,rh,rs));
            }
        }
        require(records.size()==14186,"expected exact original source 14186 minutes");
        Set<String> days=new LinkedHashSet<>();
        for(Row r:records)days.add(r.day);
        require(days.size()==11&&days.iterator().next().equals("2021-07-21")&&
            records.get(records.size()-1).day.equals("2021-07-31"),
            "11 original physical acquisition dates 2021-07-21 to 07-31");
        return records;
    }
    private static void replayCoordinator(List<Row> rows,BufferedWriter audit)
            throws Exception {
        audit.write("source_row_id,source_datetime,source_day,licor_ch4_reference_ppb,"+
            "mq4_1_original_rs_ohm,mq4_2_original_rs_ohm,mq4_3_original_rs_ohm,"+
            "retained_channels,channels_missing_r0,ratio_ready,source_curve_fits,"+
            "chemical_hypotheses,reportable_ppm,physical_tank_g3,food_data_used\n");
        int n=0;
        for(Row r:rows) {
            List<Engine.Channel> channels=Arrays.asList(
                new Engine.Channel("MQ4","MINNESOTA_MQ4_1",r.resistance[0],Double.NaN),
                new Engine.Channel("MQ4","MINNESOTA_MQ4_2",r.resistance[1],Double.NaN),
                new Engine.Channel("MQ4","MINNESOTA_MQ4_3",r.resistance[2],Double.NaN));
            // The Li-Cor CH4 reference NEVER enters the V6 inference request.
            ChemCoreTankV6.Result out=ChemCoreTankV6.analyze(
                new ChemCoreTankV6.Request(channels,
                    Collections.<FingerprintEngineV6.Input>emptyList(),
                    r.temperature,r.rh,Double.NaN));
            Engine.Result curves=out.responseCurves;
            require(curves.observedPhysicalChannels==3&&curves.rawChannelsAwaitingR0==3&&
                curves.ratioReadyChannels==0&&curves.allSourceFits.isEmpty()&&
                curves.rankedCandidates.isEmpty()&&out.chemicalEvidence.isEmpty(),
                "NO physically calibrated R0: V6 must not hallucinate source curve/gas fits");
            require(!curves.ppmReportable&&!curves.actualTankCalibrationVerified&&
                !out.physicalCalibrationProven&&!out.reportablePpm&&!out.foodModelAttached,
                "source-only original resistance may not certify field gas identity or ppm");
            for(int k=0;k<3;k++){
                Engine.Channel ch=curves.retainedRawSensorRecords.get(k);
                require(ch.sensorInstance.equals("MINNESOTA_"+TYPES[k])&&
                    ch.rs==r.resistance[k]&&Double.isNaN(ch.r0)&&
                    ch.rawResistanceValid()&&!ch.normalizedRatioAvailable(),
                    "all original physical MQ4 instance ohms retained without fake R0");
            }
            audit.write(String.format(Locale.ROOT,
                "%d,%s,%s,%.10f,%.10f,%.10f,%.10f,3,3,0,0,0,false,false,false%n",
                r.id,r.timestamp,r.day,r.methanePpb,
                r.resistance[0],r.resistance[1],r.resistance[2]));
            n++;
        }
        require(n==14186,"entire original source replayed");
        System.out.println("PASS V6_JAVA_COORDINATOR_REAL_MQ4: "+n+
            " original reference source rows; 42558 original MQ4 resistances preserved; "+
            "zero invented R0, source gas identities or reportable ppm.");
    }
    private static double percentile90(double[] errors){
        require(errors.length>0,"nonempty independent calibration day");
        double[] sorted=errors.clone();
        Arrays.sort(sorted);
        int index=Math.min(sorted.length-1,
            Math.max(0,(int)Math.ceil((sorted.length+1)*.90)-1));
        return sorted[index];
    }
    private static double median(List<Row> train) {
        double[] values=new double[train.size()];
        for(int i=0;i<train.size();i++)values[i]=train.get(i).ppm();
        Arrays.sort(values);
        int n=values.length;
        return n%2==1?values[n/2]:(values[n/2-1]+values[n/2])/2;
    }
    private static final class Predictor {
        final double median;
        final SourceNumericsV6.Pls1[] models;
        Predictor(List<Row> train){
            median=median(train);
            models=new SourceNumericsV6.Pls1[3];
            double[] y=new double[train.size()];
            for(int i=0;i<train.size();i++)y[i]=train.get(i).ppm();
            for(int k=0;k<3;k++){
                double[][] xs=new double[train.size()][];
                for(int i=0;i<train.size();i++)xs[i]=train.get(i).input[k];
                models[k]=SourceNumericsV6.fitPls1(xs,y);
                require(models[k].trainingRows==train.size(),
                    "SourceNumericsV6 PLS1 must train on train days only");
            }
        }
        double predict(int model,Row r) {
            return model==0?median:models[model-1].predict(r.input[model-1]);
        }
    }
    private static void sourceDayHoldouts(List<Row> all,BufferedWriter summary,
                                           BufferedWriter prediction) throws Exception {
        summary.write("model,test_day,calibration_day,train_days,train_rows,cal_rows,"+
            "test_rows,mae_ppm,rmse_ppm,bias_ppm,interval90_source_halfwidth_ppm,"+
            "empirical_test_coverage,analytical_4ppm_TP,analytical_4ppm_FN,"+
            "analytical_4ppm_FP,analytical_4ppm_TN,nonpositive_estimates,"+
            "research_only,no_tank_g3\n");
        prediction.write("model,test_day,source_row_id,source_timestamp,licor_reference_ppm,"+
            "source_research_estimate_ppm,absolute_error_ppm,source_prediction_nonphysical,"+
            "interval_contains_reference,never_reportable_ppm\n");
        List<String> days=new ArrayList<>(new LinkedHashSet<String>());
        for(Row r:all)if(!days.contains(r.day))days.add(r.day);
        require(days.size()==11,"11 days source audit");
        int folds=0,rows=0;
        for(int fold=0;fold<days.size();fold++){
            String testDay=days.get(fold);
            String calDay=days.get((fold+days.size()-1)%days.size());
            List<Row> train=new ArrayList<>(),cal=new ArrayList<>(),test=new ArrayList<>();
            Set<String> trainDays=new LinkedHashSet<>();
            for(Row r:all){
                if(r.day.equals(testDay))test.add(r);
                else if(r.day.equals(calDay))cal.add(r);
                else{train.add(r);trainDays.add(r.day);}
            }
            require(trainDays.size()==9&&!train.isEmpty()&&!cal.isEmpty()&&!test.isEmpty(),
                "9 TRAIN 1 CAL 1 TEST independent days with no overlap");
            Predictor p=new Predictor(train);
            for(int model=0;model<MODELS.length;model++){
                double[] errors=new double[cal.size()];
                for(int j=0;j<cal.size();j++)
                    errors[j]=Math.abs(p.predict(model,cal.get(j))-cal.get(j).ppm());
                double width=percentile90(errors);
                require(Double.isFinite(width)&&width>=0,"calibration day finite quantile");
                double absolute=0,sq=0,bias=0,coverage=0;
                int tp=0,fn=0,fp=0,tn=0,nonPositive=0;
                for(Row r:test){
                    double truth=r.ppm(),estimate=p.predict(model,r);
                    require(Double.isFinite(estimate),"source PLS1 numeric out of range");
                    double diff=estimate-truth,ae=Math.abs(diff);
                    boolean covered=ae<=width;
                    absolute+=ae;sq+=diff*diff;bias+=diff;
                    if(covered)coverage++;
                    if(estimate<=0)nonPositive++;
                    // 4 ppm is solely a numerical stress diagnostic, NOT a safety limit.
                    if(truth>=4&&estimate>=4)tp++;
                    else if(truth>=4)fn++;
                    else if(estimate>=4)fp++;
                    else tn++;
                    prediction.write(String.format(Locale.ROOT,
                        "%s,%s,%d,%s,%.10f,%.10f,%.10f,%b,%b,true%n",
                        MODELS[model],testDay,r.id,r.timestamp,truth,estimate,ae,
                        estimate<=0,covered));
                    rows++;
                }
                require(tp+fn+fp+tn==test.size(),"confusion matrix evidence conservation");
                double n=test.size();
                summary.write(String.format(Locale.ROOT,
                    "%s,%s,%s,9,%d,%d,%d,%.10f,%.10f,%.10f,%.10f,"+
                    "%.10f,%d,%d,%d,%d,%d,true,true%n",
                    MODELS[model],testDay,calDay,train.size(),cal.size(),test.size(),
                    absolute/n,Math.sqrt(sq/n),bias/n,width,coverage/n,
                    tp,fn,fp,tn,nonPositive));
                folds++;
            }
        }
        require(folds==44&&rows==4*14186,"all original source days evaluated once per model");
        System.out.println("PASS V6_JAVA_PLS1_REAL_MQ4: 44 whole-day source folds, "+
            "56744 original-source heldout predictions, TRAIN/CAL/TEST day disjoint.");
    }
    public static void main(String[] args)throws Exception {
        require(args.length==4,"required original 10ppm_data.csv, coordinator ledger, "+
                "research source day fold summaries, source heldout predictions");
        List<Row> records=read(Path.of(args[0]));
        try(BufferedWriter audit=Files.newBufferedWriter(Path.of(args[1]),
                StandardCharsets.UTF_8);
            BufferedWriter summary=Files.newBufferedWriter(Path.of(args[2]),
                StandardCharsets.UTF_8);
            BufferedWriter predictions=Files.newBufferedWriter(Path.of(args[3]),
                StandardCharsets.UTF_8)) {
            replayCoordinator(records,audit);
            sourceDayHoldouts(records,summary,predictions);
        }
        System.out.println("PASS N05-J ORIGINAL MQ4 JAVA REAL-SOURCE benchmark: "+
            "actual new ChemCoreTankV6 coordinator and SourceNumericsV6 PLS1 "+
            "source-only; no Tank G3/ppm certification; no food.");
    }
}
