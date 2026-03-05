package com.sah;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Optimized Boot Receiver for HyperOS Gesture Fix.
 * Triggers root enforcement after system initialization.
 * Refactored for SDK 35 process management.
 */
public class boot extends BroadcastReceiver {
    private static final String TAG = "HGS_BOOT";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        // Listen for standard and locked boot (Direct Boot mode support)
        if (action.equals(Intent.ACTION_BOOT_COMPLETED) || 
            action.equals(Intent.ACTION_LOCKED_BOOT_COMPLETED) ||
            action.equals(Intent.ACTION_REBOOT)) {
            
            Log.d(TAG, "Boot detected. Scheduling gesture engine enforcement...");

            // Use a background thread to prevent "Receiver not finished" ANRs
            new Thread(() -> {
                try {
                    // HyperOS stabilization delay: 
                    // Ensures SettingsProvider and OverlayManager are fully ready.
                    Thread.sleep(7000); 
                    
                    Log.i(TAG, "Applying Root Shell Fix...");
                    shell.applyRootFix(); 
                    
                } catch (InterruptedException e) {
                    Log.e(TAG, "Boot task interrupted: " + e.getMessage());
                }
            }).start();
        }
    }
}