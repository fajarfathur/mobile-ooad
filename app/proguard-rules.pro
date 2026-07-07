# Simpan model @Serializable agar kotlinx-serialization tetap bekerja saat minify.
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.kelompok1.simo.**$$serializer { *; }
-keepclassmembers class com.kelompok1.simo.** {
    *** Companion;
}
-keepclasseswithmembers class com.kelompok1.simo.** {
    kotlinx.serialization.KSerializer serializer(...);
}
