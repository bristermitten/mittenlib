package me.bristermitten.mittenlib.files.json;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Type;

public abstract class ExtraTypeAdapter<T> extends TypeAdapter<T> {
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

    public abstract Type type();
}
