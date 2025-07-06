package me.bristermitten.mittenlib.annotations.integration;

import me.bristermitten.mittenlib.config.Config;
import me.bristermitten.mittenlib.config.Source;
import me.bristermitten.mittenlib.config.names.NamingPattern;

@NamingPattern(value = me.bristermitten.mittenlib.config.names.NamingPatterns.LOWER_KEBAB_CASE)
@Source(value = "lang.yml")
@Config
public class SuperclassConfigDTO {
    public Child1DTO child1;
    public Child2DTO child2;
    public Child3DTO child3;
    public Child4DTO child4;

    @Config
    static class Child1DTO {
        int a;
    }

    @Config
    static class Child2DTO extends Child1DTO {
        int b;
    }

    @Config
    static class Child3DTO extends Child2DTO {
        int c;
    }

    @Config
    static class Child4DTO extends Child3DTO {
        int d;
    }
}