package me.bristermitten.mittenlib.config.reader;

import me.bristermitten.mittenlib.config.DeserializationContext;
import me.bristermitten.mittenlib.util.Result;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.function.Function;

public class ConfigReader {
    private final ObjectLoader loader;
    private final ObjectMapper mapper;

    @Inject
    public ConfigReader(ObjectLoader loader, ObjectMapper mapper) {
        this.loader = loader;
        this.mapper = mapper;
    }

    public <T> Result<T> load(Class<T> type, Path source, @Nullable Function<DeserializationContext, Result<T>> deserializeFunction) {
        final Function<DeserializationContext, Result<T>> mappingFunction =
                deserializeFunction == null ? ctx -> mapper.map(ctx.getData(), type) : deserializeFunction;

        return loader.load(source)
                .map(data -> new DeserializationContext(mapper, data))
                .flatMap(mappingFunction);
    }
}
