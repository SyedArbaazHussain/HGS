package com.hyperos.gesturefix;

import android.content.ComponentName;
import android.util.Log;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class HyperOSLauncherSpoofer implements IXposedHookLoadPackage {

    private static final String TAG = "HGS_LOG";

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        
        // 1. SELF-HOOK (App Status)
        if (lpparam.packageName.equals("com.hyperos.gesturefix")) {
            XposedHelpers.findAndHookMethod("com.hyperos.gesturefix.MainActivity", lpparam.classLoader, 
                "isModuleActive", XC_MethodReplacement.returnConstant(true));
        }

        // 2. THE SYSTEM FRAMEWORK HOOKS (Package: android)
        if (lpparam.packageName.equals("android")) {
            
            // HOOK: Overlay Manager (Prevents system from disabling the Gestures we force via Root)
            try {
                Class<?> oms = XposedHelpers.findClassIfExists("com.android.server.om.OverlayManagerService", lpparam.classLoader);
                if (oms != null) {
                    XposedHelpers.findAndHookMethod(oms, "setEnabled", 
                        String.class, boolean.class, int.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            String packageName = (String) param.args[0];
                            boolean enable = (boolean) param.args[1];
                            // Stop any attempt to disable the AOSP Gestural Overlay
                            if (packageName.equals("com.android.internal.systemui.navbar.gestural") && !enable) {
                                Log.e(TAG, "BLOCKED attempt to disable Gestural Overlay!");
                                param.setResult(true); 
                            }
                        }
                    });
                }
            } catch (Throwable t) { Log.e(TAG, "OMS Hook Failed: " + t.getMessage()); }

            // HOOK: Activity Manager (Prevents force-reverting to MIUI Launcher)
            try {
                Class<?> ams = XposedHelpers.findClassIfExists("com.android.server.wm.ActivityTaskManagerService", lpparam.classLoader);
                if (ams != null) {
                    XposedHelpers.findAndHookMethod(ams, "updateDefaultHomeActivity", ComponentName.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            ComponentName cn = (ComponentName) param.args[0];
                            if (cn != null && cn.getPackageName().equals("com.miui.home")) {
                                Log.e(TAG, "BLOCKED HyperOS from stealing back the Home screen!");
                                param.setResult(null); 
                            }
                        }
                    });
                }
            } catch (Throwable t) { Log.e(TAG, "AMS Error: " + t.getMessage()); }
        }

        // 3. SETTINGS BYPASS (Bypasses the "Not possible" popup in Settings)
        if (lpparam.packageName.equals("com.xiaomi.misettings") || lpparam.packageName.equals("com.android.settings")) {
            try {
                Class<?> navUtils = XposedHelpers.findClassIfExists("miui.util.MiuiNavUtils", lpparam.classLoader);
                if (navUtils != null) {
                    XposedHelpers.findAndHookMethod(navUtils, "isDefaultSysLauncher", 
                        android.content.Context.class, XC_MethodReplacement.returnConstant(true));
                    XposedHelpers.findAndHookMethod(navUtils, "isSupportFullscreenGesture", 
                        android.content.Context.class, XC_MethodReplacement.returnConstant(true));
                }
            } catch (Throwable ignored) {}
        }

        // Inside handleLoadPackage, add a specific check for SystemUI
if (lpparam.packageName.equals("com.android.systemui")) {
    try {
        // Force SystemUI to believe that the AOSP gesture engine is the primary one
        Class<?> fsgUtils = XposedHelpers.findClassIfExists("com.android.systemui.MiuiGestureUtils", lpparam.classLoader);
        if (fsgUtils != null) {
            XposedHelpers.findAndHookMethod(fsgUtils, "isFsgMode", 
                android.content.Context.class, XC_MethodReplacement.returnConstant(true));
        }
        
        // This is the most important part: tell SystemUI gestures are ALWAYS supported
        Class<?> navModeController = XposedHelpers.findClassIfExists("com.android.systemui.navigationbar.NavigationModeController", lpparam.classLoader);
        if (navModeController != null) {
            XposedHelpers.findAndHookMethod(navModeController, "getNavigationMode", 
                XC_MethodReplacement.returnConstant(2)); // 2 = GESTURAL
        }
    } catch (Throwable t) { Log.e(TAG, "SystemUI Gesture Hook Failed: " + t.getMessage()); }
}
    }
}