# ──────────────────────────────────────────────
# Google API Client + Drive
# ──────────────────────────────────────────────

# Keep all Google API Client classes (Drive, Auth, HTTP)
-keep class com.google.api.** { *; }
-keep class com.google.api.client.** { *; }
-keep class com.google.api.services.drive.** { *; }
-keep class com.google.api.services.drive.model.** { *; }

# Google Auth Library
-keep class com.google.auth.** { *; }
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.common.** { *; }

# HTTP Transport
-keep class com.google.api.client.http.** { *; }
-keep class com.google.api.client.json.** { *; }
-keep class com.google.api.client.json.gson.** { *; }
-keep class com.google.api.client.util.** { *; }

# Gson (used by GsonFactory)
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Prevent R8 from removing abstract classes used via reflection
-keepclassmembers class * extends com.google.api.client.json.GenericJson {
    <fields>;
    <init>(...);
}
-keep class * implements com.google.api.client.http.HttpTransport { *; }
-keep class * implements com.google.api.client.json.JsonFactory { *; }
-keep class * implements com.google.api.client.json.JsonObjectParser { *; }

# ──────────────────────────────────────────────
# Hilt / Dagger
# ──────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }

# ──────────────────────────────────────────────
# Room
# ──────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# ──────────────────────────────────────────────
# Kotlin Serialization
# ──────────────────────────────────────────────
-keepattributes RuntimeVisibleAnnotations
-keep class kotlinx.serialization.** { *; }
-keepclassmembers @kotlinx.serialization.Serializable class * { *; }

# ──────────────────────────────────────────────
# General Android
# ──────────────────────────────────────────────
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ──────────────────────────────────────────────
# Apache HttpClient (used by google-api-client)
# These classes don't exist on Android — tell R8 to ignore them
# ──────────────────────────────────────────────
-dontwarn javax.naming.**
-dontwarn org.ietf.jgss.**
-dontwarn org.apache.http.**
-dontwarn android.net.http.AndroidHttpClient
-dontwarn com.google.android.gms.internal.**

# ──────────────────────────────────────────────
# Google API Client internals
# ──────────────────────────────────────────────
-dontwarn com.google.api.client.extensions.android.**
-dontwarn com.google.api.client.googleapis.extensions.android.**
