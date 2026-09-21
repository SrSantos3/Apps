# Proguard rules for FocusZen
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
