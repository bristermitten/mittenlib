package me.bristermitten.mittenlib.annotations.util;

import com.squareup.javapoet.AnnotationSpec;
import me.bristermitten.mittenlib.annotations.ast.Property;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;

public class Nullity {
    public static Class<? extends Annotation> getNullityAnnotation(Property property) {
        return property.settings().isNullable() ? Nullable.class : NonNull.class;
    }

    public static AnnotationSpec getNullityAnnotationSpec(Property property) {
        return AnnotationSpec.builder(getNullityAnnotation(property)).build();
    }
}
