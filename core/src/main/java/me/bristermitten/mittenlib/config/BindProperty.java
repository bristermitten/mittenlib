package me.bristermitten.mittenlib.config;

import com.google.inject.Provider;
import com.google.inject.Provides;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifically marks a property in a configuration type as being bound to its type in Guice.
 * This is used to resolve ambiguity when a config type has multiple properties of the same type.
 * <p>
 * For example, if a config has two properties of type {@code ChildConfig}, the processor will not bind either due to the ambiguity,
 * but this annotation can be added to override this behaviour.
 * <pre>{@code
 * @Config
 * interface MyConfig {
 *
 *      SubConfig firstSubProp();
 *
 *      @BindProperty
 *      SubConfig secondSubProp();
 *
 *
 *      @Config interface SubConfig {
 *              int blah();
 *      }
 * }
 * }</pre>
 * <p>
 * This will cause a {@link Provides} method to be generated which binds {@code SubConfig} to a {@link Provider} returning {@code MyConfig#secondSubProp()}.
 * <p>
 * Annotating multiple properties of the same type with {@link BindProperty} will throw a compiler error.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface BindProperty {
}
