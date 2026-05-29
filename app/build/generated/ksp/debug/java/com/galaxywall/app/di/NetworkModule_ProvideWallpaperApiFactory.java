package com.galaxywall.app.di;

import com.galaxywall.app.data.remote.WallpaperApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import retrofit2.Retrofit;

@ScopeMetadata("javax.inject.Singleton")
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
public final class NetworkModule_ProvideWallpaperApiFactory implements Factory<WallpaperApi> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideWallpaperApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public WallpaperApi get() {
    return provideWallpaperApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideWallpaperApiFactory create(
      Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideWallpaperApiFactory(retrofitProvider);
  }

  public static WallpaperApi provideWallpaperApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideWallpaperApi(retrofit));
  }
}
