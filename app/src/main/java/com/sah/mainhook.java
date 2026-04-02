package com.sah;

import android.content.ComponentName;
import android.util.Log;
import androidx.annotation.NonNull;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.annotations.BeforeInvocation;
import io.github.libxposed.api.annotations.XposedHooker;
import java.lang.reflect.Method;

public class mainhook extends XposedModule {

    private static final String TAG = "HGS_Hook";
    private static final int NAV_BAR_MODE_GESTURAL = 2;

    public mainhook(@NonNull XposedInterface base, @NonNull ModuleLoadedParam param) {
        super(base, param);
    }

    @XposedHooker
    public static class BeforeHooker {
        @BeforeInvocation
        public static void before(XposedInterface.BeforeHookCallback callback) {
            callback.setResult(true);
        }
    }

    @XposedHooker
    public static class SetResultNullHooker {
        @BeforeInvocation
        public static void before(XposedInterface.BeforeHookCallback callback) {
            callback.setResult(null);
        }
    }

    @XposedHooker
    public static class UpdateDefaultHomeHooker {
        @BeforeInvocation
        public static void before(XposedInterface.BeforeHookCallback callback) {
            ComponentName cn = (ComponentName) callback.getArgs()[0];
            if (cn != null && (cn.getPackageName().contains("miui.home") || cn.getPackageName().contains("mi.launcher"))) {
                callback.setResult(null);
            }
        }
    }

    @XposedHooker
    public static class GetNavModeHooker {
        @BeforeInvocation
        public static void before(XposedInterface.BeforeHookCallback callback) {
            callback.setResult(NAV_BAR_MODE_GESTURAL);
        }
    }

    @XposedHooker
    public static class OnNavChangeHooker {
        @BeforeInvocation
        public static void before(XposedInterface.BeforeHookCallback callback) {
            callback.getArgs()[0] = NAV_BAR_MODE_GESTURAL;
        }
    }

    @Override
    public void onPackageLoaded(@NonNull PackageLoadedParam param) {
        String pkg = param.getPackageName();
        ClassLoader loader = param.getClassLoader();

        if (pkg.equals("com.sah.hgs")) {
            try {
                Method isModuleActive = loader.loadClass("com.sah.main").getDeclaredMethod("isModuleActive");
                hook(isModuleActive, BeforeHooker.class);
            } catch (Exception e) {
                Log.e(TAG, "S-H-F", e);
            }
        }

        if (pkg.equals("android")) {
            applyFrameworkHooks(loader);
        }

        if (pkg.equals("com.android.systemui")) {
            applySystemUIHooks(loader);
        }

        if (pkg.equals("com.miui.home")) {
            applyMiuiHomeHooks(loader);
        }
    }

    private void applyFrameworkHooks(ClassLoader loader) {
        try {
            hook(loader.loadClass("android.view.ViewConfiguration").getDeclaredMethod("isDefaultScrollCaptureEnabled"), BeforeHooker.class);
            try {
                hook(loader.loadClass("android.hardware.display.DisplayManager").getDeclaredMethod("getCompositionLuts"), SetResultNullHooker.class);
            } catch (NoSuchMethodException e) {
                try {
                    hook(loader.loadClass("android.hardware.display.IDisplayManager$Stub$Proxy").getDeclaredMethod("getCompositionLuts"), SetResultNullHooker.class);
                } catch (Exception ignored) {}
            }
            Class<?> atm = loader.loadClass("com.android.server.wm.ActivityTaskManagerService");
            hook(atm.getDeclaredMethod("isRecentsComponentHomeActivity", int.class), BeforeHooker.class);
            hook(atm.getDeclaredMethod("updateDefaultHomeActivity", ComponentName.class), UpdateDefaultHomeHooker.class);
        } catch (Throwable t) {
            Log.e(TAG, "F-H-F", t);
        }
    }

    private void applySystemUIHooks(ClassLoader loader) {
        try {
            Class<?> navCtrl = loader.loadClass("com.android.systemui.navigationbar.NavigationModeController");
            hook(navCtrl.getDeclaredMethod("getNavigationMode"), GetNavModeHooker.class);
            hook(navCtrl.getDeclaredMethod("onRequestedNavigationModeChange", int.class), OnNavChangeHooker.class);
        } catch (Throwable t) {
            Log.e(TAG, "SUI-H-F", t);
        }
    }

    private void applyMiuiHomeHooks(ClassLoader loader) {
        try {
            Class<?> config = loader.loadClass("com.miui.home.launcher.DeviceConfig");
            hook(config.getDeclaredMethod("isSystemLauncher"), BeforeHooker.class);
            try {
                hook(config.getDeclaredMethod("isSupportGesture"), BeforeHooker.class);
            } catch (NoSuchMethodException ignored) {}
        } catch (Throwable t) {
            Log.e(TAG, "MH-H-F", t);
        }
    }
}