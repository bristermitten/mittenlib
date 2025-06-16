package me.bristermitten.mittenlib.annotations.config;

import com.google.gson.reflect.TypeToken;
import com.squareup.javapoet.*;
import me.bristermitten.mittenlib.annotations.util.TypesUtil;
import me.bristermitten.mittenlib.config.*;
import me.bristermitten.mittenlib.util.Result;
import me.bristermitten.mittenlib.util.Strings;

import javax.inject.Inject;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.List;
import java.util.Map;

/**
 * Generates deserialization code for configuration classes.
 * This class is responsible for creating methods that convert from JSON/YAML data
 * to strongly typed configuration objects.
 */
public class DeserializationCodeGenerator {
    /**
     * The prefix for all generated deserialization methods.
     * For example, a method to deserialize a field called "test" would be called deserializeTest
     */
    public static final String DESERIALIZE_METHOD_PREFIX = "deserialize";
    private static final TypeName MAP_STRING_OBJ_NAME = ParameterizedTypeName.get(Map.class, String.class, Object.class);
    private static final ClassName RESULT_CLASS_NAME = ClassName.get(Result.class);

    private final Types types;
    private final TypesUtil typesUtil;
    private final FieldClassNameGenerator fieldClassNameGenerator;

    @Inject
    public DeserializationCodeGenerator(
            Types types,
            TypesUtil typesUtil,
            FieldClassNameGenerator fieldClassNameGenerator) {
        this.types = types;
        this.typesUtil = typesUtil;
        this.fieldClassNameGenerator = fieldClassNameGenerator;
    }

    /**
     * Creates a deserialization method for a specific field in a DTO class.
     *
     * @param dtoType The DTO class type
     * @param element The field element to create a deserialization method for
     * @return A method spec for the deserialization method
     */
    public MethodSpec createDeserializeMethodFor(TypeElement dtoType, VariableElement element) {
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
               if (fromMap instanceof X) return fromMap;
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
                var listType = declaredType.getTypeArguments().getFirst();
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

    /**
     * Creates the main deserialization method for a config class.
     *
     * @param typeSpecBuilder The builder for the config class
     * @param dtoType The DTO class type
     * @param className The name of the config class
     * @param variableElements The fields to deserialize
     * @param getDTOSuperclass A function to get the superclass of the DTO
     */
    public void createDeserializeMethod(TypeSpec.Builder typeSpecBuilder,
                                     TypeElement dtoType,
                                     ClassName className,
                                     List<VariableElement> variableElements,
                                     java.util.function.Function<TypeElement, TypeMirror> getDTOSuperclass) {

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
        var superClass = getDTOSuperclass.apply(dtoType);

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

    /**
     * Checks if a type is a config type (annotated with @Config).
     *
     * @param mirror The type to check
     * @return true if the type is a config type, false otherwise
     */
    public boolean isConfigType(TypeMirror mirror) {
        return mirror instanceof DeclaredType declaredType &&
                declaredType.asElement().getAnnotation(Config.class) != null;
    }

    /**
     * Gets the name of the deserialization method for a type.
     *
     * @param name The type name
     * @return The deserialization method name
     */
    public String getDeserializeMethodName(TypeName name) {
        if (name instanceof ClassName cn) {
            return DESERIALIZE_METHOD_PREFIX + cn.simpleName();
        }
        return DESERIALIZE_METHOD_PREFIX + name;
    }

    /**
     * Gets the config class name for a type.
     *
     * @param typeMirror The type
     * @param source The source element (can be null)
     * @return The config class name
     */
    private TypeName getConfigClassName(TypeMirror typeMirror, Element source) {
        return typesUtil.getConfigClassName(typeMirror, source);
    }
}