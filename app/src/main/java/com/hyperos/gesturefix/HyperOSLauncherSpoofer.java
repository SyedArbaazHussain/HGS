package com.hyperos.gesturefix;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class HyperOSLauncherSpoofer implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {

        // 1. UI HEARTBEAT (The Green Test)
        if (lpparam.packageName.equals("com.hyperos.gesturefix")) {
            XposedHelpers.findAndHookMethod(
                    "com.hyperos.gesturefix.MainActivity",
                    lpparam.classLoader,
                    "isModuleActive",
                    XC_MethodReplacement.returnConstant(true)
            );
        }

        // 2. THE SYSTEM BYPASS (The actual Fix)
        if (lpparam.packageName.equals("android")) {
            XposedBridge.log("HGS: Hooking Android Framework");

            // Hook the MIUI Nav Utility to allow full screen gestures
            try {
                Class<?> navUtils = XposedHelpers.findClassIfExists("miui.util.MiuiNavUtils", lpparam.classLoader);
                if (navUtils != null) {
                    XposedHelpers.findAndHookMethod(navUtils, "isSupportFullscreenGesture", 
                        android.content.Context.class, XC_MethodReplacement.returnConstant(true));
                    XposedHelpers.findAndHookMethod(navUtils, "isDefaultSysLauncher", 
                        android.content.Context.class, XC_MethodReplacement.returnConstant(true));
                }
            } catch (Throwable ignored) {}

            // Hook the internal check that forces a switch back to MIUI Home
            try {
                Class<?> activityTaskManager = XposedHelpers.findClassIfExists("com.android.server.wm.ActivityTaskManagerService", lpparam.classLoader);
                if (activityTaskManager != null) {
                    // Prevent the system from detecting that the current launcher is 'unsupported'
                    XposedHelpers.findAndHookMethod(activityTaskManager, "isCurrentHomeSupported", 
                        XC_MethodReplacement.returnConstant(true));
                }
            } catch (Throwable ignored) {}
        }

        // 3. LAUNCHER-SIDE HOOK
        if (lpparam.packageName.equals("com.miui.home")) {
            try {
                Class<?> deviceConfig = XposedHelpers.findClassIfExists("com.miui.home.launcher.DeviceConfig", lpparam.classLoader);
                if (deviceConfig != null) {
                    XposedHelpers.findAndHookMethod(deviceConfig, "isThirdPartyLauncherSupported", 
                        XC_MethodReplacement.returnConstant(true));
                }
            } catch (Throwable ignored) {}
        }
    }
}