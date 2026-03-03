package com.hyperos.gesturefix;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
            "android.intent.action.LOCKED_BOOT_COMPLETED".equals(intent.getAction())) {
            
            // Run in a background thread to avoid blocking the system
            new Thread(ShellUtils::applyRootFix).start();
        }
    }
}