package com.sah;

import android.util.Log;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Optimized Root Utilities for HyperOS Gesture Fix.
 * Refactored for process stability on SDK 35 and modern libxposed integration.
 */
public class shell {

    private static final String TAG = "HGS_SHELL";

    /**
     * Executes core gesture engine fixes via root shell.
     * Uses a single 'su' session for efficiency to avoid multiple prompt overhead.
     */
    public static void applyRootFix() {
        String[] commands = {
            "settings put secure navigation_mode 2",
            "settings put global force_fsg_nav_bar 1",
            "settings put secure miui_fsg_gesture_status 1",
            "content call --uri content://settings/secure --method GET_secure --arg navigation_mode",
            "wm size reset",
            "cmd overlay enable com.android.internal.systemui.navbar.gestural",
            "am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS"
        };
        runAsRoot(commands);
    }

    /**
     * Clears settings cache and force-restarts SystemUI to apply changes.
     */
    public static void clearSettingsCache() {
        String[] commands = {
            "rm -rf /data/system/users/0/settings_*.xml",
            "pkill -9 com.android.systemui"
        };
        runAsRoot(commands);
    }

    /**
     * Internal helper to execute a batch of commands in a single root process.
     * Implements proper stream closing to prevent memory leaks in modern Android.
     */
    private static void runAsRoot(String[] commands) {
        Process p = null;
        DataOutputStream os = null;
        try {
            p = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(p.getOutputStream());

            for (String cmd : commands) {
                os.writeBytes(cmd + "\n");
            }
            
            os.writeBytes("exit\n");
            os.flush();
            p.waitFor();
            
        } catch (IOException | InterruptedException e) {
            Log.e(TAG, "Root execution failed: " + e.getMessage());
        } finally {
            try {
                if (os != null) os.close();
                if (p != null) p.destroy();
            } catch (IOException ignored) {}
        }
    }
}