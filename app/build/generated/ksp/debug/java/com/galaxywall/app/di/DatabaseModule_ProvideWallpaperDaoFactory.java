package com.galaxywall.app.di;

import com.galaxywall.app.data.local.AppDatabase;
import com.galaxywall.app.data.local.WallpaperDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class DatabaseModule_ProvideWallpaperDaoFactory implements Factory<WallpaperDao> {
  private final Provider<AppDatabase> databaseProvider;

  public DatabaseModule_ProvideWallpaperDaoFactory(Provider<AppDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public WallpaperDao get() {
    return provideWallpaperDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideWallpaperDaoFactory create(
      Provider<AppDatabase> databaseProvider) {
    return new DatabaseModule_ProvideWallpaperDaoFactory(databaseProvider);
  }

  public static WallpaperDao provideWallpaperDao(AppDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideWallpaperDao(database));
  }
}
