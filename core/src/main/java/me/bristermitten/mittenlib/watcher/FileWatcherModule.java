package me.bristermitten.mittenlib.watcher;

import com.google.inject.AbstractModule;
import java.nio.file.WatchService;

/** Sets up the {@link FileWatcherService} */
public class FileWatcherModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(FileWatcherService.class).to(NativeFileWatcherService.class);
        bind(WatchService.class).toProvider(WatchServiceProvider.class);
    }
}
