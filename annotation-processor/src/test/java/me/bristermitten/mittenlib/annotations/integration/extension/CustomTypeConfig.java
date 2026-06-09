package me.bristermitten.mittenlib.annotations.integration.extension;

import java.util.List;
import me.bristermitten.mittenlib.config.Config;

@Config(requireSerialization = false)
public interface CustomTypeConfig {
    CustomType customType();

    List<CustomType> customTypes();
}
