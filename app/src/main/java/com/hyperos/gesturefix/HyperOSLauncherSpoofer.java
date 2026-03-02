package com.hyperos.gesturefix;

import de.robv.android.xposed.IXposedHookLoadPackage;
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

        // 2. SYSTEM & SETTINGS BYPASS
        // We now target Settings and SystemUI to stop the "Auto-Reset"
        if (lpparam.packageName.equals("android") || 
            lpparam.packageName.equals("com.android.settings") || 
            lpparam.packageName.equals("com.android.systemui")) {
            
            try {
                Class<?> navUtils = XposedHelpers.findClassIfExists("miui.util.MiuiNavUtils", lpparam.classLoader);
                if (navUtils != null) {
                    // Lie to the Settings app: "Yes, the current launcher is the system one."
                    XposedHelpers.findAndHookMethod(navUtils, "isDefaultSysLauncher", 
                        android.content.Context.class, XC_MethodReplacement.returnConstant(true));
                    
                    // Lie to the SystemUI: "Yes, gestures are supported here."
                    XposedHelpers.findAndHookMethod(navUtils, "isSupportFullscreenGesture", 
                        android.content.Context.class, XC_MethodReplacement.returnConstant(true));
                }
            } catch (Throwable ignored) {}
        }

        // 3. LAUNCHER-SIDE HOOK (HyperOS Launcher)
        if (lpparam.packageName.equals("com.miui.home")) {
            try {
                Class<?> deviceConfig = XposedHelpers.findClassIfExists("com.miui.home.launcher.DeviceConfig", lpparam.classLoader);
                if (deviceConfig != null) {
                    // Tell the launcher itself not to complain
                    XposedHelpers.findAndHookMethod(deviceConfig, "isThirdPartyLauncherSupported", 
                        XC_MethodReplacement.returnConstant(true));
                }
            } catch (Throwable ignored) {}
        }
    }
}