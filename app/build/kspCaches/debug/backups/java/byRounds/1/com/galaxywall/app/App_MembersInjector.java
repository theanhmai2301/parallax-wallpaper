package com.galaxywall.app;

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
public final class App_MembersInjector implements MembersInjector<App> {
  private final Provider<SettingsManager> settingsManagerProvider;

  public App_MembersInjector(Provider<SettingsManager> settingsManagerProvider) {
    this.settingsManagerProvider = settingsManagerProvider;
  }

  public static MembersInjector<App> create(Provider<SettingsManager> settingsManagerProvider) {
    return new App_MembersInjector(settingsManagerProvider);
  }

  @Override
  public void injectMembers(App instance) {
    injectSettingsManager(instance, settingsManagerProvider.get());
  }

  @InjectedFieldSignature("com.galaxywall.app.App.settingsManager")
  public static void injectSettingsManager(App instance, SettingsManager settingsManager) {
    instance.settingsManager = settingsManager;
  }
}
