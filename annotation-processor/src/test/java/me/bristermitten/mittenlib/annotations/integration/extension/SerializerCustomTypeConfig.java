package me.bristermitten.mittenlib.annotations.integration.extension;

import me.bristermitten.mittenlib.config.Config;

@Config
public interface SerializerCustomTypeConfig {
    SerializerCustomType customType();

    java.util.List<SerializerCustomType> customTypeList();

    java.util.List<java.util.List<SerializerCustomType>> nestedCustomTypeList();
}
