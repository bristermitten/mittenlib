package me.bristermitten.mittenlib.config.writer;

import com.google.inject.Inject;
import me.bristermitten.mittenlib.config.SerializationContext;
import me.bristermitten.mittenlib.config.SerializationFunction;
import me.bristermitten.mittenlib.config.reader.ObjectMapper;
import me.bristermitten.mittenlib.config.tree.DataTree;
import me.bristermitten.mittenlib.util.Result;

import java.nio.file.Path;

/**
 * Responsible for both mapping and writing data,
 * delegating to {@link SerializationFunction}s and {@link ObjectWriter}s.
 */
public class ConfigWriter {
    private final ObjectWriter writer;
    private final ObjectMapper mapper;

    @Inject
    public ConfigWriter(ObjectWriter writer, ObjectMapper mapper) {
        this.writer = writer;
        this.mapper = mapper;
    }

    /**
     * Serializes the given config instance and saves it to the given path.
     *
     * @param instance    the config instance to save
     * @param function    the serialization function to use
     * @param destination the path to save to
     * @param <T>         the type of the config
     * @return a Result indicating success or failure
     */
    public <T> Result<Void> write(T instance, SerializationFunction<T> function, Path destination) {
        return serialize(instance, function).flatMap(tree -> writer.write(tree, destination));
    }

    /**
     * Serializes the given config instance to a {@link DataTree}.
     *
     * @param instance the config instance to serialize
     * @param function the serialization function to use
     * @param <T>      the type of the config
     * @return a Result containing the serialized DataTree
     */
    public <T> Result<DataTree> serialize(T instance, SerializationFunction<T> function) {
        return Result.ok(new SerializationContext(mapper))
                .map(ctx -> function.apply(instance, ctx));
    }

    public <T> Result<Void> write(T instance, Class<T> type, Path destination) {
        return serialize(instance, type).flatMap(tree -> writer.write(tree, destination));
    }

    public <T> Result<DataTree> serialize(T instance, Class<T> type) {
        return serialize(instance, (val, ctx) -> me.bristermitten.mittenlib.config.tree.DataTreeTransforms.loadFrom(ctx.getMapper().map(val)));
    }

    /**
     * Returns the ObjectMapper used by this saver.
     *
     * @return the ObjectMapper
     */
    public ObjectMapper getMapper() {
        return mapper;
    }

    /**
     * Returns the ObjectWriter used by this saver.
     *
     * @return the ObjectWriter
     */
    public ObjectWriter getWriter() {
        return writer;
    }
}
