package com.servoc.app.ui;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Role/navigation source contracts; Android interaction test remains outstanding. */
public final class TankV5UnifiedDeveloperLabContractTest {
    private static String read(String file)throws Exception {
        return new String(Files.readAllBytes(Paths.get(
            "src/main/java/com/servoc/app/"+file)),StandardCharsets.UTF_8);
    }
    @Test public void oneDeveloperLabKeepsExperimentReferenceAndAnalysisFunctions()throws Exception{
        String dev=read("ui/DeveloperTab.java");
        String lab=read("ui/AnalyticalLabTab.java");
        String experiments=read("ui/DeveloperExperimentsTab.java");
        assertTrue(dev.contains("RoleGate.DEVELOPER_PIN.equals"));
        assertTrue(dev.contains("views=new View[3]"));
        assertTrue(dev.contains("String[] names={\"المختبر\",\"الاتصال\",\"المكتبة\"}"));
        assertFalse(dev.contains("new DeveloperExperimentsTab(context,host)"));
        assertTrue(lab.contains("new DeveloperExperimentsTab(context,host)"));
        assertTrue(lab.contains("experimentsButton=UiKit.button(c,\"التجارب المرجعية\")"));
        assertTrue(lab.contains("sessionButton=UiKit.button(c,\"الجلسة المضبوطة\")"));
        assertTrue(lab.contains("dashboardButton=UiKit.button(c,\"النتائج والتشخيص\")"));
        assertTrue(experiments.contains("approveSelected()"));
        assertTrue(experiments.contains("ReferenceLineageGuard"));
        assertTrue(lab.contains("دون اشتراط M4"));
    }
    @Test public void operatorR0DoesNotReturnAsDeveloperPage()throws Exception{
        String dev=read("ui/DeveloperTab.java");
        String settings=read("ui/SettingsTab.java");
        assertFalse(dev.contains("new DeveloperR0Tab(context,host)"));
        assertTrue(settings.contains("new DeveloperR0Tab(scroll.getContext(),host)"));
    }
}
