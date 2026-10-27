package com.servoc.app.ui;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Source-only developer navigation regression; handset click-flow remains necessary. */
public final class TankV4DeveloperLazyNavigationContractTest {
    private static String source()throws Exception{
        return new String(Files.readAllBytes(Paths.get(
                "src/main/java/com/servoc/app/ui/DeveloperTab.java")),StandardCharsets.UTF_8);
    }

    @Test public void fiveDestinationsStayPinProtectedAndLoadOnDemand()throws Exception{
        String s=source();
        assertTrue(s.contains("RoleGate.DEVELOPER_PIN.equals("));
        assertTrue(s.contains("views=new View[5]"));
        assertTrue(s.contains("String[] names={\"التجارب\",\"المختبر\",\"معايرة R0\",\"الاتصال\",\"المكتبة\"}"));
        assertTrue(s.contains("View screen=obtainView(selected)"));
        assertTrue(s.contains("case 0:"));
        assertTrue(s.contains("case 1:"));
        assertTrue(s.contains("case 2:"));
        assertTrue(s.contains("case 3:"));
        assertTrue(s.contains("case 4:"));
        assertTrue(s.contains("private View obtainView(int index)"));
        assertTrue(s.contains("if(views[index]!=null)return views[index]"));
    }

    @Test public void failedTabDoesNotBlockOthersOrSilentlyDisappear()throws Exception{
        String s=source();
        assertTrue(s.contains("catch(RuntimeException ex)"));
        assertTrue(s.contains("Cannot open developer tab"));
        assertTrue(s.contains("Do not cache a failure placeholder"));
        assertTrue(s.contains("TextView warning=UiKit.small"));
        assertTrue(s.contains("Cannot open developer tab "));
        assertTrue(s.contains("Developer refresh failure tab="));
        assertTrue(s.contains("if(r0!=null)r0.release()"));
    }
}
