package me.bristermitten.mittenlib.annotations.integration.extension;

import me.bristermitten.mittenlib.config.DeserializationContext;
import me.bristermitten.mittenlib.config.extension.CustomDeserializerFor;
import me.bristermitten.mittenlib.util.Result;

@CustomDeserializerFor(CustomType.class)
public class CustomTypeDeserializer {

    public static Result<CustomType> deserialize(DeserializationContext context) {
        return Result.ok(
                new CustomType("hello")
        );
    }
}
