package me.bristermitten.mittenlib.annotations.config;

import com.squareup.javapoet.*;
import me.bristermitten.mittenlib.annotations.util.TypesUtil;
import me.bristermitten.mittenlib.config.*;
import me.bristermitten.mittenlib.util.Result;
import me.bristermitten.mittenlib.util.Strings;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class ConfigClassBuilder {
    private static final TypeName MAP_STRING_OBJ_NAME = ParameterizedTypeName.get(Map.class, String.class, Object.class);
    private static final ClassName RESULT_CLASS_NAME = ClassName.get(Result.class);
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


        createDeserializeMethod(typeSpecBuilder, classType, className, variableElements);

        // Generate getter methods
        fieldSpecs.forEach(field ->
                typeSpecBuilder.addMethod(MethodSpec.methodBuilder(field.name)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(field.type)
                        .addStatement("return " + field.name)
                        .build()));
        // Generate copy setter methods
        fieldSpecs.forEach(field -> {
            String constructorParams = Strings.joinWith(fieldSpecs,
                    f2 -> {
                        if (f2.equals(field)) {
                            return f2.name; // we'll use the version from the parameter
                        }
                        return "this." + f2.name;
                    }, ", ");

            typeSpecBuilder.addMethod(
                    MethodSpec.methodBuilder("with" + Strings.capitalize(field.name))
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
                constructorBuilder.addStatement(format("this.%1$s = %1$s", field.name)));
        typeSpecBuilder.addMethod(constructorBuilder.build());
    }

    private void createConfigurationField(TypeElement classType, ClassName className, TypeSpec.Builder typeSpecBuilder) {
        final Source annotation = classType.getAnnotation(Source.class);
        if (annotation == null) {
            return;
        }

        // Create Configuration type
        final TypeName type = ParameterizedTypeName.get(ClassName.get(Configuration.class), className);
        final FieldSpec configField = FieldSpec.builder(type, "CONFIG")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("new $T<>($S, $T.class, $T::deserialize)", Configuration.class, annotation.value(), className, className)
                .build();
        typeSpecBuilder.addField(configField);
    }

    private MethodSpec createDeserializeMethodFor(TypeElement daoType, VariableElement element) {
        final TypeName elementType = getTypeName(element.asType());
        final Name variableName = element.getSimpleName();
        final TypeMirror safeType = TypesUtil.getSafeType(environment.getTypeUtils(), element.asType());
        final TypeName safeTypeName = getTypeName(safeType);

        final MethodSpec.Builder builder = MethodSpec.methodBuilder("deserialize_" + variableName)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(ParameterizedTypeName.get(RESULT_CLASS_NAME, safeTypeName))
                .addParameter(ParameterSpec.builder(DeserializationContext.class, "context").build())
                .addParameter(ParameterSpec.builder(ClassName.get(daoType), "dao").build());

        builder.addStatement("$T $$data = context.getData()", MAP_STRING_OBJ_NAME);
        builder.addStatement(format("$T %s", variableName), elementType);
        final String fromMapName = variableName + "FromMap";
        final String key = FieldClassNameGenerator.getConfigFieldName(element);
        builder.addStatement(format("Object %s = $$data.getOrDefault($S, dao.%s)", fromMapName, variableName), key);

        if (!isNullable(element)) {
            builder.beginControlFlow(String.format("if (%s == null)", fromMapName));
            builder.addStatement(String.format("return $T.fail($T.throwNotFound($S, $S, $T.class, %s))", fromMapName),
                    Result.class, ConfigMapLoader.class, variableName, element, daoType);
            builder.endControlFlow();
        }

        builder.beginControlFlow(format("if (%s instanceof $T)", fromMapName), safeTypeName);
        builder.addStatement(format("return $T.ok(($T) %s)", fromMapName), Result.class, elementType);
        builder.endControlFlow();

        if (isConfigType(element.asType())) {
            builder.beginControlFlow(String.format("if (%s instanceof $T)", fromMapName), Map.class);
            builder.addStatement(String.format("$1T mapData = ($1T) %s", fromMapName), MAP_STRING_OBJ_NAME);
            builder.addStatement("return $T.deserialize(context.withData(mapData))", elementType);
            builder.endControlFlow();
        }

        builder.addStatement(String.format("return context.getMapper().map(%1$s, $T.class)", fromMapName), safeTypeName);
        return builder.build();
    }

    private boolean isConfigType(TypeMirror mirror) {
        return mirror instanceof DeclaredType &&
               ((DeclaredType) mirror).asElement().getAnnotation(Config.class) != null;
    }

    private boolean isNullable(VariableElement element) {
        for (AnnotationMirror ann : element.getAnnotationMirrors()) {
            if (ann.getAnnotationType().asElement().getSimpleName().toString().equals("Nullable")) {
                return true;
            }
        }
        return false;
    }

    private void createDeserializeMethod(TypeSpec.Builder typeSpecBuilder, TypeElement daoType, TypeName className, List<VariableElement> variableElements) {

        final MethodSpec.Builder builder = MethodSpec.methodBuilder("deserialize")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ParameterizedTypeName.get(RESULT_CLASS_NAME, className))
                .addParameter(ParameterSpec.builder(DeserializationContext.class, "context").addModifiers(Modifier.FINAL).build());

        builder.addStatement("$1T dao = new $1T()", (daoType.asType()));

        final List<MethodSpec> deserializeMethods = variableElements.stream()
                .map(variableElement -> createDeserializeMethodFor(daoType, variableElement))
                .collect(Collectors.toList());

        deserializeMethods.forEach(typeSpecBuilder::addMethod);

        final StringBuilder expressionBuilder = new StringBuilder();
        expressionBuilder.append("return ");
        int i = 0;
        for (MethodSpec deserializeMethod : deserializeMethods) {
            expressionBuilder.append(deserializeMethod.name)
                    .append("(context, dao)")
                    .append(".flatMap(var")
                    .append(i++)
                    .append("->\n");
        }
        expressionBuilder.append("$T.ok(new $T(");
        for (int i1 = 0; i1 < i; i1++) {
            expressionBuilder.append("var")
                    .append(i1);
            if (i1 != i - 1) {
                expressionBuilder.append(", ");
            }
        }
        expressionBuilder.append("))"); // Close ok and new parens
        for (int $ = 0; $ < i; $++) {
            expressionBuilder.append(")"); // close all the flatMap parens
        }


        builder.addStatement(expressionBuilder.toString(), Result.class, className);

        typeSpecBuilder.addMethod(builder.build());
    }
}
