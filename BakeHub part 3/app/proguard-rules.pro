# --- BakeHub release (R8) rules ---

# Gson / Retrofit reflect over these model classes, so their field names must survive shrinking.
-keep class com.bakehub.app.data.remote.** { *; }
-keep class com.bakehub.app.data.Recipe { *; }
-keep class com.bakehub.app.data.Ingredient { *; }
-keep class com.bakehub.app.data.RecipeStep { *; }
-keep class com.bakehub.app.data.Nutrition { *; }
-keep class com.bakehub.app.data.Difficulty { *; }

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Keep line numbers for readable Play Console crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
