package com.galaxywall.app.ui.builder;

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
public final class OverlayFragment_MembersInjector implements MembersInjector<OverlayFragment> {
  private final Provider<SettingsManager> settingsManagerProvider;

  public OverlayFragment_MembersInjector(Provider<SettingsManager> settingsManagerProvider) {
    this.settingsManagerProvider = settingsManagerProvider;
  }

  public static MembersInjector<OverlayFragment> create(
      Provider<SettingsManager> settingsManagerProvider) {
    return new OverlayFragment_MembersInjector(settingsManagerProvider);
  }

  @Override
  public void injectMembers(OverlayFragment instance) {
    injectSettingsManager(instance, settingsManagerProvider.get());
  }

  @InjectedFieldSignature("com.galaxywall.app.ui.builder.OverlayFragment.settingsManager")
  public static void injectSettingsManager(OverlayFragment instance,
      SettingsManager settingsManager) {
    instance.settingsManager = settingsManager;
  }
}
