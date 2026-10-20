package com.servoc.app.auth;

/** Tank transport-mode gate. Offline is not a fallback to Direct or Remote. */
public final class TankModePolicy {
    private TankModePolicy(){}
    public static boolean isOffline(String mode){
        return "offline".equalsIgnoreCase(mode==null?"":mode.trim());
    }
    public static String transport(String mode){
        if(isOffline(mode))return "offline";
        return "remote".equalsIgnoreCase(mode)?"remote":"direct";
    }
}
