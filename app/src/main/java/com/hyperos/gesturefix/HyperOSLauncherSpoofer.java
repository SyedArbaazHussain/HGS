package com.hyperos.gesturefix;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.provider.Settings;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class HyperOSLauncherSpoofer implements IXposedHookLoadPackage {

    private static final String TAG = "HGS_FIX: ";

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        
        // 1. SELF-HOOK (Works on all LSPosed versions to show "Active")
        if (lpparam.packageName.equals("com.hyperos.gesturefix")) {
            try {
                XposedHelpers.findAndHookMethod("com.hyperos.gesturefix.MainActivity", lpparam.classLoader, 
                    "isModuleActive", XC_MethodReplacement.returnConstant(true));
            } catch (Throwable ignored) {}
        }

        // 2. TARGET CORE SYSTEM APPS
        if (lpparam.packageName.equals("android") || 
            lpparam.packageName.equals("com.android.systemui") || 
            lpparam.packageName.equals("com.android.settings") || 
            lpparam.packageName.equals("com.xiaomi.misettings")) {

            XposedBridge.log(TAG + "Active in " + lpparam.packageName);

            // HOOK: Global Gesture Flag (The source of truth for the navbar)
            try {
                XposedHelpers.findAndHookMethod(Settings.Global.class, "getInt", 
                    ContentResolver.class, String.class, int.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        String key = (String) param.args[1];
                        if ("force_fsg_nav_bar".equals(key)) {
                            param.setResult(1); 
                        }
                    }
                });
            } catch (Throwable t) { XposedBridge.log(TAG + "Settings Hook Fail"); }

            // HOOK: MiuiNavUtils (The "Gatekeeper" utility class)
            try {
                Class<?> navUtils = XposedHelpers.findClassIfExists("miui.util.MiuiNavUtils", lpparam.classLoader);
                if (navUtils != null) {
                    hookMethodIfExists(navUtils, "isDefaultSysLauncher", true);
                    hookMethodIfExists(navUtils, "isSupportFullscreenGesture", true);
                }
            } catch (Throwable ignored) {}
        }

        // 3. HOOK: Activity Manager (Preventing the Launcher Auto-Reset)
        if (lpparam.packageName.equals("android")) {
            try {
                Class<?> ams = XposedHelpers.findClassIfExists("com.android.server.wm.ActivityTaskManagerService", lpparam.classLoader);
                if (ams != null) {
                    XposedHelpers.findAndHookMethod(ams, "updateDefaultHomeActivity", ComponentName.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            ComponentName cn = (ComponentName) param.args[0];
                            if (cn != null && cn.getPackageName().equals("com.miui.home")) {
                                XposedBridge.log(TAG + "Blocking MIUI Home force-revert");
                                param.setResult(null); 
                            }
                        }
                    });
                }
            } catch (Throwable t) { XposedBridge.log(TAG + "AMS Lockdown Fail"); }
        }
    }

    // Helper to prevent errors if methods change in future HyperOS updates
    private void hookMethodIfExists(Class<?> clazz, String methodName, Object result) {
        try {
            XposedHelpers.findAndHookMethod(clazz, methodName, android.content.Context.class, XC_MethodReplacement.returnConstant(result));
        } catch (Throwable t) {
            try {
                XposedHelpers.findAndHookMethod(clazz, methodName, XC_MethodReplacement.returnConstant(result));
            } catch (Throwable t2) {
                XposedBridge.log(TAG + "Could not hook " + methodName);
            }
        }
    }
}