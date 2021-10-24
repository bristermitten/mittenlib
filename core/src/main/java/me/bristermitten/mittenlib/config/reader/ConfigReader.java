package me.bristermitten.mittenlib.config.reader;

import me.bristermitten.mittenlib.util.Result;

import javax.inject.Inject;
import java.nio.file.Path;

public class ConfigReader {
    private final ObjectLoader loader;
    private final ObjectMapper mapper;

    @Inject
    public ConfigReader(ObjectLoader loader, ObjectMapper mapper) {
        this.loader = loader;
        this.mapper = mapper;
    }

    public <T> Result<T> load(Class<T> type, Path source) {
        return loader.load(source)
                .flatMap(data -> mapper.map(data, type));
    }
}
