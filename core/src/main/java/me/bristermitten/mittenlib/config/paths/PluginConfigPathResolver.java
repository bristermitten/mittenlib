package me.bristermitten.mittenlib.config.paths;

import com.google.inject.Inject;
import me.bristermitten.mittenlib.util.Result;
import org.bukkit.plugin.Plugin;

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
    public Result<Path> getConfigPath(String configFileName) {
        return Result.ok(plugin.getDataFolder().toPath().resolve(configFileName));
  }
}
