package me.bristermitten.mittenlib.records;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
public @interface GeneratedRecord {
    Class<?> source();
}
