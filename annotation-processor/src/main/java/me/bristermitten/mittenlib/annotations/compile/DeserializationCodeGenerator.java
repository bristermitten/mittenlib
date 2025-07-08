package me.bristermitten.mittenlib.annotations.compile;

import com.google.gson.reflect.TypeToken;
import com.squareup.javapoet.*;
import io.toolisticon.aptk.tools.TypeMirrorWrapper;
import io.toolisticon.aptk.tools.corematcher.AptkCoreMatchers;
import io.toolisticon.aptk.tools.wrapper.ElementWrapper;
import io.toolisticon.aptk.tools.wrapper.TypeElementWrapper;
import me.bristermitten.mittenlib.annotations.ast.AbstractConfigStructure;
import me.bristermitten.mittenlib.annotations.ast.ConfigTypeSource;
import me.bristermitten.mittenlib.annotations.ast.CustomDeserializerInfo;
import me.bristermitten.mittenlib.annotations.ast.Property;
import me.bristermitten.mittenlib.annotations.parser.CustomDeserializers;
import me.bristermitten.mittenlib.annotations.util.TypesUtil;
import me.bristermitten.mittenlib.config.CollectionsUtils;
import me.bristermitten.mittenlib.config.DeserializationContext;
import me.bristermitten.mittenlib.config.exception.ConfigLoadingErrors;
import me.bristermitten.mittenlib.util.Enums;
import me.bristermitten.mittenlib.util.Result;
import me.bristermitten.mittenlib.util.Strings;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    public static final TypeName MAP_STRING_OBJ_NAME = ParameterizedTypeName.get(Map.class, String.class, Object.class);
    public static final ClassName RESULT_CLASS_NAME = ClassName.get(Result.class);
    final TypesUtil typesUtil;
    private final FieldNameGenerator fieldNameGenerator;
    private final ConfigurationClassNameGenerator configurationClassNameGenerator;
    private final MethodNames methodNames;
    private final CustomDeserializers customDeserializers;

    @Inject
    public DeserializationCodeGenerator(
            TypesUtil typesUtil,
            FieldNameGenerator fieldNameGenerator,
            ConfigurationClassNameGenerator configurationClassNameGenerator, MethodNames methodNames, CustomDeserializers customDeserializers) {
        this.typesUtil = typesUtil;
        this.fieldNameGenerator = fieldNameGenerator;
        this.configurationClassNameGenerator = configurationClassNameGenerator;
        this.methodNames = methodNames;
        this.customDeserializers = customDeserializers;
    }

    private CodeBlock getDeserializationFunction(CustomDeserializerInfo info) {
        if (info.isStatic()) {
            return CodeBlock.of("$T.deserialize(context)", info.deserializerClass());
        }
        throw new IllegalArgumentException("idk non-static is hard");
    }

    private CodeBlock getDeserializationFunctionReference(CustomDeserializerInfo info) {
        if (info.isStatic()) {
            return CodeBlock.of("$T::deserialize", info.deserializerClass());
        }
        throw new IllegalArgumentException("idk non-static is hard");
    }

    /**
     * Creates a deserialization method for a specific field in a DTO class.
     *
     * @param dtoType  The DTO class type
     * @param property The field element to create a deserialization method for
     * @return A method spec for the deserialization method
     */

    public @NotNull MethodSpec createDeserializeMethodFor(@NotNull TypeElement dtoType, @NotNull AbstractConfigStructure propertyAST, @NotNull Property property) {
        TypeMirror elementType = property.propertyType();
        var elementResultType = configurationClassNameGenerator.publicPropertyClassName(
                typesUtil.getBoxedType(property.propertyType())
        );


        final MethodSpec.Builder builder = MethodSpec.methodBuilder(DESERIALIZE_METHOD_PREFIX + Strings.capitalize(property.name()))
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Result.class), elementResultType))
                .addParameter(ParameterSpec.builder(DeserializationContext.class, "context").build());

        // we take the dto object as a parameter when it's a class
        if (propertyAST.source() instanceof ConfigTypeSource.ClassConfigTypeSource) {
            builder.addParameter(ParameterSpec.builder(ClassName.get(dtoType), "dao").build());
        }


        builder.addStatement("$T $$data = context.getData()", MAP_STRING_OBJ_NAME);
        builder.addStatement("$T $L", elementType, property.name());

        final String key = fieldNameGenerator.getConfigFieldName(property);
        final String fromMapName = property.name() + "FromMap";
        if (property.settings().hasDefaultValue()) {
            var defaultString = switch (propertyAST.source()) {
                case ConfigTypeSource.ClassConfigTypeSource ignored -> CodeBlock.of("dao.$L", property.name());
                case ConfigTypeSource.InterfaceConfigTypeSource ignored -> CodeBlock.of("super.$L()", property.name());
            };

            builder.addStatement("Object $L = $$data.getOrDefault($S, $L)", fromMapName, key, defaultString);
        } else {
            builder.addStatement("Object $L = $$data.get($S)", fromMapName, key);
        }

        // null check
        if (property.settings().isNullable()) {
            // Short circuit the null rather than trying any deserialization
            builder.beginControlFlow("if ($L == null)", fromMapName);
            builder.addStatement("return $T.ok(null)", Result.class);
            builder.endControlFlow();
        } else {
            builder.beginControlFlow("if ($L == null)", fromMapName);
            builder.addStatement("return $T.fail($T.notFoundException($S, $S, $T.class, $S))",
                    Result.class, ConfigLoadingErrors.class, property.name(), elementType, dtoType, key);
            builder.endControlFlow();
        }

        TypeMirrorWrapper wrappedElementType = TypeMirrorWrapper.wrap(elementType);
        boolean isGenericType = wrappedElementType.hasTypeArguments();
        Optional<TypeElementWrapper> typeElementOpt = wrappedElementType.getTypeElement();

        if (isGenericType && typeElementOpt.isPresent()) { // generic _and_ declared
            /*
             This is quite messy, but it's the only real way to solve this problem:
             When the type is a List<T> or Map<_, T> (where T is a @Config type)
             then we need to first load it as a C<Map<String, Object>>, then
             apply the deserialize function to each element.
             Otherwise, we'd fall back to using Gson, which would try to deserialise it without using the
             generated deserialization method and produce inconsistent results (and taking a performance hit)


             However, we can't just blindly do this for every type - there's no way of knowing how to convert a
             Blah<A> into a Blah<B> without some knowledge of the underlying structure.
             Map and List are the most common collection types, but this could really use some extensibility.
            */
            String canonicalName = wrappedElementType.erasure().getQualifiedName();
            ElementWrapper.wrap(property.source().element())

                    .validate()
                    .asError()
                    .check($ -> AptkCoreMatchers.BY_RAW_TYPE
                            .getValidator()
                            .hasOneOf(typeElementOpt.get().unwrap(), List.class, Map.class))
                    .validateAndIssueMessages();

            if (canonicalName.equals(List.class.getName())) {
                var listType = wrappedElementType.getTypeArguments().getFirst();
                Optional<CustomDeserializerInfo> optional = customDeserializers.getCustomDeserializer(listType);
                if (optional.isPresent()) {
                    CustomDeserializerInfo info = optional.get();
                    // TODO fallback
                    CodeBlock deserializationFunction = getDeserializationFunctionReference(info);
                    builder.addStatement("return $T.deserializeList($L, context, $L)", CollectionsUtils.class, fromMapName, deserializationFunction);
                    return builder.build();
                }
                if (typesUtil.isConfigType(listType)) {

                    TypeName listTypeName = getConfigClassName(listType, null);

                    var deserializeCodeBlock = CodeBlock.of("$T::$L", listTypeName, methodNames.getDeserializeMethodName(listTypeName));

                    var statement = CodeBlock.builder()
                            .add("return $T.deserializeList($L, context, ", CollectionsUtils.class, fromMapName)
                            .add(deserializeCodeBlock)
                            .add(")")
                            .build();
                    builder.addStatement(statement);
                    return builder.build();
                }
            } else if (canonicalName.equals("java.util.Map")) {
                var arguments = wrappedElementType.getTypeArguments();
                var keyType = arguments.get(0);
                var valueType = arguments.get(1);

                Optional<CustomDeserializerInfo> optional = customDeserializers.getCustomDeserializer(valueType);
                if (optional.isPresent()) {
                    CustomDeserializerInfo info = optional.get();
                    // TODO fallback
                    CodeBlock deserializationFunction = getDeserializationFunctionReference(info);
                    builder.addStatement("return $T.deserializeMap($L, context, $L)", CollectionsUtils.class, fromMapName, deserializationFunction);
                    return builder.build();
                }

                if (typesUtil.isConfigType(valueType)) {
                    TypeName mapTypeName = getConfigClassName(valueType, null);
                    builder.addStatement("return $T.deserializeMap($T.class, $L, context, $T::$L)",
                            CollectionsUtils.class,
                            (typesUtil.getSafeType(keyType)),
                            fromMapName,
                            mapTypeName,
                            methodNames.getDeserializeMethodName(mapTypeName));
                    return builder.build();
                }
            } else {
                throw new IllegalStateException("Unexpected generic type: " + canonicalName);
            }

        } else if (!isGenericType) {
            /*
             Construct a simple check that does
               if (fromMap instanceof X) return fromMap;
             Useful when the type is a primitive or String
             This is only safe to do with non-parameterized types, what with type erasure and all
            */
            final TypeName safeType = configurationClassNameGenerator.getConfigPropertyClassName(typesUtil.getSafeType(elementType));
            builder.beginControlFlow("if ($L instanceof $T)", fromMapName, safeType);
            builder.addStatement("return $T.ok(($T) $L)", Result.class, safeType, fromMapName);

            Optional<CustomDeserializerInfo> customDeserializerOptional = customDeserializers.getCustomDeserializer(property.propertyType());

            if (customDeserializerOptional.isPresent()) {
                CustomDeserializerInfo info = customDeserializerOptional.get();
                CodeBlock deserializationFunction = getDeserializationFunction(info);


                if (!info.isFallback()) {
                    builder.endControlFlow();
                    builder.addStatement(CodeBlock.builder().add("return ")
                            .add(deserializationFunction)
                            .build());
                    return builder.build();
                }
            }


            if (wrappedElementType.isEnum()) {
                // try to load it as a string
                builder.nextControlFlow("else if ($L instanceof $T)", fromMapName, String.class);


                switch (property.settings().enumParsingScheme()) {
                    case EXACT_MATCH -> builder.addStatement("$1T enumValue = $2T.valueOfOrNull(($3T) $4L, $1T.class)",
                            safeType,
                            Enums.class,
                            String.class,
                            fromMapName
                    );
                    case CASE_INSENSITIVE ->
                            builder.addStatement("$1T enumValue = $2T.valueOfIgnoreCase(($3T) $4L, $1T.class)",
                                    safeType,
                                    Enums.class,
                                    String.class,
                                    fromMapName
                            );
                }

                builder.beginControlFlow("if (enumValue == null)");
                builder.addStatement("return $T.fail($T.invalidEnumException($T.class, $S, $L))",
                        Result.class,
                        ConfigLoadingErrors.class,
                        safeType,
                        property.name(),
                        fromMapName);
                builder.endControlFlow();

                builder.addStatement("return $T.ok(enumValue)", Result.class);
                builder.endControlFlow();
            } else if (typesUtil.isConfigType(elementType)) {
                TypeName configClassName = getConfigClassName(elementType, dtoType);
                builder.nextControlFlow("else if ($L instanceof $T)", fromMapName, Map.class);
                builder.addStatement("$1T mapData = ($1T) $2L", MAP_STRING_OBJ_NAME, fromMapName);
                builder.addStatement("return $T.$L(context.withData(mapData))", configClassName, methodNames.getDeserializeMethodName(configClassName));
                builder.endControlFlow();
            } else {
                builder.endControlFlow();
            }


            builder.beginControlFlow("else");
            if (customDeserializerOptional.isPresent()) {
                CustomDeserializerInfo info = customDeserializerOptional.get();
                CodeBlock deserializationFunction = getDeserializationFunction(info);


                if (info.isFallback()) {
                    builder.addStatement(CodeBlock.builder().add("return ")
                            .add(deserializationFunction)
                            .build());
                    builder.endControlFlow();
                    return builder.build();
                }
            }
            builder.addStatement("return $T.fail($T.invalidPropertyTypeException($T.class, $S, $S, $L))",
                    Result.class,
                    ConfigLoadingErrors.class,
                    dtoType,
                    property.name(),
                    elementType,
                    fromMapName
            );
            builder.endControlFlow();
            return builder.build();
        }


        // If no shortcuts work, pass it to the context and do some dynamic-ish deserialization
        builder.addStatement("return context.getMapper().map($N, new $T<$T>(){})", fromMapName, TypeToken.class,
                configurationClassNameGenerator.getConfigPropertyClassName(property)
        );
        return builder.build();

    }

    /**
     * Creates the main deserialization method for a config class.
     *
     * @param typeSpecBuilder The builder for the config class
     */
    public void createDeserializeMethods(TypeSpec.@NotNull Builder typeSpecBuilder,
                                         @NotNull AbstractConfigStructure ast) {

        final MethodSpec.Builder builder = MethodSpec.methodBuilder(methodNames.getDeserializeMethodName(ast))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ParameterizedTypeName.get(RESULT_CLASS_NAME, ConfigurationClassNameGenerator.getPublicClassName(ast)))
                .addParameter(ParameterSpec.builder(DeserializationContext.class, "context").addModifiers(Modifier.FINAL).build());

        if (ast instanceof AbstractConfigStructure.Union union) {
            // union deserialization is very different
            CodeBlock.Builder deserialiseBuilder = CodeBlock.builder();
            deserialiseBuilder.add("return ");
            for (AbstractConfigStructure alternative : union.alternatives()) {
                ClassName alternativeClassName = ConfigurationClassNameGenerator.createConfigImplClassName(alternative);
                String deserializeMethodName = methodNames.getDeserializeMethodName(alternativeClassName);

                deserialiseBuilder.add("$T.$L(context).map($T.class::cast).orElse(() -> \n",
                        alternativeClassName,
                        deserializeMethodName,
                        ConfigurationClassNameGenerator.getPublicClassName(ast));
                deserialiseBuilder.indent();
            }
            deserialiseBuilder.add("$T.fail($T.noUnionMatch())", Result.class, ConfigLoadingErrors.class);
            deserialiseBuilder.add(")".repeat(union.alternatives().size())); // close all the flatMap parens

            builder.addStatement(deserialiseBuilder.build());

            typeSpecBuilder.addMethod(builder.build());
            return;
        }
        var dtoType = ast.source().element();

        if (ast.source() instanceof ConfigTypeSource.ClassConfigTypeSource) {
            builder.addStatement("$1T dto = new $1T()", dtoType.asType());
        }

        final List<MethodSpec> deserializeMethods = ast.properties().stream()
                .map(variableElement -> createDeserializeMethodFor(dtoType, ast, variableElement))
                .toList();

        deserializeMethods.forEach(typeSpecBuilder::addMethod);

        final CodeBlock.Builder expressionBuilder = CodeBlock.builder();

        expressionBuilder.add("return ");
        int i = 0;

        var superClass = switch (ast.source()) {
            case ConfigTypeSource.ClassConfigTypeSource c -> c.parent();
            case ConfigTypeSource.InterfaceConfigTypeSource ignored -> Optional.<TypeMirror>empty();//for now
        };

        // Add the superclass deserialization first, if it exists
        if (superClass.isPresent()) {
            var superConfigName = getConfigClassName(superClass.get(), dtoType);
            expressionBuilder.add("$T.$L", superConfigName, methodNames.getDeserializeMethodName(superConfigName));
            expressionBuilder.add("(context).flatMap(var$L -> \n", i++);
        }
        var deserialiseMethodArguments = switch (ast.source()) {
            case ConfigTypeSource.InterfaceConfigTypeSource ignored -> "context";
            case ConfigTypeSource.ClassConfigTypeSource ignored -> "context, dto";
        };

        for (MethodSpec deserializeMethod : deserializeMethods) {
            expressionBuilder.add("$N($L).flatMap(var$L -> \n", deserializeMethod, deserialiseMethodArguments, i++);
        }
        expressionBuilder.add("$T.ok(new $T(", Result.class, ConfigurationClassNameGenerator.createConfigImplClassName(ast));
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
     * Gets the config class name for a type.
     *
     * @param typeMirror The type
     * @param source     The source element (can be null)
     * @return The config class name
     */
    private @NotNull TypeName getConfigClassName(@NotNull TypeMirror typeMirror, Element source) {
        return configurationClassNameGenerator.getConfigClassName(typeMirror, source);
    }
}
