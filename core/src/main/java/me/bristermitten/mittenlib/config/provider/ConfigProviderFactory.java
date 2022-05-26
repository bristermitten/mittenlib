package me.bristermitten.mittenlib.config.provider;

import me.bristermitten.mittenlib.config.Configuration;
import org.jetbrains.annotations.NotNull;

/**
 * Creates {@link ConfigProvider}s for {@link Configuration}s
 */
public interface ConfigProviderFactory {
    @NotNull <T> ConfigProvider<T> createProvider(@NotNull Configuration<T> configuration);
}
