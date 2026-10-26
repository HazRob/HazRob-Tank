package com.servoc.app.calibration;

import org.json.JSONObject;
import org.junit.Test;
import java.util.Map;
import static org.junit.Assert.*;

public final class FirmwareR0ContractTest {
    private JSONObject sample(String state,boolean committed,int count)throws Exception{
        JSONObject x=new JSONObject();
        x.put("schema",FirmwareR0Contract.SCHEMA);
        x.put("device_id","AA:BB:CC:DD:EE:FF");
        x.put("state",state);x.put("committed",committed);x.put("error","");
        x.put("elapsed_ms",1000);x.put("warmup_ms",300000);x.put("duration_ms",60000);
        JSONObject channels=new JSONObject();
        for(String key:ManualR0Calibration.keys()){
            JSONObject q=new JSONObject();q.put("samples",count);q.put("r0",42.0);
            channels.put(key,q);
        }
        x.put("channels",channels);
        return x;
    }

    @Test public void readyOnlyAfterFirmwareCommitAndEightValidChannels()throws Exception{
        JSONObject response=sample("ready",true,60);
        FirmwareR0Contract.Progress p=FirmwareR0Contract.progress(response);
        assertTrue(p.committed);assertFalse(p.active());assertEquals(8,p.counts.size());
        Map<String,Double> values=FirmwareR0Contract.committedValues(response);
        assertEquals(8,values.size());assertEquals(42.0,values.get("mq135"),0.0);
    }

    @Test public void warmupAndSamplingNeverBecomeCommitted()throws Exception{
        for(String state:new String[]{"idle","warmup","sampling","cancelled","error"}){
            JSONObject x=sample(state,false,120);
            assertFalse(FirmwareR0Contract.progress(x).committed);
            try{FirmwareR0Contract.committedValues(x);fail(state);}
            catch(IllegalStateException expected){}
        }
    }

    @Test public void unknownSchemaAndForgedStateFailClosed()throws Exception{
        JSONObject x=sample("ready",true,40);
        x.put("schema","legacy");try{FirmwareR0Contract.progress(x);fail();}catch(IllegalArgumentException expected){}
        x.put("schema",FirmwareR0Contract.SCHEMA);
        x.put("state","sampling");try{FirmwareR0Contract.progress(x);fail();}catch(IllegalArgumentException expected){}
        x.put("state","ready");x.put("committed",false);
        try{FirmwareR0Contract.committedValues(x);fail();}catch(IllegalStateException expected){}
    }

    @Test public void insufficientOrBadSingleSensorRejectsWholeRevision()throws Exception{
        JSONObject x=sample("ready",true,10);
        assertEquals(8,FirmwareR0Contract.committedValues(x).size());
        x.getJSONObject("channels").getJSONObject("mq7").put("samples",9);
        try{FirmwareR0Contract.committedValues(x);fail();}catch(IllegalStateException expected){}
        x.getJSONObject("channels").getJSONObject("mq7").put("samples",11);
        x.getJSONObject("channels").getJSONObject("mq7").put("r0",0.0);
        try{FirmwareR0Contract.committedValues(x);fail();}catch(IllegalStateException expected){}
    }

    @Test public void missingAndContradictoryChannelsRejected()throws Exception{
        JSONObject x=sample("ready",true,20);
        x.getJSONObject("channels").remove("mq135");
        try{FirmwareR0Contract.progress(x);fail();}catch(IllegalArgumentException expected){}
    }
}
