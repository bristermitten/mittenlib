package me.bristermitten.mittenlib.watcher;

import org.bukkit.plugin.Plugin;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Singleton
public class FileWatcherService implements Runnable {
    private final Map<Path, Set<FileWatcher>> watchers = new ConcurrentHashMap<>();
    private final ExecutorService service;
    private final AtomicBoolean watching = new AtomicBoolean(false);

    @Inject
    public FileWatcherService(Plugin plugin) {
        service = Executors.newSingleThreadExecutor(r ->
        {
            final Thread thread = new Thread(r, String.format("%s MittenLib File Watcher", plugin.getName()));
            thread.setDaemon(true);
            return thread;
        });
    }

    public void addWatcher(FileWatcher fileWatcher) {
        final Set<FileWatcher> fileWatchers =
                watchers.computeIfAbsent(fileWatcher.getWatching(), path -> ConcurrentHashMap.newKeySet());
        fileWatchers.add(fileWatcher);
    }

    public void removeWatcher(FileWatcher fileWatcher) {
        final Set<FileWatcher> watcherSet = watchers.get(fileWatcher.getWatching());
        if (watcherSet == null) {
            return; // not in the map, nothing to do
        }
        watcherSet.remove(fileWatcher);
    }

    public void startWatching() {
        if (watching.getAndSet(true)) {
            throw new IllegalStateException("Already watching");
        }
        service.execute(this);
    }

    public void stopWatching() {
        if (!watching.getAndSet(false)) {
            throw new IllegalStateException("Not watching");
        }
        service.shutdownNow();
    }

    @Override
    public void run() {
        try (final WatchService watchService = FileSystems.getDefault().newWatchService()) {
            for (Set<FileWatcher> fileWatchers : watchers.values()) {
                for (FileWatcher fileWatcher : fileWatchers) {
                    Path toWatch = fileWatcher.getWatching();
                    if (!Files.isDirectory(toWatch)) {
                        toWatch = toWatch.getParent();
                    }

                    toWatch.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
                }
            }

            boolean poll = true;
            while (watching.get() && poll) {
                poll = pollEvents(watchService);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    private boolean pollEvents(WatchService watchService) throws InterruptedException {
        final WatchKey key = watchService.take();
        final Path at = (Path) key.watchable();
        for (WatchEvent<?> pollEvent : key.pollEvents()) {
            //noinspection unchecked
            WatchEvent<Path> event = (WatchEvent<Path>) pollEvent;

            final Path resolved = at.resolve(event.context()); // the file that changed
            final Set<FileWatcher> fileWatchers = watchers.get(resolved);
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
