package me.bristermitten.mittenlib.annotations.integration.extension;

import me.bristermitten.mittenlib.config.DeserializationContext;
import me.bristermitten.mittenlib.config.extension.CustomDeserializerFor;
import me.bristermitten.mittenlib.config.tree.DataTree;
import me.bristermitten.mittenlib.util.Result;

@CustomDeserializerFor(CustomType.class)
public class CustomTypeDeserializer {

    public static Result<CustomType> deserialize(DeserializationContext context) {
        return switch (context.getData()) {
            case DataTree.DataTreeLiteral.DataTreeLiteralString ignored -> Result.ok(new CustomType("hello"));
            default -> Result.fail(new IllegalArgumentException("Invalid data type " + context.getData()));
        };

    }
}
