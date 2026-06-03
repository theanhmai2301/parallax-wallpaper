package com.galaxywall.app.ui.builder;

import com.galaxywall.app.data.local.SettingsManager;
import com.galaxywall.app.util.NetworkMonitor;
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

  private final Provider<NetworkMonitor> networkMonitorProvider;

  public ResultFragment_MembersInjector(Provider<SettingsManager> settingsManagerProvider,
      Provider<NetworkMonitor> networkMonitorProvider) {
    this.settingsManagerProvider = settingsManagerProvider;
    this.networkMonitorProvider = networkMonitorProvider;
  }

  public static MembersInjector<ResultFragment> create(
      Provider<SettingsManager> settingsManagerProvider,
      Provider<NetworkMonitor> networkMonitorProvider) {
    return new ResultFragment_MembersInjector(settingsManagerProvider, networkMonitorProvider);
  }

  @Override
  public void injectMembers(ResultFragment instance) {
    injectSettingsManager(instance, settingsManagerProvider.get());
    injectNetworkMonitor(instance, networkMonitorProvider.get());
  }

  @InjectedFieldSignature("com.galaxywall.app.ui.builder.ResultFragment.settingsManager")
  public static void injectSettingsManager(ResultFragment instance,
      SettingsManager settingsManager) {
    instance.settingsManager = settingsManager;
  }

  @InjectedFieldSignature("com.galaxywall.app.ui.builder.ResultFragment.networkMonitor")
  public static void injectNetworkMonitor(ResultFragment instance, NetworkMonitor networkMonitor) {
    instance.networkMonitor = networkMonitor;
  }
}
