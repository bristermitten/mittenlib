package me.bristermitten.mittenlib.files.json;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import me.bristermitten.mittenlib.config.reader.ObjectMapper;
import me.bristermitten.mittenlib.util.Result;

import javax.inject.Inject;

/**
 * An {@link ObjectMapper} implementation using {@link Gson}.
 */
public class GsonObjectMapper implements ObjectMapper {
    private final Gson gson;

    @Inject
    GsonObjectMapper(Gson gson) {
        this.gson = gson;
    }

    @Override
    public <T> Result<T> map(Object map, TypeToken<T> type) {
        return Result.runCatching(() -> {
            final JsonElement tree = map instanceof JsonElement ? (JsonElement) map : gson.toJsonTree(map);
            return gson.fromJson(tree, type.getType());
        });
    }

    @Override
    public JsonElement map(Object value) {
        return gson.toJsonTree(value);
    }
}
