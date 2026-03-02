package com.hyperos.gesturefix;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class HyperOSLauncherSpoofer implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {

        // 1. SELF-HOOK (The Green Test)
        // This confirms the module can talk to its own UI
        if (lpparam.packageName.equals("com.hyperos.gesturefix")) {
            XposedHelpers.findAndHookMethod(
                    "com.hyperos.gesturefix.MainActivity",
                    lpparam.classLoader,
                    "isModuleActive",
                    XC_MethodReplacement.returnConstant(true)
            );
        }

        // 2. HYPEROS GESTURE UNLOCK
        // We target both the framework and the launcher
        if (lpparam.packageName.equals("android") || lpparam.packageName.equals("com.miui.home")) {
            XposedBridge.log("HGS: Bypassing Gesture Block for " + lpparam.packageName);

            try {
                // Core HyperOS Check: Is a 3rd party launcher allowed to use gestures?
                // We force this to ALWAYS be true.
                Class<?> deviceConfig = XposedHelpers.findClassIfExists("com.miui.home.launcher.DeviceConfig", lpparam.classLoader);
                if (deviceConfig != null) {
                    XposedHelpers.findAndHookMethod(deviceConfig, "isThirdPartyLauncherSupported", XC_MethodReplacement.returnConstant(true));
                    XposedBridge.log("HGS: Successfully hooked DeviceConfig");
                }
            } catch (Throwable t) {
                XposedBridge.log("HGS Error: " + t.getMessage());
            }
        }
    }
}