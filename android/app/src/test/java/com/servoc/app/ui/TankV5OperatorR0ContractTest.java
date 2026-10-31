package com.servoc.app.ui;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Source contract only. ESP32 NVS acceptance and device UI require bench tests. */
public final class TankV5OperatorR0ContractTest {
    private static String src(String f)throws Exception{
        return new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/"+f)),
                StandardCharsets.UTF_8);
    }
    @Test public void r0IsOperatorSettingsPageAndNotDeveloperNavigation()throws Exception{
        String s=src("ui/SettingsTab.java"),d=src("ui/DeveloperTab.java"),host=src("MainActivity.java");
        assertTrue(s.contains("openUserCalibration()"));
        assertTrue(s.contains("new DeveloperR0Tab(scroll.getContext(),host)"));
        assertTrue(s.contains("calibrationScreen.addView(userCalibration.view()"));
        assertFalse(d.contains("new DeveloperR0Tab(context,host)"));
        assertTrue(d.contains("views=new View[3]"));
        assertTrue(host.contains("if(settingsTab!=null)settingsTab.release()"));
    }
    @Test public void explicitApprovalDoesNotClaimToRollbackFirmwareNvs()throws Exception{
        String s=src("ui/DeveloperR0Tab.java");
        assertTrue(s.contains("calibrationStart(300,60)"));
        assertTrue(s.contains("autoApprove=UiKit.button(c,\"اعتماد R0 في التطبيق\")"));
        assertTrue(s.contains("autoRetry=UiKit.button(c,\"إعادة المعايرة\")"));
        assertTrue(s.contains("pendingFirmwareResult=new JSONObject(response.toString())"));
        assertTrue(s.contains("private void acceptFirmwareResult()"));
        assertTrue(s.contains("if(ChemCoreLibraryManager.measurementOrAnalysisActive(host.latestDiagnostics()))"));
        assertTrue(s.contains("host.robotApi().calibrationAccept()"));
        assertTrue(s.contains("FirmwareR0Contract.pendingValues(response)"));
        assertTrue(s.contains("ManualR0Calibration.saveFirmware(host.preferences(),actual,deviceId,actualProgress.counts)"));
        assertTrue(s.contains("المتبقي"));
        assertTrue(s.contains("إلغاء R0"));
        assertFalse(s.contains("ManualR0Calibration.saveFirmware(host.preferences(),values,deviceId,p.counts)"));
    }
}
