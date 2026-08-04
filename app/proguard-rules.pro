# --- kotlinx.serialization -------------------------------------------------
# Keep generated serializers, Companions, and the synthetic serializer() accessors
# for our @Serializable model classes.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.enough.app.**$$serializer { *; }
-keepclassmembers class com.enough.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.enough.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# Enums serialized by @SerialName (e.g. DietaryTag) must keep their entries and
# valueOf/values, or R8 can strip/rename them and by-name (de)serialization breaks.
-keepclassmembers enum com.enough.app.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    *;
}

# --- Room ------------------------------------------------------------------
# Room generates its own DAO/database implementations; keep entities and the
# generated *_Impl classes intact so reflection-free codegen keeps resolving.
-keep class com.enough.app.data.local.** { *; }
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**
