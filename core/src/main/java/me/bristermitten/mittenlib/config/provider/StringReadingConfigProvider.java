package me.bristermitten.mittenlib.config.provider;

import me.bristermitten.mittenlib.config.Configuration;
import me.bristermitten.mittenlib.config.reader.ConfigReader;

import java.nio.file.Path;
import java.util.Optional;

public class StringReadingConfigProvider<T> implements ConfigProvider<T> {
    private final Configuration<T> config;
    private final ConfigReader reader;
    private final String data;

    public StringReadingConfigProvider(String data, Configuration<T> config, ConfigReader reader) {
        this.data = data;
        this.config = config;
        this.reader = reader;
    }

    @Override
    public T get() {
        return reader.load(config.getType(), data, config.getDeserializeFunction()).getOrThrow();
    }

    @Override
    public Optional<Path> path() {
        return Optional.empty();
    }
}

