package me.bristermitten.mittenlib.config;

import me.bristermitten.mittenlib.util.Result;

public interface Deserializable<T> {
    Result<T> deserialize(DeserializationContext context);
}
