package me.bristermitten.mittenlib.annotations.config;

import com.squareup.javapoet.*;
import me.bristermitten.mittenlib.annotations.util.TypesUtil;
import me.bristermitten.mittenlib.util.Strings;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;

/**
 * Generates accessor methods (getters and setters) for configuration classes.
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
    public void createGetterMethod(TypeSpec.Builder typeSpecBuilder, VariableElement element, FieldSpec field) {
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
     * Creates "with" methods (immutable setters) for each field.
     *
     * @param typeSpecBuilder   The builder for the type spec
     * @param className         The class name
     * @param fieldSpecs        The field specs
     * @param superClass        The superclass, if any
     * @param getSuperFieldName A function to get the field name for a superclass
     */
    public void createWithMethods(
            TypeSpec.Builder typeSpecBuilder,
            ClassName className,
            Map<VariableElement, FieldSpec> fieldSpecs,
            @Nullable TypeMirror superClass,
            Function<TypeMirror, String> getSuperFieldName) {

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
    private String getFieldAccessorName(VariableElement variableElement) {
        return methodNames.safeMethodName(variableElement, (TypeElement) variableElement.getEnclosingElement());
    }
}