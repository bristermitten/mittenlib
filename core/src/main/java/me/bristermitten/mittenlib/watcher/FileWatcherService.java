package me.bristermitten.mittenlib.watcher;

import me.bristermitten.mittenlib.MittenLibConsumer;
import me.bristermitten.mittenlib.util.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;


/**
 * Handles file watching operations.
 */
@Singleton
public class FileWatcherService {
    private final Map<Path, Set<FileWatcher>> watchers = new ConcurrentHashMap<>();
    private final Set<FileWatcher> registeredWatchers = ConcurrentHashMap.newKeySet();
    private final Provider<WatchService> watchServiceProvider;
    private final ExecutorService service;
    private final AtomicBoolean watching = new AtomicBoolean(false);
    private final Logger logger = Logger.getLogger(FileWatcherService.class.getName());

    @Inject
    FileWatcherService(@NotNull Provider<WatchService> watchServiceProvider, @NotNull MittenLibConsumer consumer) {
        this.watchServiceProvider = watchServiceProvider;

        service = Executors.newSingleThreadExecutor(r ->
        {
            final Thread thread = new Thread(r, String.format("%s MittenLib File Watcher", consumer.getName()));
            thread.setDaemon(true);
            return thread;
        });
    }

    private void registerWatcher(@NotNull WatchService watchService, @NotNull FileWatcher fileWatcher) throws IOException {
        if (registeredWatchers.contains(fileWatcher)) {
            return;
        }
        Path toWatch = fileWatcher.getWatching();
        if (!Files.isDirectory(toWatch)) {
            toWatch = toWatch.getParent();
        }

        toWatch.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.OVERFLOW);
        registeredWatchers.add(fileWatcher);
    }

    /**
     * Add a watcher to the service.
     *
     * @param fileWatcher The watcher to add.
     * @return A future that will be completed once the service is ready to use - some delay may be required for the thread to startup.
     * File changes that occur before this future is completed may not be handled.
     */
    public @NotNull Future<Unit> addWatcher(@NotNull FileWatcher fileWatcher) {
        final Set<FileWatcher> fileWatchers =
                watchers.computeIfAbsent(fileWatcher.getWatching(), path -> ConcurrentHashMap.newKeySet());
        fileWatchers.add(fileWatcher);

        if (!watching.get()) {
            return startWatching();
        }
        return Unit.unitFuture();
    }

    /**
     * Removes a watcher from the service.
     * Note that the watcher will not be removed from the underlying watch service until the service is restarted -
     * the file will still be watched, but the watcher will not be notified of changes.
     * If this operation leaves no watchers, the service will be stopped.
     *
     * @param fileWatcher The watcher to remove.
     */
    public void removeWatcher(@NotNull FileWatcher fileWatcher) {
        final Set<FileWatcher> watcherSet = watchers.get(fileWatcher.getWatching());
        if (watcherSet == null) {
            return; // not in the map, nothing to do
        }
        watcherSet.remove(fileWatcher);
        if (watcherSet.isEmpty()) {
            stopWatching();
        }
    }

    /**
     * Start watching for file changes.
     *
     * @return A future that will be completed once the service is ready to use - some delay may be required for the thread to startup.
     * @throws IllegalStateException if the service is already watching (see {@link #isWatching()}
     */
    public @NotNull Future<Unit> startWatching() {
        if (watching.getAndSet(true)) {
            throw new IllegalStateException("Already watching");
        }

        CompletableFuture<Unit> future = new CompletableFuture<>();
        service.execute(() -> run(future));

        return future;
    }

    /**
     * Returns whether the service is currently watching for file changes.
     * Note that this may return true even if the service is not currently watching, if the service is in the process of starting up.
     *
     * @return whether the service is currently watching for file changes.
     * @see #startWatching()
     */
    public boolean isWatching() {
        return watching.get();
    }

    /**
     * Stop watching for file changes.
     *
     * @throws IllegalStateException if the service is not currently watching (see {@link #isWatching()}
     */
    public void stopWatching() {
        if (!watching.getAndSet(false)) {
            throw new IllegalStateException("Not watching");
        }
        service.shutdownNow();
    }

    private void run(@NotNull CompletableFuture<Unit> whenReady) {
        try (WatchService watchService = watchServiceProvider.get()) {
            registerFileWatchers(watchService);

            boolean poll = true;
            whenReady.complete(Unit.UNIT);
            while (watching.get() && poll) {
                registerFileWatchers(watchService);
                poll = pollEvents(watchService);
            }
        } catch (IOException e) {
            // Handle IO errors specifically
            whenReady.completeExceptionally(new FileWatcherException("Failed to watch files due to an IO error", e));
            logger.severe(() -> "Error watching files: " + e.getMessage());
        } catch (InterruptedException e) {
            // Handle thread interruption
            whenReady.completeExceptionally(new FileWatcherException("File watching was interrupted", e));
            logger.severe("File watching thread was interrupted");
            // Preserve the interrupt status
            Thread.currentThread().interrupt();
        }
    }

    private void registerFileWatchers(@NotNull WatchService watchService) throws IOException {
        for (Set<FileWatcher> fileWatchers : watchers.values()) {
            for (FileWatcher fileWatcher : fileWatchers) {
                registerWatcher(watchService, fileWatcher);
            }
        }
    }

    private boolean pollEvents(@NotNull WatchService watchService) throws InterruptedException {
        final WatchKey key = watchService.take();
        final Path at = (Path) key.watchable();
        for (WatchEvent<?> pollEvent : key.pollEvents()) {
            //noinspection unchecked
            WatchEvent<Path> event = (WatchEvent<Path>) pollEvent;

            final Path resolved = at.resolve(event.context()); // the file that changed
            final @Nullable Set<FileWatcher> fileWatchers = watchers.get(resolved);
            if (fileWatchers == null) {
                return key.reset();
            }
            for (FileWatcher fileWatcher : fileWatchers) {
                fileWatcher.getOnModify().accept(event);
            }
        }
        return key.reset();
    }
}
