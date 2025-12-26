package me.bristermitten.mittenlib.files.json;

import com.google.gson.Gson;
import com.google.inject.Inject;
import me.bristermitten.mittenlib.config.tree.DataTree;
import me.bristermitten.mittenlib.config.writer.ObjectWriter;
import me.bristermitten.mittenlib.util.Result;
import org.jetbrains.annotations.NotNull;

import java.io.Writer;

public class GsonObjectWriter implements ObjectWriter {
    private final Gson gson;

    @Inject
    public GsonObjectWriter(Gson gson) {
        this.gson = gson;
    }

    @Override
    public @NotNull Result<Void> write(@NotNull DataTree tree, @NotNull Writer output) {
        return Result.runCatching(() -> {
            gson.toJson(tree, output);
            return null;
        });
    }
}
