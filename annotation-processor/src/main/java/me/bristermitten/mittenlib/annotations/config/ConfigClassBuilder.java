package me.bristermitten.mittenlib.annotations.config;

import com.google.gson.reflect.TypeToken;
import com.squareup.javapoet.*;
import me.bristermitten.mittenlib.annotations.util.ElementsFinder;
import me.bristermitten.mittenlib.annotations.util.TypesUtil;
import me.bristermitten.mittenlib.config.*;
import me.bristermitten.mittenlib.util.Result;
import me.bristermitten.mittenlib.util.Strings;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class ConfigClassBuilder {
    private static final TypeName MAP_STRING_OBJ_NAME = ParameterizedTypeName.get(Map.class, String.class, Object.class);
    private static final ClassName RESULT_CLASS_NAME = ClassName.get(Result.class);
    private final ProcessingEnvironment environment;
    private final ElementsFinder elementsFinder;
    private final MethodNames methodNames;

    public ConfigClassBuilder(ProcessingEnvironment environment, ElementsFinder elementsFinder, MethodNames methodNames) {
        this.environment = environment;
        this.elementsFinder = elementsFinder;
        this.methodNames = methodNames;
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

    private void addInnerClasses(TypeSpec.Builder typeSpecBuilder, TypeElement classType) {
        classType.getEnclosedElements()
                .stream()
                .filter(TypeElement.class::isInstance)
                .map(TypeElement.class::cast)
                .filter(element -> element.getAnnotation(Config.class) != null)
                .forEach(typeElement -> {
                    TypeSpec configClass = createConfigClass(typeElement,
                            elementsFinder.getApplicableVariableElements(typeElement));
                    configClass = configClass.toBuilder().addModifiers(Modifier.STATIC).build();
                    typeSpecBuilder.addType(configClass);
                });
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
        return ConfigClassNameGenerator.generateFullConfigClassName(environment, element)
                .map(TypeName.class::cast)
                .orElseGet(() -> getStandardTypeName(typeMirror));
    }

    private TypeName getStandardTypeName(TypeMirror mirror) {
        if (mirror instanceof DeclaredType declaredType) {
            TypeElement element = (TypeElement) declaredType.asElement();
            List<? extends TypeMirror> typeArguments = ((DeclaredType) mirror).getTypeArguments();
            if (typeArguments.isEmpty()) {
                return TypeName.get(mirror);
            }
            List<TypeName> properArguments = typeArguments.stream()
                    .map(this::getTypeName)
                    .toList();

            return ParameterizedTypeName.get(ClassName.get(element), properArguments.toArray(new TypeName[0]));
        }
        return TypeName.get(mirror);
    }

    private TypeSpec createConfigClass(TypeElement classType,
                                       List<VariableElement> variableElements) {
        final ClassName className =
                ConfigClassNameGenerator.generateFullConfigClassName(environment, classType)
                        .orElseThrow(() -> new IllegalArgumentException("Cannot determine name for @Config class " + classType.getQualifiedName()));
        return createConfigClass(classType, variableElements, className);
    }

    private void createGetterMethod(TypeSpec.Builder typeSpecBuilder, VariableElement element, FieldSpec field) {
        var safeName = getFieldAccessorName(element);

        var builder = MethodSpec.methodBuilder(safeName)
                .addModifiers(Modifier.PUBLIC)
                .returns(field.type)
                .addStatement("return " + field.name);

        if (isNullable(element)) {
            builder.addAnnotation(Nullable.class);
        } else {
            builder.addAnnotation(NotNull.class);
        }

        builder.addAnnotation(AnnotationSpec.builder(Contract.class)
                .addMember("pure", CodeBlock.of("true")).build());
        typeSpecBuilder.addMethod(builder.build());
    }

    private TypeSpec createConfigClass(TypeElement classType,
                                       List<VariableElement> variableElements,
                                       ClassName className) {

        TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC);


        var superClass = classType.getSuperclass();
        if (superClass.getKind() == TypeKind.NONE) {
            superClass = null;
        }
        if (superClass != null && !superClass.toString().equals("java.lang.Object")) {
            if (!isConfigType(superClass)) {
                throw new IllegalArgumentException("Superclass of @Config class must be a @Config class, was " + superClass);
            }
            typeSpecBuilder.superclass(getTypeName(superClass));
        }

        createConfigurationField(classType, className, typeSpecBuilder);

        final Map<VariableElement, FieldSpec> fieldSpecs = variableElements.stream()
                .collect(Collectors.toMap(Function.identity(), this::createFieldSpec, (x, y) -> y, LinkedHashMap::new));

        fieldSpecs.values().forEach(typeSpecBuilder::addField);
        addInnerClasses(typeSpecBuilder, classType);
        addAllArgsConstructor(variableElements, fieldSpecs.values(), typeSpecBuilder, superClass);


        createDeserializeMethod(typeSpecBuilder, classType, className, variableElements);

        // Generate getter methods
        fieldSpecs.forEach((elem, field) -> createGetterMethod(typeSpecBuilder, elem, field));

        // Generate copy setter methods
        fieldSpecs.values().forEach(field -> {
            String constructorParams = Strings.joinWith(fieldSpecs.values(),
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

        return typeSpecBuilder.build();
    }

    public JavaFile createConfigFile(TypeElement classType,
                                     List<VariableElement> variableElements) {
        final ClassName className =
                ConfigClassNameGenerator.generateFullConfigClassName(environment, classType)
                        .orElseThrow(() -> new IllegalArgumentException("Cannot determine name for @Config class " + classType.getQualifiedName()));
        final TypeSpec configClass = createConfigClass(classType, variableElements, className);

        return JavaFile.builder(className.packageName(), configClass).build();
    }

    private void addAllArgsConstructor
            (List<VariableElement> variableElements, Collection<FieldSpec> fieldSpecs, TypeSpec.Builder typeSpecBuilder,
             @Nullable TypeMirror superclass) {
        final MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameters(variableElements.stream()
                        .map(this::createParameterSpec)
                        .toList());

        if (superclass != null) {
            List<? extends Element> superElements = environment.getTypeUtils().asElement(superclass)
                    .getEnclosedElements();
            constructorBuilder.addStatement("super($L)",
                    variableElements.stream()
                            .filter(superElements::contains)
                            .map(variableElement -> variableElement.getSimpleName().toString())
                            .collect(Collectors.joining(", ")));
        }

        fieldSpecs.forEach(field ->
                constructorBuilder.addStatement(format("this.%1$s = %1$s", field.name)));
        typeSpecBuilder.addMethod(constructorBuilder.build());
    }

    private void createConfigurationField(TypeElement classType, ClassName className, TypeSpec.Builder
            typeSpecBuilder) {
        final Source annotation = classType.getAnnotation(Source.class);
        if (annotation == null) {
            return;
        }

        // Create Configuration type
        final TypeName type = ParameterizedTypeName.get(ClassName.get(Configuration.class), className);
        final FieldSpec configField = FieldSpec.builder(type, "CONFIG")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("new $T<>($S, $T.class, $T::%s)".formatted(getDeserializeMethodName(className)), Configuration.class, annotation.value(), className, className)
                .build();
        typeSpecBuilder.addField(configField);
    }

    private String getFieldAccessorName(VariableElement variableElement) {
        return methodNames.safeMethodName(variableElement, (TypeElement) variableElement.getEnclosingElement());
    }

    private MethodSpec createDeserializeMethodFor(TypeElement daoType, VariableElement element) {
        TypeMirror typeMirror = element.asType();
        final TypeName elementType = getTypeName(typeMirror);
        final Name variableName = element.getSimpleName();
        final TypeMirror boxedType = TypesUtil.getBoxedType(environment.getTypeUtils(), typeMirror);
        final TypeName boxedTypeName = getTypeName(boxedType);

        final MethodSpec.Builder builder = MethodSpec.methodBuilder("deserialize_" + variableName)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(ParameterizedTypeName.get(RESULT_CLASS_NAME, boxedTypeName))
                .addParameter(ParameterSpec.builder(DeserializationContext.class, "context").build())
                .addParameter(ParameterSpec.builder(ClassName.get(daoType), "dao").build());

        builder.addStatement("$T $$data = context.getData()", MAP_STRING_OBJ_NAME);
        builder.addStatement(format("$T %s", variableName), elementType);
        final String fromMapName = variableName + "FromMap";
        final String key = FieldClassNameGenerator.getConfigFieldName(element);
        var defaultName = element.getSimpleName();
        builder.addStatement(format("Object %s = $$data.getOrDefault($S, dao.%s)", fromMapName, defaultName), key);

        if (!isNullable(element)) {
            builder.beginControlFlow(String.format("if (%s == null)", fromMapName));
            builder.addStatement(String.format("return $T.fail($T.throwNotFound($S, $S, $T.class, %s))", fromMapName),
                    Result.class, ConfigMapLoader.class, variableName, element, daoType);
            builder.endControlFlow();
        } else {
            // Short circuit the null rather than trying any deserialization
            builder.beginControlFlow(String.format("if (%s == null)", fromMapName));
            builder.addStatement("return $T.ok(null)", Result.class);
            builder.endControlFlow();
        }

        if (!(elementType instanceof ParameterizedTypeName)) {
            /*
             Construct a simple check that does
               if (fromMap instanceof X) return fromMap; NOSONAR this is not code you stupid program
             Useful when the type is a primitive or String
             This is only safe to do with non-parameterized types, type erasure and all
            */
            final TypeMirror safeType = TypesUtil.getSafeType(environment.getTypeUtils(), typeMirror);
            final TypeName safeTypeName = getTypeName(safeType);
            builder.beginControlFlow(format("if (%s instanceof $T)", fromMapName), safeTypeName);
            builder.addStatement(format("return $T.ok(($T) %s)", fromMapName), Result.class, elementType);
            builder.endControlFlow();
        } else if (typeMirror instanceof DeclaredType declaredType) {
            // This is really cursed, but it's the only real way
            // When the type is a List<T> or Map<_, T> then we need to first load it as a C<Map<String, Object>> then
            // apply the deserialize function to each element. Otherwise, gson would try to deserialize it without
            // using the generated deserialization method and produce inconsistent results.
            String canonicalName = environment.getTypeUtils().erasure(typeMirror).toString();
            if (canonicalName.equals("java.util.List")) {
                var listType = declaredType.getTypeArguments().get(0);
                if (isConfigType(listType)) {
                    TypeName listTypeName = getTypeName(listType);
                    builder.addStatement("return $T.deserializeList(%s, context, $T::%s)".formatted(fromMapName, getDeserializeMethodName(listTypeName)), CollectionsUtils.class,
                            listTypeName);
                    return builder.build();
                }
            }
            if (canonicalName.equals("java.util.Map")) {
                var mapType = declaredType.getTypeArguments().get(1);
                var keyType = declaredType.getTypeArguments().get(0);

                if (isConfigType(mapType)) {
                    TypeName mapTypeName = getTypeName(mapType);
                    builder.addStatement("return $T.deserializeMap($T.class, %s, context, $T::%s)".formatted(fromMapName, getDeserializeMethodName(mapTypeName)), CollectionsUtils.class,
                            getTypeName(TypesUtil.getSafeType(environment.getTypeUtils(), keyType)),
                            mapTypeName);
                    return builder.build();
                }
            }
        }

        if (isConfigType(typeMirror)) {
            builder.beginControlFlow(String.format("if (%s instanceof $T)", fromMapName), Map.class);
            builder.addStatement(String.format("$1T mapData = ($1T) %s", fromMapName), MAP_STRING_OBJ_NAME);
            builder.addStatement("return $T.%s(context.withData(mapData))".formatted(getDeserializeMethodName(elementType)), elementType);
            builder.endControlFlow();
        }


        // If no shortcuts work, pass it to the context and do some dynamic-ish deserialization
        builder.addStatement(String.format("return context.getMapper().map(%1$s, new $T<$T>(){})", fromMapName), TypeToken.class, boxedTypeName);
        return builder.build();
    }

    private boolean isConfigType(TypeMirror mirror) {
        return mirror instanceof DeclaredType declaredType &&
                declaredType.asElement().getAnnotation(Config.class) != null;
    }

    private boolean isNullable(VariableElement element) {
        for (AnnotationMirror ann : element.getAnnotationMirrors()) {
            if (ann.getAnnotationType().asElement().getSimpleName().toString().equals("Nullable")) {
                return true;
            }
        }
        return false;
    }

    private String getDeserializeMethodName(TypeName name) {
        if (name instanceof ClassName cn) {
            return "deserialize" + cn.simpleName();
        }
        return "deserialize" + name;

    }

    private void createDeserializeMethod(TypeSpec.Builder typeSpecBuilder, TypeElement daoType, ClassName
            className, List<VariableElement> variableElements) {

        final MethodSpec.Builder builder = MethodSpec.methodBuilder(getDeserializeMethodName(className))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ParameterizedTypeName.get(RESULT_CLASS_NAME, className))
                .addParameter(ParameterSpec.builder(DeserializationContext.class, "context").addModifiers(Modifier.FINAL).build());

        builder.addStatement("$1T dao = new $1T()", (daoType.asType()));

        final List<MethodSpec> deserializeMethods = variableElements.stream()
                .map(variableElement -> createDeserializeMethodFor(daoType, variableElement))
                .toList();

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
        expressionBuilder.append(")".repeat(Math.max(0, i))); // close all the flatMap parens


        builder.addStatement(expressionBuilder.toString(), Result.class, className);

        typeSpecBuilder.addMethod(builder.build());
    }
}
