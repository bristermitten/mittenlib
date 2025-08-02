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

    private CodeBlock getDeserializationFunction(CustomDeserializerInfo info, CodeBlock withDataExpression) {
        if (info.isStatic()) {
            return CodeBlock.of("$T.deserialize(context.withData($L))", info.deserializerClass(), withDataExpression);
        }
        throw new IllegalArgumentException("idk non-static is hard");
    }

    private CodeBlock getDeserializationFunctionReference(CustomDeserializerInfo info) {
        if (info.isStatic()) {
            return CodeBlock.of("$T::deserialize", info.deserializerClass());
        }
        throw new IllegalArgumentException("idk non-static is hard");
    }

    private Optional<TypeName> getDataTreeType(TypeName type) {
        type = type.isBoxedPrimitive() ? type.unbox() : type;
        if (type.equals(TypeName.INT) || type.equals(TypeName.LONG) || type.equals(TypeName.SHORT) || type.equals(TypeName.BYTE)) {
            return Optional.of(ClassName.get(DataTree.DataTreeLiteral.DataTreeLiteralInt.class));
        }
        if (type.equals(TypeName.FLOAT) || type.equals(TypeName.DOUBLE)) {
            return Optional.of(ClassName.get(DataTree.DataTreeLiteral.DataTreeLiteralFloat.class));
        }
        if (type.equals(TypeName.BOOLEAN)) {
            return Optional.of(ClassName.get(DataTree.DataTreeLiteral.DataTreeLiteralBoolean.class));
        }
        if (type.equals(ClassName.get(String.class))) {
            return Optional.of(ClassName.get(DataTree.DataTreeLiteral.DataTreeLiteralString.class));
        }
        if (type instanceof ParameterizedTypeName p) {
            if (p.rawType.equals(ClassName.get(Map.class))) {
                return Optional.of(ClassName.get(DataTree.DataTreeMap.class));
            }
            if (p.rawType.equals(ClassName.get(List.class))) {
                return Optional.of(ClassName.get(DataTree.DataTreeArray.class));
            }
        }
        return Optional.empty();
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
        var elementResultType = configurationClassNameGenerator.publicPropertyClassName(
                typesUtil.getBoxedType(property.propertyType())
        );

        final MethodSpec.Builder builder = createMethodBuilder(property, elementResultType, daoName);
        setupInitialStatements(builder, propertyAST, property);
        handleNullChecks(builder, property, dtoType, elementType);

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

    private MethodSpec.Builder createMethodBuilder(Property property,
                                                   TypeName elementResultType,
                                                   @Nullable ClassName daoName) {
        final MethodSpec.Builder builder = MethodSpec.methodBuilder(DESERIALIZE_METHOD_PREFIX + Strings.capitalize(property.name()))
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Result.class), elementResultType))
                .addParameter(ParameterSpec.builder(DeserializationContext.class, "context").build());

        // add the dao as a parameter if necessary
        if (daoName != null) {
            builder.addParameter(ParameterSpec.builder(daoName, "dao", Modifier.FINAL).build());
        }

        return builder;
    }

    private void setupInitialStatements(MethodSpec.Builder builder,
                                        AbstractConfigStructure propertyAST,
                                        Property property) {
        builder.addStatement("$T $$data = context.getData()", DataTree.class);
        final String key = fieldNameGenerator.getConfigFieldName(property);
        final String fromMapName = property.name() + "FromMap";
        if (property.settings().hasDefaultValue()) {

            var defaultString = switch (propertyAST.source()) {
                case ConfigTypeSource.InterfaceConfigTypeSource ignored -> CodeBlock.of("dao.$L()", property.name());
                case ConfigTypeSource.ClassConfigTypeSource ignored -> CodeBlock.of("dao.$L", property.name());
            };

            builder.addStatement("Object $L = $$data.getOrDefault($S, $L)", fromMapName, key, defaultString);
        } else {
            builder.addStatement("$T $L = $$data.get($S)", DataTree.class, fromMapName, key);
        }
    }

    private void handleNullChecks(MethodSpec.Builder builder, Property property,
                                  TypeElement dtoType, TypeMirror elementType) {
        final String key = fieldNameGenerator.getConfigFieldName(property);
        final String fromMapName = property.name() + "FromMap";

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

        final String fromMapName = property.name() + "FromMap";

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
            builder.addStatement("return $T.deserializeList($L, context, $L)",
                    CollectionsUtils.class, fromMapName, deserializationFunction);
            return Optional.of(builder.build());
        }

        if (typesUtil.isConfigType(listType)) {
            TypeName listTypeName = getConfigClassName(listType, null);
            var deserializeCodeBlock = CodeBlock.of("$T::$L", listTypeName,
                    methodNames.getDeserializeMethodName(listTypeName));

            var statement = CodeBlock.builder()
                    .add("return $T.deserializeList($L, context, ", CollectionsUtils.class, fromMapName)
                    .add(deserializeCodeBlock)
                    .add(")")
                    .build();
            builder.addStatement(statement);
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
            builder.addStatement("return $T.deserializeMap($L, context, $L)",
                    CollectionsUtils.class, fromMapName, deserializationFunction);
            return Optional.of(builder.build());
        }

        if (typesUtil.isConfigType(valueType)) {
            TypeName mapTypeName = getConfigClassName(valueType, null);
            builder.addStatement("return $T.deserializeMap($T.class, $L, context, $T::$L)",
                    CollectionsUtils.class,
                    typesUtil.getSafeType(keyType),
                    fromMapName,
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
        final String fromMapName = property.name() + "FromMap";
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

        handleInvalidPropertyType(builder, property, dtoType, elementType, fromMapName);

        return false;
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
        var treeType = getDataTreeType(safeType);
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
        builder.addStatement("$1T mapData = ($1T) $2L", DataTree.DataTreeMap.class, fromMapName);
        builder.addStatement("return $T.$L(context.withData(mapData))",
                configClassName, methodNames.getDeserializeMethodName(configClassName));
        builder.endControlFlow();
    }

    private void handleInvalidPropertyType(MethodSpec.Builder builder, Property property,
                                           TypeElement dtoType, TypeMirror elementType, String fromMapName) {
        if (!property.settings().hasDefaultValue()) {
            return; // no need to check this
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
    }

    private void addEnumDeserialisation(Property property, MethodSpec.Builder builder, String fromMapName, TypeName safeType, CodeBlock convert) {
        switch (property.settings().enumParsingScheme()) {
            case EXACT_MATCH -> builder.addStatement("$1T enumValue = $2T.valueOfOrNull(($3T) $4L, $1T.class)",
                    safeType,
                    Enums.class,
                    String.class,
                    convert
            );
            case CASE_INSENSITIVE -> builder.addStatement("$1T enumValue = $2T.valueOfIgnoreCase(($3T) $4L, $1T.class)",
                    safeType,
                    Enums.class,
                    String.class,
                    convert
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
                        ParameterSpec.builder(DeserializationContext.class, "context", Modifier.FINAL).build()
                );

        if (ast instanceof AbstractConfigStructure.Union union) {
            // union deserialization is very different
            CodeBlock.Builder deserialiseBuilder = CodeBlock.builder();
            deserialiseBuilder.add("return ");
            for (AbstractConfigStructure alternative : union.alternatives()) {
                ClassName alternativeClassName = configurationClassNameGenerator.translateConfigClassName(alternative);
                String deserializeMethodName = methodNames.getDeserializeMethodName(alternativeClassName);

                deserialiseBuilder.add("$T.$L(context).map($T.class::cast).orElse(() -> \n",
                        alternativeClassName,
                        deserializeMethodName,
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
            builder.addStatement("$1T dao = new $1T()", daoName);
        }

        final List<MethodSpec> deserializeMethods = ast.properties().stream()
                .map(variableElement -> createDeserializeMethodFor(dtoType, ast, variableElement, daoName))
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
        var deserialiseMethodArguments = (daoName != null) ? "context, dao" : "context";
        for (MethodSpec deserializeMethod : deserializeMethods) {
            expressionBuilder.add("$N($L).flatMap(var$L -> \n", deserializeMethod, deserialiseMethodArguments, i++);
        }
        expressionBuilder.add("$T.ok(new $T(", Result.class, configurationClassNameGenerator.translateConfigClassName(ast));
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
    private TypeName getConfigClassName(TypeMirror typeMirror, @Nullable Element source) {
        return configurationClassNameGenerator.getConfigClassName(typeMirror, source);
    }

}
