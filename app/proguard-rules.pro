# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep WebRTC classes
-keep class org.webrtc.** { *; }
-keep class io.github.100mslive.** { *; }

# Keep JNI methods
-keepclassmembers class * {
    native <methods>;
}

# Keep all your custom adapter and observer classes
-keep class com.huan.capture.** { *; }

# 如果使用了 Gson 或其他反射框架，也需要保留字段
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}










-keep class com.uc.** { *; }
-keep class com.uc.webview.base.* {*;}
-keep class com.uc.webview.base.annotations.* {*;}
-keep class com.uc.webview.base.build.* {*;}
-keep class com.uc.webview.base.cyclone.* {*;}
-keep class com.uc.webview.base.io.* {*;}
-keep class com.uc.webview.base.klog.* {*;}
-keep class com.uc.webview.base.task.* {*;}
-keep class com.uc.webview.base.timing.* {*;}
-keep class com.uc.webview.base.zip.* {*;}
-keep class com.uc.webview.export.* {*;}
-keep class com.uc.webview.export.devtools.* {*;}
-keep class com.uc.webview.export.extension.* {*;}
-keep class com.uc.webview.export.internal.interfaces.* {*;}
-keep class com.uc.webview.export.internal.setup.* {*;}
-keep class com.uc.webview.export.media.* {*;}
-keep class com.uc.webview.export.multiprocess.* {*;}
-keep class com.uc.webview.export.multiprocess.helper.* {*;}
-keep class com.uc.webview.export.utility.* {*;}
-keep class com.uc.webview.internal.* {*;}
-keep class com.uc.webview.internal.android.* {*;}
-keep class com.uc.webview.internal.interfaces.* {*;}
-keep class com.uc.webview.internal.setup.* {*;}
-keep class com.uc.webview.internal.setup.component.* {*;}
-keep class com.uc.webview.internal.setup.download.* {*;}
-keep class com.uc.webview.internal.setup.download.impl.* {*;}
-keep class com.uc.webview.internal.setup.verify.* {*;}
-keep class com.uc.webview.internal.stats.* {*;}
-keep class com.uc.webview.stat.* {*;}
-dontwarn com.uc.core.**
-dontwarn org.chromium.**
-keep class org.chromium.** {*;}
-keepattributes *Annotation*

#---------------------------------------------------------------
# used to keep sdk interface
-keeppackagenames com.uc.webview.**

-keepclasseswithmembers class com.uc.webview.base.annotations.* { *; }
-keepclasseswithmembers class com.uc.webview.base.build.CoreType { *; }
-keepclasseswithmembers class com.uc.webview.base.build.NativeLibraries { *; }
-keepclasseswithmembers class com.uc.webview.J.N { *; }

-keep class com.uc.webview.base.cyclone.** { *; }

#---------------------------------------------------------------
# used to keep classes used by sdk

-keep @interface com.uc.webview.base.annotations.Api
-keep @interface com.uc.webview.base.annotations.Interface
-keep @interface com.uc.webview.base.annotations.Reflection

-keep @interface com.uc.webview.export.JavascriptInterface
-keep @interface com.uc.webview.export.AsyncJavascriptInterface

-keep @com.uc.webview.base.annotations.Api class com.uc.webview.** { *; }
-keepclasseswithmembers class com.uc.webview.** { @com.uc.webview.base.annotations.Api <fields>;}
-keepclasseswithmembers class com.uc.webview.** { @com.uc.webview.base.annotations.Api <methods>;}

-keep @com.uc.webview.base.annotations.Interface class ** { *; }
-keepclasseswithmembers class com.uc.webview.** { @com.uc.webview.base.annotations.Interface <fields>;}
-keepclasseswithmembers class com.uc.webview.** { @com.uc.webview.base.annotations.Interface <methods>;}

-keep @com.uc.webview.base.annotations.Reflection class * { *; }
-keepclasseswithmembers class com.uc.webview.** { @com.uc.webview.base.annotations.Reflection <fields>;}
-keepclasseswithmembers class com.uc.webview.** { @com.uc.webview.base.annotations.Reflection <methods>;}

-keepclasseswithmembers class com.uc.webview.** { @com.uc.webview.export.JavascriptInterface *; }
-keepclasseswithmembers class com.uc.webview.** { @com.uc.webview.export.AsyncJavascriptInterface *; }

