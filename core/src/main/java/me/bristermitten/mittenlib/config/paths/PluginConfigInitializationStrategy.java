package me.bristermitten.mittenlib.config.paths;

import me.bristermitten.mittenlib.util.PathUtil;
import me.bristermitten.mittenlib.util.Result;
import me.bristermitten.mittenlib.util.Unit;
import org.bukkit.plugin.Plugin;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A {@link ConfigInitializationStrategy} that copies a resource from a plugin's jar to its data folder if it does not exist
 */
public class PluginConfigInitializationStrategy implements ConfigInitializationStrategy {
    private final Plugin plugin;

    @Inject
    public PluginConfigInitializationStrategy(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Result<Unit> initializeConfig(String filePath) {
        final Path dataFolder = plugin.getDataFolder().toPath();
        final Path inDataFolder = dataFolder.resolve(filePath);
        if (Files.exists(inDataFolder)) {
            return Unit.unitResult();
        }
        try {
            final URL resource = plugin.getClass().getClassLoader().getResource(filePath);
            if (resource == null) {
                return Result.fail(
                        new UnknownResourceException("Could not find resource " + filePath + " in plugin " + plugin.getName())
                );
            }
            final Path inJar = PathUtil.resourceToPath(resource);

            Files.createDirectories(inDataFolder.getParent());
            Files.copy(inJar, inDataFolder);
        } catch (IOException | URISyntaxException e) {
            return Result.fail(e);
        }
        return Unit.unitResult();
    }

    /**
     * Thrown when a resource could not be found in a plugin
     */
    public static class UnknownResourceException extends RuntimeException {
        /**
         * @param message the message
         */
        public UnknownResourceException(String message) {
            super(message);
        }
    }
}
