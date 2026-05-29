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
public final class ResultFragment_MembersInjector implements MembersInjector<ResultFragment> {
  private final Provider<SettingsManager> settingsManagerProvider;

  public ResultFragment_MembersInjector(Provider<SettingsManager> settingsManagerProvider) {
    this.settingsManagerProvider = settingsManagerProvider;
  }

  public static MembersInjector<ResultFragment> create(
      Provider<SettingsManager> settingsManagerProvider) {
    return new ResultFragment_MembersInjector(settingsManagerProvider);
  }

  @Override
  public void injectMembers(ResultFragment instance) {
    injectSettingsManager(instance, settingsManagerProvider.get());
  }

  @InjectedFieldSignature("com.galaxywall.app.ui.builder.ResultFragment.settingsManager")
  public static void injectSettingsManager(ResultFragment instance,
      SettingsManager settingsManager) {
    instance.settingsManager = settingsManager;
  }
}
