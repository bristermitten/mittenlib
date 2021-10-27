package me.bristermitten.mittenlib.config.reader;

import me.bristermitten.mittenlib.util.Result;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;

public class ConfigReader {
    private final ObjectLoader loader;
    private final ObjectMapper mapper;

    @Inject
    public ConfigReader(ObjectLoader loader, ObjectMapper mapper) {
        this.loader = loader;
        this.mapper = mapper;
    }

    public <T> Result<T> load(Class<T> type, Path source, @Nullable Function<Map<String, Object>, Result<T>> deserializeFunction) {
        final Function<Map<String, Object>, Result<T>> mappingFunction =
                deserializeFunction == null ? map -> mapper.map(map, type) : deserializeFunction;

        return loader.load(source)
                .flatMap(mappingFunction);
    }
}
