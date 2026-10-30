package com.hazrob.tank.chemcore.v6;
import java.util.ArrayList;
import java.util.List;

/** Deterministic synthetic-data only: not a physical Tank G3 calibration. */
public final class CleanAirReferenceStatisticsV14Test {
    private static int assertions;
    private static void check(boolean b,String why){
        assertions++;if(!b)throw new AssertionError("V14 clean air "+why);
    }
    public static void main(String[] args){
        List<double[]> windows=new ArrayList<>();
        double r0=20.0;
        for(int window=0;window<3;window++){
            double[] rs=new double[24];
            for(int i=0;i<rs.length;i++)
                rs[i]=r0*(1.0+(window-1)*0.002+(i%12-6)*0.001);
            windows.add(rs);
        }
        CleanAirReferenceStatisticsV14.Fit one=
            CleanAirReferenceStatisticsV14.fit(r0,windows,1);
        check(one.numericalBandAvailable,"all three windows derive bound");
        check(one.totalSamples==72,"all raw samples counted");
        check(one.windows==3,"three windows retained");
        check(one.inside(r0),"R0 equals mean of same air within band");
        check(!one.inside(14.0),"large Rs drop outside measured blank");
        check(one.status.equals("WITHIN_RUN_ONLY_NOT_MULTI_DAY_NORMALITY"),
              "three windows on one day not sufficient for stable natural reference");
        check(!one.validatedNormalAir,"no virtual clean-air purity");
        CleanAirReferenceStatisticsV14.Fit multi=
            CleanAirReferenceStatisticsV14.fit(r0,windows,3);
        check(multi.numericalBandAvailable,"multi-day research band math");
        check(!multi.validatedNormalAir,"multi-day blanks never validate atmosphere");
        check(multi.status.equals("RESEARCH_ENVELOPE_REQUIRES_T_RH_HEATER_AND_HOLDOUT"),
              "requires temperature humidity heater and independent holdout");
        List<double[]> missing=new ArrayList<>(windows);
        missing.set(1,new double[]{19.9,20.1});
        check(!CleanAirReferenceStatisticsV14.fit(r0,missing,3).numericalBandAvailable,
              "reject short second observation window");
        check(!CleanAirReferenceStatisticsV14.fit(Double.NaN,windows,3).numericalBandAvailable,
              "reject missing manufacturer or device R0");
        check(!CleanAirReferenceStatisticsV14.fit(r0,windows,0).numericalBandAvailable,
              "reject unknown collection period");
        List<double[]> constant=new ArrayList<>();
        for(int j=0;j<3;j++){double[] r=new double[12];java.util.Arrays.fill(r,r0);constant.add(r);}
        check(!CleanAirReferenceStatisticsV14.fit(r0,constant,3).numericalBandAvailable,
              "zero-variation blank cannot create unjustified zero-width reference");
        System.out.println("PASS CleanAirReferenceStatisticsV14Test assertions="+assertions+
            " source=synthetic deviceG3=false");
    }
}
