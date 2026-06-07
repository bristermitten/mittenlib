package me.bristermitten.mittenlib.annotations.integration.extension.fallback;

import me.bristermitten.mittenlib.config.Config;

@Config(requireSerialization = false)
public interface CustomTypeFallbackConfig {
    CustomTypeFallback customType();
}
