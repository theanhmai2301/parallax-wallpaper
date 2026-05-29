package com.galaxywall.app.ui.builder;

import android.content.Context;
import com.galaxywall.app.data.repository.WallpaperRepository;
import com.galaxywall.app.wallpaper.WallpaperApplier;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class BuilderViewModel_Factory implements Factory<BuilderViewModel> {
  private final Provider<Context> contextProvider;

  private final Provider<WallpaperRepository> repositoryProvider;

  private final Provider<WallpaperApplier> applierProvider;

  public BuilderViewModel_Factory(Provider<Context> contextProvider,
      Provider<WallpaperRepository> repositoryProvider,
      Provider<WallpaperApplier> applierProvider) {
    this.contextProvider = contextProvider;
    this.repositoryProvider = repositoryProvider;
    this.applierProvider = applierProvider;
  }

  @Override
  public BuilderViewModel get() {
    return newInstance(contextProvider.get(), repositoryProvider.get(), applierProvider.get());
  }

  public static BuilderViewModel_Factory create(Provider<Context> contextProvider,
      Provider<WallpaperRepository> repositoryProvider,
      Provider<WallpaperApplier> applierProvider) {
    return new BuilderViewModel_Factory(contextProvider, repositoryProvider, applierProvider);
  }

  public static BuilderViewModel newInstance(Context context, WallpaperRepository repository,
      WallpaperApplier applier) {
    return new BuilderViewModel(context, repository, applier);
  }
}
