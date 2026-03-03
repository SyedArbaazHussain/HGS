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
        
        // 1. SELF-HOOK
        if (lpparam.packageName.equals("com.hyperos.gesturefix")) {
            XposedHelpers.findAndHookMethod("com.hyperos.gesturefix.MainActivity", lpparam.classLoader, 
                "isModuleActive", XC_MethodReplacement.returnConstant(true));
        }

        // 2. THE SYSTEM & SYSTEMUI OVERRIDES
        if (lpparam.packageName.equals("android") || lpparam.packageName.equals("com.android.systemui")) {
            
            // Force the MiuiSettings to report Gestures are ON
            try {
                Class<?> miuiSettings = XposedHelpers.findClassIfExists("android.provider.MiuiSettings$System", lpparam.classLoader);
                if (miuiSettings != null) {
                    XposedHelpers.findAndHookMethod(miuiSettings, "getBoolean", 
                        android.content.ContentResolver.class, String.class, boolean.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if ("force_fsg_nav_bar".equals(param.args[1])) {
                                param.setResult(true);
                            }
                        }
                    });
                }
            } catch (Throwable ignored) {}

            // AMS Reset Blocker
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

        // 3. SETTINGS BYPASS
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
    }
}