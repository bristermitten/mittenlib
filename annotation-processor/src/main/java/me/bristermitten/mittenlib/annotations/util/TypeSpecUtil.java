package me.bristermitten.mittenlib.annotations.util;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.MethodSpec;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Repeatable;
import java.util.function.Consumer;

public class TypeSpecUtil {

    public static void methodAddAnnotation(MethodSpec.@NotNull Builder builder, @NotNull Class<?> annotation) {
        // check if the builder already has the annotation, if so, only add it if the annotation is repeatable
        methodAddAnnotation(builder, annotation, b -> {
        });
    }

    public static void methodAddAnnotation(MethodSpec.@NotNull Builder builder, @NotNull Class<?> annotation, @NotNull Consumer<AnnotationSpec.Builder> builderConsumer) {
        // check if the builder already has the annotation, if so, only add it if the annotation is repeatable
        boolean hasAnnotation = builder.annotations.stream()
                .anyMatch(existing -> existing.type.toString().equals(annotation.getCanonicalName()));


        if (!hasAnnotation || annotation.isAnnotationPresent(Repeatable.class)) {
            AnnotationSpec.Builder annotationBuilder = AnnotationSpec.builder(annotation);
            builderConsumer.accept(annotationBuilder);
            builder.addAnnotation(annotationBuilder.build());
        }
    }
}
