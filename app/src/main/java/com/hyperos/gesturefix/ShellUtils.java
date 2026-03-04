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
            os.writeBytes("settings put secure hide_gesture_line 0\n");
            os.writeBytes("settings put system hide_gesture_line 0\n");
            os.writeBytes("settings put global hide_gesture_line 0\n");

            // 3. NUCLEAR OVERLAY REFRESH
            os.writeBytes("cmd overlay disable com.android.internal.systemui.navbar.gestural\n");
            os.writeBytes("cmd overlay enable com.android.internal.systemui.navbar.gestural\n");
            os.writeBytes("cmd overlay disable com.android.internal.systemui.navbar.threebutton\n");

            // 4. TOUCH INTERACTION & INTERACTION CACHE FIX
            os.writeBytes("settings put secure show_gesture_app_switch_hint 1\n");
            os.writeBytes("settings put secure full_screen_gesture_back_type 1\n");
            os.writeBytes("settings delete secure last_setup_shown\n");
            
            // Fix: Explicitly enable edge touch regions for back gestures
            os.writeBytes("settings put secure edge_handwriting_enabled 1\n");

            // 5. AGGRESSIVE RESTART SEQUENCE
            os.writeBytes("pkill -f com.android.systemui\n");
            os.writeBytes("pkill -f com.miui.home\n");
            os.writeBytes("pkill -f com.xiaomi.misettings\n");
            os.writeBytes("pkill -f com.android.settings\n");
            
            // Critical: Reset the hardware input processor to bind the new gesture touch areas
            os.writeBytes("pkill -f android.hardware.input.processor\n");

            // Wait 1.5 seconds for SystemUI to respawn
            os.writeBytes("sleep 1.5\n");

            // 6. HARDWARE COMPOSER (HWC) & LAYOUT RESET
            os.writeBytes("wm size reset\n");
            os.writeBytes("wm density reset\n");

            // 7. INPUT METHOD & OVERRIDE CLEANUP
            os.writeBytes("settings put secure icon_blacklist navbar\n");
            os.writeBytes("settings put secure show_ime_with_hard_keyboard 0\n");
            
            // Fix: Force re-evaluation of the navigation bar state
            os.writeBytes("am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS\n");

            os.writeBytes("exit\n");
            os.flush();
            p.waitFor();
        } catch (Exception ignored) {}
    }
}