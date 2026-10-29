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
        return s.startsWith(FIRMWARE_PREFIX)&&s.length()>FIRMWARE_PREFIX.length()
                &&s.length()<=100 &&s.indexOf('\n')<0 &&s.indexOf('\r')<0;
    }

    public static Result verify(String banner,JSONObject diag){
        if(!looksLikeTank(banner))
            return reject("هوية 192.168.4.1 غير معروفة: الاستجابة لا تخص ESP32-Tank الرئيسي.");
        if(diag==null)
            return reject("الروبوت متصل لكن استجابة /diag غير متوفرة؛ لم يُسمح بالتحكم.");
        String shown=banner.trim(),reported=diag.optString("fw","").trim();
        if(!shown.equals(reported))
            return reject("تعارض هوية ESP32 بين / و /diag؛ لم يُسمح بالتحكم.");
        if(!diag.has("api")||diag.optInt("api",-1)!=SUPPORTED_API)
            return reject("بروتوكول ESP32 غير متوافق: المطلوب API"+SUPPORTED_API+
                    " والمستلم "+diag.optString("api","غير معروف")+".");
        String profile=diag.optString("hardware_profile","").trim();
        if(!HARDWARE_PROFILE.equals(profile))
            return reject("ملف عتاد ESP32 غير متوافق: "+profile);
        JSONObject drive=diag.optJSONObject("drive");
        JSONObject pins=drive==null?null:drive.optJSONObject("pins");
        if(drive==null||!DRIVE_DRIVER.equals(drive.optString("driver",""))
                ||pins==null||pins.optInt("left",-1)!=16||pins.optInt("right",-1)!=17)
            return reject("تكوين قيادة الجنازير غير مطابق لمتحكم Tank (سيرفو 16/17).");
        JSONObject connection=diag.optJSONObject("connection");
        if(connection==null||!"direct".equalsIgnoreCase(connection.optString("radio_profile","")))
            return reject("ESP32 ليس في وضع Direct؛ لا يمكن فتح التحكم المباشر.");
        String deviceId=diag.optString("device_id","").trim();
        if(!deviceId.matches("(?i)[0-9a-f]{2}(:[0-9a-f]{2}){5}"))
            return reject("معرّف ESP32 الرئيسي غير صالح في /diag.");
        return new Result(true,"تم قبول ESP32-Tank عبر عقد API4، الفيرموير "+shown,shown,deviceId);
    }
}
