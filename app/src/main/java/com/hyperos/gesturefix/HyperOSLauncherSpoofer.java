package com.hyperos.gesturefix;

import android.content.ContentResolver;
import android.provider.Settings;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class HyperOSLauncherSpoofer implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {

        // 1. UI HEARTBEAT
        if (lpparam.packageName.equals("com.hyperos.gesturefix")) {
            XposedHelpers.findAndHookMethod("com.hyperos.gesturefix.MainActivity", lpparam.classLoader, "isModuleActive", XC_MethodReplacement.returnConstant(true));
        }

        // 2. THE GLOBAL DATABASE HOOK (Works for Settings, SystemUI, and Framework)
        if (lpparam.packageName.equals("android") || 
            lpparam.packageName.equals("com.android.systemui") || 
            lpparam.packageName.equals("com.xiaomi.misettings")) {

            try {
                // Hook the Global Settings provider to force Gestures to "On"
                XposedHelpers.findAndHookMethod(Settings.Global.class, "getInt", 
                    ContentResolver.class, String.class, int.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String name = (String) param.args[1];
                        // "force_fsg_nav_bar" is the internal name for Full Screen Gestures
                        if ("force_fsg_nav_bar".equals(name)) {
                            param.setResult(1); 
                        }
                    }
                });

                // Force MiuiNavUtils to report 'System Launcher' status
                Class<?> navUtils = XposedHelpers.findClassIfExists("miui.util.MiuiNavUtils", lpparam.classLoader);
                if (navUtils != null) {
                    XposedHelpers.findAndHookMethod(navUtils, "isDefaultSysLauncher", 
                        android.content.Context.class, XC_MethodReplacement.returnConstant(true));
                    XposedHelpers.findAndHookMethod(navUtils, "isSupportFullscreenGesture", 
                        android.content.Context.class, XC_MethodReplacement.returnConstant(true));
                }
            } catch (Throwable t) {
                XposedBridge.log("HGS Master Hook Error: " + t.getMessage());
            }
        }

        // 3. PREVENT LAUNCHER AUTO-REVERT
        if (lpparam.packageName.equals("android")) {
            try {
                Class<?> activityTaskManager = XposedHelpers.findClassIfExists("com.android.server.wm.ActivityTaskManagerService", lpparam.classLoader);
                if (activityTaskManager != null) {
                    // Block the check that resets the launcher
                    XposedHelpers.findAndHookMethod(activityTaskManager, "isCurrentHomeSupported", 
                        XC_MethodReplacement.returnConstant(true));
                }
            } catch (Throwable ignored) {}
        }
    }
}