# Keep Room's generated implementations and entity constructors.
-keep class ir.tivan.controller.data.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { public *; }
-dontwarn androidx.room.paging.**

# Kotlin coroutines internals referenced reflectively.
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**

# Compose keeps its own rules via consumer files; silence AndroidX warnings.
-dontwarn androidx.compose.**

# The embedded sherpa-onnx TTS engine (libsherpa-onnx-jni.so) binds to these
# Kotlin classes by exact name/field/signature via JNI — R8 renaming or
# stripping anything here without a keep rule crashes with a bare
# NoSuchMethodError/UnsatisfiedLinkError (an Error, not an Exception) the
# instant TivanSpeaker tries to load the model, i.e. on every app launch.
-keep class com.k2fsa.sherpa.onnx.** { *; }
-keepclassmembers class com.k2fsa.sherpa.onnx.** { *; }
-dontwarn com.k2fsa.sherpa.onnx.**
