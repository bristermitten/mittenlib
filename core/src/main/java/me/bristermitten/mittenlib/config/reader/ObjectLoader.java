package me.bristermitten.mittenlib.config.reader;

import me.bristermitten.mittenlib.util.Result;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Map;

public interface ObjectLoader {
    @NotNull Result<Map<Object, Object>> load(@NotNull final Path source);
}
