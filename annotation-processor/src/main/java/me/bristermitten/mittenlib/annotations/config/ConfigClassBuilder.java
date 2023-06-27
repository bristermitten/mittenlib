package me.bristermitten.mittenlib.annotations.config;

import com.google.gson.reflect.TypeToken;
import com.squareup.javapoet.*;
import me.bristermitten.mittenlib.annotations.util.ElementsFinder;
import me.bristermitten.mittenlib.annotations.util.Stringify;
import me.bristermitten.mittenlib.annotations.util.TypesUtil;
import me.bristermitten.mittenlib.config.*;
import me.bristermitten.mittenlib.config.generate.GenerateToString;
import me.bristermitten.mittenlib.util.Result;
import me.bristermitten.mittenlib.util.Strings;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Does most of the processing for the config annotation processor.
 * Turns a @Config annotated class into a new config class.
 */
public class ConfigClassBuilder {
    /**
     * The prefix for all generated deserialization methods.
     * For example, a method to deserialize a field called "test" would be called deserializeTest
     */
    public static final String DESERIALIZE_METHOD_PREFIX = "deserialize";
    private static final TypeName MAP_STRING_OBJ_NAME = ParameterizedTypeName.get(Map.class, String.class, Object.class);
    private static final ClassName RESULT_CLASS_NAME = ClassName.get(Result.class);
    private final ElementsFinder elementsFinder;
    private final Types types;

    private final MethodNames methodNames;
    private final TypesUtil typesUtil;
    private final ConfigurationClassNameGenerator classNameGenerator;
    private final ToStringGenerator toStringGenerator;

    private final FieldClassNameGenerator fieldClassNameGenerator;

    private final GeneratedTypeCache generatedTypeCache;

    @Inject
    ConfigClassBuilder(ElementsFinder elementsFinder,
                       Types types,
                       MethodNames methodNames,
                       TypesUtil typesUtil,
                       ConfigurationClassNameGenerator classNameGenerator,
                       ToStringGenerator toStringGenerator,
                       FieldClassNameGenerator fieldClassNameGenerator, GeneratedTypeCache generatedTypeCache) {
        this.elementsFinder = elementsFinder;
        this.types = types;
        this.methodNames = methodNames;
        this.typesUtil = typesUtil;
        this.classNameGenerator = classNameGenerator;
        this.toStringGenerator = toStringGenerator;
        this.fieldClassNameGenerator = fieldClassNameGenerator;
        this.generatedTypeCache = generatedTypeCache;
    }

    private FieldSpec createFieldSpec(VariableElement element) {
        return FieldSpec.builder(
                        getConfigClassName(element.asType(), element),
                        element.getSimpleName().toString()
                ).addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .build();
    }

