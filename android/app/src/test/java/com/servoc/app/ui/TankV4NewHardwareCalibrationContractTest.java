package com.servoc.app.ui;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** V4 source/architectural contracts. Actual ESP32 and phone measurements remain separate gates. */
public final class TankV4NewHardwareCalibrationContractTest {
    private static String src(String path)throws Exception{
        return new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/"+path)),StandardCharsets.UTF_8);
    }

    @Test public void mqButtonBelongsToLowerActionRowNotCameraOverlay()throws Exception{
        String control=src("ui/ControlTab.java");
        assertTrue(control.contains("actions.addView(relayButton,UiKit.weight("));
        assertFalse(control.contains("cameraBox.addView(relayButton"));
        assertTrue(control.contains("relayButton.setOnClickListener(v->toggleRelay())"));
        assertTrue(control.contains("host.robotApi().mqPower(target)"));
    }

    @Test public void everyNewReadingUsesLatestAcceptedR0()throws Exception{
        String main=src("MainActivity.java");
        String api=src("communication/RobotApi.java");
        String proto=src("communication/RobotProtocolV4.java");
        String calibration=src("calibration/ManualR0Calibration.java");
        String normalizer=src("calibration/AcceptedR0Normalizer.java");
        assertTrue(main.contains("ManualR0Calibration.load(prefs)"));
        assertTrue(api.contains("CanonicalMeasurementNormalizer.enrich"));
        assertTrue(api.contains("applyActiveR0(new JSONObject(s.toString()))"));
        assertTrue(api.contains("reconcileMq(measurement.optJSONObject(\"mq\"))"));
        assertTrue(proto.contains("ManualR0Calibration.apply(hw.key,x)"));
        assertTrue(calibration.contains("p.getString(\"manual_r0_\"+k"));
        assertTrue(calibration.contains("if(!e.commit())return false"));
        assertTrue(calibration.contains("q.put(\"ratio\",rs/r0)"));
        assertTrue(normalizer.contains("return Double.isFinite(rs)&&rs>0?rs/r0:Double.NaN"));
        assertTrue(normalizer.contains("recordedPhysicalRatio(recorded)"));
    }

    @Test public void gasAlertCannotReUseObsoleteHealthBaselineAfterR0Revision()throws Exception{
        String main=src("MainActivity.java");
        assertTrue(main.contains("private void syncOperationalR0()"));
        assertTrue(main.contains("activeR0Revisions.put(key,revision)"));
        assertTrue(main.contains("sensorHealth.setBaseline(key,Double.isFinite(r0)&&r0>0?1.0:Double.NaN)"));
        assertTrue(main.contains("latestDiag=snap;syncOperationalR0();sensorHealth.accept(snap,now);updateGasAlert(snap,now)"));
        assertTrue(main.contains("if(changed)gasAlertCoordinator.reset()"));
        assertFalse(main.contains("sensorHealth.setBaseline(key,q);"));
    }

    @Test public void sessionsSpinnerAdaptersDoNotRebindOnEveryRefresh()throws Exception{
        String field=src("ui/FieldSessionsTab.java");
        assertTrue(field.contains("if(!boundSiteIds.equals(siteOptionIds)||!boundSiteLabels.equals(siteNames))"));
        assertTrue(field.contains("if(!boundSessionIds.equals(sessionOptionIds)||!boundSessionLabels.equals(names))"));
        assertTrue(field.contains("if(refreshing)return;"));
        assertTrue(field.contains("showSessionLoadError();"));
    }
}
