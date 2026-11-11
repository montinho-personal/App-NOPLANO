# kotlinx.serialization (backup em JSON)
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class app.noplano.** { *** Companion; }
-keepclasseswithmembers class app.noplano.** { kotlinx.serialization.KSerializer serializer(...); }
