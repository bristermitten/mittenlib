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

/**
 * Responsible for both reading and mapping data,
 * delegating to {@link ObjectLoader}s and {@link ObjectMapper}s
 */
public class ConfigReader {
    private final ObjectLoader loader;
    private final ObjectMapper mapper;

    @Inject
    ConfigReader(ObjectLoader loader, ObjectMapper mapper) {
        this.loader = loader;
        this.mapper = mapper;
    }

    /**
     * Read the data from the given path, and map it to the given type
     *
     * @param type                the type to map to
     * @param source              the path to read from
     * @param deserializeFunction the function to use to deserialize the data
     * @param <T>                 the type to map to
     * @return the result of the mapping
     */
    public <T> Result<T> load(Class<T> type, Path source, @Nullable DeserializationFunction<T> deserializeFunction) {
        return read(loader.load(source), deserializeFunction, type);
    }

    /**
     * Read the data from the given string, and map it to the given type
     *
     * @param type                the type to map to
     * @param source              the string to read from
     * @param deserializeFunction the function to use to deserialize the data
     * @param <T>                 the type to map to
     * @return the result of the mapping
     */
    public <T> Result<T> load(Class<T> type, String source, @Nullable DeserializationFunction<T> deserializeFunction) {
        return read(loader.load(source), deserializeFunction, type);
    }

    /**
     * Read the data from the given reader, and map it to the given type
     *
     * @param type                the type to map to
     * @param source              the reader to read from
     * @param deserializeFunction the function to use to deserialize the data
     * @param <T>                 the type to map to
     * @return the result of the mapping
     */
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

    /**
     * Create a new {@link ConfigReader} with the given {@link ObjectLoader} instead of the existing one
     *
     * @param loader the loader to use
     * @return the new ConfigReader
     */
    public ConfigReader withLoader(ObjectLoader loader) {
        return new ConfigReader(loader, mapper);
    }
}
