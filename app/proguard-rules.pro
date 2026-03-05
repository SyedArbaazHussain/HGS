# 1. Resource Synchronization
# Ensures java_init.list is updated if classes are renamed
-adaptresourcefilenames META-INF/xposed/*.list
-adaptresourcecontents META-INF/xposed/*.list

# 2. Xposed Entry Point Protection
# Do not rename or remove the main module class
-keep names class com.sah.hgs { *; }

# 3. UI & Internal Hook Protection
# Specifically keep the method hooked by the module to verify status
-keepnames class com.sah.main {
    public boolean isModuleActive();
    *;
}

# 4. Process Stability Protection
# Protect classes that handle su shell execution and boot signals
-keep class com.sah.boot { *; }
-keep class com.sah.shell { *; }

# 5. Libxposed API & Annotation Protection
# Required for the framework to find and execute your hooks via annotations
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keep class io.github.libxposed.api.** { *; }

# 6. Annotated Hooker Protection
# Prevents R8 from shrinking the static hooker classes and their callback methods
-keep @io.github.libxposed.api.annotations.XposedHooker class * { *; }
-keepclassmembers class * {
    @io.github.libxposed.api.annotations.BeforeInvocation public static void before(io.github.libxposed.api.XposedInterface$BeforeHookCallback);
    @io.github.libxposed.api.annotations.AfterInvocation public static void after(io.github.libxposed.api.XposedInterface$AfterHookCallback);
}

# 7. General Android Stability for SDK 35
-dontwarn android.util.**
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}