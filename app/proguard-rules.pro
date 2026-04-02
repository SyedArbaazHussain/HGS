-adaptresourcefilenames assets/libxposed_init
-adaptresourcecontents assets/libxposed_init

-keep class com.sah.mainhook { *; }
-keepclassmembers class com.sah.mainhook { *; }

-keepclassmembers class com.sah.main {
    public boolean isModuleActive();
}

-keep class com.sah.boot { *; }
-keep class com.sah.shell { *; }

-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keep class io.github.libxposed.api.** { *; }

-keep @io.github.libxposed.api.annotations.XposedHooker class * { *; }
-keepclassmembers class * {
    @io.github.libxposed.api.annotations.BeforeInvocation public static void before(io.github.libxposed.api.XposedInterface$BeforeHookCallback);
    @io.github.libxposed.api.annotations.AfterInvocation public static void after(io.github.libxposed.api.XposedInterface$AfterHookCallback);
}

-dontwarn android.util.**
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}