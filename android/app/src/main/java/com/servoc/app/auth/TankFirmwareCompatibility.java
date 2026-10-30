package com.servoc.app.auth;

import org.json.JSONObject;

/**
 * Main ESP32 API4 compatibility gate. App version and firmware build version
 * are independent; the /diag contract (not Android versionName) decides whether
 * the main controller is compatible. The camera is an independent capability.
 *
 * This is protocol/device recognition, NOT cryptographic authentication: HTTP
 * on the open local AP offers no authenticated proof of physical device identity.
 */
public final class TankFirmwareCompatibility {
    private TankFirmwareCompatibility(){}

    public static final int SUPPORTED_API=4;
    public static final String FIRMWARE_PREFIX="ESP32-Tank ";
    public static final String HARDWARE_PROFILE="tank-doit-v1-2servo-8mq-echo-bme";
    public static final String DRIVE_DRIVER="dual_continuous_servo";

    public static final class Result {
        public final boolean ready;
        public final String detail;
        public final String firmware;
        public final String deviceId;
        private Result(boolean ok,String detail,String fw,String deviceId){
            this.ready=ok;this.detail=detail;this.firmware=fw;this.deviceId=deviceId;
        }
    }

    private static Result reject(String reason){return new Result(false,reason,"","");}

    /** Only recognizable main Tank firmware banners may proceed to /diag verification. */
    public static boolean looksLikeTank(String banner){
        if(banner==null)return false;
        String s=banner.trim();
        // Release number and suffix are display metadata, never admission tokens.
        return (s.equals("ESP32-Tank")||s.startsWith(FIRMWARE_PREFIX)||
                s.startsWith("ESP32-Tank/")||s.startsWith("ESP32-Tank-"))
                &&s.length()<=120 &&s.indexOf('\n')<0 &&s.indexOf('\r')<0;
    }

    /**
     * Direct compatibility is independent from versionName, firmware FW,
     * API version numbers, and build labels. The main ESP32 identity and
     * physical motor configuration remain independently inspected.
     * Firmware endpoint-level safety / command support is not bypassed.
     */
    public static Result verify(String banner,JSONObject diag){
        if(!looksLikeTank(banner))
            return reject("الاستجابة لا تخص متحكم ESP32-Tank الرئيسي.");
        if(diag==null)
            return reject("لم يستجب المتحكم الرئيسي لاستعلام التشخيص.");
        String profile=diag.optString("hardware_profile","");
        if(!profile.isEmpty()&&!HARDWARE_PROFILE.equals(profile))
            return reject("ملف عتاد ESP32 غير متوافق مع قيادة HazRob Tank.");
        JSONObject drive=diag.optJSONObject("drive");
        if(drive!=null){
            String driver=drive.optString("driver","");
            if(!driver.isEmpty()&&!DRIVE_DRIVER.equals(driver))
                return reject("نوع محرك الحركة غير مدعوم؛ لن تُفتح قيادة غير آمنة.");
            JSONObject pins=drive.optJSONObject("pins");
            if(pins!=null&&(pins.has("left")&&pins.optInt("left",-1)!=16||
                pins.has("right")&&pins.optInt("right",-1)!=17))
                return reject("أطراف السيرفو لا تطابق إعدادات قيادة Tank.");
        }
        String deviceId=diag.optString("device_id","").trim();
        String shown=banner.trim();
        return new Result(true,
            "اتصال ESP32-Tank متاح بصرف النظر عن إصدار التطبيق/الفيرموير/API؛ "+
            "الأوامر غير المدعومة وسلامة المحركات تخضع للفيرموير.",
            shown,deviceId);
    }
}
