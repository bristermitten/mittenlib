package me.bristermitten.mittenlib.config.provider;

import me.bristermitten.mittenlib.config.Configuration;
import me.bristermitten.mittenlib.config.paths.ConfigInitializationStrategy;
import me.bristermitten.mittenlib.config.paths.ConfigPathResolver;
import me.bristermitten.mittenlib.config.reader.ConfigReader;
import me.bristermitten.mittenlib.watcher.FileWatcherService;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.nio.file.Path;

public class SimpleConfigProviderFactory implements ConfigProviderFactory {

    private final ConfigReader reader;
    private final FileWatcherService watcherService;
    private final ConfigInitializationStrategy initializationStrategy;
    private final ConfigPathResolver pathResolver;

    @Inject
    public SimpleConfigProviderFactory(ConfigReader reader, FileWatcherService service, ConfigInitializationStrategy initializationStrategy, ConfigPathResolver pathResolver) {
        this.reader = reader;
        this.watcherService = service;
        this.initializationStrategy = initializationStrategy;
        this.pathResolver = pathResolver;
    }

    public <T> ConfigProvider<T> createSimpleProvider(Configuration<T> configuration) {
        initializationStrategy.initializeConfig(configuration.getFileName());
        final Path configPath = pathResolver.getConfigPath(configuration.getFileName());
        return new ReadingConfigProvider<>(configPath, configuration, reader);
    }

    @Override
    public <T> @NotNull ConfigProvider<T> createProvider(@NotNull Configuration<T> configuration) {
        final ConfigProvider<T> simpleProvider = createSimpleProvider(configuration);
        final CachingConfigProvider<T> cachingConfigProvider = new CachingConfigProvider<>(simpleProvider);
        if (cachingConfigProvider.path().isPresent()) {
            return new FileWatchingConfigProvider<>(cachingConfigProvider, watcherService);
        }
        return cachingConfigProvider;
    }
}
