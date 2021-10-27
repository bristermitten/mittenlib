package me.bristermitten.mittenlib.files.json;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import me.bristermitten.mittenlib.config.reader.ObjectMapper;
import me.bristermitten.mittenlib.util.CompositeType;
import me.bristermitten.mittenlib.util.Result;

import javax.inject.Inject;
import java.lang.reflect.Type;
import java.util.Map;

public class GsonObjectMapper implements ObjectMapper {
    private static final Type MAP_OBJ_OBJ_TYPE = new CompositeType(Map.class, Object.class, Object.class);
    private final Gson gson;

    @Inject
    public GsonObjectMapper(Gson gson) {
        this.gson = gson;
    }

    @Override
    public <T> Result<T> map(Map<String, Object> map, Class<T> type) {
        return Result.runCatching(() -> {
            final JsonElement tree = gson.toJsonTree(map);
            return gson.fromJson(tree, type);
        });
    }

    @Override
    public <T> Map<String, Object> map(T t) {
        final JsonElement tree = gson.toJsonTree(t);
        return gson.fromJson(tree, MAP_OBJ_OBJ_TYPE);
    }
}
