package me.bristermitten.mittenlib.config.paths;

import me.bristermitten.mittenlib.util.PathUtil;
import org.bukkit.plugin.Plugin;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class PluginConfigInitializationStrategy implements ConfigInitializationStrategy {
    private final Plugin plugin;

    @Inject
    public PluginConfigInitializationStrategy(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initializeConfig(String filePath) {
        final Path dataFolder = plugin.getDataFolder().toPath();
        final Path inDataFolder = dataFolder.resolve(filePath);
        if (Files.exists(inDataFolder)) {
            return; // nothing to do
        }
        try {
            final URL resource = plugin.getClass().getClassLoader().getResource(filePath);
            Objects.requireNonNull(resource, "Could not find " + filePath + " in jar for " + plugin);

            final Path inJar = PathUtil.resourceToPath(resource);

            Files.createDirectories(dataFolder);
            Files.copy(inJar, inDataFolder);
        } catch (IOException | URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
