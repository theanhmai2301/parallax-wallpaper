package com.galaxywall.app.ui.home;

import com.galaxywall.app.data.repository.WallpaperRepository;
import com.galaxywall.app.util.NetworkMonitor;
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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<WallpaperRepository> repositoryProvider;

  private final Provider<NetworkMonitor> networkMonitorProvider;

  public HomeViewModel_Factory(Provider<WallpaperRepository> repositoryProvider,
      Provider<NetworkMonitor> networkMonitorProvider) {
    this.repositoryProvider = repositoryProvider;
    this.networkMonitorProvider = networkMonitorProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(repositoryProvider.get(), networkMonitorProvider.get());
  }

  public static HomeViewModel_Factory create(Provider<WallpaperRepository> repositoryProvider,
      Provider<NetworkMonitor> networkMonitorProvider) {
    return new HomeViewModel_Factory(repositoryProvider, networkMonitorProvider);
  }

  public static HomeViewModel newInstance(WallpaperRepository repository,
      NetworkMonitor networkMonitor) {
    return new HomeViewModel(repository, networkMonitor);
  }
}
