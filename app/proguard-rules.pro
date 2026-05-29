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
