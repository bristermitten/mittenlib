package me.bristermitten.mittenlib.config.provider.construct;

import me.bristermitten.mittenlib.config.provider.ConfigProvider;

/**
 * A {@link ConfigProviderImprover} that does nothing.
 */
public class NoOpConfigProviderImprover implements ConfigProviderImprover {

    @Override
    public <T> ConfigProvider<T> improve(ConfigProvider<T> provider) {
        return provider;
    }
}
