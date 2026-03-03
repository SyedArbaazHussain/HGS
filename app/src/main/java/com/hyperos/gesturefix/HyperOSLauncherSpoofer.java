package com.hyperos.gesturefix;

import android.content.ComponentName;
import android.content.Context;
import android.util.Log;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class HyperOSLauncherSpoofer implements IXposedHookLoadPackage {

    private static final String TAG = "HGS_LOG";

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        
        // 1. SELF-HOOK: Update app UI status to "ACTIVE"
        if (lpparam.packageName.equals("com.hyperos.gesturefix")) {
            XposedHelpers.findAndHookMethod("com.hyperos.gesturefix.MainActivity", lpparam.classLoader, 
                "isModuleActive", XC_MethodReplacement.returnConstant(true));
        }

        // 2. SYSTEM FRAMEWORK HOOKS (android)
        if (lpparam.packageName.equals("android")) {
            
            // HOOK: Overlay Manager - Prevents System from disabling forced Gestures
            try {
                Class<?> oms = XposedHelpers.findClassIfExists("com.android.server.om.OverlayManagerService", lpparam.classLoader);
                if (oms != null) {
                    XposedHelpers.findAndHookMethod(oms, "setEnabled", 
                        String.class, boolean.class, int.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            String packageName = (String) param.args[0];
                            boolean enable = (boolean) param.args[1];
                            if (packageName.equals("com.android.internal.systemui.navbar.gestural") && !enable) {
                                Log.d(TAG, "BLOCKED attempt to disable Gestural Overlay!");
                                param.setResult(true); 
                            }
                        }
                    });
                }
            } catch (Throwable t) { Log.e(TAG, "OMS Hook Failed: " + t.getMessage()); }

            // HOOK: Activity Manager - Stop MIUI from stealing the Home Screen
            try {
                Class<?> ams = XposedHelpers.findClassIfExists("com.android.server.wm.ActivityTaskManagerService", lpparam.classLoader);
                if (ams != null) {
                    XposedHelpers.findAndHookMethod(ams, "updateDefaultHomeActivity", ComponentName.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            ComponentName cn = (ComponentName) param.args[0];
                            if (cn != null && cn.getPackageName().equals("com.miui.home")) {
                                Log.d(TAG, "Preventing auto-revert to MIUI Launcher.");
                                param.setResult(null); 
                            }
                        }
                    });
                }
            } catch (Throwable t) { Log.e(TAG, "AMS Hook Failed: " + t.getMessage()); }
            
            // NEW: Framework-level Xiaomi Gesture Support Bypass
            try {
                Class<?> miuiSettings = XposedHelpers.findClassIfExists("android.provider.MiuiSettings$System", lpparam.classLoader);
                if (miuiSettings != null) {
                    XposedHelpers.findAndHookMethod(miuiSettings, "isSupportFullscreenGesture", 
                        Context.class, XC_MethodReplacement.returnConstant(true));
                }
            } catch (Throwable t) { Log.e(TAG, "Framework Gesture Hook Failed: " + t.getMessage()); }
        }

        // 3. SETTINGS BYPASS (Xiaomi Settings app logic)
        if (lpparam.packageName.equals("com.xiaomi.misettings") || lpparam.packageName.equals("com.android.settings")) {
            try {
                Class<?> navUtils = XposedHelpers.findClassIfExists("miui.util.MiuiNavUtils", lpparam.classLoader);
                if (navUtils != null) {
                    XposedHelpers.findAndHookMethod(navUtils, "isDefaultSysLauncher", 
                        Context.class, XC_MethodReplacement.returnConstant(true));
                    
                    XposedHelpers.findAndHookMethod(navUtils, "isSupportFullscreenGesture", 
                        Context.class, XC_MethodReplacement.returnConstant(true));
                }
            } catch (Throwable ignored) {}
        }

        // 4. SYSTEM UI HOOKS (Touch engine and Nav Bar)
        if (lpparam.packageName.equals("com.android.systemui")) {
            try {
                // Hook MIUI Specific Gesture Utils (Core Xiaomi Logic)
                Class<?> miuiUtils = XposedHelpers.findClassIfExists("com.android.systemui.MiuiGestureUtils", lpparam.classLoader);
                if (miuiUtils != null) {
                    XposedHelpers.findAndHookMethod(miuiUtils, "isFsgMode", 
                        Context.class, XC_MethodReplacement.returnConstant(true));
                }

                // NEW: Hook Xiaomi internal NavigationUtils for SystemUI
                Class<?> miuiNavUtils = XposedHelpers.findClassIfExists("com.android.systemui.NavigationUtils", lpparam.classLoader);
                if (miuiNavUtils != null) {
                    XposedHelpers.findAndHookMethod(miuiNavUtils, "isFsgMode", 
                        Context.class, XC_MethodReplacement.returnConstant(true));
                }

                // NEW: Hook GestureStubView (This handles the back gesture touch area)
                Class<?> gestureStub = XposedHelpers.findClassIfExists("com.android.systemui.statusbar.phone.GestureStubView", lpparam.classLoader);
                if (gestureStub != null) {
                    XposedHelpers.findAndHookMethod(gestureStub, "isGestureEnable", 
                        Context.class, XC_MethodReplacement.returnConstant(true));
                }
                
                // Force Navigation Mode to GESTURAL (2)
                Class<?> navModeController = XposedHelpers.findClassIfExists("com.android.systemui.navigationbar.NavigationModeController", lpparam.classLoader);
                if (navModeController != null) {
                    XposedHelpers.findAndHookMethod(navModeController, "getNavigationMode", 
                        XC_MethodReplacement.returnConstant(2)); 
                }
                
                // Hook modern AOSP Navigation Bar Controller for HyperOS
                Class<?> navBarController = XposedHelpers.findClassIfExists("com.android.systemui.navigationbar.NavigationBarController", lpparam.classLoader);
                if (navBarController != null) {
                    XposedHelpers.findAndHookMethod(navBarController, "isGesturalMode", 
                        int.class, XC_MethodReplacement.returnConstant(true));
                }
            } catch (Throwable t) { Log.e(TAG, "SystemUI Hook Failed: " + t.getMessage()); }
        }
    }
}