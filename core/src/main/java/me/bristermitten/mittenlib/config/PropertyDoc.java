package me.bristermitten.mittenlib.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides documentation for a configuration property that will be shown in error messages.
 * This allows developers to customize the error messages that users see when there are issues
 * with specific configuration properties.
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PropertyDoc {
    
    /**
     * A brief description of what this property does.
     * This will be shown in error messages to help users understand the property.
     * 
     * @return the description of the property
     */
    String description() default "";
    
    /**
     * An example value for this property.
     * This will be shown in error messages to help users understand what kind of value is expected.
     * 
     * Example: "localhost" for a hostname property, "3306" for a port property
     * 
     * @return an example value for the property
     */
    String example() default "";
    
    /**
     * Additional notes or tips about this property.
     * This can include common mistakes, best practices, or other helpful information.
     * 
     * @return additional notes about the property
     */
    String note() default "";
}
