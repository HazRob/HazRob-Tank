package com.servoc.app.ui;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Regression source contract; device screenshots and transport provisioning still required. */
public final class TankV4DeveloperConnectionContrastContractTest {
    private static String source()throws Exception{
        return new String(Files.readAllBytes(Paths.get(
                "src/main/java/com/servoc/app/ui/DeveloperConnectionDiagnosticsTab.java")),
                StandardCharsets.UTF_8);
    }

    @Test public void diagnosticsUsesReadableLightControls()throws Exception{
        String s=source();
        assertTrue(s.contains("root.setBackgroundColor(0xFFF7F8FA)"));
        assertTrue(s.contains("summary.setTextColor(0xFF111827)"));
        assertTrue(s.contains("status.setTextColor(0xFF111827)"));
        assertTrue(s.contains("remember.setTextColor(0xFF111827)"));
        assertTrue(s.contains("l.setTextColor(0xFF303846)"));
        assertFalse(s.contains("setBackgroundColor(0xFF0E1116)"));
        assertFalse(s.contains("remember.setTextColor(Color.WHITE)"));
    }

    @Test public void wanCredentialStoreReadsNoWidgetStateFromIoWorker()throws Exception{
        String s=source();
        assertTrue(s.contains("final boolean rememberWan=remember.isChecked()"));
        assertTrue(s.contains("if(rememberWan)store.save(s,p);else store.clear()"));
        assertFalse(s.contains("if(remember.isChecked())store.save(s,p)"));
        assertTrue(s.contains("host.robotApi().remoteProvision(s,p)"));
        assertTrue(s.contains("new RemoteWifiCredentialStore(context,host.preferences()).clear()"));
    }
}
