package me.bristermitten.mittenlib.annotations.compile.deserializer;

import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import io.toolisticon.aptk.tools.TypeMirrorWrapper;
import me.bristermitten.mittenlib.annotations.ast.CustomDeserializerInfo;
import me.bristermitten.mittenlib.annotations.ast.Property;
import me.bristermitten.mittenlib.annotations.compile.ConfigurationClassNameGenerator;
import me.bristermitten.mittenlib.annotations.parser.CustomDeserializers;
import me.bristermitten.mittenlib.annotations.util.TypesUtil;
import me.bristermitten.mittenlib.config.exception.ConfigLoadingErrors;
import me.bristermitten.mittenlib.config.extension.CustomDeserializerFor;
import me.bristermitten.mittenlib.config.extension.Fallback;
import me.bristermitten.mittenlib.config.extension.UseObjectMapperSerialization;
import me.bristermitten.mittenlib.config.reader.ObjectMapper;
import me.bristermitten.mittenlib.config.tree.DataTree;
import me.bristermitten.mittenlib.config.tree.DataTreeTransforms;
import me.bristermitten.mittenlib.util.Enums;
import me.bristermitten.mittenlib.util.Result;
import me.bristermitten.mittenlib.util.Strings;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.Optional;

/**
 * Generates deserialization code for non-generic types (including primitive types, boxed primitives,
 * strings, enums, config-annotated classes, custom-deserialized classes, and objectMapper fallbacks).
 *
 * <p>The generator processes types in the following order of priority:
 * <ol>
 *   <li>Direct Type Match (short-circuits if object already matches target type)</li>
 *   <li>{@link DataTree} Type Match (converts literal {@link DataTree} values to primitives/strings)</li>
 *   <li>Custom Deserializers ({@link CustomDeserializerFor}) without {@link Fallback}</li>
 *   <li>Enums</li>
 *   <li>Config Types</li>
 *   <li>Custom Deserializers ({@link CustomDeserializerFor}) with {@link Fallback}</li>
 *   <li>Invalid Property / Object Mapper Fallback, either throwing an error or trying the {@link ObjectMapper} if nothing else works</li>
 * </ol>
 */
public class NonGenericTypeDeserializerGenerator {

    private final TypesUtil typesUtil;
    private final ConfigurationClassNameGenerator configurationClassNameGenerator;
    private final CustomDeserializers customDeserializers;

    @Inject
    NonGenericTypeDeserializerGenerator(
            TypesUtil typesUtil,
            ConfigurationClassNameGenerator configurationClassNameGenerator,
            CustomDeserializers customDeserializers) {
        this.typesUtil = typesUtil;
        this.configurationClassNameGenerator = configurationClassNameGenerator;
        this.customDeserializers = customDeserializers;
    }

    /**
     * Creates a {@link CodeBlock} invoking a custom deserializer.
     * Only supports static deserializers for now.
     *
     * @param info               the custom deserializer metadata
     * @param withDataExpression the code block providing the deserialization data (e.g. {@code context.withData(data)})
     * @return a code block invoking the custom deserializer
     */
    private CodeBlock getDeserializationFunction(CustomDeserializerInfo info, CodeBlock withDataExpression) {
        if (info.isStatic()) {
            return CodeBlock.of("$T.deserialize(context.withData($L))", info.deserializerClass(), withDataExpression);
        }
        String fieldName = Strings.uncapitalize(info.deserializerClass().getSimpleName().toString());
        return CodeBlock.of("this.$L.apply(context.withData($L))", fieldName, withDataExpression);
    }

    /**
     * Translates a {@link DataTree} literal value to its corresponding JVM primitive type.
     *
     * @param type         the target type we want to convert to (e.g. {@code int}, {@code float})
     * @param dataTreeType the actual type wrapper inside the {@link DataTree}
     * @param value        the code block representing the expression to convert
     * @return a code block performing the primitive conversion (e.g. {@code ((Integer) value).intValue()})
     */
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
     * Generate deserialization code for non-generic properties.
     * Tries all the cases described in the documentation of {@link NonGenericTypeDeserializerGenerator}, in order.
     *
     * @param builder            the method spec builder
     * @param property           the property being processed
     * @param dtoType            the enclosing DTO class element
     * @param elementType        the property type mirror
     * @param wrappedElementType the wrapped property type mirror
     * @return true if deserialization was fully generated/handled, false otherwise
     */
    public boolean handleNonGenericType(MethodSpec.Builder builder, Property property,
                                        TypeElement dtoType, TypeMirror elementType,
                                        TypeMirrorWrapper wrappedElementType) {
        final String fromMapName = property.name() + "FromMap";
        final TypeName safeType = configurationClassNameGenerator.getConfigPropertyClassName(typesUtil.getSafeType(elementType));

        handleDirectTypeMatch(builder, property, fromMapName, safeType);
        handleDataTreeTypeMatch(builder, fromMapName, safeType);

        Optional<CustomDeserializerInfo> customDeserializerOptional = customDeserializers.getCustomInfo(property.propertyType());
        if (customDeserializerOptional.isPresent()) {
            if (handleCustomDeserializer(builder, fromMapName, customDeserializerOptional.get(), false)) {
                return true;
            }
        }

        if (wrappedElementType.isEnum()) {
            handleEnumType(builder, property, fromMapName, safeType);
        } else if (typesUtil.isConfigType(elementType)) {
            handleConfigType(builder, elementType, fromMapName);
        }

        if (customDeserializerOptional.isPresent()) {
            if (handleCustomDeserializer(builder, fromMapName, customDeserializerOptional.get(), true)) {
                return true;
            }
        }

        return handleInvalidPropertyType(builder, property, dtoType, elementType, fromMapName);
    }

