package me.bristermitten.mittenlib.watcher;

import javax.inject.Provider;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.WatchService;

public class WatchServiceProvider implements Provider<WatchService> {
    @Override
    public WatchService get() {
        try {
            return FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
