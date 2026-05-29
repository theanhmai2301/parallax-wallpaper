package com.galaxywall.app.ui.favorite;

import com.galaxywall.app.data.repository.WallpaperRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class FavoriteViewModel_Factory implements Factory<FavoriteViewModel> {
  private final Provider<WallpaperRepository> repositoryProvider;

  public FavoriteViewModel_Factory(Provider<WallpaperRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public FavoriteViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static FavoriteViewModel_Factory create(Provider<WallpaperRepository> repositoryProvider) {
    return new FavoriteViewModel_Factory(repositoryProvider);
  }

  public static FavoriteViewModel newInstance(WallpaperRepository repository) {
    return new FavoriteViewModel(repository);
  }
}
