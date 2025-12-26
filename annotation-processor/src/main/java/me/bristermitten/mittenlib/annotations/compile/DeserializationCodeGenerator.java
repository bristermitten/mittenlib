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
import me.bristermitten.mittenlib.annotations.codegen.CodeGenNames;
import me.bristermitten.mittenlib.annotations.codegen.FlatMapChainBuilder;
import me.bristermitten.mittenlib.annotations.codegen.Scope;
import me.bristermitten.mittenlib.annotations.codegen.Variable;
import me.bristermitten.mittenlib.annotations.parser.CustomDeserializers;
import me.bristermitten.mittenlib.annotations.util.TypesUtil;
import me.bristermitten.mittenlib.config.CollectionsUtils;
import me.bristermitten.mittenlib.config.DeserializationContext;
import me.bristermitten.mittenlib.config.exception.ConfigLoadingErrors;
import me.bristermitten.mittenlib.config.extension.UseObjectMapperSerialization;
import me.bristermitten.mittenlib.config.tree.DataTree;
import me.bristermitten.mittenlib.config.tree.DataTreeTransforms;
import me.bristermitten.mittenlib.util.Enums;
import me.bristermitten.mittenlib.util.Result;
import me.bristermitten.mittenlib.util.Strings;
import org.jspecify.annotations.Nullable;

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
    public static final String DESERIALIZE_METHOD_PREFIX = CodeGenNames.Methods.DESERIALIZE_PREFIX;
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

    private CodeBlock getDeserializationFunction(CustomDeserializerInfo info, CodeBlock withDataExpression) {
        if (info.isStatic()) {
            return CodeBlock.of("$T.deserialize($L.withData($L))", info.deserializerClass(), CodeGenNames.Variables.CONTEXT, withDataExpression);
        }
        throw new IllegalArgumentException("idk non-static is hard");
    }

    private CodeBlock getDeserializationFunctionReference(CustomDeserializerInfo info) {
        if (info.isStatic()) {
            return CodeBlock.of("$T::deserialize", info.deserializerClass());
        }
        throw new IllegalArgumentException("idk non-static is hard");
    }


    public CodeBlock dataTreeConvert(TypeName type, TypeName dataTreeType, CodeBlock value) {
        type = type.isBoxedPrimitive() ? type.unbox() : type;
        if (dataTreeType.equals(ClassName.get(DataTree.DataTreeLiteral.DataTreeLiteralInt.class))) {
            if (type.equals(TypeName.INT)) {
                return CodeBlock.of("($L).intValue()", value);
            }
            if (type.equals(TypeName.SHORT)) {
                return CodeBlock.of("($L).shortValue()", value);
            }
            if (type.equals(TypeName.BYTE)) {
                return CodeBlock.of("($L).byteValue()", value);
            }
            if (type.equals(TypeName.LONG)) {
                return CodeBlock.of("($L).longValue()", value);
            }
        }
        if (dataTreeType.equals(ClassName.get(DataTree.DataTreeLiteral.DataTreeLiteralFloat.class))) {
            if (type.equals(TypeName.FLOAT)) {
                return CodeBlock.of("($L).floatValue()", value);
            }
            if (type.equals(TypeName.DOUBLE)) {
                return CodeBlock.of("($L).doubleValue()", value);
            }
        }
        return value;
    }

    /**
     * Creates a deserialization method for a specific field in a DTO class.
     *
     * @param dtoType     The DTO class type
     * @param property    The field element to create a deserialization method for
     * @param propertyAST The AST representation of the property
     * @param daoName     The DAO class name, if applicable (can be null)
     * @return A method spec for the deserialization method
     */

    public MethodSpec createDeserializeMethodFor(TypeElement dtoType,
                                                 AbstractConfigStructure propertyAST,
                                                 Property property,
                                                 @Nullable ClassName daoName) {
        TypeMirror elementType = property.propertyType();
        TypeName elementTypeName = TypeName.get(elementType).withoutAnnotations();
        var elementResultType = configurationClassNameGenerator.publicPropertyClassName(
                typesUtil.getBoxedType(property.propertyType())
        );

        final MethodSpec.Builder builder = createDeserializeMethodBuilder(property, elementResultType, daoName);
        setupInitialStatements(builder, propertyAST, property);
        handleNullChecks(builder, property, dtoType, elementTypeName);

        TypeMirrorWrapper wrappedElementType = TypeMirrorWrapper.wrap(elementType);
        boolean isGenericType = wrappedElementType.hasTypeArguments();
        Optional<TypeElementWrapper> typeElementOpt = wrappedElementType.getTypeElement();

        if (isGenericType && typeElementOpt.isPresent()) {
            Optional<MethodSpec> methodSpec = handleGenericType(builder, property, wrappedElementType, typeElementOpt.get());
            if (methodSpec.isPresent()) {
                return methodSpec.get();
            }
        } else if (!isGenericType) {
            if (handleNonGenericType(builder, property, dtoType, elementType, wrappedElementType)) {
                return builder.build();
            }
        }

        // If no shortcuts work, pass it to the context and do some dynamic-ish deserialization
        String fromMapName = property.name() + "FromMap";
        builder.addStatement("return context.getMapper().map($N, new $T<$T>(){})", fromMapName, TypeToken.class,
                elementResultType
        );
        return builder.build();
    }

    private MethodSpec.Builder createDeserializeMethodBuilder(Property property,
                                                              TypeName elementResultType,
                                                              @Nullable ClassName daoName) {
        final MethodSpec.Builder builder = MethodSpec.methodBuilder(DESERIALIZE_METHOD_PREFIX + Strings.capitalize(property.name()))
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Result.class), elementResultType))
                .addParameter(ParameterSpec.builder(DeserializationContext.class, CodeGenNames.Variables.CONTEXT).build());

        // add the dao as a parameter if necessary
        if (daoName != null) {
            builder.addParameter(ParameterSpec.builder(daoName, CodeGenNames.Variables.DAO, Modifier.FINAL).build());
        }

        return builder;
    }

    /**
     * Gets the name of the "fromMap" variable for a property.
     * This variable holds the value extracted from the DataTree.
     *
     * @param property The property
     * @return The fromMap variable name
     */
    private String getFromMapVariableName(Property property) {
        return property.name() + CodeGenNames.Suffixes.FROM_MAP;
    }

    private void setupInitialStatements(MethodSpec.Builder builder,
                                        AbstractConfigStructure propertyAST,
                                        Property property) {
        builder.addStatement("$T $L = $L.getData()", DataTree.class, CodeGenNames.Variables.DATA, CodeGenNames.Variables.CONTEXT);
        final String key = fieldNameGenerator.getConfigFieldName(property);
        final String fromMapName = getFromMapVariableName(property);
        if (property.settings().hasDefaultValue()) {

            var defaultString = switch (propertyAST.source()) {
                case ConfigTypeSource.InterfaceConfigTypeSource ignored -> CodeBlock.of("$L.$L()", CodeGenNames.Variables.DAO, property.name());
                case ConfigTypeSource.ClassConfigTypeSource ignored -> CodeBlock.of("$L.$L", CodeGenNames.Variables.DAO, property.name());
            };

            builder.addStatement("Object $L = $L.getOrDefault($S, $L)", fromMapName, CodeGenNames.Variables.DATA, key, defaultString);
        } else {
            builder.addStatement("$T $L = $L.get($S)", DataTree.class, fromMapName, CodeGenNames.Variables.DATA, key);
        }
    }

    private void handleNullChecks(MethodSpec.Builder builder,
                                  Property property,
                                  TypeElement dtoType,
                                  TypeName elementTypeName) {
        final String key = fieldNameGenerator.getConfigFieldName(property);
        final String fromMapName = getFromMapVariableName(property);

        if (property.settings().isNullable()) {
            // Short circuit the null rather than trying any deserialization
            builder.beginControlFlow("if ($L == null)", fromMapName);
            builder.addStatement("return $T.ok(null)", Result.class);
            builder.endControlFlow();
        } else {
            builder.beginControlFlow("if ($L == null)", fromMapName);
            builder.addStatement("return $T.fail($T.notFoundException($S, $S, $T.class, $S))",
                    Result.class,
                    ConfigLoadingErrors.class,
                    property.name(),
                    elementTypeName,
                    dtoType,
                    key);
            builder.endControlFlow();
        }
    }

    private Optional<MethodSpec> handleGenericType(MethodSpec.Builder builder, Property property,
                                                   TypeMirrorWrapper wrappedElementType,
                                                   TypeElementWrapper elementType) {
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
                        .hasOneOf(elementType.unwrap(), List.class, Map.class))
                .validateAndIssueMessages();

        final String fromMapName = getFromMapVariableName(property);

        if (canonicalName.equals(List.class.getName())) {
            return handleListType(builder, wrappedElementType, fromMapName);
        } else if (canonicalName.equals(Map.class.getName())) {
            return handleMapType(builder, wrappedElementType, fromMapName);
        } else {
            throw new IllegalStateException("Unexpected generic type: " + canonicalName);
        }
    }

    private Optional<MethodSpec> handleListType(MethodSpec.Builder builder,
                                                TypeMirrorWrapper wrappedElementType,
                                                String fromMapName) {
        var listType = wrappedElementType.getTypeArguments().getFirst();
        Optional<CustomDeserializerInfo> optional = customDeserializers.getCustomDeserializer(listType);

        if (optional.isPresent()) {
            CustomDeserializerInfo info = optional.get();
            // TODO fallback
            CodeBlock deserializationFunction = getDeserializationFunctionReference(info);
            builder.addStatement("return $T.deserializeList($L, $L, $L)",
                    CollectionsUtils.class, fromMapName, CodeGenNames.Variables.CONTEXT, deserializationFunction);
            return Optional.of(builder.build());
        }

        if (typesUtil.isConfigType(listType)) {
            TypeName listTypeName = getConfigClassName(listType, null);
            var deserializeCodeBlock = CodeBlock.of("$T::$L", listTypeName,
                    methodNames.getDeserializeMethodName(listTypeName));

            builder.addStatement("return $T.deserializeList($L, $L, $L)", CollectionsUtils.class, fromMapName, CodeGenNames.Variables.CONTEXT, deserializeCodeBlock);
            return Optional.of(builder.build());
        }

        return Optional.empty();
    }

    private Optional<MethodSpec> handleMapType(MethodSpec.Builder builder,
                                               TypeMirrorWrapper wrappedElementType, String fromMapName) {
        var arguments = wrappedElementType.getTypeArguments();
        var keyType = arguments.get(0);
        var valueType = arguments.get(1);

        Optional<CustomDeserializerInfo> optional = customDeserializers.getCustomDeserializer(valueType);
        if (optional.isPresent()) {
            CustomDeserializerInfo info = optional.get();
            // TODO fallback
            CodeBlock deserializationFunction = getDeserializationFunctionReference(info);
            builder.addStatement("return $T.deserializeMap($L, $L, $L)",
                    CollectionsUtils.class, fromMapName, CodeGenNames.Variables.CONTEXT, deserializationFunction);
            return Optional.of(builder.build());
        }

        if (typesUtil.isConfigType(valueType)) {
            TypeName mapTypeName = getConfigClassName(valueType, null);
            builder.addStatement("return $T.deserializeMap($T.class, $L, $L, $T::$L)",
                    CollectionsUtils.class,
                    typesUtil.getSafeType(keyType),
                    fromMapName,
                    CodeGenNames.Variables.CONTEXT,
                    mapTypeName,
                    methodNames.getDeserializeMethodName(mapTypeName));
            return Optional.of(builder.build());
        }

        return Optional.empty();
    }

    private boolean handleNonGenericType(MethodSpec.Builder builder, Property property,
                                         TypeElement dtoType, TypeMirror elementType,
                                         TypeMirrorWrapper wrappedElementType) {
        /*
         Construct a simple check that does
           if (fromMap instanceof X) return fromMap;
         Useful when the type is a primitive or String
         This is only safe to do with non-parameterized types, what with type erasure and all
        */
        final String fromMapName = getFromMapVariableName(property);
        final TypeName safeType = configurationClassNameGenerator.getConfigPropertyClassName(typesUtil.getSafeType(elementType));

        handleDirectTypeMatch(builder, property, fromMapName, safeType);
        handleDataTreeTypeMatch(builder, fromMapName, safeType);

        Optional<CustomDeserializerInfo> customDeserializerOptional = customDeserializers.getCustomDeserializer(property.propertyType());
        if (customDeserializerOptional.isPresent()) {
            if (handleCustomDeserializer(builder, fromMapName, customDeserializerOptional.get(), false)) {
                return true;
            }
        }

        if (wrappedElementType.isEnum()) {
            handleEnumType(builder, property, fromMapName, safeType);
        } else if (typesUtil.isConfigType(elementType)) {
            handleConfigType(builder, dtoType, elementType, fromMapName);
        }


        if (customDeserializerOptional.isPresent()) {
            if (handleCustomDeserializer(builder, fromMapName, customDeserializerOptional.get(), true)) {
                return true;
            }
        }

        return handleInvalidPropertyType(builder, property, dtoType, elementType, fromMapName);
    }

    private void handleDirectTypeMatch(MethodSpec.Builder builder, Property property,
                                       String fromMapName, TypeName safeType) {
        // if there's a default value then there's a chance that field instanceof <PropertyType>
        // so we check this first as an easy short-circuit
        if (property.settings().hasDefaultValue()) {
            builder.beginControlFlow("if ($L instanceof $T)", fromMapName, safeType);
            builder.addStatement("return $T.ok(($T) $L)", Result.class, safeType, fromMapName);
            builder.endControlFlow();
        }
    }

    private void handleDataTreeTypeMatch(MethodSpec.Builder builder, String fromMapName, TypeName safeType) {
        // now check if the tree type would directly match any of the primitives (int, string, etc)
        // and add a short-circuit for that
        var treeType = typesUtil.getDataTreeType(safeType);
        if (treeType.isPresent()) {
            builder.beginControlFlow("if ($L instanceof $T)", fromMapName, treeType.get());
            var convert = dataTreeConvert(safeType, treeType.get(), CodeBlock
                    .of("(($T) $L).value()", treeType.get(), fromMapName));

            builder.addStatement("return $T.ok($L)", Result.class, convert);
            builder.endControlFlow();
        }
    }

    private boolean handleCustomDeserializer(MethodSpec.Builder builder, String fromMapName,
                                             CustomDeserializerInfo info,
                                             boolean isFallback) {


        if (info.isFallback() == isFallback) {
            CodeBlock deserializationFunction = getDeserializationFunction(info, CodeBlock.of(
                    "$T.loadFrom($L)", DataTreeTransforms.class, fromMapName
            ));

            builder.addStatement(CodeBlock.builder().add("return ")
                    .add(deserializationFunction)
                    .build());
            return true;
        }
        return false;
    }

    private void handleEnumType(MethodSpec.Builder builder, Property property,
                                String fromMapName, TypeName safeType) {
        // try to load it as a string
        if (property.settings().hasDefaultValue()) {
            builder.beginControlFlow("if ($L instanceof $T)", fromMapName, String.class);
            addEnumDeserialisation(property, builder, fromMapName, safeType, CodeBlock.of("$L", fromMapName));
            builder.endControlFlow();
        }

        builder.beginControlFlow("if ($L instanceof $T)", fromMapName, DataTree.DataTreeLiteral.DataTreeLiteralString.class);
        {
            var convert = CodeBlock.of("(($T) $L).value()", DataTree.DataTreeLiteral.DataTreeLiteralString.class, fromMapName);
            addEnumDeserialisation(property, builder, fromMapName, safeType, convert);
        }
        builder.endControlFlow();
    }

    private void handleConfigType(MethodSpec.Builder builder, TypeElement dtoType,
                                  TypeMirror elementType, String fromMapName) {
        TypeName configClassName = getConfigClassName(elementType, dtoType);
        builder.beginControlFlow("if ($L instanceof $T)", fromMapName, DataTree.DataTreeMap.class);
        builder.addStatement("$1T $2L = ($1T) $3L", DataTree.DataTreeMap.class, CodeGenNames.Variables.MAP_DATA, fromMapName);
        builder.addStatement("return $T.$L($L.withData($L))",
                configClassName, methodNames.getDeserializeMethodName(configClassName), CodeGenNames.Variables.CONTEXT, CodeGenNames.Variables.MAP_DATA);
        builder.endControlFlow();
    }

    private boolean handleInvalidPropertyType(MethodSpec.Builder builder, Property property,
                                              TypeElement dtoType, TypeMirror elementType, String fromMapName) {

        // Check if the property is annotated with @UseObjectMapperSerialization
        // If so, use ObjectMapper as a fallback for deserialization
        var useObjectMapperSerialization = typesUtil.getAnnotation(property.source().element(), UseObjectMapperSerialization.class);
        if (useObjectMapperSerialization != null) {
            // Use ObjectMapper to deserialize the value
            TypeName propertyTypeName = configurationClassNameGenerator.publicPropertyClassName(property);
            builder.addStatement("return $L.getMapper().map($T.toPOJO($T.loadFrom($L)), $T.get($T.class))",
                    CodeGenNames.Variables.CONTEXT,
                    DataTreeTransforms.class,
                    DataTreeTransforms.class,
                    fromMapName,
                    TypeToken.class,
                    propertyTypeName);
            return true;
        }
        if (!property.settings().hasDefaultValue()) {
            return false; // no need to check this
        }
        builder.beginControlFlow("if (!($L instanceof $T))", fromMapName, DataTree.class);
        builder.addStatement("return $T.fail($T.invalidPropertyTypeException($T.class, $S, $S, $L))",
                Result.class,
                ConfigLoadingErrors.class,
                dtoType,
                property.name(),
                elementType,
                fromMapName
        );
        builder.endControlFlow();
        return false;
    }

    private void addEnumDeserialisation(Property property, MethodSpec.Builder builder, String fromMapName, TypeName safeType, CodeBlock convert) {
        switch (property.settings().enumParsingScheme()) {
            case EXACT_MATCH -> builder.addStatement("$1T $2L = $3T.valueOfOrNull(($4T) $5L, $1T.class)",
                    safeType,
                    CodeGenNames.Variables.ENUM_VALUE,
                    Enums.class,
                    String.class,
                    convert
            );
            case CASE_INSENSITIVE -> builder.addStatement("$1T $2L = $3T.valueOfIgnoreCase(($4T) $5L, $1T.class)",
                    safeType,
                    CodeGenNames.Variables.ENUM_VALUE,
                    Enums.class,
                    String.class,
                    convert
            );
        }
        builder.beginControlFlow("if ($L == null)", CodeGenNames.Variables.ENUM_VALUE);
        builder.addStatement("return $T.fail($T.invalidEnumException($T.class, $S, $L))",
                Result.class,
                ConfigLoadingErrors.class,
                safeType,
                property.name(),
                fromMapName);
        builder.endControlFlow();

        builder.addStatement("return $T.ok($L)", Result.class, CodeGenNames.Variables.ENUM_VALUE);
    }

    /**
     * Creates the main deserialization method for a config class.
     *
     * @param typeSpecBuilder The builder for the config class
     */
    public void createDeserializeMethods(TypeSpec.Builder typeSpecBuilder,
                                         AbstractConfigStructure ast,
                                         @Nullable ClassName daoName
    ) {

        final MethodSpec.Builder builder = MethodSpec.methodBuilder(methodNames.getDeserializeMethodName(ast))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ParameterizedTypeName.get(RESULT_CLASS_NAME, configurationClassNameGenerator.getPublicClassName(ast)))
                .addParameter(
                        ParameterSpec.builder(DeserializationContext.class, CodeGenNames.Variables.CONTEXT, Modifier.FINAL).build()
                );

        if (ast instanceof AbstractConfigStructure.Union union) {
            // union deserialization is very different
            CodeBlock.Builder deserialiseBuilder = CodeBlock.builder();
            deserialiseBuilder.add("return ");
            for (AbstractConfigStructure alternative : union.alternatives()) {
                ClassName alternativeClassName = configurationClassNameGenerator.translateConfigClassName(alternative);
                String deserializeMethodName = methodNames.getDeserializeMethodName(alternativeClassName);

                deserialiseBuilder.add("$T.$L($L).map($T.class::cast).orElse(() -> \n",
                        alternativeClassName,
                        deserializeMethodName,
                        CodeGenNames.Variables.CONTEXT,
                        configurationClassNameGenerator.getPublicClassName(ast));
                deserialiseBuilder.indent();
            }
            deserialiseBuilder.add("$T.fail($T.noUnionMatch())", Result.class, ConfigLoadingErrors.class);
            deserialiseBuilder.add(")".repeat(union.alternatives().size())); // close all the flatMap parens

            builder.addStatement(deserialiseBuilder.build());

            typeSpecBuilder.addMethod(builder.build());
            return;
        }
        var dtoType = ast.source().element();

        if (daoName != null) {
            builder.addStatement("$1T $2L = new $1T()", daoName, CodeGenNames.Variables.DAO);
        }

        final List<MethodSpec> deserializeMethods = ast.properties().stream()
                .map(variableElement -> createDeserializeMethodFor(dtoType, ast, variableElement, daoName))
                .toList();

        deserializeMethods.forEach(typeSpecBuilder::addMethod);

        final FlatMapChainBuilder chain = new FlatMapChainBuilder();

        var superClass = switch (ast.source()) {
            case ConfigTypeSource.ClassConfigTypeSource c -> c.parent();
            case ConfigTypeSource.InterfaceConfigTypeSource ignored -> Optional.<TypeMirror>empty();//for now
        };

        // Add the superclass deserialization first, if it exists
        if (superClass.isPresent()) {
            var superConfigName = getConfigClassName(superClass.get(), dtoType);
            chain.addOperation(
                "$T.$L($L)",
                superConfigName,
                superConfigName,
                methodNames.getDeserializeMethodName(superConfigName),
                CodeGenNames.Variables.CONTEXT
            );
        }
        
        var deserialiseMethodArguments = (daoName != null) ? CodeGenNames.Variables.CONTEXT + ", " + CodeGenNames.Variables.DAO : CodeGenNames.Variables.CONTEXT;
        for (MethodSpec deserializeMethod : deserializeMethods) {
            // Infer the return type from the method's return type
            // All deserialize methods return Result<T> where T is the config property type
            TypeName returnType = deserializeMethod.returnType;
            if (returnType instanceof ParameterizedTypeName paramType) {
                // Validate that this looks like Result<T>
                if (paramType.rawType.equals(ClassName.get(Result.class)) && !paramType.typeArguments.isEmpty()) {
                    // Extract T from Result<T>
                    returnType = paramType.typeArguments.get(0);
                } else {
                    throw new IllegalStateException("Expected Result<T> return type, got: " + returnType);
                }
            }
            chain.addOperation(
                "$N($L)",
                returnType,
                deserializeMethod,
                deserialiseMethodArguments
            );
        }
        
        CodeBlock chainExpression = chain.buildWithConstructor(
            Result.class,
            configurationClassNameGenerator.translateConfigClassName(ast)
        );

        builder.addStatement(chainExpression);

        typeSpecBuilder.addMethod(builder.build());
    }

    /**
     * Gets the config class name for a type.
     *
     * @param typeMirror The type
     * @param source     The source element (can be null)
     * @return The config class name
     */
    private TypeName getConfigClassName(TypeMirror typeMirror, @Nullable Element source) {
        return configurationClassNameGenerator.getConfigClassName(typeMirror, source);
    }

}
