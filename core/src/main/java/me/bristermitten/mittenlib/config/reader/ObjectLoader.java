package me.bristermitten.mittenlib.config.reader;

import me.bristermitten.mittenlib.config.tree.DataTree;
import me.bristermitten.mittenlib.util.Result;
import me.bristermitten.mittenlib.util.lambda.SafeSupplier;
import org.jetbrains.annotations.NotNull;

import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Responsible for parsing a tree-like file (JSON, YAML, etc) into a Map of {@link String} keys and {@link Object} values.
 */
public interface ObjectLoader {
    /**
     * Loads an object from the given {@link Path}.
     * Implementations may want to provide a more efficient implementation of this method.
     *
     * @param source the source to load from
     * @return the loaded object
     */
    @NotNull
    default Result<@NotNull DataTree> load(@NotNull final Path source) {
        return Result.tryWithResources(
                (SafeSupplier<Reader>) () -> Files.newBufferedReader(source),
                this::load);
    }

    /**
     * Loads an object from the given source.
     * This method should not close the given {@link Reader}, but may exhaust it.
     *
     * @param source the source to load from
     * @return the loaded object
     */
    @NotNull Result<@NotNull DataTree> load(@NotNull final Reader source);

    @NotNull
    default Result<@NotNull DataTree> load(@NotNull final String source) {
        return Result.tryWithResources(
                (SafeSupplier<Reader>) () -> new StringReader(source),
                this::load);
    }
}
