package me.bristermitten.mittenlib.config;

import com.google.inject.Injector;
import me.bristermitten.mittenlib.config.reader.ObjectMapper;
import org.jetbrains.annotations.NotNull;

/**
 * Context for serializing a config
 */
public class SerializationContext {
    private final ObjectMapper mapper;

    /**
     * Create a new SerializationContext
     *
     * @param mapper   the mapper to use
     */
    public SerializationContext(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Returns the mapper to use for serialization.
     * @return the mapper to use
     */
    public ObjectMapper getMapper() {
        return mapper;
    }
}
