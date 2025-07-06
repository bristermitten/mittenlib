package me.bristermitten.mittenlib.annotations.integration;

import io.toolisticon.cute.PassIn;
import me.bristermitten.mittenlib.config.Config;
import me.bristermitten.mittenlib.config.ConfigUnion;
import me.bristermitten.mittenlib.config.Source;

@Config
@PassIn
@ConfigUnion
@Source("integration/UnionConfig_dummy.yml")
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
