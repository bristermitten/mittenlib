package me.bristermitten.mittenlib.config;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import me.bristermitten.mittenlib.config.provider.ConfigProvider;
import me.bristermitten.mittenlib.config.provider.DelegatingConfigProvider;
import me.bristermitten.mittenlib.util.CompositeType;

import java.util.Objects;
import java.util.Set;

/**
 * Guice module for binding configurations.
 */
public class ConfigDataModule extends AbstractModule {
    private final Set<Configuration<?>> configurations;

    public ConfigDataModule(Set<Configuration<?>> configurations) {
        this.configurations = configurations;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void configure() {
        Multibinder<Configuration<?>> configurationMultibinder =
                Multibinder.newSetBinder(binder(), new TypeLiteral<Configuration<?>>() {});

        Multibinder<ConfigProvider<?>> configProviderMultibinder =
                Multibinder.newSetBinder(binder(), new TypeLiteral<ConfigProvider<?>>() {});

        configurations.forEach(
                configuration -> {
                    final Class<?> key = configuration.getType();

                    // Bind Configuration<T> dynamically
                    final TypeLiteral<Configuration<?>> configType =
                            (TypeLiteral<Configuration<?>>)
                                    TypeLiteral.get(new CompositeType(Configuration.class, key));
                    bind(configType).toInstance(configuration);
                    configurationMultibinder.addBinding().toInstance(configuration);

                    // Bind ConfigProvider<T> to DelegatingConfigProvider<T> in Singleton scope
                    final TypeLiteral<ConfigProvider<?>> providerType =
                            (TypeLiteral<ConfigProvider<?>>)
                                    TypeLiteral.get(new CompositeType(ConfigProvider.class, key));
                    final TypeLiteral<DelegatingConfigProvider<?>> delegatingProviderType =
                            (TypeLiteral<DelegatingConfigProvider<?>>)
                                    TypeLiteral.get(new CompositeType(DelegatingConfigProvider.class, key));

                    bind(providerType).to(delegatingProviderType).in(Singleton.class);
                    configProviderMultibinder.addBinding().to(delegatingProviderType);

                    // Bind T to the provider key
                    bind((Class<? super Object>) key).toProvider(Key.get(providerType));
                });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConfigDataModule)) return false;
        ConfigDataModule that = (ConfigDataModule) o;
        return Objects.equals(configurations, that.configurations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(configurations);
  }
}
