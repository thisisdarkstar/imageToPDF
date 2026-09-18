# =======================================================================
# ProGuard / R8 Rules for Image to PDF
# Optimized for Google Play Store Release Builds
# =======================================================================

# -----------------------------------------------------------------------
# Stack Trace De-obfuscation (Essential for Play Console Vitals & Crashlytics)
# -----------------------------------------------------------------------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve parameter annotations and type annotations
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# -----------------------------------------------------------------------
# General Code Optimization & Shrinking
# -----------------------------------------------------------------------
-allowaccessmodification
-repackageclasses ''
-dontwarn

# -----------------------------------------------------------------------
# Kotlin Standard Library & Coroutines
# -----------------------------------------------------------------------
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keep class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keep class kotlinx.coroutines.CoroutineExceptionHandler { *; }
-dontwarn kotlinx.coroutines.**

# -----------------------------------------------------------------------
# Jetpack Compose
# -----------------------------------------------------------------------
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material3.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
    androidx.compose.runtime.RecomposeScopeImpl *;
}
-dontwarn androidx.compose.**

# -----------------------------------------------------------------------
# Coil Image Loading Engine
# -----------------------------------------------------------------------
-keep class io.coilkt.coil.** { *; }
-keep class coil.** { *; }
-keepclassmembers class * implements coil.decode.Decoder { *; }
-keepclassmembers class * implements coil.fetch.Fetcher { *; }
-dontwarn coil.**
-dontwarn io.coilkt.coil.**

# -----------------------------------------------------------------------
# AndroidX Lifecycle, ViewModel, and Navigation
# -----------------------------------------------------------------------
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class androidx.lifecycle.ViewModelProvider$Factory { *; }
-keep class androidx.navigation.** { *; }
-keep class androidx.navigation3.** { *; }
-dontwarn androidx.navigation.**
-dontwarn androidx.navigation3.**

# -----------------------------------------------------------------------
# Application Data Models & Engine
# Keep data classes, enums, and properties to guarantee safe serialization & state
# -----------------------------------------------------------------------
-keep class com.example.imagetopdf.model.** { *; }
-keepclassmembers enum com.example.imagetopdf.model.** { *; }

-keep class com.example.imagetopdf.engine.** { *; }
-keep class com.example.imagetopdf.data.** { *; }
-keep class com.example.imagetopdf.theme.ThemeMode { *; }

# -----------------------------------------------------------------------
# Android Core & FileProvider
# -----------------------------------------------------------------------
-keep public class * extends androidx.core.content.FileProvider
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
