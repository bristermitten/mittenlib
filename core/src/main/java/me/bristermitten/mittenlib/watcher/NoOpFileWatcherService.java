package me.bristermitten.mittenlib.watcher;

import java.util.concurrent.Future;
import me.bristermitten.mittenlib.util.Unit;
import org.jetbrains.annotations.NotNull;

/** A no-op implementation of {@link FileWatcherService} for testing. */
public class NoOpFileWatcherService implements FileWatcherService {
    @Override
    public @NotNull Future<Unit> addWatcher(@NotNull FileWatcher fileWatcher) {
        return Unit.unitFuture();
    }

    @Override
    public void removeWatcher(@NotNull FileWatcher fileWatcher) {
        // no-op
    }
}
