package com.hyperos.gesturefix;

import android.content.ComponentName;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class HyperOSLauncherSpoofer implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        
        // 1. HEARTBEAT
        if (lpparam.packageName.equals("com.hyperos.gesturefix")) {
            XposedHelpers.findAndHookMethod("com.hyperos.gesturefix.MainActivity", lpparam.classLoader, "isModuleActive", XC_MethodReplacement.returnConstant(true));
        }

        // 2. THE SYSTEM LOCKDOWN (android framework)
        if (lpparam.packageName.equals("android")) {
            XposedBridge.log("HGS: Protecting Launcher from Reset...");

            try {
                // LOCKDOWN: Prevent the system from changing the default home app automatically
                Class<?> ams = XposedHelpers.findClass("com.android.server.am.ActivityManagerService", lpparam.classLoader);
                XposedHelpers.findAndHookMethod(ams, "updateDefaultHomeActivity", ComponentName.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        ComponentName cn = (ComponentName) param.args[0];
                        // If the system tries to force 'com.miui.home' back, we block it!
                        if (cn != null && cn.getPackageName().equals("com.miui.home")) {
                            XposedBridge.log("HGS: Blocked attempt to force MIUI Home reset.");
                            param.setResult(null); // Stop the method from executing
                        }
                    }
                });

                // NAV BYPASS: Keep the gesture utilities happy
                Class<?> navUtils = XposedHelpers.findClassIfExists("miui.util.MiuiNavUtils", lpparam.classLoader);
                if (navUtils != null) {
                    XposedHelpers.findAndHookMethod(navUtils, "isSupportFullscreenGesture", android.content.Context.class, XC_MethodReplacement.returnConstant(true));
                    XposedHelpers.findAndHookMethod(navUtils, "isDefaultSysLauncher", android.content.Context.class, XC_MethodReplacement.returnConstant(true));
                }
            } catch (Throwable t) {
                XposedBridge.log("HGS Framework Error: " + t.getMessage());
            }
        }

        // 3. SETTINGS & UI SPOOF
        if (lpparam.packageName.equals("com.android.settings") || 
            lpparam.packageName.equals("com.xiaomi.misettings") || 
            lpparam.packageName.equals("com.android.systemui")) {
            
            try {
                Class<?> navUtils = XposedHelpers.findClassIfExists("miui.util.MiuiNavUtils", lpparam.classLoader);
                if (navUtils != null) {
                    XposedHelpers.findAndHookMethod(navUtils, "isDefaultSysLauncher", android.content.Context.class, XC_MethodReplacement.returnConstant(true));
                }
            } catch (Throwable ignored) {}
        }
    }
}