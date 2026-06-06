package me.bristermitten.mittenlib.annotations.integration.extension;

import me.bristermitten.mittenlib.config.DeserializationContext;
import me.bristermitten.mittenlib.config.extension.CustomDeserializerFor;
import me.bristermitten.mittenlib.util.Result;

@CustomDeserializerFor(SerializerCustomType.class)
public class SerializerCustomTypeDeserializer {
    public static Result<SerializerCustomType> deserialize(DeserializationContext context) {
        return Result.ok(new SerializerCustomType("deserialized"));
    }
}
