package me.bristermitten.mittenlib.lang;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * Creates a {@link TypeAdapter} that can serialize elements with just a message to plain
 * Strings (i.e not complex objects), and vice versa
 */
public class LangMessageTypeAdapterFactory implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (!type.getRawType().equals(LangMessage.class)) {
            return null;
        }

        //noinspection unchecked
        final TypeAdapter<LangMessage> delegateAdapter = (TypeAdapter<LangMessage>) gson.getDelegateAdapter(this, type);
        //noinspection unchecked
        return (TypeAdapter<T>) new TypeAdapter<LangMessage>() {
            @Override
            public void write(JsonWriter out, LangMessage value) throws IOException {
                if (value.getTitle() == null && value.getSubtitle() == null && value.getActionBar() == null && value.getSound() == null) {
                    if (value.getMessage() == null) {
                        throw new IllegalArgumentException("Empty LangElement!");
                    }
                    out.value(value.getMessage()); // just write the message as a string
                    return;
                }
                delegateAdapter.write(out, value);
            }

            @Override
            public LangMessage read(JsonReader in) throws IOException {
                if (in.peek() == JsonToken.STRING) {
                    return new LangMessage(in.nextString(), null, null, null, null);
                }
                return delegateAdapter.read(in);
            }
        };
    }
}
