# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Gson / models
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.appsbay.chineseclassicalliteratural.Model.** { *; }
-keep class com.appsbay.chineseclassicalliteratural.data.** { *; }

# Parcelable CREATORs
-keepclassmembers class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# Enums used by Parcelable Book
-keepclassmembers enum * {
  public static **[] values();
  public static ** valueOf(java.lang.String);
}

# Play Billing
-keep class com.android.vending.billing.** { *; }
-keep class com.android.billingclient.** { *; }

# Google Mobile Ads / mediation
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.mediation.** { *; }
-dontwarn com.google.android.gms.ads.**

# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Picasso
-dontwarn com.squareup.okhttp.**
-keep class com.squareup.picasso.** { *; }

# ShineButton
-keep class com.sackcentury.shinebuttonlib.** { *; }

# Kotlin
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings {
  <fields>;
}
-keepclassmembers class kotlin.Metadata {
  public <methods>;
}

# ViewModels referenced by class name
-keep class * extends androidx.lifecycle.ViewModel {
  <init>(android.app.Application);
  <init>();
}

# Firebase
-dontwarn com.google.firebase.**

# Room
# Room loads the generated <Database>_Impl class reflectively by name, so R8 must
# not remove it. Reached here through androidx.work, which play-services-ads pulls
# in transitively: without this, WorkManagerInitializer crashes the app on launch.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**
