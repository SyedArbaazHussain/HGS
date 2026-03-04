# 1. Xposed Entry Point Protection
# Ensures the class defined in assets/xposed_init is never renamed or removed
-keep class com.hyperos.gesturefix.HyperOSLauncherSpoofer { *; }

# 2. Module Status & Activity Protection
# Prevents R8 from inlining the status check or obfuscating UI components
-keepclassmembers class com.hyperos.gesturefix.MainActivity {
    public boolean isModuleActive();
}
-keep class com.hyperos.gesturefix.MainActivity { *; }

# 3. Persistence & Logic Protection
# Ensures Boot and Shell logic remain intact even if not explicitly invoked by UI
-keep class com.hyperos.gesturefix.BootReceiver { *; }
-keep class com.hyperos.gesturefix.ShellUtils { *; }

# 4. Xposed API Framework
# Keep the Xposed API and ignore warnings about missing system classes it references
-keep class de.robv.android.xposed.** { *; }
-dontwarn de.robv.android.xposed.**

# 5. Reflection and Metadata Protection
# Prevents stripping attributes that Xposed uses to identify and hook the module
-keepattributes Signature, RuntimeVisibleAnnotations, AnnotationDefault, EnclosingMethod, InnerClasses
-keepclassmembers class * {
    @de.robv.android.xposed.hook.* <methods>;
}

# 6. AndroidX & Material Design Protection
# Required for FileProvider (log export) and Material Cards (UI Dashboard)
-keep class androidx.core.content.FileProvider { *; }
-keep class com.google.android.material.** { *; }
-dontwarn androidx.core.**
-dontwarn com.google.android.material.**

# 7. Optimization Overrides
# Prevents stripping of native system calls used in Shell scripts
-keepclasseswithmembernames class * {
    native <methods>;
}