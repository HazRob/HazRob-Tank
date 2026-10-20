package com.servoc.app.communication;

import org.junit.Test;
import static org.junit.Assert.*;

/** Pure hostless negative test: offline must fail before any HTTP/remote transport access. */
public final class TankOfflineNetworkGuardTest {
    @Test public void offlineBlocksRawGetPostAndDriveBeforeAnyNetwork()throws Exception{
        RobotApi api=new RobotApi("192.168.4.1");
        RobotApi.setTankOffline(true);
        try{
            assertTrue(RobotApi.tankOffline());
            try{api.getText("/drive?l=100&r=100",100);fail("Offline GET accepted");}
            catch(IllegalStateException expected){assertTrue(expected.getMessage().contains("offline"));}
            try{api.postForm("/remote/provision","ssid=invalid",100);fail("Offline POST accepted");}
            catch(IllegalStateException expected){assertTrue(expected.getMessage().contains("offline"));}
            try{api.drive(100,100);fail("Offline drive accepted");}
            catch(IllegalStateException expected){assertTrue(expected.getMessage().contains("offline"));}
        }finally{RobotApi.setTankOffline(false);}
    }
}
