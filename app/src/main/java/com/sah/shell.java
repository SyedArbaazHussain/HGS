package com.sah;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class boot extends BroadcastReceiver {

    private static final String TAG = "HGS_BOOT";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        if (action.equals(Intent.ACTION_BOOT_COMPLETED) || 
            action.equals(Intent.ACTION_LOCKED_BOOT_COMPLETED) ||
            action.equals("android.intent.action.QUICKBOOT_POWERON")) {
            
            Log.d(TAG, "Boot detected: " + action);
            
            new Thread(() -> {
                try {
                    Thread.sleep(5000);
                    shell.applyRootFix();
                } catch (InterruptedException e) {
                    Log.e(TAG, "Boot fix interrupted", e);
                }
            }).start();
        }
    }
}