package me.bristermitten.mittenlib.config.paths;

import com.google.inject.Inject;
import me.bristermitten.mittenlib.config.GeneratedConfig;
import me.bristermitten.mittenlib.util.PathUtil;
import me.bristermitten.mittenlib.util.Result;
import me.bristermitten.mittenlib.util.Unit;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A {@link ConfigInitializationStrategy} that copies a resource from a plugin's jar to its data
 * folder if it does not exist
 */
public class PluginConfigInitializationStrategy implements ConfigInitializationStrategy {
    private final Plugin plugin;

    @Inject
    public PluginConfigInitializationStrategy(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    @Deprecated
    public Result<Unit> initializeConfig(String filePath) {
        return initializeConfig(filePath, Object.class);
    }

    @Override
    public <T> Result<Unit> initializeConfig(String filePath, Class<T> configClass) {
        final Path dataFolder = plugin.getDataFolder().toPath();
        final Path inDataFolder = dataFolder.resolve(filePath);
        if (Files.exists(inDataFolder)) {
            return Unit.unitResult();
        }
        try {
            final URL resource = plugin.getClass().getClassLoader().getResource(filePath);
            if (resource == null) { // jar resource doesn't exist
                final GeneratedConfig annotation = configClass.getAnnotation(GeneratedConfig.class);
                if (annotation != null && annotation.isDynamicallyInitializable()) {
                    Files.createDirectories(inDataFolder.getParent());
                    return Unit.unitResult();
                }

                String message = "Could not find resource " + filePath + " in plugin " + plugin.getName();
                if (annotation != null && annotation.uninitializableProperties().length > 0) {
                    message +=
                            ". The config type "
                                    + configClass.getName()
                                    + " is not dynamically initializable because the following required properties lack default values: "
                                    + String.join(", ", annotation.uninitializableProperties())
                                    + ". Either provide a default config file in your jar's resources at '"
                                    + filePath
                                    + "', or add default values to these properties.";
                } else if (annotation != null) {
                    message +=
                            ". The config type "
                                    + configClass.getName()
                                    + " is not dynamically initializable. "
                                    + "Either provide a default config file in your jar's resources at '"
                                    + filePath
                                    + "', or ensure all required properties have default values.";
                }

                return Result.fail(new UnknownResourceException(message));
            }

            Files.createDirectories(inDataFolder.getParent());
            PathUtil.resourceToPath(
                    resource,
                    inJar -> {
                        Files.copy(inJar, inDataFolder);
                        return null;
                    });

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
