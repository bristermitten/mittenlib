package me.bristermitten.mittenlib.watcher;

import java.util.concurrent.Future;
import me.bristermitten.mittenlib.util.Unit;
import org.jetbrains.annotations.NotNull;

/** Handles file watching operations. */
public interface FileWatcherService {
    /**
     * Add a watcher to the service.
     *
     * @param fileWatcher The watcher to add.
     * @return A future that will be completed once the service is ready to use.
     */
    @NotNull Future<Unit> addWatcher(@NotNull FileWatcher fileWatcher);

    /**
     * Removes a watcher from the service.
     *
     * @param fileWatcher The watcher to remove.
     */
    void removeWatcher(@NotNull FileWatcher fileWatcher);
}
