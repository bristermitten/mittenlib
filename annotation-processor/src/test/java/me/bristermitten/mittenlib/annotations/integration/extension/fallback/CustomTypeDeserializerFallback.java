package me.bristermitten.mittenlib.annotations.integration.extension.fallback;

import me.bristermitten.mittenlib.config.DeserializationContext;
import me.bristermitten.mittenlib.config.extension.CustomDeserializerFor;
import me.bristermitten.mittenlib.config.extension.Fallback;
import me.bristermitten.mittenlib.util.Result;

@CustomDeserializerFor(CustomTypeFallback.class)
@Fallback
public class CustomTypeDeserializerFallback {

    public static Result<CustomTypeFallback> deserialize(DeserializationContext context) {
        return Result.fail(new UnsupportedOperationException("This should not be called"));
    }
}
