package me.bristermitten.mittenlib.files.json;

import com.google.gson.Gson;
import com.google.inject.Inject;
import me.bristermitten.mittenlib.config.tree.DataTree;
import me.bristermitten.mittenlib.config.writer.ObjectWriter;
import me.bristermitten.mittenlib.util.Result;
import org.jetbrains.annotations.NotNull;

import java.io.Writer;

/**
 * An {@link ObjectWriter} implementation that writes {@link DataTree} instances
 * to JSON using a provided {@link Gson} instance.
 * <p>
 * This relies on a registered {@code TypeAdapter} for {@link DataTree} so that
 * the tree can be written directly without transforming it to an intermediate POJO.
 */
public class GsonObjectWriter implements ObjectWriter {
    private final Gson gson;

    @Inject
    public GsonObjectWriter(Gson gson) {
        this.gson = gson;
    }

    @Override
    /**
     * Writes the given {@link DataTree} to the supplied {@link Writer} in JSON format using Gson.
     * <p>
     * This method does not close or flush the provided {@code Writer}; the caller is responsible
     * for managing the lifecycle of the writer.
     * </p>
     *
     * @param tree   the configuration tree to serialize, not {@code null}
     * @param output the writer to which the JSON representation of the tree will be written, not {@code null}
     * @return a {@link Result} that is successful if the write completes without throwing,
     *         or failed with the thrown exception otherwise
     */
    public @NotNull Result<Void> write(@NotNull DataTree tree, @NotNull Writer output) {
        return Result.runCatching(() -> {
            gson.toJson(tree, output); // we don't need to transform to POJO since we have a TypeAdapter for DataTree
            return null;
        });
    }
}
