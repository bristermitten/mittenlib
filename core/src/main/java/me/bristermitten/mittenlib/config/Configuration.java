package me.bristermitten.mittenlib.config;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Function;

public class Configuration<T> {
    private final String fileName;
    private final Class<T> type;
    private final @Nullable Function<Map<String, Object>, T> deserializeFunction;

    public Configuration(String fileName, Class<T> type) {
        this.fileName = fileName;
        this.type = type;
    }

    public String getFileName() {
        return fileName;
    }

    public Class<T> getType() {
        return type;
    }
}
