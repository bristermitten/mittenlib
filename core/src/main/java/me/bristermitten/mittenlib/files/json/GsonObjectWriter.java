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
    public @NotNull Result<Void> write(@NotNull DataTree tree, @NotNull Writer output) {
        return Result.runCatching(() -> {
            gson.toJson(tree, output); // we don't need to transform to POJO since we have a TypeAdapter for DataTree
            return null;
        });
    }
}
