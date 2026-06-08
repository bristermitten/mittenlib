package me.bristermitten.mittenlib.config;

import com.google.inject.AbstractModule;
import me.bristermitten.mittenlib.config.paths.ConfigInitializationStrategy;
import me.bristermitten.mittenlib.config.paths.ConfigPathResolver;
import me.bristermitten.mittenlib.config.paths.PluginConfigInitializationStrategy;
import me.bristermitten.mittenlib.config.paths.PluginConfigPathResolver;

/**
 * Overrides some elements of {@link ConfigInfrastructureModule} to provide more plugin-specific
 * implementations.
 */
public class PluginConfigModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ConfigInitializationStrategy.class).to(PluginConfigInitializationStrategy.class);
        bind(ConfigPathResolver.class).to(PluginConfigPathResolver.class);
    }
}
