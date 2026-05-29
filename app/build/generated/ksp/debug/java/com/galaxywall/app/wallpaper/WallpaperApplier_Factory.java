package com.galaxywall.app.wallpaper;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class WallpaperApplier_Factory implements Factory<WallpaperApplier> {
  private final Provider<Context> contextProvider;

  public WallpaperApplier_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public WallpaperApplier get() {
    return newInstance(contextProvider.get());
  }

  public static WallpaperApplier_Factory create(Provider<Context> contextProvider) {
    return new WallpaperApplier_Factory(contextProvider);
  }

  public static WallpaperApplier newInstance(Context context) {
    return new WallpaperApplier(context);
  }
}