# sdk glue
-keep @com.uc.webview.base.annotations.Api class com.uc.sdk_glue.** { *; }
-keepclasseswithmembers class com.uc.sdk_glue.** { @com.uc.webview.base.annotations.Api <fields>;}
-keepclasseswithmembers class com.uc.sdk_glue.** { @com.uc.webview.base.annotations.Api <methods>;}

-keep @com.uc.webview.base.annotations.Interface class ** { *; }
-keepclasseswithmembers class com.uc.sdk_glue.** { @com.uc.webview.base.annotations.Interface <fields>;}
-keepclasseswithmembers class com.uc.sdk_glue.** { @com.uc.webview.base.annotations.Interface <methods>;}

-keep @com.uc.webview.base.annotations.Reflection class * { *; }
-keepclasseswithmembers class com.uc.sdk_glue.** { @com.uc.webview.base.annotations.Reflection <fields>;}
-keepclasseswithmembers class com.uc.sdk_glue.** { @com.uc.webview.base.annotations.Reflection <methods>;}

# pictureviewer
-keep @interface com.uc.pictureviewer.interfaces.Api
-keep @com.uc.pictureviewer.interfaces.Api class com.uc.pictureviewer.** { *; }

# imagecodec
-keep @interface com.uc.imagecodec.export.annotations.Api
-keep @interface com.uc.imagecodec.export.annotations.Jni
-keep @com.uc.imagecodec.export.annotations.Api class com.uc.imagecodec.** { *; }
-keepclasseswithmembers class com.uc.imagecodec.** { @com.uc.imagecodec.export.annotations.Api <fields>;}
-keepclasseswithmembers class com.uc.imagecodec.** { @com.uc.imagecodec.export.annotations.Api <methods>;}
-keepclasseswithmembers class com.uc.imagecodec.** { @com.uc.imagecodec.export.annotations.Jni <fields>;}
-keepclasseswithmembers class com.uc.imagecodec.** { @com.uc.imagecodec.export.annotations.Jni <methods>;}

#-----------------------
# multi proc setting
-keep @interface com.uc.webview.export.multiprocess.Api
-keep @com.uc.webview.export.multiprocess.Api class com.uc.webview.export.multiprocess.** { *; }
-keepclasseswithmembers class com.uc.webview.export.multiprocess.** { @com.uc.webview.export.multiprocess.Api <fields>;}
-keepclasseswithmembers class com.uc.webview.export.multiprocess.** { @com.uc.webview.export.multiprocess.Api <init>(...);}
-keepclasseswithmembers class com.uc.webview.export.multiprocess.** { @com.uc.webview.export.multiprocess.Api <methods>;}
# 3.0, 4.0 compatable
-keep @com.uc.webview.export.multiprocess.Api class com.uc.sandboxExport.** { *; }
-keepclasseswithmembers class com.uc.sandboxExport.** { @com.uc.webview.export.multiprocess.Api <fields>;}
-keepclasseswithmembers class com.uc.sandboxExport.** { @com.uc.webview.export.multiprocess.Api <init>(...);}
-keepclasseswithmembers class com.uc.sandboxExport.** { @com.uc.webview.export.multiprocess.Api <methods>;}
# multi proc setting
#-----------------------

#-----------------------
# uc media
-keep class com.uc.media.annotation.KeepForRuntime { *; }
-keep @com.uc.media.annotation.KeepForRuntime class * { *; }

-keepclasseswithmembers class * { @com.uc.media.annotation.KeepForRuntime <fields>; }
-keepclasseswithmembers class * { @com.uc.media.annotation.KeepForRuntime <methods>; }

# for ucmedia apollo plugin
-keep class com.uc.media.plugins.apollo.MediaPlayerImpl { *; }
-keep class com.uc.media.plugins.apollo.MediaPlayerFactoryImpl { *; }
-keep class com.UCMobile.Apollo.**, io.vov.** { *; }
#-----------------------

# wpk
-keep @interface com.uc.wpk.Api
-keep @com.uc.wpk.Api class com.uc.wpk.export.** { *; }
-keep @com.uc.wpk.Api class com.uc.wpk.UCDataFlow { *; }
-keepclasseswithmembers class com.uc.wpk.export.**  { @com.uc.imagecodec.export.annotations.Api <methods>;}
-keepclasseswithmembers class com.uc.wpk.UCDataFlow { @com.uc.imagecodec.export.annotations.Api <methods>;}

#---------------------------------------------------------------
# used to skip java 8 class. it will be processed in desugar step
#-dontwarn java.lang.invoke.**
-dontwarn java.lang.invoke.LambdaMetafactory

