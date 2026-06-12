package me.bristermitten.mittenlib.annotations.util;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import java.lang.annotation.Repeatable;
import java.util.function.Consumer;

public class TypeSpecUtil {

    public static void methodAddAnnotation(MethodSpec.Builder builder, Class<?> annotation) {
        // check if the builder already has the annotation, if so, only add it if the annotation is
        // repeatable
        methodAddAnnotation(builder, annotation, b -> {});
    }

    public static void methodAddAnnotation(
            MethodSpec.Builder builder, Class<?> annotation, Consumer<AnnotationSpec.Builder> builderConsumer) {
        // check if the builder already has the annotation, if so, only add it if the annotation is
        // repeatable
        boolean hasAnnotation = builder.build().annotations().stream()
                .anyMatch(existing -> existing.type().equals(TypeName.get(annotation)));

        if (!hasAnnotation || annotation.isAnnotationPresent(Repeatable.class)) {
            AnnotationSpec.Builder annotationBuilder = AnnotationSpec.builder(annotation);
            builderConsumer.accept(annotationBuilder);
            builder.addAnnotation(annotationBuilder.build());
        }
    }
}
