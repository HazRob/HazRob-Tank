package com.servoc.app.chemcore;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Screening warnings are instantaneous and conditional, never M4 compliance or certified exposure. */
public final class TankScreeningExposureWarningTest {
    private static JSONObject analysis(double ppm,long uid,String kind,double limit,String unit,double durationMin)throws Exception {
        JSONObject candidate=new JSONObject()
            .put("chemical_uid",uid).put("label","test gas")
            .put("rank",1).put("screening_fit_ppm",ppm)
            .put("evidence_tier","M2_LITERATURE_OR_MANUFACTURER_SUPPORTED");
        JSONObject reference=new JSONObject()
            .put("authority","TEST REFERENCE").put("limit_type",kind)
            .put("unit",unit).put("value",limit).put("duration_min",durationMin)
            .put("source_id","test-source");
        JSONObject entry=new JSONObject().put("chemical_uid",uid)
            .put("label","test gas").put("limits",new JSONArray().put(reference));
        JSONObject catalog=new JSONObject().put("candidates",new JSONArray().put(entry));
        return new JSONObject().put("candidates",new JSONArray().put(candidate))
            .put(TankExposureReferenceCatalog.KEY,catalog)
            .put("ppm_reportable",false).put("evidence_tier","M2_LITERATURE_OR_MANUFACTURER_SUPPORTED");
    }
    @Test public void twa8hAlertsAtTheInstantWithoutClaimingEightHourExposure()throws Exception {
        JSONObject out=TankScreeningExposureWarning.evaluate(analysis(150,99,"TWA",100,"ppm",480));
        assertEquals("CONDITIONAL_WARNING",out.getString("status"));
        assertFalse(out.getBoolean("time_weighted_average_evaluated"));
        assertFalse(out.getBoolean("m4_required_for_screening_warning"));
        JSONObject w=out.getJSONArray("warnings").getJSONObject(0);
        assertEquals("SUSTAINED_EXPOSURE_HYPOTHETICAL",w.getString("warning_kind"));
        assertEquals(480.,w.getDouble("reference_duration_min"),0.0001);
        assertEquals(1.5,w.getDouble("ratio_to_reference"),0.0001);
        assertFalse(w.getBoolean("regulatory_exceedance_proven"));
        assertTrue(TankScreeningExposureWarning.summary(
            new JSONObject().put(TankScreeningExposureWarning.KEY,out),2)
            .contains("لو استمر هذا التركيز"));
    }
    @Test public void lflImmediatelyWarnsAndConvertsVolPercentToPpm()throws Exception {
        JSONObject out=TankScreeningExposureWarning.evaluate(analysis(12000,99,"LFL",1.0,"vol%",0));
        JSONObject w=out.getJSONArray("warnings").getJSONObject(0);
        assertEquals(10000.,w.getDouble("reference_ppm"),0.01);
        assertEquals(120.,w.getDouble("percent_lfl"),0.01);
        assertEquals("POSSIBLE_LFL_REACHED",w.getString("warning_kind"));
    }
    @Test public void tenPercentLflIsOnlyPrecautionary()throws Exception {
        JSONObject out=TankScreeningExposureWarning.evaluate(analysis(2000,99,"LEL",1.0,"vol%",0));
        JSONObject w=out.getJSONArray("warnings").getJSONObject(0);
        assertEquals("PRECAUTIONARY_LFL_APPROACH",w.getString("warning_kind"));
        assertFalse(w.getBoolean("regulatory_exceedance_proven"));
    }
    @Test public void exposureNotCertifiedAndNoFalseSafeVerdictWhenBelowThreshold()throws Exception {
        JSONObject out=TankScreeningExposureWarning.evaluate(analysis(30,99,"TWA",100,"ppm",480));
        assertEquals(0,out.getJSONArray("warnings").length());
        assertEquals("NOT_MADE",out.getString("occupational_compliance_determination"));
        assertTrue(out.getBoolean("no_safety_clearance"));
    }
    @Test public void nonPpmMassLimitIsNotSilentlyCompared()throws Exception {
        JSONObject out=TankScreeningExposureWarning.evaluate(analysis(900,99,"TWA",500,"mg/m3",480));
        assertEquals("NO_COMPARABLE_REFERENCE",out.getString("status"));
        assertEquals(0,out.getJSONArray("warnings").length());
    }
    @Test public void absentNumericalFitNeverCreatesAWarning()throws Exception {
        JSONObject out=TankScreeningExposureWarning.evaluate(analysis(Double.NaN,99,"TWA",100,"ppm",480));
        assertEquals("NO_VALID_PPM_FIT",out.getString("status"));
    }
}
