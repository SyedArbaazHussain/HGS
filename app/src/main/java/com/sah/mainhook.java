package com.sah;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.Log;
import androidx.annotation.NonNull;
import io.github.libxposed.api.XposedModule;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class mainhook extends XposedModule {

    private static final String TAG = "HGS_Hook";
    private static final long CACHE_EXPIRY = TimeUnit.MINUTES.toMillis(10);
    private final Set<String> launcherCache = Collections.synchronizedSet(new HashSet<>());
    private long lastUpdate = 0;

    public mainhook(@NonNull NativeContext nativeContext, @NonNull ModuleContext moduleContext) {
        super(nativeContext, moduleContext);
    }

    @Override
    public void onPackageReady(@NonNull PackageReadyParam param) {
        String pkg = param.getPackageName();

        if (pkg.equals("com.sah.hgs")) {
            try {
                Class<?> mainActivity = param.getClassLoader().loadClass("com.sah.main");
                hooker(mainActivity.getDeclaredMethod("isModuleActive")).replace(true);
            } catch (Exception e) {
                Log.e(TAG, "Self-hook failed", e);
            }
        }

        if (pkg.equals("com.android.systemui")) {
            applySystemUIHooks(param);
        }

        if (pkg.equals("com.miui.home")) {
            applyMiuiHomeHooks(param);
        }

        if (pkg.equals("android")) {
            applyFrameworkHooks(param);
        }
    }

    private void applySystemUIHooks(PackageReadyParam param) {
        try {
            ClassLoader loader = param.getClassLoader();
            Class<?> proxy = loader.loadClass("com.android.systemui.recents.OverviewProxyService");
            hooker(proxy.getDeclaredMethod("isEnabled")).replace(true);

            Class<?> qsc = loader.loadClass("com.android.systemui.shared.system.QuickStepContract");
            hooker(qsc.getDeclaredMethod("isGesturalMode", int.class)).replace(true);
        } catch (Throwable t) {
            Log.e(TAG, "SystemUI error", t);
        }
    }

    private void applyMiuiHomeHooks(PackageReadyParam param) {
        try {
            Class<?> config = param.getClassLoader().loadClass("com.miui.home.launcher.DeviceConfig");
            hooker(config.getDeclaredMethod("isSystemLauncher")).replace(true);
            try {
                hooker(config.getDeclaredMethod("isSupportGesture")).replace(true);
            } catch (NoSuchMethodException ignored) {}
        } catch (Throwable t) {
            Log.e(TAG, "MiuiHome error", t);
        }
    }

    private void applyFrameworkHooks(PackageReadyParam param) {
        try {
            Class<?> pms = param.getClassLoader().loadClass("com.android.server.pm.PackageManagerService");
            hooker(pms.getDeclaredMethod("isSystemApp", String.class)).before(callback -> {
                String pkg = (String) callback.getArgs()[0];
                if (isTargetPackage(pkg)) {
                    callback.returnAndSkip(true);
                }
            });
        } catch (Throwable t) {
            Log.e(TAG, "Framework error", t);
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
            Log.e(TAG, "Cache update failed", e);
        }
    }

    private Resources getModuleResources() {
        try {
            ApplicationInfo ai = getModuleApplicationInfo();
            AssetManager am = AssetManager.class.newInstance();
            Method addPath = AssetManager.class.getDeclaredMethod("addAssetPath", String.class);
            addPath.invoke(am, ai.sourceDir);
            return new Resources(am, Resources.getSystem().getDisplayMetrics(), Resources.getSystem().getConfiguration());
        } catch (Exception e) {
            return null;
        }
    }
}