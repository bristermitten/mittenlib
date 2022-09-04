package me.bristermitten.mittenlib.files.json;

import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * A thin wrapper over a {@link TypeAdapter} that also stores the type it is for.
 * This is necessary for dynamic binding as Gson does not provide a way to get the type of a TypeAdapter,
 * but requires it for registering ({@link GsonBuilder#registerTypeAdapter(Type, Object)}).
 *
 * @param <T> the type of the TypeAdapter
 */
public abstract class ExtraTypeAdapter<T> extends TypeAdapter<T> {
    /**
     * Creates an {@link ExtraTypeAdapter} from an existing {@link TypeAdapter}.
     *
     * @param type    the type of the TypeAdapter
     * @param adapter the TypeAdapter
     * @param <T>     the type of the TypeAdapter
     * @return the ExtraTypeAdapter
     */
    public static <T> ExtraTypeAdapter<T> of(Type type, TypeAdapter<T> adapter) {
        return new ExtraTypeAdapter<T>() {
            @Override
            public Type type() {
                return type;
            }

            @Override
            public T read(JsonReader reader) throws IOException {
                return adapter.read(reader);
            }

            @Override
            public void write(JsonWriter writer, T value) throws IOException {
                adapter.write(writer, value);
            }
        };
    }

    /**
     * @return The type that this {@link ExtraTypeAdapter} is for.
     */

    public abstract Type type();
}
