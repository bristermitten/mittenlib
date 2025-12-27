package me.bristermitten.mittenlib.config;

import me.bristermitten.mittenlib.config.reader.ObjectMapper;
import me.bristermitten.mittenlib.config.tree.DataTree;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

/**
 * Context for deserializing a config, making it easier to pass around
 */
public class DeserializationContext {
    private final ObjectMapper mapper;
    private final DataTree data;
    private final @Nullable Path sourcePath;

    /**
     * Create a new DeserializationContext
     *
     * @param mapper the mapper to use
     * @param data   the data to deserialise
     */
    public DeserializationContext(ObjectMapper mapper, DataTree data) {
        this(mapper, data, null);
    }

    /**
     * Create a new DeserializationContext with source path information
     *
     * @param mapper     the mapper to use
     * @param data       the data to deserialise
     * @param sourcePath the path to the source file being deserialized, or null if not available
     */
    public DeserializationContext(ObjectMapper mapper, DataTree data, @Nullable Path sourcePath) {
        this.mapper = mapper;
        this.data = data;
        this.sourcePath = sourcePath;
    }

    /**
     * Returns the mapper to use for deserialization.
     * In generated config code, this is only used as a last resort.
     * @return the mapper to use
     */
    public ObjectMapper getMapper() {
        return mapper;
    }

    /**
     * Returns the data to deserialize. This may be a child of the original data, or the original data itself.
     * @return the data to deserialize
     */
    public @NotNull DataTree getData() {
        return data;
    }

    /**
     * Returns the source path of the configuration file being deserialized, if available.
     * This is useful for providing better error messages.
     * @return the source path, or null if not available
     */
    public @Nullable Path getSourcePath() {
        return sourcePath;
    }

    public DeserializationContext withData(DataTree data) {
        return new DeserializationContext(this.mapper, data, this.sourcePath);
    }
}
