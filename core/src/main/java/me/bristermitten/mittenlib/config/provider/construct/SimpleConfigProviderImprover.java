package me.bristermitten.mittenlib.config.provider.construct;

import com.google.inject.Inject;
import me.bristermitten.mittenlib.config.provider.CachingConfigProvider;
import me.bristermitten.mittenlib.config.provider.ConfigProvider;
import me.bristermitten.mittenlib.config.provider.FileWatchingConfigProvider;
import me.bristermitten.mittenlib.watcher.FileWatcherService;

public class SimpleConfigProviderImprover implements ConfigProviderImprover {
    private final FileWatcherService watcherService;

    @Inject
    SimpleConfigProviderImprover(FileWatcherService watcherService) {
        this.watcherService = watcherService;
    }

    @Override
    public <T> ConfigProvider<T> improve(ConfigProvider<T> provider) {
        // Always apply caching
        final CachingConfigProvider<T> cachingConfigProvider = new CachingConfigProvider<>(provider);

        if (cachingConfigProvider.path().isPresent()) {
            return new FileWatchingConfigProvider<>(cachingConfigProvider, watcherService);
        }
        return cachingConfigProvider;
    }
}
