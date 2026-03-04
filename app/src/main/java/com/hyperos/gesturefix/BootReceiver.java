package com.hyperos.gesturefix;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "HGS_LOG";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
            "android.intent.action.LOCKED_BOOT_COMPLETED".equals(action) ||
            Intent.ACTION_POWER_CONNECTED.equals(action)) {
            
            Log.d(TAG, "Boot/Power event detected: Enforcing Gesture Fix...");

            // Run in a background thread to avoid ANR (Application Not Responding)
            new Thread(() -> {
                try {
                    // Wait 5 seconds for SystemUI and Settings storage to stabilize
                    Thread.sleep(5000);
                    ShellUtils.applyRootFix();
                } catch (InterruptedException e) {
                    Log.e(TAG, "Boot execution interrupted");
                }
            }).start();
        }
    }
}