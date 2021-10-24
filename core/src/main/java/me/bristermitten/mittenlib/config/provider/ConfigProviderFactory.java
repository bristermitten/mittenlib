package me.bristermitten.mittenlib.config.provider;

import me.bristermitten.mittenlib.config.Configuration;
import me.bristermitten.mittenlib.config.reader.ConfigReader;
import me.bristermitten.mittenlib.watcher.FileWatcherService;

import javax.inject.Inject;

public class ConfigProviderFactory {

    private final ConfigReader reader;
    private final FileWatcherService watcherService;

    @Inject
    public ConfigProviderFactory(ConfigReader reader, FileWatcherService service) {
        this.reader = reader;
        this.watcherService = service;
    }

    <T> ConfigProvider<T> createSimpleProvider(Configuration<T> configuration) {
        return new ReadingConfigProvider<>(configuration, reader);
    }

    <T> ConfigProvider<T> createProvider(Configuration<T> configuration) {
        final ConfigProvider<T> simpleProvider = createSimpleProvider(configuration);
        final CachingConfigProvider<T> cachingConfigProvider = new CachingConfigProvider<>(simpleProvider);
        if (cachingConfigProvider.path().isPresent()) {
            return new FileWatchingConfigProvider<>(cachingConfigProvider, watcherService);
        }
        return cachingConfigProvider;
    }
}
