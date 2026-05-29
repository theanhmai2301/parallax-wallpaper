package com.galaxywall.app.data.remote;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
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
public final class RemoteWallpaperSource_Factory implements Factory<RemoteWallpaperSource> {
  private final Provider<WallpaperApi> apiProvider;

  public RemoteWallpaperSource_Factory(Provider<WallpaperApi> apiProvider) {
    this.apiProvider = apiProvider;
  }

  @Override
  public RemoteWallpaperSource get() {
    return newInstance(apiProvider.get());
  }

  public static RemoteWallpaperSource_Factory create(Provider<WallpaperApi> apiProvider) {
    return new RemoteWallpaperSource_Factory(apiProvider);
  }

  public static RemoteWallpaperSource newInstance(WallpaperApi api) {
    return new RemoteWallpaperSource(api);
  }
}
