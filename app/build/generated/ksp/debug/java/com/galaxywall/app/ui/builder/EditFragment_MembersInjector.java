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
public final class EditFragment_MembersInjector implements MembersInjector<EditFragment> {
  private final Provider<SettingsManager> settingsManagerProvider;

  public EditFragment_MembersInjector(Provider<SettingsManager> settingsManagerProvider) {
    this.settingsManagerProvider = settingsManagerProvider;
  }

  public static MembersInjector<EditFragment> create(
      Provider<SettingsManager> settingsManagerProvider) {
    return new EditFragment_MembersInjector(settingsManagerProvider);
  }

  @Override
  public void injectMembers(EditFragment instance) {
    injectSettingsManager(instance, settingsManagerProvider.get());
  }

  @InjectedFieldSignature("com.galaxywall.app.ui.builder.EditFragment.settingsManager")
  public static void injectSettingsManager(EditFragment instance, SettingsManager settingsManager) {
    instance.settingsManager = settingsManager;
  }
}
