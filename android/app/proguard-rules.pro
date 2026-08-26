# Keep Room's generated implementations and entity constructors.
-keep class ir.tivan.controller.data.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { public *; }
-dontwarn androidx.room.paging.**

# Kotlin coroutines internals referenced reflectively.
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**

# Compose keeps its own rules via consumer files; silence AndroidX warnings.
-dontwarn androidx.compose.**
