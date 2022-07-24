package me.bristermitten.mittenlib.config.provider.construct;

import me.bristermitten.mittenlib.config.provider.ConfigProvider;

/**
 * Improves a {@link ConfigProvider} by wrapping it with (possible) extra functionality.
 * This could include caching, auto reloading, etc.
 */
public interface ConfigProviderImprover {
    <T> ConfigProvider<T> improve(ConfigProvider<T> provider);
}
