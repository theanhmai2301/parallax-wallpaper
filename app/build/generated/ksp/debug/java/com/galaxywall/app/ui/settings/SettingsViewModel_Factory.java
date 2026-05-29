package com.galaxywall.app.ui.settings;

import com.galaxywall.app.data.local.SettingsManager;
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<SettingsManager> settingsManagerProvider;

  public SettingsViewModel_Factory(Provider<SettingsManager> settingsManagerProvider) {
    this.settingsManagerProvider = settingsManagerProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(settingsManagerProvider.get());
  }

  public static SettingsViewModel_Factory create(
      Provider<SettingsManager> settingsManagerProvider) {
    return new SettingsViewModel_Factory(settingsManagerProvider);
  }

  public static SettingsViewModel newInstance(SettingsManager settingsManager) {
    return new SettingsViewModel(settingsManager);
  }
}
