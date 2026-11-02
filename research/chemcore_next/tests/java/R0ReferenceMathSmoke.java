package com.hazrob.tank.chemcore.next;

public final class R0ReferenceMathSmoke {
    private static int count=0;
    private static void check(boolean ok,String message) {
        count++;
        if(!ok)throw new AssertionError("P02.2 "+count+": "+message);
    }
    private static void reject(Runnable test,String reason){
        boolean invalid=false;
        try{test.run();}catch(IllegalArgumentException expected){invalid=true;}
        check(invalid,reason);
    }
    public static void main(String[] args){
        R0ReferenceMath.Reference air=new R0ReferenceMath.Reference(
            "tank-uid-mq2","MQ2","HEATER_STEADY",
            R0ReferenceMath.FIRMWARE_DEVICE_AIR,98.3);
        R0ReferenceMath.SourceK source=new R0ReferenceMath.SourceK(
            "MQSensorsLib@pinned-research","MQ2","HEATER_STEADY",9.83,false);
        R0ReferenceMath.Comparison c=R0ReferenceMath.compare(air,49.15,source);
        check(air.kind==R0ReferenceMath.ReferenceKind.DEVICE_CLEAN_AIR,
            "actual V6 clean-air firmware definition identified");
        check(Math.abs(c.measuredAirRsKohm-98.3)<1e-12,
            "R_air is measured 98.3k not factory R0");
        check(Math.abs(c.sourceR0Kohm-10.0)<1e-12,
            "R0_source=R_air/K=10 kOhm only as research normalization");
        check(Math.abs(c.rsOverAir-0.5)<1e-12,"Rs/R_air physically observed ratio");
        check(Math.abs(c.rsOverSourceR0-4.915)<1e-12,"Rs/R0_source source-curve ratio");
        check(c.status==R0ReferenceMath.Status.SOURCE_K_RESEARCH_ONLY,
            "unqualified factory K is research only");
        check(!c.sourceCurveUseQualified,"no G3 from mere datasheet K");
        check(c.sourceRoot.equals("MQSensorsLib@pinned-research"),
            "source lineage explicit");
        check(!air.rAirKohm.equals(c.sourceR0Kohm),
            "air and source references not silently overwritten");

        R0ReferenceMath.Comparison airOnly=R0ReferenceMath.compare(air,49.15,null);
        check(airOnly.status==R0ReferenceMath.Status.SOURCE_K_NOT_SUPPLIED,
            "air ratio remains available without any manufacturer K");
        check(airOnly.sourceR0Kohm==null&&airOnly.rsOverSourceR0==null,
            "no source R0 invented");
        check(Math.abs(airOnly.rsOverAir-0.5)<1e-12,
            "air quality gate can operate separately");

        R0ReferenceMath.SourceK mismatch=new R0ReferenceMath.SourceK(
            "MQ4-manufacturer-source","MQ4","HEATER_STEADY",4.4,false);
        R0ReferenceMath.Comparison m=R0ReferenceMath.compare(air,49.15,mismatch);
        check(m.status==R0ReferenceMath.Status.SOURCE_MODEL_MISMATCH,
            "source K from wrong MQ model is forbidden");
        check(m.rsOverSourceR0==null,"wrong model cannot create curve ratio");
        check(m.rsOverAir!=null,"model mismatch does not erase actual air response");
        R0ReferenceMath.SourceK heater=new R0ReferenceMath.SourceK(
            "MQ2-other-heater","MQ2","HEATER_CYCLE_HIGH",9.83,false);
        R0ReferenceMath.Comparison h=R0ReferenceMath.compare(air,49.15,heater);
        check(h.status==R0ReferenceMath.Status.SOURCE_HEATER_MISMATCH,
            "heater-phase mismatched data cannot produce source R0");
        check(!h.sourceCurveUseQualified,"wrong heater blocks manufacturer curve");

        R0ReferenceMath.Reference legacy=new R0ReferenceMath.Reference(
            "tank-uid-mq2","MQ2","HEATER_STEADY",
            R0ReferenceMath.FIRMWARE_LEGACY_SCALED,10.0);
        R0ReferenceMath.Comparison lc=R0ReferenceMath.compare(legacy,49.15,source);
        check(lc.status==R0ReferenceMath.Status.LEGACY_ONLY_NO_AIR_REFERENCE,
            "historical V5 source-scaled R0 is not ambient Rs");
        check(lc.measuredAirRsKohm==null,"legacy source R0 cannot be treated as R_air");
        check(lc.rsOverAir==null,"legacy record has no known clean-air ratio");
        check(!lc.sourceCurveUseQualified,"legacy ratio not G3 qualified");
        check(Math.abs(lc.rsOverSourceR0-4.915)<1e-12,"legacy ratio retained for audit, not accepted");

        R0ReferenceMath.Reference noDefinition=new R0ReferenceMath.Reference(
            "tank-uid-mq2","MQ2","HEATER_STEADY","UNRECOGNIZED_R0",10.0);
        check(R0ReferenceMath.compare(noDefinition,49.15,source).status==
            R0ReferenceMath.Status.INVALID_REFERENCE,
            "unrecognized firmware R0 definition rejected");
        check(R0ReferenceMath.compare(air,null,source).rsOverAir==null,
            "missing Rs cannot create noise or gas evidence");
        check(R0ReferenceMath.compare(air,Double.NaN,source).status==
            R0ReferenceMath.Status.INVALID_REFERENCE,"NaN Rs rejected");
        check(R0ReferenceMath.compare(air,-1.0,source).status==
            R0ReferenceMath.Status.INVALID_REFERENCE,"negative Rs rejected");
        check(R0ReferenceMath.compare(new R0ReferenceMath.Reference(
            "x","MQ2","HEATER_STEADY",R0ReferenceMath.FIRMWARE_DEVICE_AIR,null),
            49.15,source).status==R0ReferenceMath.Status.INVALID_REFERENCE,
            "missing clean-air reference not concealed");

        R0ReferenceMath.SourceK simulatedVerified=new R0ReferenceMath.SourceK(
            "internal-lab-simulated-for-G1","MQ2","HEATER_STEADY",9.83,true);
        R0ReferenceMath.Comparison qualifiedOnlyIfRealLab=R0ReferenceMath.compare(
            air,49.15,simulatedVerified);
        check(qualifiedOnlyIfRealLab.sourceCurveUseQualified,
            "model eligibility requires external lab-verified metadata");
        check(qualifiedOnlyIfRealLab.status==R0ReferenceMath.Status.SOURCE_MATCH_QUALIFIED,
            "lab-verified metadata can mark source-model compatibility, not G3");
        reject(()->new R0ReferenceMath.SourceK("x","MQ2","HEATER_STEADY",0.0,false),
            "zero K rejected");
        reject(()->new R0ReferenceMath.SourceK("x","MQ2","HEATER_STEADY",
            Double.POSITIVE_INFINITY,false),"inf K rejected");
        reject(()->new R0ReferenceMath.SourceK("x","MQ2","HEATER_STEADY",
            Double.NaN,false),"NaN K rejected");
        reject(()->new R0ReferenceMath.Reference("","MQ2","HEATER_STEADY",
            R0ReferenceMath.FIRMWARE_DEVICE_AIR,10.0),"missing sensor UID rejected");

        System.out.println("CHEMCORE_NEXT_P02_DUAL_R0_PASS assertions="+count+
            " source_kgain_not_device_r0=true"+
            " lab_G3_claim=false");
    }
}
