package me.bristermitten.mittenlib.annotations.integration;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import me.bristermitten.mittenlib.config.Config;

@Config
public interface FeaturesConfig {
    default Map<String, Boolean> flags() {
        return Collections.emptyMap();
    }

    default Map<String, Boolean> flagsWithDefaults() {
        Map<String, Boolean> map = new LinkedHashMap<>();
        map.put("a", true);
        map.put("b", false);
        return map;
    }
}
