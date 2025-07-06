package me.bristermitten.mittenlib.annotations.config;

import com.squareup.javapoet.*;
import io.toolisticon.aptk.tools.wrapper.AnnotationMirrorWrapper;
import me.bristermitten.mittenlib.annotations.util.PrivateAnnotations;
import me.bristermitten.mittenlib.annotations.util.TypeSpecUtil;
import me.bristermitten.mittenlib.annotations.util.TypesUtil;
import me.bristermitten.mittenlib.util.Strings;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;

/**
 * Generates accessor methods for configuration classes.
 * This class creates getter methods for fields and "with" methods that act as immutable setters,
 * returning new instances with modified values. It also handles method overriding and annotation
 * preservation when generating accessor methods.
 */
public class AccessorGenerator {
    private final TypesUtil typesUtil;
    private final MethodNames methodNames;

    @Inject
    public AccessorGenerator(
            TypesUtil typesUtil,
            MethodNames methodNames) {
        this.typesUtil = typesUtil;
        this.methodNames = methodNames;
    }

    /**
     * Creates a getter method for a field.
     *
     * @param typeSpecBuilder The builder for the type spec
     * @param element         The variable element
     * @param field           The field spec
     */
    public void createGetterMethod(TypeSpec.@NotNull Builder typeSpecBuilder, @NotNull VariableElement element, @NotNull FieldSpec field) {
        var safeName = getFieldAccessorName(element);

        var builder = MethodSpec.methodBuilder(safeName)
                .addModifiers(Modifier.PUBLIC)
                .returns(field.type)
                .addStatement("return " + field.name);

        if (typesUtil.isNullable(element)) {
            builder.addAnnotation(Nullable.class);
        } else {
            builder.addAnnotation(NotNull.class);
        }

        builder.addAnnotation(AnnotationSpec.builder(Contract.class)
                .addMember("pure", CodeBlock.of("true")).build());
        typeSpecBuilder.addMethod(builder.build());
    }

    /**
     * Creates a getter method that overrides an existing method.
     * This method preserves annotations from the original method (except for private annotations)
     * and adds appropriate nullability and contract annotations.
     *
     * @param typeSpecBuilder The builder for the type spec
     * @param overriding The executable element being overridden
     * @param fromField The field spec that the getter will return
     */
    public void createGetterMethodOverriding(TypeSpec.@NotNull Builder typeSpecBuilder, @NotNull ExecutableElement overriding, @NotNull FieldSpec fromField) {
        var builder = MethodSpec.methodBuilder(overriding.getSimpleName().toString())
                .addModifiers(Modifier.PUBLIC)
                .returns(fromField.type)
                .addStatement("return " + fromField.name)
                .addAnnotation(Override.class);

        for (AnnotationMirror annotationMirror : overriding.getAnnotationMirrors()) {
            if (PrivateAnnotations.isPrivate(AnnotationMirrorWrapper.wrap(annotationMirror)
                    .asElement()
                    .getQualifiedName())) {
                continue;
            }


            builder.addAnnotation(AnnotationSpec.get(annotationMirror));
        }
        if (typesUtil.isNullable(overriding)) {
            TypeSpecUtil.methodAddAnnotation(builder, Nullable.class);
        } else {
            TypeSpecUtil.methodAddAnnotation(builder, NotNull.class);
        }

        TypeSpecUtil.methodAddAnnotation(builder, Contract.class,
                b -> b.addMember("pure", CodeBlock.of("true")));
        typeSpecBuilder.addMethod(builder.build());
    }

    /**
     * Creates "with" methods (immutable setters) for each field.
     *
     * @param typeSpecBuilder   The builder for the type spec
     * @param className         The class name
     * @param fieldSpecs        The field specs
     * @param superClass        The superclass, if any
     * @param getSuperFieldName A function to get the field name for a superclass
     */
    public void createWithMethods(
            TypeSpec.@NotNull Builder typeSpecBuilder,
            ClassName className,
            @NotNull Map<VariableElement, FieldSpec> fieldSpecs,
            @Nullable TypeMirror superClass,
            @NotNull Function<TypeMirror, String> getSuperFieldName) {

        fieldSpecs.values().forEach(field -> {
            // Create a string representing the constructor parameters
            String constructorParams = Strings.joinWith(fieldSpecs.values(),
                    f2 -> {
                        if (f2.name.equals(field.name)) {
                            return f2.name; // we'll use the version from the parameter
                        }
                        return "this." + f2.name;
                    }, ", ");

            if (superClass != null) {
                var joiner = new StringJoiner(",")
                        .add(getSuperFieldName.apply(superClass));
                if (!constructorParams.isEmpty()) {
                    joiner.add(constructorParams);
                }
                constructorParams = joiner.toString();
            }
            typeSpecBuilder.addMethod(
                    MethodSpec.methodBuilder("with" + Strings.capitalize(field.name))
                            .addModifiers(Modifier.PUBLIC)
                            .returns(className)
                            .addParameter(ParameterSpec.builder(field.type, field.name).addModifiers(Modifier.FINAL).build())
                            .addStatement("return new $T(" + constructorParams + ")", className)
                            .build());
        });
    }

    /**
     * Gets the accessor name for a field.
     *
     * @param variableElement The variable element
     * @return The accessor name
     */
    private String getFieldAccessorName(@NotNull VariableElement variableElement) {
        return methodNames.safeMethodName(variableElement, (TypeElement) variableElement.getEnclosingElement());
    }
}
