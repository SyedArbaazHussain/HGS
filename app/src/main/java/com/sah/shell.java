package com.sah;

import java.io.DataOutputStream;

/**
 * Root execution utilities for modifying system settings and managing display state.
 */
public class shell {
    
    public static void applyRootFix() {
        try {
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.writeBytes("settings put secure navigation_mode 2\n");
            os.writeBytes("settings put global force_fsg_nav_bar 1\n");
            os.writeBytes("settings put secure miui_fsg_gesture_status 1\n");
            os.writeBytes("content call --uri content://settings/secure --method GET_secure --arg navigation_mode\n");
            os.writeBytes("wm size reset\n");
            os.writeBytes("cmd overlay enable com.android.internal.systemui.navbar.gestural\n");
            os.writeBytes("am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS\n");
            os.writeBytes("exit\n");
            os.flush();
            p.waitFor();
        } catch (Exception ignored) {}
    }

    public static void clearSettingsCache() {
        try {
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());

            os.writeBytes("rm -rf /data/system/users/0/settings_*.xml\n");
            os.writeBytes("pkill -9 com.android.systemui\n");

            os.writeBytes("exit\n");
            os.flush();
            p.waitFor();
            os.close(); 
            p.destroy();
        } catch (Exception ignored) {}
    }
}