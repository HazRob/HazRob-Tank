package com.servoc.app.data;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import com.servoc.app.calibration.ManualR0Calibration;
import java.util.Map;

/**
 * Durable copy of an APPROVED device-specific firmware R0/NVS readback.
 * The approved flag is operator/NVS acceptance of the OPERATIONAL baseline,
 * not a laboratory test or G3 ppm validation. Current V5 firmware quick
 * calibration is 300 s heat + 60 s measure; it is NOT the user's 3x3min
 * extended field procedure. Do not invent extra windows, raw Rs minima,
 * heater phases, or clean-air purity.
 */
public final class MqDeviceR0LedgerV14 {
    private MqDeviceR0LedgerV14(){}
    public static final String FIRMWARE_PROFILE="V5_FIRMWARE_300S_WARM_60S_MEASURE";
    public static int recordFirmwareApproved(ExperimentsDb store,
            String deviceId,Map<String,Double> r0,
            Map<String,Integer> counts){
        if(store==null||deviceId==null||deviceId.trim().isEmpty()||
           r0==null||counts==null||r0.size()!=8||counts.size()!=8)
            throw new IllegalArgumentException("Full device R0 provenance required");
        for(String key:ManualR0Calibration.keys()){
            Double v=r0.get(key);Integer n=counts.get(key);
            if(v==null||!Double.isFinite(v)||v<=0||n==null||n<10)
                throw new IllegalArgumentException("Invalid accepted firmware sample "+key);
            if(!"firmware_clean_air_r0".equals(ManualR0Calibration.getSource(key))||
               !deviceId.equals(ManualR0Calibration.getDeviceId(key))||
               !v.equals(ManualR0Calibration.get(key)))
                throw new IllegalArgumentException("Firmware NVS/phone R0 mismatch "+key);
        }
        SQLiteDatabase db=store.getWritableDatabase();
        int n=0;db.beginTransaction();
        try{
            for(String key:ManualR0Calibration.keys()){
                ContentValues c=new ContentValues();
                c.put("device_id",deviceId);c.put("sensor_key",key);
                c.put("calibration_revision",ManualR0Calibration.getSession(key));
                c.put("r0_kohm",r0.get(key));c.put("sample_count",counts.get(key));
                c.put("window_count",1);
                c.put("reference_air_context",
                    "OPERATOR_DECLARED_AIR_SOURCE_SCALED_R0_NOT_AIR_RS");
                // Legacy V5 firmware divides observed clean-air Rs by a
                // manufacturer graph factor. The stored R0 is NOT the same
                // air Rs mean used by the V6 unity-centered response gate.
                c.put("r0_definition","LEGACY_MQ_FACTOR_SCALED_R0_NOT_AIR_RS");
                c.put("heater_profile",FIRMWARE_PROFILE);
                c.put("provenance",
                    "ESP32_NVS_READBACK_AND_PHONE_ACCEPTED_NO_TANK_G3"+
                    ";LEGACY_MQ_FACTOR_NORMALIZATION_NOT_RS_AIR_MEAN;QUICK_PROTOCOL_NOT_EXTENDED_THREE_WINDOW_STANDARD");
                c.put("created_utc",ManualR0Calibration.getUtc(key));
                c.put("calibration_end_utc",ManualR0Calibration.getUtc(key));
                c.put("approved",1);
                long inserted=db.insertWithOnConflict("mq_device_clean_air_baselines",
                    null,c,SQLiteDatabase.CONFLICT_IGNORE);
                if(inserted!=-1)n++;
            }
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
        return n;
    }
}
