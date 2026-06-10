package me.bristermitten.mittenlib.annotations.util;

import java.util.Set;
import me.bristermitten.mittenlib.collections.Sets;
import me.bristermitten.mittenlib.config.names.ConfigName;

public class PrivateAnnotations {
    private static final Set<String> PRIVATE_ANNOTATIONS = Sets.of(ConfigName.class.getName());

    public static boolean isPrivate(String annotation) {
        return PRIVATE_ANNOTATIONS.contains(annotation);
    }
}
