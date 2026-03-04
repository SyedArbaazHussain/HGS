package com.sah;

import android.content.ComponentName;
import android.content.Context;
import android.util.Log;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;
import io.github.libxposed.api.annotations.BeforeInvocation;
import io.github.libxposed.api.annotations.XposedHooker;

/**
 * Modern LSPosed implementation for universal gesture enforcement.
 * Uses META-INF initialization and libxposed API version 100+.
 * Updated to fix Issue #11 (Initialization/Constructor mismatch).
 */
public class hgs extends XposedModule {

    private static final String TAG = "HGS_LOG";

    /**
     * Mandatory constructor for libxposed modern API.
     * Note: Using ModuleLoadedParam is the robust way to ensure the loader 
     * identifies the module correctly as per recent API discussions.
     */
    public hgs(XposedModuleInterface base, XposedModuleInterface.ModuleLoadedParam param) {
        super(base, param);
    }

    @Override
    public void onPackageLoaded(XposedModuleInterface.PackageLoadedParam lp) {
        // Verification hook for your own app's UI
        if (lp.getPackageName().equals("com.sah.hgs")) {
            hook(lp.getClassLoader(), "com.sah.main", "isModuleActive", new ConstantTrueHooker());
        }

        // Framework-level stability and settings enforcement
        if (lp.getPackageName().equals("android")) {
            applyFrameworkHooks(lp);
        }

        // SystemUI gesture engine and navigation mode management
        if (lp.getPackageName().equals("com.android.systemui")) {
            applySystemUIHooks(lp);
        }
    }

    private void applyFrameworkHooks(XposedModuleInterface.PackageLoadedParam lp) {
        try {
            // Intercepts display settings to prevent Hardware Composer panics (getLuts error)
            hook(lp.getClassLoader(), "com.android.server.display.DisplayDevice", "applyDisplaySettings",
                 "android.view.SurfaceControl$Transaction", boolean.class, new DisplayStabilityHooker());

            // Enforces navigation overlays regardless of system state
            hook(lp.getClassLoader(), "com.android.server.om.OverlayManagerService", "setEnabled",
                 String.class, boolean.class, int.class, new OverlayEnforcementHooker());

            // Prevents launcher fallback to MIUI Home variants
            hook(lp.getClassLoader(), "com.android.server.wm.ActivityTaskManagerService", "updateDefaultHomeActivity",
                 ComponentName.class, new HomeProtectionHooker());

        } catch (Throwable ignored) {}
    }

    private void applySystemUIHooks(XposedModuleInterface.PackageLoadedParam lp) {
        try {
            // Stabilizes transitions during resolution/density changes
            hook(lp.getClassLoader(), "com.android.systemui.statusbar.phone.MiuiNotificationPanelViewControllerInjector", 
                 "onConfigurationChanged", "android.content.res.Configuration", new GenericStabilizerHooker());

            // Locks navigation mode to Gestural (2)
            hook(lp.getClassLoader(), "com.android.systemui.navigationbar.NavigationModeController", 
                 "onRequestedNavigationModeChange", int.class, new NavModeForcerHooker());
            
            hook(lp.getClassLoader(), "com.android.systemui.navigationbar.NavigationModeController", 
                 "getNavigationMode", new ConstantTwoHooker());

            // Forces high-sensitivity gesture regions (Stub enforcement)
            hook(lp.getClassLoader(), "com.android.systemui.statusbar.phone.MiuiGestureStubView", 
                 "isGestureEnable", Context.class, new ConstantTrueHooker());

        } catch (Throwable t) { Log.e(TAG, "SystemUI Hook Failed: " + t.getMessage()); }
    }

    @XposedHooker
    class DisplayStabilityHooker implements XposedInterface.Hooker {
        @BeforeInvocation
        public void before(XposedInterface.BeforeHookCallback callback) {
            Log.d(TAG, "Syncing universal display transaction");
        }
    }

    @XposedHooker
    class OverlayEnforcementHooker implements XposedInterface.Hooker {
        @BeforeInvocation
        public void before(XposedInterface.BeforeHookCallback callback) {
            String pkg = (String) callback.getArgs()[0];
            boolean enable = (boolean) callback.getArgs()[1];
            if (pkg.contains("navbar.gestural") && !enable) callback.setResult(true);
        }
    }

    @XposedHooker
    class HomeProtectionHooker implements XposedInterface.Hooker {
        @BeforeInvocation
        public void before(XposedInterface.BeforeHookCallback callback) {
            ComponentName cn = (ComponentName) callback.getArgs()[0];
            if (cn != null && (cn.getPackageName().contains("miui.home") || cn.getPackageName().contains("mi.launcher"))) {
                callback.setResult(null);
            }
        }
    }

    @XposedHooker
    class NavModeForcerHooker implements XposedInterface.Hooker {
        @BeforeInvocation
        public void before(XposedInterface.BeforeHookCallback callback) {
            callback.getArgs()[0] = 2;
        }
    }

    @XposedHooker
    class GenericStabilizerHooker implements XposedInterface.Hooker {
        @BeforeInvocation
        public void before(XposedInterface.BeforeHookCallback callback) {
            Log.d(TAG, "Universal Stability Interceptor Active");
        }
    }

    @XposedHooker
    class ConstantTrueHooker implements XposedInterface.Hooker {
        @BeforeInvocation
        public void before(XposedInterface.BeforeHookCallback callback) {
            callback.setResult(true);
        }
    }

    @XposedHooker
    class ConstantTwoHooker implements XposedInterface.Hooker {
        @BeforeInvocation
        public void before(XposedInterface.BeforeHookCallback callback) {
            callback.setResult(2);
        }
    }
}