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
public final class PreviewFragment_MembersInjector implements MembersInjector<PreviewFragment> {
  private final Provider<SettingsManager> settingsManagerProvider;

  public PreviewFragment_MembersInjector(Provider<SettingsManager> settingsManagerProvider) {
    this.settingsManagerProvider = settingsManagerProvider;
  }

  public static MembersInjector<PreviewFragment> create(
      Provider<SettingsManager> settingsManagerProvider) {
    return new PreviewFragment_MembersInjector(settingsManagerProvider);
  }

  @Override
  public void injectMembers(PreviewFragment instance) {
    injectSettingsManager(instance, settingsManagerProvider.get());
  }

  @InjectedFieldSignature("com.galaxywall.app.ui.builder.PreviewFragment.settingsManager")
  public static void injectSettingsManager(PreviewFragment instance,
      SettingsManager settingsManager) {
    instance.settingsManager = settingsManager;
  }
}
