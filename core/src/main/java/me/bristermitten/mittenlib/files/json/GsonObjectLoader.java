package me.bristermitten.mittenlib.files.json;

import com.google.gson.Gson;
import me.bristermitten.mittenlib.config.reader.ObjectLoader;
import me.bristermitten.mittenlib.config.tree.DataTree;
import me.bristermitten.mittenlib.util.Result;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.io.Reader;

/**
 * An {@link ObjectLoader} that uses Gson to parse a JSON string
 */
public class GsonObjectLoader implements ObjectLoader {
    private final Gson gson;

    @Inject
    GsonObjectLoader(Gson gson) {
        this.gson = gson;
    }

    @Override
    public @NotNull Result<DataTree> load(@NotNull Reader source) {
        return Result.runCatching(() ->
                gson.fromJson(source, DataTree.class));
    }
}
