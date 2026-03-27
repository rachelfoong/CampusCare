# Add project specific ProGuard rules here.

# ============================================================
# Part 3 - Obfuscation Rules
# ============================================================

# Rename source file attributes to hide original file names
-renamesourcefileattribute x

# Remove debug information (line numbers, variable names)
-keepattributes Exceptions, InnerClasses, Signature, Deprecated, EnclosingMethod

# Aggressive obfuscation — use short meaningless names (a, b, c...)
-repackageclasses ''
-allowaccessmodification

# Remove logging calls to prevent information leakage
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# Hide obfuscation utility classes themselves
-keep class com.university.campuscare.remote.StringObfuscator { *; }
-keep class com.university.campuscare.remote.ObfuscatedStrings { *; }

# ============================================================
# Part 2 Device Diagnostics - Firebase & Firestore Keep Rules
# ============================================================

# Keep Firebase classes
#-keep class com.google.firebase.** { *; }
#-keep class com.google.firebase.auth.** { *; }
#-keep class com.google.firebase.firestore.** { *; }
#-keep class com.google.firebase.storage.** { *; }

# Keep Firestore internal classes
#-keep class com.google.android.gms.internal.firestore.** { *; }

# Keep Google Play Services classes
#-keep class com.google.android.gms.** { *; }

# Keep Kotlin coroutines
#-keep class kotlinx.coroutines.** { *; }
#-keepclassmembers class kotlinx.coroutines.** { *; }


# 1. Keep the Firebase annotations so mapping survives renaming
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,Signature

# 2. Prevent R8 from stripping the no-argument constructor from models
# This allows the class and fields to be renamed (e.g., Location -> ia.l)
# but keeps the constructor Firebase needs to instantiate the object.
-keepclassmembers class com.university.campuscare.data.model.** {
    public <init>();
}

# 3. Preserve PropertyName annotations for Firestore mapping
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}

# 4. Handle Enums (Important for IssueStatus, IssueUrgency, etc.)
# Keeps the values() and valueOf() methods which Firebase uses.
-keepclassmembers enum com.university.campuscare.data.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 5. WorkManager Support (Necessary for BackgroundImageUploadWorker)
# Workers are instantiated by the system using reflection.
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# 6. Aggressive Renaming
# This renames packages and classes to make the APK a "Black Box".
-overloadaggressively
-useuniqueclassmembernames
-repackageclasses 'com.university.campuscare.internal'
-allowaccessmodification

# Keep our data models and repositories
#-keep class com.university.campuscare.data.model.ClientProfile { *; }
#-keep class com.university.campuscare.data.repository.ClientProfileRepository { *; }
#-keep class com.university.campuscare.utils.ClientProfileHelper { *; }
#
## Keep helper methods for reflection
#-keepclassmembers class com.university.campuscare.data.model.ClientProfile {
#    public <methods>;
#    public <fields>;
#}

# Keep AndroidViewModel and LifecycleOwner for ViewModel support
-keep class androidx.lifecycle.** { *; }
-keep class androidx.lifecycle.viewmodel.** { *; }