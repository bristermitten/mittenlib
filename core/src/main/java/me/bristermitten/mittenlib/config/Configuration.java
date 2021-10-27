package me.bristermitten.mittenlib.config;

import me.bristermitten.mittenlib.util.Result;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Function;

public class Configuration<T> {
    private final String fileName;
    private final Class<T> type;
    private final @Nullable Function<Map<String, Object>, Result<T>> deserializeFunction;

    public Configuration(String fileName, Class<T> type, @Nullable Function<Map<String, Object>, Result<T>> deserializeFunction) {
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
    public Function<Map<String, Object>, Result<T>> getDeserializeFunction() {
        return deserializeFunction;
    }
}
