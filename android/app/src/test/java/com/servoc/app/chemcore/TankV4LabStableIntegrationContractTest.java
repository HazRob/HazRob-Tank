package com.servoc.app.chemcore;

import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

/** Laboratory/R0/Stable packaging contracts; build, install and device tests remain separate. */
public final class TankV4LabStableIntegrationContractTest {
    private static String read(String path)throws Exception{
        return new String(Files.readAllBytes(Paths.get(path)),StandardCharsets.UTF_8);
    }

    @Test public void referenceExperimentsFreezeEightAcceptedR0Readings()throws Exception{
        String s=read("src/main/java/com/servoc/app/ui/DeveloperExperimentsTab.java");
        assertTrue(s.contains("qualifiedReferenceChannels(source,d.optString("));
        assertTrue(s.contains("qualifiedArchivedCapture(r)"));
        assertTrue(s.contains("r0_channels_qualified"));
        assertTrue(s.contains("r0_procedure"));
        assertTrue(s.contains("firmware_clean_air_r0"));
        assertTrue(s.contains("r0_device_id"));
        assertTrue(s.contains("UiKit.outlinedSpinner(referenceKindSpinner)"));
        assertTrue(s.contains("UiKit.outlinedSpinner(modelSensorSpinner)"));
    }

    @Test public void labDifferentiatesCalibrationFromOperationalConditioning()throws Exception{
        String session=read("src/main/java/com/servoc/app/ui/ChemCoreAnalysisTab.java");
        assertTrue(session.contains("ChemCoreLabQuality.evaluate(d,host.driveLeft(),host.driveRight())"));
        assertTrue(session.contains("if(!gate.fingerprintNormalizationReady())"));
        assertTrue(session.contains("ليس معايرة R0"));
        String lab=read("src/main/java/com/servoc/app/ui/AnalyticalLabTab.java");
        assertTrue(lab.contains("مصدرها: "));
        assertTrue(lab.contains("q.referenceValidatedChannels"));
    }

    @Test public void installedStableMustBeVersionCompatibleAndContentVerified()throws Exception{
        String rel=read("src/main/java/com/servoc/app/ReleaseAudit.java");
        assertTrue(rel.contains("VERSION=\"5.0\""));
        String config=read("build.gradle");
        assertTrue(config.contains("assets.srcDir generatedHazrobAssets"));
        assertTrue(config.contains("prepareHr069EmbeddedStable"));
        String stableBuilder=read("../tools/build_hr069_embedded_stable.py");
        assertTrue(stableBuilder.contains("\"calibration_count\":0"));
        String prepareV5=read("../tools/prepare_v5_stable_source.py");
        assertTrue(prepareV5.contains("version=\"5.0\""));
        assertTrue(prepareV5.contains("min_app_version"));
        assertTrue(prepareV5.contains("max_app_version"));
        String host=read("src/main/java/com/servoc/app/MainActivity.java");
        assertTrue(host.contains("ChemCoreEmbeddedStableInstaller.ensureInstalled"));
        assertTrue(host.contains("tank_v4_stable_bootstrap_result"));
        assertTrue(host.contains("tank_v4_stable_bootstrap_active"));
        String library=read("src/main/java/com/servoc/app/ui/CentralLibraryDashboardTab.java");
        assertTrue(library.contains("tank_v4_stable_bootstrap_result"));
        assertTrue(library.contains("SHA-256"));
    }
}
