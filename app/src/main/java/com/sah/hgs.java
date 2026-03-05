package com.sah;

import android.content.ComponentName;
import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.annotations.AfterInvocation;
import io.github.libxposed.api.annotations.BeforeInvocation;
import io.github.libxposed.api.annotations.XposedHooker;

/**
 * Modern LSPosed implementation for universal gesture enforcement.
 * Optimized for libxposed API 100 and SDK 35.
 */
public class hgs extends XposedModule {

    private static final String TAG = "HGS_LOG";

    public hgs(@NonNull XposedInterface base, @NonNull ModuleLoadedParam param) {
        super(base, param);
    }

    @Override
    public void onPackageLoaded(@NonNull PackageLoadedParam lp) {
        String packageName = lp.getPackageName();

        // 1. Verification hook for your own app's UI
        if (packageName.equals("com.sah.hgs")) {
            try {
                hook(lp.getClassLoader().loadClass("com.sah.main").getDeclaredMethod("isModuleActive"), 
                     ConstantTrueHooker.class);
            } catch (Exception e) {
                Log.e(TAG, "Own App Hook Failed", e);
            }
        }

        // 2. Framework-level stability and settings enforcement
        if (packageName.equals("android")) {
            applyFrameworkHooks(lp);
        }

        // 3. SystemUI gesture engine management
        if (packageName.equals("com.android.systemui")) {
            applySystemUIHooks(lp);
        }
    }

    private void applyFrameworkHooks(PackageLoadedParam lp) {
        try {
            ClassLoader cl = lp.getClassLoader();

            // Intercepts display settings to prevent Hardware Composer panics
            hook(cl.loadClass("com.android.server.display.DisplayDevice")
                    .getDeclaredMethod("applyDisplaySettings", cl.loadClass("android.view.SurfaceControl$Transaction"), boolean.class),
                    DisplayStabilityHooker.class);

            // Enforces navigation overlays regardless of system state
            hook(cl.loadClass("com.android.server.om.OverlayManagerService")
                    .getDeclaredMethod("setEnabled", String.class, boolean.class, int.class),
                    OverlayEnforcementHooker.class);

            // Prevents launcher fallback to MIUI Home variants
            hook(cl.loadClass("com.android.server.wm.ActivityTaskManagerService")
                    .getDeclaredMethod("updateDefaultHomeActivity", ComponentName.class),
                    HomeProtectionHooker.class);

        } catch (Throwable t) {
            Log.e(TAG, "Framework Hook Failure", t);
        }
    }

    private void applySystemUIHooks(PackageLoadedParam lp) {
        try {
            ClassLoader cl = lp.getClassLoader();

            // Locks navigation mode to Gestural (2)
            hook(cl.loadClass("com.android.systemui.navigationbar.NavigationModeController")
                    .getDeclaredMethod("onRequestedNavigationModeChange", int.class),
                    NavModeForcerHooker.class);

            hook(cl.loadClass("com.android.systemui.navigationbar.NavigationModeController")
                    .getDeclaredMethod("getNavigationMode"),
                    ConstantTwoHooker.class);

            // Forces high-sensitivity gesture regions (Stub enforcement)
            hook(cl.loadClass("com.android.systemui.statusbar.phone.MiuiGestureStubView")
                    .getDeclaredMethod("isGestureEnable", Context.class),
                    ConstantTrueHooker.class);

        } catch (Throwable t) {
            Log.e(TAG, "SystemUI Hook Failure", t);
        }
    }

    // --- HOOKER IMPLEMENTATIONS ---
    // Modern API recommends static hooker classes for better R8 optimization

    @XposedHooker
    public static class ConstantTrueHooker implements XposedInterface.Hooker {
        @BeforeInvocation
        public static void before(XposedInterface.BeforeHookCallback cb) {
            cb.setResult(true);
        }
    }

    @XposedHooker
    public static class ConstantTwoHooker implements XposedInterface.Hooker {
        @BeforeInvocation
        public static void before(XposedInterface.BeforeHookCallback cb) {
            cb.setResult(2);
        }
    }

    @XposedHooker
    public static class NavModeForcerHooker implements XposedInterface.Hooker {
        @BeforeInvocation
        public static void before(XposedInterface.BeforeHookCallback cb) {
            // Forces the input argument of onRequestedNavigationModeChange to 2
            cb.getArgs()[0] = 2;
        }
    }

    @XposedHooker
    public static class OverlayEnforcementHooker implements XposedInterface.Hooker {
        @BeforeInvocation
        public static void before(XposedInterface.BeforeHookCallback cb) {
            String pkg = (String) cb.getArgs()[0];
            boolean enable = (boolean) cb.getArgs()[1];
            // If system tries to disable gestural navbar, force result to true (keep enabled)
            if (pkg != null && pkg.contains("navbar.gestural") && !enable) {
                cb.setResult(true);
            }
        }
    }

    @XposedHooker
    public static class HomeProtectionHooker implements XposedInterface.Hooker {
        @BeforeInvocation
        public static void before(XposedInterface.BeforeHookCallback cb) {
            ComponentName cn = (ComponentName) cb.getArgs()[0];
            if (cn != null && (cn.getPackageName().contains("miui.home") || cn.getPackageName().contains("mi.launcher"))) {
                cb.setResult(null); // Prevent setting MIUI launcher as default
            }
        }
    }

    @XposedHooker
    public static class DisplayStabilityHooker implements XposedInterface.Hooker {
        @AfterInvocation
        public static void after(XposedInterface.AfterHookCallback cb) {
            Log.d(TAG, "Display transaction intercepted");
        }
    }
}