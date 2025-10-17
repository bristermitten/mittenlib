package me.bristermitten.mittenlib.config;

import me.bristermitten.mittenlib.config.reader.ObjectMapper;
import me.bristermitten.mittenlib.config.tree.DataTree;
import org.jetbrains.annotations.NotNull;

/**
 * Context for deserializing a config, making it easier to pass around
 */
public class DeserializationContext {
    private final ObjectMapper mapper;
    private final DataTree data;

    /**
     * Create a new DeserializationContext
     *
     * @param mapper the mapper to use
     * @param data   the data to deserialise
     */
    public DeserializationContext(ObjectMapper mapper, DataTree data) {
        this.mapper = mapper;
        this.data = data;
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

    public DeserializationContext withData(DataTree data) {
        return new DeserializationContext(this.mapper, data);
    }
}
