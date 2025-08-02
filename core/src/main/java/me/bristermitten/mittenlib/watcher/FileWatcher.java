package me.bristermitten.mittenlib.watcher;

import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.function.Consumer;

/**
 * A watcher for a file. This contains a {@link Path} to watch,
 * and a {@link Consumer} to call when the file changes.
 * This will generally be processed by {@link FileWatcherService}
 */
public class FileWatcher {
    private final Path watching;
    private final Consumer<WatchEvent<Path>> onModify;

    /**
     * Create a new FileWatcher
     *
     * @param watching the path to watch. This can either be a file or a directory.
     * @param onModify the consumer to call when the file changes
     */
    public FileWatcher(Path watching, Consumer<WatchEvent<Path>> onModify) {
        this.watching = watching;
        this.onModify = onModify;
    }

    /**
     * Get the path being watched.
     *
     * @return the path being watched
     */
    public Path getWatching() {
        return watching;
    }

    /**
     * Get the consumer to call when the file changes.
     *
     * @return the consumer to call when the file changes
     */
    public Consumer<WatchEvent<Path>> getOnModify() {
        return onModify;
    }
}
