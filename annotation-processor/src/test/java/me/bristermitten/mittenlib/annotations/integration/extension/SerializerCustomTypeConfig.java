package me.bristermitten.mittenlib.annotations.integration.extension;

import java.util.List;
import me.bristermitten.mittenlib.config.Config;

@Config
public interface SerializerCustomTypeConfig {
    SerializerCustomType customType();

    List<SerializerCustomType> customTypeList();

    List<List<SerializerCustomType>> nestedCustomTypeList();
}
