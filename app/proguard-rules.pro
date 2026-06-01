# kotlinx.serialization keeps generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

# Keep serializer() companion methods for all @Serializable classes.
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class io.github.duminandrew.obsidiangitsync.**$$serializer { *; }
-keepclassmembers class io.github.duminandrew.obsidiangitsync.** {
    *** Companion;
}
-keepclasseswithmembers class io.github.duminandrew.obsidiangitsync.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit / OkHttp.
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes Signature, Exceptions
