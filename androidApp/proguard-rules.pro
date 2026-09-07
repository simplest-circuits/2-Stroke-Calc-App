# Add project specific ProGuard rules here.
# Libraries (Hilt, Retrofit, Firebase, Compose, kotlinx.serialization) ship consumer rules.

-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Gson uses reflection on field / enum names for persisted and catalog models.
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

-keepclassmembers class com.simplestsoft.twostrokecalc.domain.model.** { <fields>; }
-keepclassmembers enum com.simplestsoft.twostrokecalc.domain.model.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepclassmembers class com.simplestsoft.twostrokecalc.data.vehicles.ReminderPersistedState { <fields>; }
