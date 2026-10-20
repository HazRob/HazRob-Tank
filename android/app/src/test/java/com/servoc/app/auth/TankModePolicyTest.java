package com.servoc.app.auth;
import org.junit.Test;
import static org.junit.Assert.*;
public final class TankModePolicyTest {
    @Test public void offlineIsExplicitAndNeverMappedToDirect(){
        assertTrue(TankModePolicy.isOffline("offline"));
        assertTrue(TankModePolicy.isOffline(" OFFLINE "));
        assertEquals("offline",TankModePolicy.transport("offline"));
        assertEquals("direct",TankModePolicy.transport("direct"));
        assertEquals("remote",TankModePolicy.transport("remote"));
        assertFalse(TankModePolicy.isOffline(null));
    }
}
