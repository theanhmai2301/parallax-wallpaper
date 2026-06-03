# Keep data models used by Room and JSON catalog parsing
-keep class com.galaxywall.app.data.model.** { *; }

# Hilt / Dagger generated code
-dontwarn dagger.hilt.**

# Coroutines
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# WallpaperService entry point referenced from manifest/XML
-keep class com.galaxywall.app.wallpaper.ParallaxWallpaperService { *; }

# Custom views referenced from XML
-keep class com.galaxywall.app.ui.customview.** { *; }

# Keep network DTOs: Gson maps JSON keys to Kotlin field names by reflection,
# and these have no @SerializedName, so their names must survive R8 minify
# (otherwise /api/* responses parse to null and Home/Category stay empty).
-keep class com.galaxywall.app.data.remote.** { *; }

# Gson type adapters / generic signatures (reflection-based serialization)
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses
