# Allows obfuscation of entry points by adapting resource filename references
# This ensures META-INF/xposed/java_init.list stays synchronized
-adaptresourcefilenames META-INF/xposed/*.list

# Protect the Xposed Entry Point
-keep class com.sah.hgs { *; }

# Protect the UI and the specific hook target method
-keep class com.sah.main {
    public boolean isModuleActive();
    *;
}

# Protect logic required for root shell execution and boot persistence
-keep class com.sah.boot { *; }
-keep class com.sah.shell { *; }

# Prevents stripping of libxposed API and annotated hookers
-keep class io.github.libxposed.api.** { *; }
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Specifically protect your @XposedHooker inner classes in hgs.java
-keep @io.github.libxposed.api.annotations.XposedHooker class * { *; }
-keepclassmembers class * {
    @io.github.libxposed.api.annotations.BeforeInvocation *;
    @io.github.libxposed.api.annotations.AfterInvocation *;
}