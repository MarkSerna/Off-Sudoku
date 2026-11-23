# Preserve Kotlin Serialization metadata and generated serializers for type-safe routes and snapshot JSON
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class com.logicminigames.sudoku.navigation.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.logicminigames.sudoku.navigation.**$$serializer { *; }

-keepclassmembers class com.logicminigames.core.data.logic.BoardStateSerializer$SerializedSnapshot* {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.logicminigames.core.data.logic.BoardStateSerializer$SerializedSnapshot$$serializer { *; }

# Strip verbose debug logs in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
