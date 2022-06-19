package me.bristermitten.mittenlib.config;

import org.jetbrains.annotations.Nullable;

public class Configuration<T> {
    private final String fileName;
    private final Class<T> type;

    private final @Nullable DeserializationFunction<T> deserializeFunction;

    public Configuration(String fileName, Class<T> type, @Nullable DeserializationFunction<T> deserializeFunction) {
        this.fileName = fileName;
        this.type = type;
        this.deserializeFunction = deserializeFunction;
    }

    public String getFileName() {
        return fileName;
    }

    public Class<T> getType() {
        return type;
    }

    @Nullable
    public DeserializationFunction<T> getDeserializeFunction() {
        return deserializeFunction;
    }
}
