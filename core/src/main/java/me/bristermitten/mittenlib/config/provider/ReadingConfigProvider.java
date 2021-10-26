package me.bristermitten.mittenlib.config.provider;

import me.bristermitten.mittenlib.config.Configuration;
import me.bristermitten.mittenlib.config.reader.ConfigReader;

import java.nio.file.Path;
import java.util.Optional;

public class ReadingConfigProvider<T> implements ConfigProvider<T> {
    private final Configuration<T> config;
    private final ConfigReader reader;
    private final Path path;

    public ReadingConfigProvider(Path path, Configuration<T> config, ConfigReader reader) {
        this.path = path;
        this.config = config;
        this.reader = reader;
    }

    @Override
    public T get() {
        return reader.load(config.getType(), path)
                .getOrThrow();
    }

    @Override
    public Optional<Path> path() {
        return Optional.of(path);
    }
}

