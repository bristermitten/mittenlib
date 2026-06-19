package me.bristermitten.mittenlib.testing;

import com.google.inject.AbstractModule;
import me.bristermitten.mittenlib.MittenLib;
import me.bristermitten.mittenlib.files.FileTypeModule;
import me.bristermitten.mittenlib.watcher.FileWatcherService;
import me.bristermitten.mittenlib.watcher.NoOpFileWatcherService;
import org.bukkit.plugin.Plugin;

/**
 * A subclass of {@link MittenLib} designed for testing.
 * Binds no-op / mocking implementations for core services where possible.
 *
 * @param <T> the type of the plugin
 */
public class TestMittenLib<T extends Plugin> extends MittenLib<T> {

    /**
     * Create a new TestMittenLib instance.
     *
     * @param plugin the plugin instance
     */
    public TestMittenLib(T plugin) {
        super(plugin);
        addTestModules();
    }

    /**
     * Create a new TestMittenLib instance.
     *
     * @param plugin the plugin instance
     * @param <T> the type of the plugin
     * @return the test bootstrap builder
     */
    public static <T extends Plugin> TestMittenLib<T> testing(T plugin) {
        return new TestMittenLib<>(plugin);
    }

    private void addTestModules() {
        addModule(new FileTypeModule());
        addModule(new AbstractModule() {
            @Override
            protected void configure() {
                bind(FileWatcherService.class).to(NoOpFileWatcherService.class);
            }
        });
    }
}
