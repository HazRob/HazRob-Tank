package com.hazrob.tank.chemcore.next;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipFile;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * JDK 17 scientific numeric oracle: compare new Next temporal extraction to
 * 90 ORIGINAL UCI exposures x10 original source channels x3 published features.
 * Original raw archive and verified 90x30 extraction have independent SHA pins.
 */
public final class NextTemporalOriginalUciOracle {
    private NextTemporalOriginalUciOracle() {}
    private static final String RAW_ZIP_SHA=
        "813ebb7d00ed9172114b68d27ab3213170d6c220b8b23907dce91e9838cd48ab";
    private static final String FEATURES_SHA=
        "c3f930b18b4d2ebfab8d691b1e2b5dcbe2ff5f94cd082a2e4fdd46d913a0d6da";
    private static int checks;
    private static void check(boolean yes,String what) {
        checks++;
        if(!yes)throw new AssertionError("Next source temporal oracle "+checks+": "+what);
    }
    private static String sha256(Path path)throws Exception{
        MessageDigest md=MessageDigest.getInstance("SHA-256");
        byte[] src=Files.readAllBytes(path);
        byte[] digest=md.digest(src);
        StringBuilder b=new StringBuilder();
        for(byte x:digest)b.append(String.format(Locale.ROOT,"%02x",x&0xff));
        return b.toString();
    }
    private static boolean close(double x,double y) {
        return Double.isFinite(x)&&Double.isFinite(y)&&
            Math.abs(x-y)<=Math.max(4e-13,Math.abs(y)*2e-12);
    }
    private static void expectReject(Runnable action){
        boolean refused=false;
        try{action.run();}catch(IllegalArgumentException expected){refused=true;}
        check(refused,"invalid source trace must be refused");
    }
    public static void main(String[] args)throws Exception {
        check(args.length==2,"args: original UCI zip + verified extracted features");
        Path archive=Path.of(args[0]),verified=Path.of(args[1]);
        check(sha256(archive).equals(RAW_ZIP_SHA),"original archive SHA");
        check(sha256(verified).equals(FEATURES_SHA),"separate extracted feature SHA");

        double[] source=new double[900];
        for(int i=0;i<source.length;i++){
            source[i]=i<300?100:(i<360?150:(i<600?100:(i<660?125:(i<840?100:200))));
        }
        TemporalSourceFingerprint.Features test=
            TemporalSourceFingerprint.extractOriginalUciWindowProtocol(source);
        check(close(test.baseline,100.0),"baseline preserved");
        check(close(test.early,0.5),"known early ratio");
        check(close(test.middle,0.25),"known middle ratio");
        check(close(test.endpoint,1.0),"known end ratio");
        check(!test.gasIdentityConfirmed&&!test.ppmCalibrationQualified,"no chemical label");
        expectReject(()->TemporalSourceFingerprint.extractOriginalUciWindowProtocol(new double[100]));
        expectReject(()->TemporalSourceFingerprint.extractOriginalUciWindowProtocol(new double[900]));
        double[] invalid=source.clone();invalid[301]=Double.NaN;
        expectReject(()->TemporalSourceFingerprint.extractOriginalUciWindowProtocol(invalid));

        int exposure=0,channels=0,properties=0;
        List<double[]> mq135Training=new ArrayList<>();
        try (ZipFile z=new ZipFile(archive.toFile());
             BufferedReader src=new BufferedReader(new InputStreamReader(
                 z.getInputStream(z.getEntry("gsalc.csv")),StandardCharsets.UTF_8));
             BufferedReader canonical=Files.newBufferedReader(verified,StandardCharsets.UTF_8)) {
            String head=canonical.readLine();
            check(head!=null&&head.startsWith("row_id,gas,ppb,f00,"),"verified UCI feature header");
            String rawline;
            while((rawline=src.readLine())!=null){
                String line=canonical.readLine();
                check(line!=null,"90 original rows matched source verified");
                String[] orig=rawline.split(",",-1);
                String[] expected=line.split(",",-1);
                check(orig.length==9002,"source 9000 raw samples plus gas and concentration");
                check(expected.length==33,"verified 30 source features");
                check(exposure==Integer.parseInt(expected[0]),"row IDs match original exposure");
                check(orig[0].trim().equalsIgnoreCase(expected[1]),"physical source gas label order");
                check(orig[1].trim().equalsIgnoreCase(expected[2]+"ppb"),"original ppb not ppm");
                double[] raw=new double[900];
                for(int channel=0;channel<10;channel++){
                    for(int p=0;p<900;p++){
                        raw[p]=Double.parseDouble(orig[2+900*channel+p]);
                    }
                    TemporalSourceFingerprint.Features f=
                        TemporalSourceFingerprint.extractOriginalUciWindowProtocol(raw);
                    double[] calc=f.exactSourceWindows();
                    for(int w=0;w<3;w++){
                        int featureIndex=channel+(w==0?0:w==1?10:20);
                        double reference=Double.parseDouble(expected[3+featureIndex]);
                        check(close(calc[w],reference),
                            "source coefficient row="+exposure+" channel="+channel+
                            " window="+w+" actual="+calc[w]+" expected="+reference);
                        properties++;
                    }
                    if(channel==4&&Integer.parseInt(expected[2])!=200
                            &&expected[1].equals("acetone")) {
                        mq135Training.add(calc.clone());
                    }
                    channels++;
                }
                exposure++;
            }
            check(canonical.readLine()==null,"no fabricated extra feature exposures");
        }
        check(exposure==90,"90 original physical exposures");
        check(channels==900,"90x10 traces processed");
        check(properties==2700,"90x10x3 source windows numeric parity");
        check(mq135Training.size()==10,"10 acetone MQ135 source training samples");
        TemporalSourceFingerprint.Prototype p=
            new TemporalSourceFingerprint.Prototype(
                TemporalSourceFingerprint.SOURCE_ROOT,"MQ135","acetone",mq135Training.toArray(new double[0][]));
        check(p.sourceTrainingExposures==10,"training prototype size");
        check(p.sourceCompatible(TemporalSourceFingerprint.SOURCE_ROOT,"MQ135",3),"source compatible");
        check(Double.isFinite(p.researchDistance(p.median,TemporalSourceFingerprint.SOURCE_ROOT,"MQ135")),
            "robust within-source prototype numeric research only");
        check(Double.isNaN(p.researchDistance(p.median,"UNRELATED_SOURCE","MQ135")),
            "incompatible source root must abstain");
        check(Double.isNaN(p.researchDistance(p.median,TemporalSourceFingerprint.SOURCE_ROOT,"MQ8")),
            "different sensor cannot reuse MQ135 source prototype");
        check(!p.isApprovedForGasInference&&!p.hasPhysicalTankG3Evidence,
            "neither classification nor G3 qualified");
        System.out.println("CHEMCORE_NEXT_UCI_TEMPORAL_SOURCE_PARITY_PASS"+
            " original_exposures="+exposure+" sensor_traces="+channels+
            " temporal_feature_numeric_parity="+properties+" checks="+checks+
            " source_R0_not_available=true Tank_G3=0");
    }
}
