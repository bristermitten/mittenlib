package me.bristermitten.mittenlib.annotations.integration.extension;

import me.bristermitten.mittenlib.config.Config;

import java.util.List;

@Config
public interface SerializerCustomTypeConfig {
    SerializerCustomType customType();

    List<SerializerCustomType> customTypeList();

    List<List<SerializerCustomType>> nestedCustomTypeList();
}
