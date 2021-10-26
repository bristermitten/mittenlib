package me.bristermitten.mittenlib.config;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import me.bristermitten.mittenlib.config.provider.ConfigProvider;
import me.bristermitten.mittenlib.config.provider.LazyConfigProvider;
import me.bristermitten.mittenlib.config.reader.ObjectLoader;
import me.bristermitten.mittenlib.config.reader.SearchingObjectLoader;
import me.bristermitten.mittenlib.util.CompositeType;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ConfigModule extends AbstractModule {
    private final Set<Configuration<?>> configurations;

    public ConfigModule(Set<Configuration<?>> configurations) {
        this.configurations = configurations;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void configure() {
        bind(ObjectLoader.class).to(SearchingObjectLoader.class);
        configurations.stream()
                .collect(Collectors.toMap(Function.identity(), LazyConfigProvider::new))
                .forEach((configuration, provider) -> {
                    final Class<?> key = configuration.getType();
                    // beware of evil generic type erasure hell
                    bind((Class<? super Object>) key).toProvider(provider); // bind the type itself, T to Provider<T>

                    // bind the provider to its instance
                    final TypeLiteral<ConfigProvider<?>> providerType =
                            (TypeLiteral<ConfigProvider<?>>) TypeLiteral.get(new CompositeType(ConfigProvider.class, key));
                    bind(providerType).toInstance(provider);
                });
    }
}
