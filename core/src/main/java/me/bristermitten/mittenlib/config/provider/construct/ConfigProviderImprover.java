package me.bristermitten.mittenlib.config.provider.construct;

import me.bristermitten.mittenlib.config.provider.ConfigProvider;

/**
 * Improves a {@link ConfigProvider} by wrapping it with (possible) extra functionality.
 * This could include caching, auto reloading, etc.
 */
public interface ConfigProviderImprover {
    /**
     * Improve a {@link ConfigProvider} by wrapping it with (possible) extra functionality.
     * This method should be idempotent, and should not modify the original {@link ConfigProvider}
     * If there are no improvements to be made, the original {@link ConfigProvider} should be returned
     *
     * @param provider the provider to improve
     * @param <T>      the type of the config
     * @return the improved provider
     */
    <T> ConfigProvider<T> improve(ConfigProvider<T> provider);
}
