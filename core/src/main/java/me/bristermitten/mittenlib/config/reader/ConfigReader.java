package me.bristermitten.mittenlib.config.reader;

import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import me.bristermitten.mittenlib.config.DeserializationContext;
import me.bristermitten.mittenlib.config.DeserializationFunction;
import me.bristermitten.mittenlib.config.tree.DataTree;
import me.bristermitten.mittenlib.util.Result;
import org.jetbrains.annotations.NotNull;

import java.io.Reader;
import java.nio.file.Path;

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
     * @param function            the deserialization function to use
     * @param source              the path to read from
     * @param <T>                 the type to map to
     * @return the result of the mapping
     */
    public <T> Result<? extends T> load(DeserializationFunction<T> function, Path source) {
        return read(loader.load(source), function);
    }

    public <T> Result<? extends T> load(DeserializationFunction<T> function, String source) {
        return read(loader.load(source), function);
    }

    public <T> Result<? extends T> load(DeserializationFunction<T> function, Reader source) {
        return read(loader.load(source), function);
    }

    private <T> Result<T> read(Result<@NotNull DataTree> rawData, DeserializationFunction<T> mappingFunction) {
        return rawData
                .map(data -> new DeserializationContext(mapper, data))
                .flatMap(mappingFunction::apply);
    }

    public <T> Result<? extends T> load(Class<T> type, Path source) {
        return load(ctx -> ctx.getMapper().map(ctx.getData(), TypeToken.get(type)), source);
    }

    public <T> Result<? extends T> load(Class<T> type, String source) {
        return load(ctx -> ctx.getMapper().map(ctx.getData(), TypeToken.get(type)), source);
    }

    public <T> Result<? extends T> load(Class<T> type, Reader source) {
        return load(ctx -> ctx.getMapper().map(ctx.getData(), TypeToken.get(type)), source);
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

    /**
     * Get the {@link ObjectMapper} used by this reader
     *
     * @return the object mapper
     */
    public ObjectMapper getMapper() {
        return mapper;
    }

    public ObjectLoader getLoader() {
        return loader;
    }
}
