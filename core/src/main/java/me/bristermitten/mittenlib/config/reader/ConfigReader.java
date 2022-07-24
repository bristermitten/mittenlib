package me.bristermitten.mittenlib.config.reader;

import com.google.gson.reflect.TypeToken;
import me.bristermitten.mittenlib.config.DeserializationContext;
import me.bristermitten.mittenlib.config.DeserializationFunction;
import me.bristermitten.mittenlib.util.Result;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.io.Reader;
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

    public <T> Result<T> load(Class<T> type, Path source, @Nullable DeserializationFunction<T> deserializeFunction) {
        return read(loader.load(source), deserializeFunction, type);
    }

    public <T> Result<T> load(Class<T> type, String source, @Nullable DeserializationFunction<T> deserializeFunction) {
        return read(loader.load(source), deserializeFunction, type);
    }

    public <T> Result<T> load(Class<T> type, Reader source, @Nullable DeserializationFunction<T> deserializeFunction) {
        return read(loader.load(source), deserializeFunction, type);
    }

    private <T> Result<T> read(Result<Map<String, Object>> rawData, @Nullable DeserializationFunction<T> deserializeFunction, Class<T> type) {
        final Function<DeserializationContext, Result<T>> mappingFunction =
                deserializeFunction == null
                        ? ctx -> mapper.map(ctx.getData(), TypeToken.get(type))
                        : deserializeFunction;

        return rawData
                .map(data -> new DeserializationContext(mapper, data))
                .flatMap(mappingFunction);
    }

    public ConfigReader withLoader(ObjectLoader loader) {
        return new ConfigReader(loader, mapper);
    }
}
