package me.bristermitten.mittenlib.config.writer;

import me.bristermitten.mittenlib.config.reader.ObjectLoader;
import me.bristermitten.mittenlib.config.tree.DataTree;
import me.bristermitten.mittenlib.util.Result;
import me.bristermitten.mittenlib.util.lambda.SafeSupplier;
import org.jetbrains.annotations.NotNull;

import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;


/**
 * Inverse to {@link ObjectLoader}
 */
public interface ObjectWriter {
    @NotNull
    default Result<Void> write(@NotNull DataTree tree, @NotNull Path path) {
        return Result.tryWithResources(
                (SafeSupplier<Writer>) () -> Files.newBufferedWriter(path),
                writer -> write(tree, writer)
        );
    }

    @NotNull Result<Void> write(@NotNull DataTree tree, @NotNull Writer output);

    @NotNull
    default Result<String> write(@NotNull DataTree tree) {
        return Result.<String, StringWriter>tryWithResources(
                StringWriter::new,
                writer -> write(tree, writer)
                        .map(x -> writer.toString())
        );
    }
}
