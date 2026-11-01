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
