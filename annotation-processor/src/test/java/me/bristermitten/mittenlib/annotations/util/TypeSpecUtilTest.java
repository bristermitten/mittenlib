package me.bristermitten.mittenlib.annotations.util;

import static org.junit.jupiter.api.Assertions.*;

import com.palantir.javapoet.MethodSpec;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.junit.jupiter.api.Test;

class TypeSpecUtilTest {

    @Retention(RetentionPolicy.RUNTIME)
    @interface NonRepeatableAnnotation {}

    @Retention(RetentionPolicy.RUNTIME)
    @Repeatable(RepeatableAnnotations.class)
    @interface RepeatableAnnotation {}

    @Retention(RetentionPolicy.RUNTIME)
    @interface RepeatableAnnotations {
        RepeatableAnnotation[] value();
    }

    @Test
    void testMethodAddAnnotationAddsNonRepeatableOnce() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("test");
        TypeSpecUtil.methodAddAnnotation(builder, NonRepeatableAnnotation.class);
        assertEquals(1, builder.build().annotations().size());

        TypeSpecUtil.methodAddAnnotation(builder, NonRepeatableAnnotation.class);
        assertEquals(1, builder.build().annotations().size()); // Should not add again
    }

    @Test
    void testMethodAddAnnotationAddsRepeatableMultipleTimes() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("test");
        TypeSpecUtil.methodAddAnnotation(builder, RepeatableAnnotation.class);
        assertEquals(1, builder.build().annotations().size());

        TypeSpecUtil.methodAddAnnotation(builder, RepeatableAnnotation.class);
        assertEquals(2, builder.build().annotations().size()); // Should add again
    }
}
