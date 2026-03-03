package com.hyperos.gesturefix;

import java.io.DataOutputStream;

public class ShellUtils {
    public static void applyRootFix() {
        try {
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());

            // Gesture Configuration
            os.writeBytes("settings put secure navigation_mode 2\n");
            os.writeBytes("settings put global force_fsg_nav_bar 1\n");
            os.writeBytes("settings put secure sw_fs_gesture_fixed_mode 1\n");
            os.writeBytes("settings put secure sw_fs_gesture_navigation_mode 1\n");
            os.writeBytes("settings put secure miui_fsg_gesture_status 1\n");

            // Overlays
            os.writeBytes("cmd overlay disable com.android.internal.systemui.navbar.gestural\n");
            os.writeBytes("cmd overlay enable com.android.internal.systemui.navbar.gestural\n");
            os.writeBytes("cmd overlay disable com.android.internal.systemui.navbar.threebutton\n");

            // UI Refresh
            os.writeBytes("pkill -f com.android.systemui\n");
            os.writeBytes("pkill -f com.miui.home\n"); 
            
            os.writeBytes("exit\n");
            os.flush();
            p.waitFor();
        } catch (Exception ignored) {}
    }
}