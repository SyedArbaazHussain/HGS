package com.hyperos.gesturefix;

import java.io.DataOutputStream;

public class ShellUtils {
    public static void applyRootFix() {
        try {
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());

            // 1. Core Gesture Configuration
            os.writeBytes("settings put secure navigation_mode 2\n");
            os.writeBytes("settings put global force_fsg_nav_bar 1\n");
            os.writeBytes("settings put secure miui_fsg_gesture_status 1\n");

            // 2. FORCED "SHOW PILL" COMMANDS
            // On HyperOS, 'hide_gesture_line' must be 0 for the pill to appear.
            // We set it in multiple namespaces to ensure it sticks.
            os.writeBytes("settings put secure hide_gesture_line 0\n");
            os.writeBytes("settings put system hide_gesture_line 0\n");
            os.writeBytes("settings put global hide_gesture_line 0\n");

            // 3. NUCLEAR OVERLAY REFRESH
            // Toggling the overlay forces SystemUI to re-inflate the navigation bar layout
            os.writeBytes("cmd overlay disable com.android.internal.systemui.navbar.gestural\n");
            os.writeBytes("cmd overlay enable com.android.internal.systemui.navbar.gestural\n");
            os.writeBytes("cmd overlay disable com.android.internal.systemui.navbar.threebutton\n");

            // 4. AGGRESSIVE RESTART SEQUENCE
            // We kill SystemUI, the Launcher, and the Xiaomi Cloud/Settings sync 
            // processes that might revert our changes.
            os.writeBytes("pkill -f com.android.systemui\n");
            os.writeBytes("pkill -f com.miui.home\n");
            os.writeBytes("pkill -f com.xiaomi.misettings\n");
            os.writeBytes("pkill -f com.android.settings\n");

            os.writeBytes("exit\n");
            os.flush();
            p.waitFor();
        } catch (Exception ignored) {}
    }
}