package com.servoc.app.calibration;

import org.json.JSONObject;
import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

public final class TankV5PendingR0ProtocolTest {
    private static JSONObject result(String state,boolean committed,boolean pending,int samples)throws Exception{
        JSONObject r=new JSONObject().put("schema",FirmwareR0Contract.SCHEMA)
            .put("state",state).put("committed",committed).put("pending",pending)
            .put("device_id","11:22:33:44:55:66").put("error","")
            .put("elapsed_ms",60000).put("warmup_ms",300000).put("duration_ms",60000);
        JSONObject channels=new JSONObject();
        for(String name:ManualR0Calibration.keys())
            channels.put(name,new JSONObject().put("r0",32.75).put("samples",samples));
        return r.put("channels",channels);
    }
    @Test public void pendingEightChannelsCanBeReviewedButNotImported()throws Exception{
        JSONObject p=result("pending",false,true,25);
        assertEquals(8,FirmwareR0Contract.pendingValues(p).size());
        try{FirmwareR0Contract.committedValues(p);fail("pending is not NVS");}
        catch(IllegalStateException expected){}
    }
    @Test public void incompleteStageAndContradictoryFlagsFailClosed()throws Exception{
        JSONObject bad=result("pending",false,true,9);
        try{FirmwareR0Contract.pendingValues(bad);fail("insufficient samples");}
        catch(IllegalStateException expected){}
        bad=result("pending",true,true,20);
        try{FirmwareR0Contract.progress(bad);fail("both committed and pending");}
        catch(IllegalArgumentException expected){}
        bad=result("pending",false,false,20);
        try{FirmwareR0Contract.progress(bad);fail("unacknowledged pending");}
        catch(IllegalArgumentException expected){}
    }
    @Test public void committedReadbackRequiresAllEightAndNoPending()throws Exception{
        JSONObject accepted=result("ready",true,false,40);
        assertEquals(8,FirmwareR0Contract.committedValues(accepted).size());
        try{FirmwareR0Contract.pendingValues(accepted);fail("not pending");}
        catch(IllegalStateException expected){}
    }
    @Test public void firmwareNeverWritesBeforeExplicitAccept()throws Exception{
        String source=Files.readString(Paths.get("../firmware/ESP32-Tank/Hardware.cpp"),StandardCharsets.UTF_8);
        String api=Files.readString(Paths.get("../firmware/ESP32-Tank/ESP32-Tank.ino"),StandardCharsets.UTF_8);
        int scan=source.indexOf("void calibrationUpdate(){");
        int commit=source.indexOf("bool calibrationAccept(){");
        assertTrue(scan>=0&&commit>scan);
        String update=source.substring(scan,source.indexOf("/* link health",scan));
        assertFalse(update.contains("saveR0Record(rec)"));
        assertTrue(update.contains("pendingR0=rec;pendingR0Valid=true;calState=CAL_PENDING"));
        assertTrue(source.contains("if(!saveR0Record(candidate))"));
        assertTrue(source.contains("pendingR0Valid=false;pendingR0=R0Record();calState=CAL_CANCEL;"));
        assertTrue(api.contains("if(cmd==\"accept\"){bool ok=calibrationAccept()"));
        assertTrue(source.contains("tank-r0-fw-v5/2"));
    }
}
