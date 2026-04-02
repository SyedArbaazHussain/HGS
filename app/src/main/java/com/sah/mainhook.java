package com.sah;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.util.Log;
import androidx.annotation.NonNull;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.annotations.BeforeInvocation;
import io.github.libxposed.api.annotations.XposedHooker;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class mainhook extends XposedModule {

    private static final String TAG = "HGS_Hook";
    private static final int NAV_BAR_MODE_GESTURAL = 2;
    private static final long CACHE_EXPIRY = TimeUnit.MINUTES.toMillis(10);
    private final Set<String> launcherCache = Collections.synchronizedSet(new HashSet<>());
    private long lastUpdate = 0;

    // API 101: Constructor must not take arguments
    public mainhook() {
        super();
    }

    @XposedHooker
    public static class BeforeHooker implements XposedInterface.Hooker {
        @BeforeInvocation
        public static void before(XposedInterface.BeforeHookCallback callback) {
            callback.setResult(true);
        }
    }

    @XposedHooker
    public static class SetResultNullHooker implements XposedInterface.Hooker {
        @BeforeInvocation
        public static void before(XposedInterface.BeforeHookCallback callback) {
            callback.setResult(null);
        }
    }

    @Override
    public void onPackageLoaded(@NonNull PackageLoadedParam param) {
        String pkg = param.getPackageName();
        ClassLoader loader = param.getClassLoader();

        if (pkg.equals("com.sah.hgs")) {
            try {
                hook(loader.loadClass("com.sah.main").getDeclaredMethod("isModuleActive"), BeforeHooker.class);
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
            
            // Manual lambda hook for dynamic logic
            hook(atm.getDeclaredMethod("updateDefaultHomeActivity", ComponentName.class), new XposedInterface.Hooker() {
                @BeforeInvocation
                public void before(XposedInterface.BeforeHookCallback callback) {
                    ComponentName cn = (ComponentName) callback.getArgs()[0];
                    if (cn != null && (cn.getPackageName().contains("miui.home") || cn.getPackageName().contains("mi.launcher"))) {
                        callback.setResult(null);
                    }
                }
            });

        } catch (Throwable t) {
            Log.e(TAG, "F-H-F", t);
        }
    }

    private void applySystemUIHooks(ClassLoader loader) {
        try {
            Class<?> navCtrl = loader.loadClass("com.android.systemui.navigationbar.NavigationModeController");
            
            hook(navCtrl.getDeclaredMethod("getNavigationMode"), new XposedInterface.Hooker() {
                @BeforeInvocation
                public void before(XposedInterface.BeforeHookCallback callback) {
                    callback.setResult(NAV_BAR_MODE_GESTURAL);
                }
            });

            hook(navCtrl.getDeclaredMethod("onRequestedNavigationModeChange", int.class), new XposedInterface.Hooker() {
                @BeforeInvocation
                public void before(XposedInterface.BeforeHookCallback callback) {
                    callback.getArgs()[0] = NAV_BAR_MODE_GESTURAL;
                }
            });

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

    private void updateCache() {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
            // In API 101, use getSystemContext() if available or the base interface
            List<ResolveInfo> resolves = getSystemContext().getPackageManager().queryIntentActivities(intent, 0);
            
            synchronized (launcherCache) {
                launcherCache.clear();
                for (ResolveInfo ri : resolves) {
                    if (ri.activityInfo != null) {
                        launcherCache.add(ri.activityInfo.packageName);
                    }
                }
            }
            lastUpdate = System.currentTimeMillis();
        } catch (Exception e) {
            Log.e(TAG, "C-U-F", e);
        }
    }
}