    private ParameterSpec createParameterSpec(VariableElement element) {
        return ParameterSpec.builder(
                        getConfigClassName(element.asType(), element),
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

    private TypeName getConfigClassName(TypeMirror typeMirror, @Nullable Element source) {
        return typesUtil.getConfigClassName(typeMirror, source);
    }


    private void createGetterMethod(TypeSpec.Builder typeSpecBuilder, VariableElement element, FieldSpec field) {
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

    private TypeSpec createConfigClass(TypeElement classType,
                                       List<VariableElement> variableElements) {
        final ClassName className =
                classNameGenerator.generateConfigurationClassName(classType)
                        .orElseThrow(() -> new IllegalArgumentException("Cannot determine name for @Config class " + classType.getQualifiedName()));

        return createConfigClass(classType, variableElements, className);
    }

    private @Nullable TypeMirror getDTOSuperclass(TypeElement dtoType) {
        var superClass = dtoType.getSuperclass();
        if (superClass.getKind() == TypeKind.NONE || superClass.toString().equals("java.lang.Object")) {
            superClass = null;
        }
        if (superClass != null && !isConfigType(superClass)) {
            throw new IllegalArgumentException("Superclass of @Config class must be a @Config class, was " + superClass);
        }
        return superClass;
    }

    private String getSuperFieldName(TypeMirror superClass) {
        var configName = typesUtil.getConfigClassName(superClass);

        return "parent" + Strings.capitalize(typesUtil.getSimpleName(configName));
    }

    private TypeSpec createConfigClass(TypeElement classType,
                                       List<VariableElement> variableElements,
                                       ClassName className) {

        var typeSpecBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(
                        AnnotationSpec.builder(GeneratedConfig.class)
                                .addMember("source", "$T.class", ClassName.get(classType))
                                .build());


        var superClass = getDTOSuperclass(classType);
        if (superClass != null) {
            // Store the super instance
            var superclassName = getConfigClassName(superClass, classType);
            typeSpecBuilder.superclass(superclassName);

            var superParamName = getSuperFieldName(superClass);
            typeSpecBuilder.addField(FieldSpec.builder(superclassName, superParamName)
                    .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                    .build()
            );
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
                        .add(getSuperFieldName(superClass));
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

        GenerateToString generateToString = typesUtil.getAnnotation(classType, GenerateToString.class);
        if (generateToString != null) {
            var toString = toStringGenerator.generateToString(typeSpecBuilder, className);
            typeSpecBuilder.addMethod(toString);
        }

        return typeSpecBuilder.build();
    }

    /**
     * Create a Java source file that can deserialize data described in a given DTO class
     *
     * @param classType The DTO class
     * @return A {@link JavaFile} representing the generated source file
     */
    public JavaFile createConfigFile(TypeElement classType) {
        final ClassName className =
                classNameGenerator.generateConfigurationClassName(classType)
                        .orElseThrow(() -> new IllegalArgumentException("Cannot determine name for @Config class " + classType.getQualifiedName()));

        var matchingFields =
                elementsFinder.getApplicableVariableElements(classType);

        final TypeSpec configClass = createConfigClass(classType, matchingFields, className);
        var file = JavaFile.builder(className.packageName(), configClass).build();
        generatedTypeCache.getGeneratedSpecs().put(classType, Stringify.fullyQualifiedName(file));
        return file;
    }

    private void addAllArgsConstructor(List<VariableElement> variableElements,
                                       Collection<FieldSpec> fieldSpecs,
                                       TypeSpec.Builder typeSpecBuilder,
                                       @Nullable TypeMirror superclass) {
        final MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC);

        if (superclass != null) {
            var superclassName = getConfigClassName(superclass, variableElements.stream().findFirst().map(VariableElement::getEnclosingElement).orElse(null));
            var superParamName = getSuperFieldName(superclass);
            var parameter = ParameterSpec.builder(superclassName, superParamName)
                    .addModifiers(Modifier.FINAL)
                    .build();
            constructorBuilder.addParameter(parameter);


            var superElements = elementsFinder.getApplicableVariableElements(superclass);
            List<String> collect = superElements.stream()
                    .map(VariableElement.class::cast)
                    .map(variableElement -> superParamName + "." + getFieldAccessorName(variableElement) + "()")
                    .toList();

            if (getDTOSuperclass((TypeElement) types.asElement(superclass)) != null) {
                var newCollect = new ArrayList<>(collect);
                newCollect.add(0, superParamName);
                collect = newCollect;
            }

            constructorBuilder.addStatement("super($L)", String.join(", ", collect));

            constructorBuilder.addStatement("this.$L = $L", superParamName, superParamName);
        }

        constructorBuilder.addParameters(variableElements.stream()
                .map(this::createParameterSpec)
                .toList());

        fieldSpecs.forEach(field ->
                constructorBuilder.addStatement("this.$N = $N", field, field.name));
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
                .initializer("new $T<>($S, $T.class, $T::$L)", Configuration.class, annotation.value(), className, className, getDeserializeMethodName(className))
                .build();
        typeSpecBuilder.addField(configField);
    }

    private String getFieldAccessorName(VariableElement variableElement) {
        return methodNames.safeMethodName(variableElement, (TypeElement) variableElement.getEnclosingElement());
    }

    private MethodSpec createDeserializeMethodFor(TypeElement dtoType, VariableElement element) {
        TypeMirror typeMirror = element.asType();
        final TypeName elementType = getConfigClassName(typeMirror, dtoType);
        final Name variableName = element.getSimpleName();
        final TypeMirror boxedType = typesUtil.getBoxedType(typeMirror);
        final TypeName boxedTypeName = getConfigClassName(boxedType, dtoType);

        final MethodSpec.Builder builder = MethodSpec.methodBuilder(DESERIALIZE_METHOD_PREFIX + Strings.capitalize(variableName.toString()))
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(ParameterizedTypeName.get(RESULT_CLASS_NAME, boxedTypeName))
                .addParameter(ParameterSpec.builder(DeserializationContext.class, "context").build())
                .addParameter(ParameterSpec.builder(ClassName.get(dtoType), "dao").build());

        builder.addStatement("$T $$data = context.getData()", MAP_STRING_OBJ_NAME);
        builder.addStatement("$T $L", elementType, variableName);
        final String fromMapName = variableName + "FromMap";
        final String key = fieldClassNameGenerator.getConfigFieldName(element);
        var defaultName = element.getSimpleName();
        builder.addStatement("Object $L = $$data.getOrDefault($S, dao.$L)", fromMapName, key, defaultName);

        if (typesUtil.isNullable(element)) {
            // Short circuit the null rather than trying any deserialization
            builder.beginControlFlow("if ($L == null)", fromMapName);
            builder.addStatement("return $T.ok(null)", Result.class);
            builder.endControlFlow();
        } else {
            builder.beginControlFlow("if ($L == null)", fromMapName);
            builder.addStatement("return $T.fail($T.throwNotFound($S, $S, $T.class, $L))",
                    Result.class, ConfigMapLoader.class, variableName, element, dtoType, fromMapName);
            builder.endControlFlow();
        }

        if (!(elementType instanceof ParameterizedTypeName)) {
            /*
             Construct a simple check that does
               if (fromMap instanceof X) return fromMap; NOSONAR this is not code you stupid program
             Useful when the type is a primitive or String
             This is only safe to do with non-parameterized types, type erasure and all
            */
            final TypeMirror safeType = typesUtil.getSafeType(typeMirror);
            final TypeName safeTypeName = getConfigClassName(safeType, dtoType);
            builder.beginControlFlow("if ($L instanceof $T)", fromMapName, safeTypeName);
            builder.addStatement("return $T.ok(($T) $L)", Result.class, elementType, fromMapName);
            builder.endControlFlow();
        } else if (typeMirror instanceof DeclaredType declaredType) {
            /*
             This is really cursed, but it's the only real way
             When the type is a List<T> or Map<_, T> then we need to first load it as a C<Map<String, Object>> then
             apply the deserialize function to each element. Otherwise, gson would try to deserialize it without
             using the generated deserialization method and produce inconsistent results.

             However, we can't just blindly do this for every type - there's no way of knowing how to convert a
             Blah<A> into a Blah<B> without some knowledge of the underlying structure.
             Map and List are the most common collection types, but this could really use some extensibility.
            */
            String canonicalName = types.erasure(typeMirror).toString();
            if (canonicalName.equals("java.util.List")) {
                var listType = declaredType.getTypeArguments().get(0);
                if (isConfigType(listType)) {
                    TypeName listTypeName = getConfigClassName(listType, null);
                    builder.addStatement("return $T.deserializeList($L, context, $T::$L)", CollectionsUtils.class,
                            fromMapName,
                            listTypeName,
                            getDeserializeMethodName(listTypeName));
                    return builder.build();
                }
            }
            if (canonicalName.equals("java.util.Map")) {
                var mapType = declaredType.getTypeArguments().get(1);
                var keyType = declaredType.getTypeArguments().get(0);

                if (isConfigType(mapType)) {
                    TypeName mapTypeName = getConfigClassName(mapType, null);
                    builder.addStatement("return $T.deserializeMap($T.class, $L, context, $T::$L)",
                            CollectionsUtils.class,
                            getConfigClassName(typesUtil.getSafeType(keyType), null),
                            fromMapName,
                            mapTypeName,
                            getDeserializeMethodName(mapTypeName));
                    return builder.build();
                }
            }
        }

        if (isConfigType(typeMirror)) {
            builder.beginControlFlow("if ($L instanceof $T)", fromMapName, Map.class);
            builder.addStatement("$1T mapData = ($1T) $2L", MAP_STRING_OBJ_NAME, fromMapName);
            builder.addStatement("return $T.$L(context.withData(mapData))", elementType, getDeserializeMethodName(elementType));
            builder.endControlFlow();
        }


        // If no shortcuts work, pass it to the context and do some dynamic-ish deserialization
        builder.addStatement("return context.getMapper().map($N, new $T<$T>(){})", fromMapName, TypeToken.class, boxedTypeName);
        return builder.build();
    }

    private boolean isConfigType(TypeMirror mirror) {
        return mirror instanceof DeclaredType declaredType &&
                declaredType.asElement().getAnnotation(Config.class) != null;
    }


    private String getDeserializeMethodName(TypeName name) {
        if (name instanceof ClassName cn) {
            return DESERIALIZE_METHOD_PREFIX + cn.simpleName();
        }
        return DESERIALIZE_METHOD_PREFIX + name;
    }

    private void createDeserializeMethod(TypeSpec.Builder typeSpecBuilder,
                                         TypeElement dtoType,
                                         ClassName className,
                                         List<VariableElement> variableElements) {

        final MethodSpec.Builder builder = MethodSpec.methodBuilder(getDeserializeMethodName(className))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ParameterizedTypeName.get(RESULT_CLASS_NAME, className))
                .addParameter(ParameterSpec.builder(DeserializationContext.class, "context").addModifiers(Modifier.FINAL).build());

        builder.addStatement("$1T dto = new $1T()", (dtoType.asType()));

        final List<MethodSpec> deserializeMethods = variableElements.stream()
                .map(variableElement -> createDeserializeMethodFor(dtoType, variableElement))
                .toList();

        deserializeMethods.forEach(typeSpecBuilder::addMethod);

        final CodeBlock.Builder expressionBuilder = CodeBlock.builder();

        expressionBuilder.add("return ");
        int i = 0;
        var superClass = getDTOSuperclass(dtoType);

        // Add the superclass deserialization first, if it exists
        if (superClass != null) {
            var superConfigName = getConfigClassName(superClass, dtoType);
            expressionBuilder.add("$T.$L", superConfigName, getDeserializeMethodName(superConfigName));
            expressionBuilder.add("(context).flatMap(var$L -> \n", i++);
        }
        for (MethodSpec deserializeMethod : deserializeMethods) {
            expressionBuilder.add("$N(context, dto).flatMap(var$L -> \n", deserializeMethod, i++);
        }
        expressionBuilder.add("$T.ok(new $T(", Result.class, className);
        for (int i1 = 0; i1 < i; i1++) {
            expressionBuilder.add("var$L", i1);
            if (i1 != i - 1) {
                expressionBuilder.add(", ");
            }
        }
        expressionBuilder.add("))"); // Close ok and new parens
        expressionBuilder.add(")".repeat(Math.max(0, i))); // close all the flatMap parens


        builder.addStatement(expressionBuilder.build());

        typeSpecBuilder.addMethod(builder.build());
    }

}
