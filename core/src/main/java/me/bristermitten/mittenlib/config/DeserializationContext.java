package me.bristermitten.mittenlib.config;

import me.bristermitten.mittenlib.config.reader.ObjectMapper;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Context for deserializing a config, making it easier to pass around
 */
public class DeserializationContext {
    private final ObjectMapper mapper;
    private final Map<String, Object> data;

    /**
     * Create a new DeserializationContext
     *
     * @param mapper the mapper to use
     * @param data   the data to deserialize
     */
    public DeserializationContext(ObjectMapper mapper, Map<String, Object> data) {
        this.mapper = mapper;
        this.data = data;
    }

    /**
     * @return the mapper to use
     */
    public ObjectMapper getMapper() {
        return mapper;
    }

    /**
     * @return the data to deserialize
     */
    public @NotNull Map<String, Object> getData() {
        return data;
    }

    public DeserializationContext withData(Map<String, Object> data) {
        return new DeserializationContext(this.mapper, data);
    }
}
