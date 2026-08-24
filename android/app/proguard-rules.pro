# Family sideload builds currently ship without minify. Rules kept for a future shrink.
-keepattributes *Annotation*, InnerClasses
-dontwarn kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keep @kotlinx.serialization.Serializable class com.zekkers.watthome.** { *; }
