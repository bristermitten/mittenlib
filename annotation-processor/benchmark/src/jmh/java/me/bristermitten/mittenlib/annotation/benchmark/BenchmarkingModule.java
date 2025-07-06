package me.bristermitten.mittenlib.annotation.benchmark;

import com.google.inject.AbstractModule;
import me.bristermitten.mittenlib.MittenLibConsumer;
import me.bristermitten.mittenlib.config.paths.ConfigInitializationStrategy;
import me.bristermitten.mittenlib.config.paths.ConfigPathResolver;
import me.bristermitten.mittenlib.config.paths.JarResourcesConfigPathResolver;
import me.bristermitten.mittenlib.config.paths.NoOpConfigInitializationStrategy;
import me.bristermitten.mittenlib.watcher.FileWatcherModule;

public class BenchmarkingModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new FileWatcherModule());
        bind(ConfigInitializationStrategy.class).to(NoOpConfigInitializationStrategy.class);
        bind(ConfigPathResolver.class).to(JarResourcesConfigPathResolver.class);

        bind(MittenLibConsumer.class)
                .toInstance(new MittenLibConsumer("Benchmark"));
    }


}
