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

# ============================================================
# Part 2 Device Diagnostics - Firebase & Firestore Keep Rules
# ============================================================

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-keep class com.google.firebase.auth.** { *; }
-keep class com.google.firebase.firestore.** { *; }
-keep class com.google.firebase.storage.** { *; }

# Keep Firestore internal classes
-keep class com.google.android.gms.internal.firestore.** { *; }

# Keep Google Play Services classes
-keep class com.google.android.gms.** { *; }

# Keep Kotlin coroutines
-keep class kotlinx.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** { *; }

# Keep our data models and repositories
-keep class com.university.campuscare.data.model.ClientProfile { *; }
-keep class com.university.campuscare.data.repository.ClientProfileRepository { *; }
-keep class com.university.campuscare.utils.ClientProfileHelper { *; }

# Keep helper methods for reflection
-keepclassmembers class com.university.campuscare.data.model.ClientProfile {
    public <methods>;
    public <fields>;
}

# Keep AndroidViewModel and LifecycleOwner for ViewModel support
-keep class androidx.lifecycle.** { *; }
-keep class androidx.lifecycle.viewmodel.** { *; }