# Room instantiates generated *_Impl databases by reflection (WorkManager's WorkDatabase, pulled in by the ads SDK).
-keep class * extends androidx.room.RoomDatabase { <init>(); }

# Keep line numbers for readable stack traces (Play Console crash reports).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# MediaPipe builds its graph config from protobuf-lite messages via reflection, and calls Java from JNI.
-keep class com.google.mediapipe.** { *; }
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }
-dontwarn com.google.mediapipe.**
# Flogger (used by MediaPipe) finds its caller on the stack by class name, so its names must survive R8.
-keep class com.google.common.flogger.** { *; }
-dontwarn com.google.common.flogger.**

# OkHttp (via Kakao SDK) probes optional TLS providers that aren't on Android; safe to ignore.
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# Kakao SDK's Retrofit 2.9 predates R8 full mode (AGP default): keep the generic return types its API calls need
# (Kakao's documented rules). Without them the share validation call can fail and fall back to the share sheet.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>

# ML Kit (subject segmentation for closet cut-outs) discovers its components by reflection.
# Without this, R8 strips the registrars' no-arg constructors and SubjectSegmentation.getClient() fails in release.
-keep class * implements com.google.firebase.components.ComponentRegistrar { <init>(); }
