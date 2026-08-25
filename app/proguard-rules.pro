-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keep,includedescriptorclasses class com.riddle.diary.**$$serializer { *; }
-keepclassmembers class com.riddle.diary.** {
    *** Companion;
}
-keepclasseswithmembers class com.riddle.diary.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-dontwarn okhttp3.**
-dontwarn okio.**
