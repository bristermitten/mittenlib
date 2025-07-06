package me.bristermitten.mittenlib.annotations.integration;

import io.toolisticon.cute.PassIn;
import me.bristermitten.mittenlib.config.Config;
import me.bristermitten.mittenlib.config.ConfigUnion;

@Config
@PassIn
@ConfigUnion
public interface UnionConfig {
    String common();

    @Config
    interface Child1Config extends UnionConfig {
        String hello();
    }

    @Config
    interface Child2Config extends UnionConfig {
        int world();
    }
}
