# Keep kotlinx.serialization generated serializers for our model classes.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.enough.app.**$$serializer { *; }
-keepclassmembers class com.enough.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.enough.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
