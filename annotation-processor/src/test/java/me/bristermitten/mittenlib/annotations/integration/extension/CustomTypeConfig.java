package me.bristermitten.mittenlib.annotations.integration.extension;

import me.bristermitten.mittenlib.config.Config;

import java.util.List;

@Config
public interface CustomTypeConfig {
    CustomType customType();

    List<CustomType> customTypes();
}
