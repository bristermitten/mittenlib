package me.bristermitten.mittenlib.files.json;

import com.google.gson.Gson;
import me.bristermitten.mittenlib.config.reader.ObjectLoader;
import me.bristermitten.mittenlib.util.CompositeType;
import me.bristermitten.mittenlib.util.Result;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class GsonObjectLoader implements ObjectLoader {
    private static final Type MAP_OBJ_OBJ_TYPE = new CompositeType(Map.class, Object.class, Object.class);
    private final Gson gson;

    @Inject
    public GsonObjectLoader(Gson gson) {
        this.gson = gson;
    }

    @Override
    @NotNull
    public Result<Map<Object, Object>> load(@NotNull Path source) {
        return Result.runCatching(() -> gson.fromJson(Files.newBufferedReader(source), MAP_OBJ_OBJ_TYPE));
    }
}
