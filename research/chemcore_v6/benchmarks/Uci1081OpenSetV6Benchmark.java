package com.hazrob.tank.chemcore.v6;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * N05-H ORIGINAL UCI1081 unknown-gas experiment with genuinely different
 * gas IDs in CALIBRATION-negative vs TEST-negative.
 *
 * For each test-unknown gas and concentration-heldout level:
 * - 4 known gases x 2 other concentrations x 5 reps: 40 TRAIN exposures
 * - 4 known gases x withheld level x first 2 reps: 8 CAL known
 * - different gas (not in TRAIN or TEST-unknown): 15 CAL unknown exposures
 * - 4 known gases x withheld level x last 3 reps: 12 TEST known
 * - completely different source gas: 15 TEST unknown exposures.
 *
 * Entire source exposure rows are indivisible. The unknown TEST gas never
 * occurs in train or calibration, including feature model fit. The unknown
 * calibration gas never occurs in train or test. There is no tuning on TEST.
 *
 * This DOES NOT establish device/day independence or Tank G3.
 */
public final class Uci1081OpenSetV6Benchmark {
    private Uci1081OpenSetV6Benchmark(){}
    private static final String[] GASES={"ethanol","acetone","toluene",
        "ethyl acetate","isopropanol","hexane"};
    private static final int[] LEVELS={50,100,200};
    private static final String[] MODES={"MQ135_only","full_source_10_channels"};
    private static void require(boolean yes,String why) {
        if(!yes)throw new IllegalArgumentException(why);
    }
    private static final class Exposure {
        final int id,ppb;
        final String gas;
        final double[] x;
        Exposure(int id,String gas,int level,double[] features) {
            this.id=id;this.gas=gas;ppb=level;x=features;
        }
    }
    private static List<Exposure> load(Path file)throws Exception{
        List<Exposure> rows=new ArrayList<>();
        try(BufferedReader reader=Files.newBufferedReader(file,StandardCharsets.UTF_8)){
            String head=reader.readLine();
            require(head!=null&&head.startsWith("row_id,gas,ppb,f00,"),
                    "verified original 90x30 UCI feature schema");
            String line;
            while((line=reader.readLine())!=null){
                String[] parts=line.split(",",-1);
                require(parts.length==33,"one original exposure row must have 30 features");
                int id=Integer.parseInt(parts[0]),ppb=Integer.parseInt(parts[2]);
                require(id==rows.size(),"source row IDs must be exactly ordered");
                require(Arrays.asList(GASES).contains(parts[1]),"six source gas names");
                require(ppb==50||ppb==100||ppb==200,"source ppb levels");
                double[] f=new double[30];
                for(int i=0;i<30;i++){
                    f[i]=Double.parseDouble(parts[i+3]);
                    require(Double.isFinite(f[i]),"complete original source feature");
                }
                rows.add(new Exposure(id,parts[1],ppb,f));
            }
        }
        require(rows.size()==90,"must be 90 original exposures");
        for(String gas:GASES)for(int ppb:LEVELS){
            int n=0;for(Exposure x:rows)if(x.gas.equals(gas)&&x.ppb==ppb)n++;
            require(n==5,"must have 5 replicate exposures per gas/level");
        }
        return rows;
    }
    private static double[] select(Exposure row,String mode){
        if(mode.equals("MQ135_only"))
            return new double[]{row.x[4],row.x[14],row.x[24]};
        return row.x.clone();
    }
    private static int replicateIndex(List<Exposure> original,Exposure target){
        int r=0;
        for(Exposure e:original){
            if(e.id==target.id)return r;
            if(e.gas.equals(target.gas)&&e.ppb==target.ppb)r++;
        }
        throw new IllegalArgumentException("source row not found");
    }
    private static void writeRecordIds(BufferedWriter output,String mode,int level,
                      String calibrationUnknown,String sealedUnknown,String partition,
                      List<Exposure> entries)throws Exception {
        for(Exposure e:entries)
            output.write(mode+","+level+","+calibrationUnknown+","+sealedUnknown+
                        ","+partition+","+e.id+","+e.gas+","+e.ppb+"\n");
    }
    private static final class Result {
        int knownCorrectWithoutGate,knownCorrectAfterGate,knownKept,
            unknownWronglyAccepted,unknownRejected;
    }
    private static Result assess(List<Exposure> examples,boolean isUnknown,
                    ClassicalLearningV6.SoftmaxModel classifier,
                    OpenSetResearchV6.Threshold cutoff,
                    String mode,int heldoutLevel,String calUnknown,String testUnknown,
                    BufferedWriter out)throws Exception{
        Result r=new Result();
        for(Exposure row:examples){
            var candidate=classifier.scores(select(row,mode));
            var decision=OpenSetResearchV6.apply(cutoff,candidate);
            if(isUnknown){
                if(decision.unknown)r.unknownRejected++;
                else r.unknownWronglyAccepted++;
            }else{
                if(decision.candidateLabel.equals(row.gas))r.knownCorrectWithoutGate++;
                if(!decision.unknown){
                    r.knownKept++;
                    if(decision.reportedLabel.equals(row.gas))r.knownCorrectAfterGate++;
                }
            }
            out.write(String.format(Locale.ROOT,
                "%s,%d,%s,%s,%s,%d,%s,%s,%s,%s,%.12f,%.12f,%b,%s%n",
                mode,heldoutLevel,calUnknown,testUnknown,
                isUnknown?"test_unknown":"test_known",row.id,row.gas,
                decision.candidateLabel,decision.reportedLabel,
                "SOURCE_ONLY",decision.maximumRelativeScore,
                decision.selectedSourceThreshold,decision.unknown,
                String.join("|",decision.allCandidateLabels)));
        }
        return r;
    }
    public static void main(String[] args)throws Exception {
        require(args.length==4,"expected verified source features, metrics, predictions, audit partitions");
        List<Exposure> original=load(Path.of(args[0]));
        try(BufferedWriter metrics=Files.newBufferedWriter(Path.of(args[1]),StandardCharsets.UTF_8);
            BufferedWriter predictions=Files.newBufferedWriter(Path.of(args[2]),StandardCharsets.UTF_8);
            BufferedWriter ledger=Files.newBufferedWriter(Path.of(args[3]),StandardCharsets.UTF_8)){
            metrics.write("mode,heldout_ppb,calibration_unknown_gas,heldout_test_unknown_gas,"+
              "train_known,calibration_known,calibration_unknown,test_known,test_unknown,"+
              "cutoff,cal_known_accept_rate,cal_unknown_reject_rate,cal_balanced_objective,"+
              "test_known_raw_correct,test_known_retained,test_known_correct_accepted,"+
              "test_unknown_rejected,test_unknown_false_known,"+
              "test_known_accuracy_before_gate,test_known_correct_coverage,"+
              "test_known_retention,test_unknown_rejection_rate,"+
              "test_unknown_false_known_accept_rate,open_set_correct_fraction\n");
            predictions.write("mode,heldout_ppb,calibration_unknown_gas,heldout_test_unknown_gas,"+
              "partition,source_row_id,actual_gas,pre_gate_candidate,reported_label,"+
              "model_scope,max_score,source_only_cutoff,abstained,all_source_candidate_labels\n");
            ledger.write("mode,heldout_ppb,calibration_unknown_gas,heldout_test_unknown_gas,"+
              "partition,source_row_id,gas,original_ppb\n");
            for(String mode:MODES)for(int heldout:LEVELS)
                for(int testGas=0;testGas<GASES.length;testGas++){
                    String unknownTest=GASES[testGas];
                    String unknownCal=GASES[(testGas+1)%GASES.length];
                    List<Exposure> train=new ArrayList<>(),calKnown=new ArrayList<>(),
                        calUnknown=new ArrayList<>(),testKnown=new ArrayList<>(),
                        testUnknown=new ArrayList<>();
                    for(Exposure e:original){
                        if(e.gas.equals(unknownTest)){testUnknown.add(e);continue;}
                        if(e.gas.equals(unknownCal)){calUnknown.add(e);continue;}
                        if(e.ppb!=heldout){train.add(e);continue;}
                        if(replicateIndex(original,e)<2)calKnown.add(e);
                        else testKnown.add(e);
                    }
                    require(train.size()==40&&calKnown.size()==8&&calUnknown.size()==15&&
                            testKnown.size()==12&&testUnknown.size()==15,
                            "train/calibration/unknown partitions must use original 90 source rows");
                    Set<Integer> ids=new HashSet<>();
                    for(List<Exposure> partition:Arrays.asList(
                        train,calKnown,calUnknown,testKnown,testUnknown))
                        for(Exposure e:partition)
                            require(ids.add(e.id),"source exposure leakage between partitions");
                    require(ids.size()==90,"all 90 original exposures accounted for");
                    double[][] fitX=new double[train.size()][];
                    String[] fitY=new String[train.size()];
                    for(int i=0;i<train.size();i++){
                        fitX[i]=select(train.get(i),mode);
                        fitY[i]=train.get(i).gas;
                    }
                    var classifier=ClassicalLearningV6.fitSoftmax(
                        new ClassicalLearningV6.Dataset(fitX,fitY),800,.15,.005);
                    require(classifier.classes.size()==4&&
                            classifier.sourceTrainingRows==40,
                            "four genuinely trained known gas classes");
                    double[] knownScores=new double[calKnown.size()];
                    double[] unknownScores=new double[calUnknown.size()];
                    for(int i=0;i<knownScores.length;i++)
                        knownScores[i]=classifier.scores(select(calKnown.get(i),mode)).get(0).score;
                    for(int i=0;i<unknownScores.length;i++)
                        unknownScores[i]=classifier.scores(select(calUnknown.get(i),mode)).get(0).score;
                    var threshold=OpenSetResearchV6.fitCalibrationThreshold(
                        knownScores,unknownScores);
                    writeRecordIds(ledger,mode,heldout,unknownCal,unknownTest,"train_known",train);
                    writeRecordIds(ledger,mode,heldout,unknownCal,unknownTest,"calibration_known",calKnown);
                    writeRecordIds(ledger,mode,heldout,unknownCal,unknownTest,"calibration_unknown",calUnknown);
                    writeRecordIds(ledger,mode,heldout,unknownCal,unknownTest,"test_known",testKnown);
                    writeRecordIds(ledger,mode,heldout,unknownCal,unknownTest,"test_unknown",testUnknown);
                    Result known=assess(testKnown,false,classifier,threshold,mode,heldout,
                        unknownCal,unknownTest,predictions);
                    Result unknown=assess(testUnknown,true,classifier,threshold,mode,heldout,
                        unknownCal,unknownTest,predictions);
                    require(known.knownKept<=12&&unknown.unknownRejected+
                      unknown.unknownWronglyAccepted==15,"sealed test partitions fully evaluated");
                    String stat=String.format(Locale.ROOT,
                        "%s,%d,%s,%s,40,8,15,12,15,%.12f,%.8f,%.8f,%.8f,"+
                        "%d,%d,%d,%d,%d,%.8f,%.8f,%.8f,%.8f,%.8f,%.8f%n",
                        mode,heldout,unknownCal,unknownTest,
                        threshold.minimumScore,threshold.calibrationKnownRetained,
                        threshold.calibrationUnknownRejected,
                        threshold.calibrationBalancedAcceptance,
                        known.knownCorrectWithoutGate,known.knownKept,
                        known.knownCorrectAfterGate,unknown.unknownRejected,
                        unknown.unknownWronglyAccepted,
                        known.knownCorrectWithoutGate/12.0,
                        known.knownCorrectAfterGate/12.0,
                        known.knownKept/12.0,
                        unknown.unknownRejected/15.0,
                        unknown.unknownWronglyAccepted/15.0,
                        (known.knownCorrectAfterGate+unknown.unknownRejected)/27.0);
                    metrics.write(stat);
                    System.out.print("N05H_SEALED_SOURCE "+stat);
                }
        }
        System.out.println("PASS N05-H original UCI1081 36 independent gas-calibration/test-negative folds, "+
            "972 sealed test decisions and all hypotheses preserved; Tank G3 still 0/80.");
    }
}
