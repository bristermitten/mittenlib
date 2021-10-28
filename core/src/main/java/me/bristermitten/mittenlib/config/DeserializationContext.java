package me.bristermitten.mittenlib.config;

import me.bristermitten.mittenlib.config.reader.ObjectMapper;

import java.util.Map;

public class DeserializationContext {
    private final ObjectMapper mapper;
    private final Map<String, Object> data;

    public DeserializationContext(ObjectMapper mapper, Map<String, Object> data) {
        this.mapper = mapper;
        this.data = data;
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public DeserializationContext withData(Map<String, Object> data) {
        return new DeserializationContext(this.mapper, data);
    }
}
