package com.galaxywall.app.data.local;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class AssetCatalogSource_Factory implements Factory<AssetCatalogSource> {
  private final Provider<Context> contextProvider;

  public AssetCatalogSource_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public AssetCatalogSource get() {
    return newInstance(contextProvider.get());
  }

  public static AssetCatalogSource_Factory create(Provider<Context> contextProvider) {
    return new AssetCatalogSource_Factory(contextProvider);
  }

  public static AssetCatalogSource newInstance(Context context) {
    return new AssetCatalogSource(context);
  }
}
