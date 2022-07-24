package me.bristermitten.mittenlib.config.reader;

import me.bristermitten.mittenlib.util.Result;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public interface ObjectLoader {
    @NotNull
    default Result<Map<String, Object>> load(@NotNull final Path source) {
        return Result.computeCatching(() -> {
            try (BufferedReader reader = Files.newBufferedReader(source)) {
                return load(reader);
            }
        });
    }

    /**
     * Loads an object from the given source.
     * This method is not required to close the reader.
     *
     * @param source the source to load from
     * @return the loaded object
     */
    @NotNull Result<Map<String, Object>> load(@NotNull final Reader source);

    @NotNull
    default Result<Map<String, Object>> load(@NotNull final String source) {
        try (StringReader stringReader = new StringReader(source)) {
            return load(stringReader);
        }
    }
}
