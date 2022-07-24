package me.bristermitten.mittenlib.annotation.benchmark;

import com.google.inject.AbstractModule;
import me.bristermitten.mittenlib.MittenLibConsumer;
import me.bristermitten.mittenlib.config.paths.ConfigInitializationStrategy;
import me.bristermitten.mittenlib.config.paths.ConfigPathResolver;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.util.HashMap;
import java.util.Map;

public class BenchmarkingModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ConfigInitializationStrategy.class).to(NoOpConfigInitializationStrategy.class);
        bind(ConfigPathResolver.class).toInstance(configFileName -> {
            try {
                //noinspection ConstantConditions
                URI uri = getClass().getClassLoader().getResource(configFileName).toURI();

                return getSystem(uri).getPath(configFileName);
            } catch (URISyntaxException | IOException e) {
                throw new RuntimeException(e);
            }
        });

        bind(MittenLibConsumer.class)
                .toInstance(new MittenLibConsumer("Benchmark"));
    }

    private FileSystem getSystem(URI uri) throws IOException {
        try {
            return FileSystems.getFileSystem(uri);
        } catch (FileSystemNotFoundException e) {
            Map<String, String> env = new HashMap<>();
            env.put("create", "true");
            return FileSystems.newFileSystem(uri, env);
        }
    }
}
