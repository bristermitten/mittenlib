package me.bristermitten.mittenlib.annotations.util;

import java.io.IOException;
import java.lang.annotation.*;
import java.nio.charset.StandardCharsets;
import me.bristermitten.mittenlib.config.generate.CascadeToInnerClasses;

public class IntegrationTests {
    public static String loadResourceString(String resource) throws IOException {
        try (var res = IntegrationTests.class.getClassLoader().getResourceAsStream(resource)) {
            if (res == null) {
                throw new IOException("Resource not found: " + resource);
            }
            return new String(res.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface NonRepeatableAnnotation {}

    @Repeatable(RepeatableAnnotation.Container.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface RepeatableAnnotation {
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.TYPE)
        @interface Container {
            RepeatableAnnotation[] value();
        }
    }

    @CascadeToInnerClasses
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface CascadingAnnotation {}
}
