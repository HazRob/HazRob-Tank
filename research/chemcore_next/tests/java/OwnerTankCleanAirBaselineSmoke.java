package com.hazrob.tank.chemcore.next;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Owner-supplied eight MQ device reference: real screenshot numbers, not a
 * measurement of later gas exposures. Test only R_air algebra and source
 * fail-closed behavior. Never infer clean-air noise, molecular identity or ppm.
 */
public final class OwnerTankCleanAirBaselineSmoke {
    private static int assertions;
    private static final String FIXTURE =
        "research/chemcore_next/device_calibration/tank_8mq_v6_quick_clean_air_2026-10-31T032955Z.json";
    private static final Pattern SENSOR_VALUE =
        Pattern.compile("^\\s*\"(MQ[0-9]+)\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)\\s*,?\\s*$");
    private static void ok(boolean truth,String why) {
        assertions++;
        if(!truth) throw new AssertionError("OWNER-RAIR "+assertions+": "+why);
    }
    private static void near(double measured,double expected,String why) {
        ok(Double.isFinite(measured)&&Math.abs(measured-expected)<1e-8,why);
    }
    public static void main(String[] args) throws Exception {
        String source=Files.readString(Path.of(FIXTURE),StandardCharsets.UTF_8);
        ok(source.contains("\"r0_definition\": \"DEVICE_CLEAN_AIR_RS_MEAN\""),
            "firmware protocol qualifies snapshot as air Rs mean");
        ok(source.contains("\"samples_per_sensor\": 437"),
            "owner screenshot sample count preserved");
        ok(source.contains("\"measurement_timestamp_utc\": \"2026-10-31T03:29:55Z\""),
            "screenshot time retained, not inferred from owner relative date");
        ok(source.contains("\"physical_tank_g3_qualified\": false"),
            "one clean-air calibration does not promote G3");
        ok(source.contains("\"overwrites_android_calibration\": false"),
            "research fixture must not alter device settings");
        Map<String,Double> observed=new LinkedHashMap<>();
        for(String line:source.split("\\R")) {
            Matcher m=SENSOR_VALUE.matcher(line);
            if(m.matches()) {
                Double old=observed.put(m.group(1),Double.parseDouble(m.group(2)));
                ok(old==null,"no duplicate sensor channel in screenshot fixture");
            }
        }
        Map<String,Double> expected=new LinkedHashMap<>();
        expected.put("MQ2",92.1417313);expected.put("MQ3",25.5138416);
        expected.put("MQ4",37.8870316);expected.put("MQ5",158.0950317);
        expected.put("MQ7",62.5898705);expected.put("MQ8",36.5759659);
        expected.put("MQ9",56.4506149);expected.put("MQ135",113.6592255);
        ok(observed.size()==8&&observed.keySet().equals(expected.keySet()),
            "all eight and only eight MQ sensor references captured");
        for(Map.Entry<String,Double> e:expected.entrySet()) {
            String model=e.getKey();
            double rAir=observed.get(model);
            near(rAir,e.getValue(),"screenshot numeric reference exact for "+model);
            R0ReferenceMath.Reference ref=new R0ReferenceMath.Reference(
                "owner-tank:slot:"+model,model,"HEATER_PHASE_UNVERIFIED",
                R0ReferenceMath.FIRMWARE_DEVICE_AIR,rAir);
            R0ReferenceMath.Comparison q1=R0ReferenceMath.compare(ref,rAir,null);
            near(q1.rsOverAir,1.0,"baseline/reference Q=1 for "+model);
            ok(q1.status==R0ReferenceMath.Status.SOURCE_K_NOT_SUPPLIED,
                "no manufacturer R0 transformation without sourced K "+model);
            ok(!q1.sourceCurveUseQualified,"no G3 qualified factory curve for "+model);
            // Deliberately synthetic arithmetic check, NOT a subsequent Tank Rs measurement.
            R0ReferenceMath.Comparison q08=R0ReferenceMath.compare(ref,rAir*.8,null);
            near(q08.rsOverAir,.8,"synthetic Q=.8 for "+model);
            ok(!q08.sourceCurveUseQualified,"synthetic exposure not source-qualified "+model);
        }
        System.out.println("CHEMCORE_NEXT_OWNER_TANK_8MQ_AIR_BASELINE_PASS assertions="
            +assertions+" device_reference_only=true g3=false identity=false ppm=false");
    }
}
