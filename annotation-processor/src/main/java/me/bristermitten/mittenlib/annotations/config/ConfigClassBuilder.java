package me.bristermitten.mittenlib.annotations.config;

import com.squareup.javapoet.*;
import me.bristermitten.mittenlib.annotations.util.StringUtil;
import me.bristermitten.mittenlib.config.Config;
import me.bristermitten.mittenlib.config.ConfigMapLoader;
import me.bristermitten.mittenlib.config.Configuration;
import me.bristermitten.mittenlib.config.Source;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConfigClassBuilder {
    private static final TypeName MAP_STRING_OBJ_NAMe = ParameterizedTypeName.get(Map.class, String.class, Object.class);
    private final ProcessingEnvironment environment;

    public ConfigClassBuilder(ProcessingEnvironment environment) {
        this.environment = environment;
    }

    private FieldSpec createFieldSpec(VariableElement element) {
        return FieldSpec.builder(
                        getTypeName(element.asType()),
                        element.getSimpleName().toString()
                ).addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .build();
    }

    private ParameterSpec createParameterSpec(VariableElement element) {
        return ParameterSpec.builder(
                        getTypeName(element.asType()),
                        element.getSimpleName().toString()
                ).addModifiers(Modifier.FINAL)
                .build();
    }

    private TypeName getTypeName(TypeMirror typeMirror) {
        if (typeMirror.getKind().isPrimitive()) {
            return TypeName.get(typeMirror);
        }
        final TypeElement element = (TypeElement) environment.getTypeUtils().asElement(typeMirror);
        final PackageElement packageElement = environment.getElementUtils().getPackageOf(element);
        if (packageElement.isUnnamed()) {
            throw new IllegalArgumentException("Unnamed packages are not supported");
        }
        String packageName = packageElement.toString();
        return ConfigClassNameGenerator.generateConfigClassName(element)
                .map(className -> (TypeName) ClassName.get(packageName, className))
                .orElseGet(() -> TypeName.get(typeMirror));
    }

    public JavaFile createConfigClass(TypeElement classType, List<VariableElement> variableElements) {
        final PackageElement packageOf = environment.getElementUtils().getPackageOf(classType);
        final String packageName = packageOf.isUnnamed() ? "" : packageOf.toString();
        final String simpleClassName = ConfigClassNameGenerator.generateConfigClassName(classType)
                .orElseThrow(() -> new IllegalArgumentException("Cannot determine name for @Config class " + classType.getQualifiedName()));
        ClassName className = ClassName.get(packageName, simpleClassName);
        TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        createConfigurationField(classType, className, typeSpecBuilder);

        final List<FieldSpec> fieldSpecs = variableElements.stream()
                .map(this::createFieldSpec)
                .collect(Collectors.toList());

        fieldSpecs.forEach(typeSpecBuilder::addField);

        addAllArgsConstructor(variableElements, fieldSpecs, typeSpecBuilder);

        typeSpecBuilder.addMethod(
                createDeserializeMethod(classType, className, variableElements)
        );

        // Generate getter methods
        fieldSpecs.forEach(field ->
                typeSpecBuilder.addMethod(MethodSpec.methodBuilder(field.name)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(field.type)
                        .addStatement("return " + field.name)
                        .build()));
        // Generate copy setter methods
        fieldSpecs.forEach(field -> {
            String constructorParams = fieldSpecs.stream()
                    .map(f2 -> {
                        if (f2.equals(field)) {
                            return f2.name; // we'll use the version from the parameter
                        }
                        return "this." + f2.name;
                    }).
                    collect(Collectors.joining(", "));

            typeSpecBuilder.addMethod(
                    MethodSpec.methodBuilder("with" + StringUtil.capitalize(field.name))
                            .addModifiers(Modifier.PUBLIC)
                            .returns(className)
                            .addParameter(ParameterSpec.builder(field.type, field.name).addModifiers(Modifier.FINAL).build())
                            .addStatement("return new $T(" + constructorParams + ")", className)
                            .build());
        });

        return JavaFile.builder(packageName, typeSpecBuilder.build())
                .build();
    }

    private void addAllArgsConstructor(List<VariableElement> variableElements, List<FieldSpec> fieldSpecs, TypeSpec.Builder typeSpecBuilder) {
        final MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameters(variableElements.stream()
                        .map(this::createParameterSpec)
                        .collect(Collectors.toList()));

        fieldSpecs.forEach(field ->
                constructorBuilder.addStatement(String.format("this.%1$s = %1$s", field.name)));
        typeSpecBuilder.addMethod(constructorBuilder.build());
    }

    private void createConfigurationField(TypeElement classType, ClassName className, TypeSpec.Builder typeSpecBuilder) {
        final Source annotation = classType.getAnnotation(Source.class);
        if (annotation != null) {
            // Create Configuration type
            final FieldSpec configField = FieldSpec.builder(className, "CONFIG")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("new $T<>($S, $T.class)", Configuration.class, annotation.value(), className)
                    .build();
            typeSpecBuilder.addField(configField);
        }
    }

    private MethodSpec createDeserializeMethod(TypeElement daoType, TypeName className, List<VariableElement> variableElements) {
        final MethodSpec.Builder builder = MethodSpec.methodBuilder("deserialize")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(className)
                .addParameter(ParameterSpec.builder(MAP_STRING_OBJ_NAMe, "data").addModifiers(Modifier.FINAL).build());
        // create the dao
        builder.addStatement("$1T dao = new $1T()", (daoType.asType()));
        for (VariableElement element : variableElements) {
            final TypeMirror typeMirror = element.asType();
            final TypeMirror safeType = getSafeType(environment.getTypeUtils(), typeMirror);
            final TypeName elementType = getTypeName(typeMirror);
            final TypeName safeElementType = getTypeName(safeType);
            final Name variableName = element.getSimpleName();
            String key = variableName.toString();

            final boolean isConfigType = typeMirror instanceof DeclaredType &&
                                         ((DeclaredType) typeMirror).asElement().getAnnotation(Config.class) != null;

            builder.addStatement(String.format("$T %s", variableName), elementType);
            final String fromMapName = variableName + "FromMap";
            builder.addStatement(String.format("Object %s = data.getOrDefault($S, dao.%s)", fromMapName, variableName), key);
            builder.beginControlFlow(String.format("if (%s instanceof $T)", fromMapName), safeElementType);
            builder.addStatement(String.format("%s = ($T) %s", variableName, fromMapName), elementType);
            if (isConfigType) {
                builder.nextControlFlow(String.format("else if (%s instanceof Map)", fromMapName));
                builder.addStatement(String.format("%s = $T.deserialize(($T) %s)", variableName, fromMapName),
                        elementType, MAP_STRING_OBJ_NAMe);
            }
            builder.nextControlFlow("else");
            builder.addStatement(String.format("throw $T.throwNotFound($S, $S, $T.class, %s)", fromMapName), ConfigMapLoader.class, variableName, typeMirror, daoType);
            builder.endControlFlow();
        }

        builder.addStatement(String.format("return new $T(%s)",
                        variableElements.stream().map(VariableElement::getSimpleName).collect(Collectors.joining(", "))),
                className);

        return builder.build();
    }

    private TypeMirror getSafeType(Types types, TypeMirror typeMirror) {
        if (typeMirror.getKind().isPrimitive()) {
            return types.boxedClass((PrimitiveType) typeMirror).asType();
        }
        return types.erasure(typeMirror);
    }
}
