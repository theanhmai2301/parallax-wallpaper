package com.galaxywall.app.data.repository;

import com.galaxywall.app.data.local.WallpaperDao;
import com.galaxywall.app.data.remote.RemoteWallpaperSource;
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
public final class WallpaperRepository_Factory implements Factory<WallpaperRepository> {
  private final Provider<RemoteWallpaperSource> remoteProvider;

  private final Provider<WallpaperDao> daoProvider;

  public WallpaperRepository_Factory(Provider<RemoteWallpaperSource> remoteProvider,
      Provider<WallpaperDao> daoProvider) {
    this.remoteProvider = remoteProvider;
    this.daoProvider = daoProvider;
  }

  @Override
  public WallpaperRepository get() {
    return newInstance(remoteProvider.get(), daoProvider.get());
  }

  public static WallpaperRepository_Factory create(Provider<RemoteWallpaperSource> remoteProvider,
      Provider<WallpaperDao> daoProvider) {
    return new WallpaperRepository_Factory(remoteProvider, daoProvider);
  }

  public static WallpaperRepository newInstance(RemoteWallpaperSource remote, WallpaperDao dao) {
    return new WallpaperRepository(remote, dao);
  }
}
