package me.bristermitten.mittenlib.config.provider.construct;

import me.bristermitten.mittenlib.config.provider.CachingConfigProvider;
import me.bristermitten.mittenlib.config.provider.ConfigProvider;
import me.bristermitten.mittenlib.config.provider.FileWatchingConfigProvider;
import me.bristermitten.mittenlib.watcher.FileWatcherService;

import javax.inject.Inject;

public class SimpleConfigProviderImprover implements ConfigProviderImprover {
    private final FileWatcherService watcherService;

    @Inject
    SimpleConfigProviderImprover(FileWatcherService watcherService) {
        this.watcherService = watcherService;
    }

    @Override
    public <T> ConfigProvider<T> improve(ConfigProvider<T> provider) {
        final CachingConfigProvider<T> cachingConfigProvider = new CachingConfigProvider<>(provider);
        // Always apply caching

        if (cachingConfigProvider.path().isPresent()) {
            return new FileWatchingConfigProvider<>(cachingConfigProvider, watcherService);
        }
        return cachingConfigProvider;
    }
}
