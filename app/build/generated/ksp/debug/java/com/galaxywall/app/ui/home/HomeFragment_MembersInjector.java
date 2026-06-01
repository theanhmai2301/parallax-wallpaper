package com.galaxywall.app.ui.home;

import com.galaxywall.app.data.local.SettingsManager;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class HomeFragment_MembersInjector implements MembersInjector<HomeFragment> {
  private final Provider<SettingsManager> settingsManagerProvider;

  public HomeFragment_MembersInjector(Provider<SettingsManager> settingsManagerProvider) {
    this.settingsManagerProvider = settingsManagerProvider;
  }

  public static MembersInjector<HomeFragment> create(
      Provider<SettingsManager> settingsManagerProvider) {
    return new HomeFragment_MembersInjector(settingsManagerProvider);
  }

  @Override
  public void injectMembers(HomeFragment instance) {
    injectSettingsManager(instance, settingsManagerProvider.get());
  }

  @InjectedFieldSignature("com.galaxywall.app.ui.home.HomeFragment.settingsManager")
  public static void injectSettingsManager(HomeFragment instance, SettingsManager settingsManager) {
    instance.settingsManager = settingsManager;
  }
}
