package com.galaxywall.app.ui.home;

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
public final class HomeFragment_MembersInjector implements MembersInjector<HomeFragment> {
  private final Provider<NetworkMonitor> networkMonitorProvider;

  public HomeFragment_MembersInjector(Provider<NetworkMonitor> networkMonitorProvider) {
    this.networkMonitorProvider = networkMonitorProvider;
  }

  public static MembersInjector<HomeFragment> create(
      Provider<NetworkMonitor> networkMonitorProvider) {
    return new HomeFragment_MembersInjector(networkMonitorProvider);
  }

  @Override
  public void injectMembers(HomeFragment instance) {
    injectNetworkMonitor(instance, networkMonitorProvider.get());
  }

  @InjectedFieldSignature("com.galaxywall.app.ui.home.HomeFragment.networkMonitor")
  public static void injectNetworkMonitor(HomeFragment instance, NetworkMonitor networkMonitor) {
    instance.networkMonitor = networkMonitor;
  }
}
