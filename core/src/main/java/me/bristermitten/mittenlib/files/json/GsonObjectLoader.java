package me.bristermitten.mittenlib.files.json;

import com.google.gson.Gson;
import me.bristermitten.mittenlib.config.reader.ObjectLoader;
import me.bristermitten.mittenlib.util.CompositeType;
import me.bristermitten.mittenlib.util.Result;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * An {@link ObjectLoader} that uses Gson to parse a JSON string
 */
public class GsonObjectLoader implements ObjectLoader {
    private static final Type MAP_STRING_OBJ_TYPE = new CompositeType(Map.class, String.class, Object.class);
    private final Gson gson;

    @Inject
    GsonObjectLoader(Gson gson) {
        this.gson = gson;
    }

    @Override
    public @NotNull Result<Map<String, Object>> load(@NotNull Reader source) {
        return Result.runCatching(() ->
                gson.fromJson(source, MAP_STRING_OBJ_TYPE));
    }
}
