package me.bristermitten.mittenlib.config.paths;

import com.google.inject.Inject;
import java.nio.file.Path;
import me.bristermitten.mittenlib.util.Result;
import org.bukkit.plugin.Plugin;

/** A {@link ConfigPathResolver} that resolves paths relative to a {@link Plugin}'s data folder */
public class PluginConfigPathResolver implements ConfigPathResolver {
    private static final ThreadLocal<Path> ACTIVE_PATH = new ThreadLocal<>();

    public static void setActivePath(Path path) {
        ACTIVE_PATH.set(path);
    }

    public static void clearActivePath() {
        ACTIVE_PATH.remove();
    }

    private final Plugin plugin;

    @Inject
    public PluginConfigPathResolver(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Result<Path> getConfigPath(String configFileName) {
        Path active = ACTIVE_PATH.get();
        if (active != null) {
            return Result.ok(active.resolve(configFileName));
        }
        return Result.ok(plugin.getDataFolder().toPath().resolve(configFileName));
    }
}
