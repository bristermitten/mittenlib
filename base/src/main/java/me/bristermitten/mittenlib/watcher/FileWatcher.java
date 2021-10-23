package me.bristermitten.mittenlib.watcher;

import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.function.Consumer;

public class FileWatcher {
    private final Path watching;
    private final Consumer<WatchEvent<Path>> onModify;

    public FileWatcher(Path watching, Consumer<WatchEvent<Path>> onModify) {
        this.watching = watching;
        this.onModify = onModify;
    }

    public Path getWatching() {
        return watching;
    }

    public Consumer<WatchEvent<Path>> getOnModify() {
        return onModify;
    }
}
