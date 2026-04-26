# ProGuard rules for Mnemora

# ViewModel
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keepclassmembers class * extends androidx.lifecycle.AndroidViewModel {
    <init>(...);
}

# Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }
-keep class * extends dagger.hilt.android.internal.managers.ActivityComponentManager { *; }
-keepclassmembers @dagger.hilt.android.HiltAndroidApp class * {
    dagger.hilt.android.internal.managers.ActivityComponentManager componentManager;
}

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontwarn kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keepclassmembers class * implements kotlinx.serialization.Serializable {
    <fields>;
}
-keepclassmembers class com.hihusky.mnemora.data.model.** { *; }
-keepclassmembers class com.hihusky.mnemora.data.local.db.entity.** { *; }

# OkHttp
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Coil
-keep public class * implements coil.decode.Decoder { *; }
-keep public class * implements coil.fetch.Fetcher { *; }

# General Compose
-keep class androidx.compose.material.icons.** { *; }
