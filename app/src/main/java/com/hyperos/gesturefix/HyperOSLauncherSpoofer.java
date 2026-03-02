package com.hyperos.gesturefix;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class HyperOSLauncherSpoofer implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {

        // 1. HEARTBEAT (The Green Status)
        if (lpparam.packageName.equals("com.hyperos.gesturefix")) {
            XposedHelpers.findAndHookMethod("com.hyperos.gesturefix.MainActivity", lpparam.classLoader, "isModuleActive", XC_MethodReplacement.returnConstant(true));
        }

        // 2. THE BYPASS (Targeting all potential "Gatekeepers")
        if (lpparam.packageName.equals("android") || 
            lpparam.packageName.equals("com.android.settings") || 
            lpparam.packageName.equals("com.xiaomi.misettings") || 
            lpparam.packageName.equals("com.android.systemui")) {
            
            XposedBridge.log("HGS: Hooking Navigation Gatekeeper in " + lpparam.packageName);

            try {
                // This utility class is the primary check across HyperOS apps
                Class<?> navUtils = XposedHelpers.findClassIfExists("miui.util.MiuiNavUtils", lpparam.classLoader);
                if (navUtils != null) {
                    // Force system to report gestures are supported for ANY launcher
                    XposedHelpers.findAndHookMethod(navUtils, "isSupportFullscreenGesture", 
                        android.content.Context.class, XC_MethodReplacement.returnConstant(true));
                    
                    // Force system to think the current launcher is the official one
                    XposedHelpers.findAndHookMethod(navUtils, "isDefaultSysLauncher", 
                        android.content.Context.class, XC_MethodReplacement.returnConstant(true));
                }
            } catch (Throwable t) {
                XposedBridge.log("HGS Error in " + lpparam.packageName + ": " + t.getMessage());
            }
        }

        // 3. MIUI HOME BYPASS (Prevent the launcher from resetting itself)
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