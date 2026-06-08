package me.bristermitten.mittenlib.config.provider.construct;

import me.bristermitten.mittenlib.config.provider.ConfigProvider;

/**
 * Improves a {@link ConfigProvider} by wrapping it with (possible) extra functionality. This could
 * include caching, auto reloading, etc. The exact functionality is up to the implementation.
 */
public interface ConfigProviderImprover {
    /**
     * Improve a {@link ConfigProvider} by wrapping it with (possible) extra functionality. This
     * method should be pure and idempotent where possible. If there are no improvements to be made,
     * the original {@link ConfigProvider} should be returned
     *
     * @param provider the provider to improve
     * @param <T>      the type of the config
     * @return an improved provider
     */
    <T> ConfigProvider<T> improve(ConfigProvider<T> provider);
}