    /**
     * Generates a short-circuit when a property has a default value and the
     * raw input value is already of the expected target type.
     * Specifically, if the property has a default value we generate:
     * <pre>{@code
     * if (deserialisedValue instanceof ExpectedType) {
     *     return Result.ok((ExpectedType) deserialisedValue);
     * }
     * }</pre>
     * The justification for the default value requirement is not obvious upon immediate inspection.
     * However, if a property doesn't have a default value, we instead load it as a {@link DataTree}, so the {@code instanceof} would fail with a compile time error.
     * If there is a default value, we instead load it as {@link Object} so can try this case safely.
     *
     * @param builder     the method spec builder
     * @param property    the property being processed
     * @param fromMapName the name of the variable containing the raw data (e.g. {@code countFromMap})
     * @param safeType    the expected target type (e.g. {@code Integer})
     */
    private void handleDirectTypeMatch(MethodSpec.Builder builder, Property property,
                                       String fromMapName, TypeName safeType) {
        if (property.settings().hasDefaultValue()) {
            builder.beginControlFlow("if ($L instanceof $T)", fromMapName, safeType);
            builder.addStatement("return $T.ok(($T) $L)", Result.class, safeType, fromMapName);
            builder.endControlFlow();
        }
    }

    /**
     * Generates type checking and conversion logic when the raw input value is wrapped in
     * a {@link DataTree} literal (e.g. integer, float, string) that matches a primitive/string target.
     * Generates the code:
     * <pre>{@code
     *     if (deserialisedValue instanceof DataTree.Expected) {
     *         return Result.ok([generatedDataTreeConvert](((DataTree.Expected) deserialisedValue).value());
     *     }
     * }</pre>
     *
     * @param builder     the method spec builder
     * @param fromMapName the name of the variable containing the raw data (e.g. {@code countFromMap})
     * @param safeType    the expected target type (e.g. {@code Integer})
     */
    private void handleDataTreeTypeMatch(MethodSpec.Builder builder, String fromMapName, TypeName safeType) {
        var treeType = typesUtil.getDataTreeType(safeType);
        if (treeType.isPresent()) {
            builder.beginControlFlow("if ($L instanceof $T)", fromMapName, treeType.get());
            var convert = dataTreeConvert(safeType, treeType.get(), CodeBlock
                    .of("(($T) $L).value()", treeType.get(), fromMapName));

            builder.addStatement("return $T.ok($L)", Result.class, convert);
            builder.endControlFlow();
        }
    }

    /**
     * Generate code to invoke a custom deserialiser
     */
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

    /**
     * Generates code to parse enum property values from either raw {@link String} values or
     * {@link DataTree.DataTreeLiteral.DataTreeLiteralString} literal strings.
     */
    private void handleEnumType(MethodSpec.Builder builder, Property property,
                                String fromMapName, TypeName safeType) {
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

    /**
     * Generates recursive deserialization logic for nested configurations by casting
     * the property value to a {@link DataTree.DataTreeMap} and invoking the nested implementation's deserialize method.
     */
    private void handleConfigType(MethodSpec.Builder builder,
                                  TypeMirror elementType, String fromMapName) {
        String loaderFieldName = configurationClassNameGenerator.getDeserializerProviderFieldName(elementType);
        builder.beginControlFlow("if ($L instanceof $T)", fromMapName, DataTree.DataTreeMap.class);
        builder.addStatement("$1T mapData = ($1T) $2L", DataTree.DataTreeMap.class, fromMapName);
        builder.addStatement("return this.$L.get().apply(context.withData(mapData))", loaderFieldName);
        builder.endControlFlow();
    }

    /**
     * Handles type mismatches or fallbacks. If annotated with {@link UseObjectMapperSerialization},
     * generates code to delegate deserialization to our {@link ObjectMapper}.
     * Otherwise, it generates a failure result indicating an invalid property type.
     *
     * @param builder     the method spec builder
     * @param property    the property being processed, used to determine the return type and check for annotations
     * @param dtoType     the enclosing DTO class element, used for error reporting
     * @param elementType the property type mirror, used for error reporting
     * @param fromMapName the name of the variable containing the raw data (e.g. {@code countFromMap})
     * @return true if handled, false otherwise
     */
    private boolean handleInvalidPropertyType(MethodSpec.Builder builder, Property property,
                                              TypeElement dtoType, TypeMirror elementType, String fromMapName) {
        var useObjectMapperSerialization = typesUtil.getAnnotation(property.source().element(), UseObjectMapperSerialization.class);
        if (useObjectMapperSerialization != null) {
            TypeName propertyTypeName = configurationClassNameGenerator.publicPropertyClassName(property);
            builder.addStatement("return context.getMapper().map($T.toPOJO($T.loadFrom($L)), $T.get($T.class))",
                    DataTreeTransforms.class,
                    DataTreeTransforms.class,
                    fromMapName,
                    TypeToken.class,
                    propertyTypeName);
            return true;
        }
        if (!property.settings().hasDefaultValue()) {
            return false;
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

    /**
     * Helper method to generate enum lookup logic using either exact case matching
     * or case-insensitive matching depending on property configuration.
     *
     * @param property    the property being processed, used to determine the parsing scheme and property name
     * @param builder     the method spec builder
     * @param fromMapName the name of the variable containing the raw data (e.g. {@code typeFromMap})
     * @param safeType    the expected target type (e.g. {@code MyEnum})
     * @param convert     the code block performing the conversion to String (e.g. {@code typeFromMap.value()})
     */
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
}
