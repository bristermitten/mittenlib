package me.bristermitten.mittenlib.annotations.parser.configs;

import io.toolisticon.cute.PassIn;
import me.bristermitten.mittenlib.config.Config;

@Config
@PassIn
public interface IntersectionConfig {
    String base();

    @Config
    interface ChildIntersectionConfig extends IntersectionConfig {
        String extra();
    }
}
