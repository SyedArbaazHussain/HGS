package com.sah;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.util.Log;
import androidx.annotation.NonNull;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModuleInterface;
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

    public mainhook(@NonNull XposedModuleInterface.NativeContext nativeContext, @NonNull XposedModuleInterface.ModuleContext moduleContext) {
        super(nativeContext, moduleContext);
    }

    private XposedInterface.Hooker hooker(Method method) {
        return hook(method);
    }

    @Override
    public void onPackageLoaded(@NonNull PackageLoadedParam param) {
        String pkg = param.getPackageName();
        ClassLoader loader = param.getClassLoader();

        if (pkg.equals("com.sah.hgs")) {
            try {
                Class<?> mainActivity = loader.loadClass("com.sah.main");
                hooker(mainActivity.getDeclaredMethod("isModuleActive")).replace(true);
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
            hooker(loader.loadClass("android.view.ViewConfiguration").getDeclaredMethod("isDefaultScrollCaptureEnabled")).replace(true);

            try {
                hooker(loader.loadClass("android.hardware.display.DisplayManager").getDeclaredMethod("getCompositionLuts")).replace(null);
            } catch (NoSuchMethodException e) {
                try {
                    hooker(loader.loadClass("android.hardware.display.IDisplayManager$Stub$Proxy").getDeclaredMethod("getCompositionLuts")).replace(null);
                } catch (Exception ignored) {}
            }

            Class<?> atm = loader.loadClass("com.android.server.wm.ActivityTaskManagerService");
            hooker(atm.getDeclaredMethod("isRecentsComponentHomeActivity", int.class)).replace(true);
            
            hooker(atm.getDeclaredMethod("updateDefaultHomeActivity", ComponentName.class)).before(callback -> {
                ComponentName cn = (ComponentName) callback.getArgs()[0];
                if (cn != null && (cn.getPackageName().contains("miui.home") || cn.getPackageName().contains("mi.launcher"))) {
                    callback.returnAndSkip(null);
                }
            });

            try {
                Class<?> pms = loader.loadClass("com.android.server.pm.PackageManagerService");
                hooker(pms.getDeclaredMethod("isSystemApp", String.class)).before(callback -> {
                    String targetPkg = (String) callback.getArgs()[0];
                    if (isTargetPackage(targetPkg)) {
                        callback.returnAndSkip(true);
                    }
                });
            } catch (Exception ignored) {}

            try {
                Class<?> oms = loader.loadClass("com.android.server.om.OverlayManagerService");
                hooker(oms.getDeclaredMethod("setEnabled", String.class, boolean.class, int.class)).before(callback -> {
                    String overlayPkg = (String) callback.getArgs()[0];
                    if (overlayPkg != null && overlayPkg.contains("navbar.gestural")) {
                        callback.getArgs()[1] = true;
                    }
                });
            } catch (Exception ignored) {}

        } catch (Throwable t) {
            Log.e(TAG, "F-H-F", t);
        }
    }

    private void applySystemUIHooks(ClassLoader loader) {
        try {
            Class<?> navCtrl = loader.loadClass("com.android.systemui.navigationbar.NavigationModeController");
            hooker(navCtrl.getDeclaredMethod("getNavigationMode")).replace(NAV_BAR_MODE_GESTURAL);
            hooker(navCtrl.getDeclaredMethod("onRequestedNavigationModeChange", int.class)).before(callback -> {
                callback.getArgs()[0] = NAV_BAR_MODE_GESTURAL;
            });

            try {
                Class<?> gestureStub = loader.loadClass("com.android.systemui.statusbar.phone.MiuiGestureStubView");
                hooker(gestureStub.getDeclaredMethod("isGestureEnable", Context.class)).replace(true);
            } catch (Exception ignored) {}

            try {
                Class<?> proxy = loader.loadClass("com.android.systemui.recents.OverviewProxyService");
                hooker(proxy.getDeclaredMethod("isEnabled")).replace(true);
            } catch (Exception ignored) {}

        } catch (Throwable t) {
            Log.e(TAG, "SUI-H-F", t);
        }
    }

    private void applyMiuiHomeHooks(ClassLoader loader) {
        try {
            Class<?> config = loader.loadClass("com.miui.home.launcher.DeviceConfig");
            hooker(config.getDeclaredMethod("isSystemLauncher")).replace(true);
            try {
                hooker(config.getDeclaredMethod("isSupportGesture")).replace(true);
            } catch (NoSuchMethodException ignored) {}
        } catch (Throwable t) {
            Log.e(TAG, "MH-H-F", t);
        }
    }

    private boolean isTargetPackage(String pkg) {
        if (pkg == null) return false;
        if (pkg.equals("com.sah.hgs")) return true;

        long now = System.currentTimeMillis();
        if (now - lastUpdate > CACHE_EXPIRY || launcherCache.isEmpty()) {
            updateCache();
        }
        return launcherCache.contains(pkg);
    }

    private void updateCache() {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
            List<ResolveInfo> resolves = getModuleContext().getPackageManager().queryIntentActivities(intent, 0);
            
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