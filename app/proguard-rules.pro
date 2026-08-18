# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class com.rahmatsobrian.sirohaequ.data.model.** {
    *** Companion;
}
-keepclassmembers class com.rahmatsobrian.sirohaequ.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.rahmatsobrian.sirohaequ.**$$serializer { *; }
-keepclassmembers class com.rahmatsobrian.sirohaequ.** {
    *** Companion;
}
-keepclasseswithmembers class com.rahmatsobrian.sirohaequ.** {
    kotlinx.serialization.KSerializer serializer(...);
}
