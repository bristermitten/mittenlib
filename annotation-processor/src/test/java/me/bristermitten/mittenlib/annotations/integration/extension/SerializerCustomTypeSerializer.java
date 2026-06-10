package me.bristermitten.mittenlib.annotations.integration.extension;

import me.bristermitten.mittenlib.config.SerializationContext;
import me.bristermitten.mittenlib.config.extension.CustomSerializerFor;
import me.bristermitten.mittenlib.config.tree.DataTree;

@CustomSerializerFor(SerializerCustomType.class)
public class SerializerCustomTypeSerializer {
    public static DataTree serialize(SerializerCustomType value, SerializationContext context) {
        return DataTree.string("serialized-" + value.value());
    }
}
