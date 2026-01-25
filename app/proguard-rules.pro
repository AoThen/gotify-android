# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Kotlin
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-keep class kotlinx.coroutines.** { *; }

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Jackson (if used by gson-fire)
-dontwarn org.codehaus.jackson.**
-keep class org.codehaus.jackson.** { *; }

# Coil
-keep class coil.** { *; }
-keep interface coil.** { *; }
-dontwarn coil.**

# Markwon
-dontwarn org.commonmark.**
-dontwarn io.noties.markwon.**
-keep class io.noties.markwon.** { *; }
-keep interface io.noties.markwon.** { *; }

# ThreeTenBP
-dontwarn org.threeten.bp.**
-keep class org.threeten.bp.** { *; }
-keep class org.threeten.bp.format.** { *; }

# Tinylog
-dontwarn org.tinylog.**
-keep class org.tinylog.** { *; }

# AndroidX Security
-keep class androidx.security.crypto.** { *; }

# WebSocket
-dontwarn okhttp3.internal.**
-keep class okhttp3.internal.** { *; }

# Preserve line numbers for debugging
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*

# Preserve native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep ViewBinding generated classes
-keep public class * extends androidx.viewbinding.ViewBinding {
    public static *** bind(android.view.View);
    public static *** inflate(android.view.LayoutInflater);
}

# Keep data classes used in serialization
-keepclassmembers class com.github.gotify.client.model.** {
    *;
}

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# R8 full mode
-dontwarn org.apache.oltu.oauth2.**
-keep class org.apache.oltu.oauth2.** { *; }

# DNSJava (dnsjava library) - ignore platform-specific dependencies
# REMOVED - now using pure Kotlin SRV lookup instead
# -dontwarn com.sun.jna.**
# -dontwarn javax.naming.**
# -dontwarn lombok.**
# -dontwarn org.slf4j.**
-dontwarn org.slf4j.impl.StaticLoggerBinder
# -dontwarn sun.net.spi.nameservice.**
# -keep class org.xbill.DNS.** { *; }

# Obfuscate and optimize
-renamesourcefileattribute SourceFile
