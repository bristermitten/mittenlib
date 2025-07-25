package me.bristermitten.mittenlib.codegen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Specifies how match methods should be generated for a union.
 * See {@link MatchStrategies} for available strategies.
 */
@Target(ElementType.TYPE)
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface MatchStrategy {
    MatchStrategies value() default MatchStrategies.NOMINAL;
}
