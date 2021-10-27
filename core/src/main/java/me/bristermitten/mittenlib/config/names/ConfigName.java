package me.bristermitten.mittenlib.config.names;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
public @interface ConfigName {
    String value();
}
