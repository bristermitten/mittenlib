package me.bristermitten.mittenlib.config.paths;

import org.bukkit.plugin.Plugin;

import javax.inject.Inject;
import java.nio.file.Path;

/**
 * A {@link ConfigPathResolver} that resolves paths relative to a {@link Plugin}'s data folder
 */
public class PluginConfigPathResolver implements ConfigPathResolver {
    private final Plugin plugin;

    @Inject
    public PluginConfigPathResolver(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Path getConfigPath(String configFileName) {
        return plugin.getDataFolder().toPath().resolve(configFileName);
    }
}
