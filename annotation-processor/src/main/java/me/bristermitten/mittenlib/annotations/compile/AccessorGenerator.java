package me.bristermitten.mittenlib.annotations.compile;

import com.squareup.javapoet.*;
import io.toolisticon.aptk.tools.wrapper.AnnotationMirrorWrapper;
import me.bristermitten.mittenlib.annotations.ast.AbstractConfigStructure;
import me.bristermitten.mittenlib.annotations.ast.ConfigTypeSource;
import me.bristermitten.mittenlib.annotations.ast.Property;
import me.bristermitten.mittenlib.annotations.util.PrivateAnnotations;
import me.bristermitten.mittenlib.annotations.util.TypeSpecUtil;
import me.bristermitten.mittenlib.annotations.util.TypesUtil;
import me.bristermitten.mittenlib.util.Strings;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import javax.inject.Inject;
import javax.lang.model.element.*;
import java.util.StringJoiner;

/**
 * Generates accessor methods for configuration classes.
 * This class creates getter methods for fields and "with" methods that act as immutable setters,
 * returning new instances with modified values. It also handles method overriding and annotation
 * preservation when generating accessor methods.
 */
public class AccessorGenerator {
    private final TypesUtil typesUtil;
    private final MethodNames methodNames;

    private final ConfigurationClassNameGenerator configurationClassNameGenerator;

    @Inject
    public AccessorGenerator(
            TypesUtil typesUtil,
            MethodNames methodNames, ConfigurationClassNameGenerator configurationClassNameGenerator) {
        this.typesUtil = typesUtil;
        this.methodNames = methodNames;
        this.configurationClassNameGenerator = configurationClassNameGenerator;
    }

    /**
     * Creates a getter method for a field.
     *
     * @param typeSpecBuilder The builder for the type spec
     * @param element         The variable element
     * @param field           The field spec
     */
    public void createGetterMethod(TypeSpec.Builder typeSpecBuilder, @NonNull VariableElement element, @NonNull FieldSpec field) {
        var safeName = getFieldAccessorName(element);

        var builder = MethodSpec.methodBuilder(safeName)
                .addModifiers(Modifier.PUBLIC)
                .returns(field.type)
                .addStatement("return " + field.name);


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
     * @param overriding      The executable element being overridden
     * @param fromField       The field spec that the getter will return
     */
    public void createGetterMethodOverriding(TypeSpec.@NonNull Builder typeSpecBuilder, @NonNull ExecutableElement overriding, @NonNull FieldSpec fromField) {
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


        TypeSpecUtil.methodAddAnnotation(builder, Contract.class,
                b -> b.addMember("pure", CodeBlock.of("true")));
        typeSpecBuilder.addMethod(builder.build());
    }

    /**
     * Creates "with" methods (immutable setters) for each field.
     *
     * @param typeSpecBuilder The builder for the type spec
     * @param ast             The config ast
     */
    public void createWithMethods(
            TypeSpec.Builder typeSpecBuilder,
            AbstractConfigStructure ast) {


        for (Property field : ast.properties()) {
            ClassName configImplClassName = configurationClassNameGenerator.generateConfigurationClassName(ast.source().element());
            MethodSpec.Builder withMethodBuilder = MethodSpec.methodBuilder("with" + Strings.capitalize(field.name()))
                    .addModifiers(Modifier.PUBLIC)
                    .returns(configImplClassName)
                    .addParameter(
                            ParameterSpec.builder(configurationClassNameGenerator.publicPropertyClassName(field), field.name())
                                    .addModifiers(Modifier.FINAL).build()
                    );

            if (ast instanceof AbstractConfigStructure.Union) {
                // make the with method abstract and then alternatives can override it
                withMethodBuilder.addModifiers(Modifier.ABSTRACT);
                continue;
            }

            // Create a string representing the constructor parameters
            String constructorParams = Strings.joinWith(ast.properties(),
                    f2 -> {
                        if (f2.name().equals(field.name())) {
                            return f2.name(); // we'll use the version from the parameter
                        }
                        return "this." + f2.name();
                    }, ", ");

            if (ast.source() instanceof ConfigTypeSource.ClassConfigTypeSource classSource && classSource.parent().isPresent()) {
//                var superClass = classSource.parent().get(); // TODO
                var joiner = new StringJoiner(", ")
                        .add("this.parent");
                if (!constructorParams.isEmpty()) {
                    joiner.add(constructorParams);
                }
                constructorParams = joiner.toString();
            }


            typeSpecBuilder.addMethod(
                    withMethodBuilder
                            .addStatement("return new $T(" + constructorParams + ")", configImplClassName)
                            .build());
        }
    }

    /**
     * Gets the accessor name for a field.
     *
     * @param variableElement The variable element
     * @return The accessor name
     */
    private String getFieldAccessorName(@NonNull VariableElement variableElement) {
        return methodNames.safeMethodName(variableElement, (TypeElement) variableElement.getEnclosingElement());
    }
}
