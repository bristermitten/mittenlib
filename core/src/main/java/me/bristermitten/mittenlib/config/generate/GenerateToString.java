package me.bristermitten.mittenlib.config.generate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Inherited
@CascadeToInnerClasses
public @interface GenerateToString {
}
