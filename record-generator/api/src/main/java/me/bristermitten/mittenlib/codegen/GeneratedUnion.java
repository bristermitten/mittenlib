package me.bristermitten.mittenlib.codegen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
public @interface GeneratedUnion {
    Class<?> source();
}
