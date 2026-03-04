package com.sah;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Triggers root-level enforcement on system boot or power connection.
 */
public class boot extends BroadcastReceiver {
    private static final String TAG = "HGS_LOG";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null && (action.equals(Intent.ACTION_BOOT_COMPLETED) || 
            action.equals("android.intent.action.LOCKED_BOOT_COMPLETED"))) {
            
            new Thread(() -> {
                try {
                    // 5-second delay is smart for HyperOS to let SettingsProvider initialize
                    Thread.sleep(5000); 
                    
                    // FIXED: Changed ShellUtils to shell
                    shell.applyRootFix(); 
                    
                } catch (InterruptedException e) {
                    Log.e(TAG, "Boot execution interrupted");
                }
            }).start();
        }
    }
}