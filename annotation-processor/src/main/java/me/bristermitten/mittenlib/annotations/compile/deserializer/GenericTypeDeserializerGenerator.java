package me.bristermitten.mittenlib.annotations.compile.deserializer;

import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import io.toolisticon.aptk.tools.TypeMirrorWrapper;
import io.toolisticon.aptk.tools.corematcher.AptkCoreMatchers;
import io.toolisticon.aptk.tools.wrapper.ElementWrapper;
import io.toolisticon.aptk.tools.wrapper.TypeElementWrapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import me.bristermitten.mittenlib.annotations.ast.CustomDeserializerInfo;
import me.bristermitten.mittenlib.annotations.ast.Property;
import me.bristermitten.mittenlib.annotations.compile.ConfigurationClassNameGenerator;
import me.bristermitten.mittenlib.annotations.parser.CustomDeserializers;
import me.bristermitten.mittenlib.annotations.util.TypesUtil;
import me.bristermitten.mittenlib.config.CollectionsUtils;
import me.bristermitten.mittenlib.config.DeserializationFunction;
import me.bristermitten.mittenlib.config.tree.DataTree;
import me.bristermitten.mittenlib.config.tree.DataTreeTransforms;
import me.bristermitten.mittenlib.util.Result;
import me.bristermitten.mittenlib.util.Strings;

/**
 * Generates deserialization code for generic collection types (specifically {@link List} and {@link
 * Map}) where the element or value types are custom configuration types.
 */
public class GenericTypeDeserializerGenerator {

    private final TypesUtil typesUtil;
    private final ConfigurationClassNameGenerator configurationClassNameGenerator;
    private final CustomDeserializers customDeserializers;
    private final NonGenericTypeDeserializerGenerator nonGenericTypeDeserializerGenerator;

    @Inject
    GenericTypeDeserializerGenerator(
            TypesUtil typesUtil,
            ConfigurationClassNameGenerator configurationClassNameGenerator,
            CustomDeserializers customDeserializers,
            NonGenericTypeDeserializerGenerator nonGenericTypeDeserializerGenerator) {
        this.typesUtil = typesUtil;
        this.configurationClassNameGenerator = configurationClassNameGenerator;
        this.customDeserializers = customDeserializers;
        this.nonGenericTypeDeserializerGenerator = nonGenericTypeDeserializerGenerator;
    }

    /**
     * Creates a reference to a deserialization function, either as a method reference (for static) or
     * a field reference (for non-static, injected).
     *
     * <p>Generates:
     *
     * <pre>
     *     MyDeserializer::deserialize
     * </pre>
     *
     * <p>or
     *
     * <pre>
     *     this.myDeserializer
     * </pre>
     *
     * @param info the custom deserializer metadata, used to determine if the generated code should be
     *     static or not
     * @return a code block referencing the deserializer
     */
    private CodeBlock getDeserializationFunctionReference(CustomDeserializerInfo info) {
        if (info.isStatic()) {
            return CodeBlock.of("$T::deserialize", info.deserializerClass());
        }
        String fieldName =
                Strings.uncapitalize(info.deserializerClass().getSimpleName().toString());
        return CodeBlock.of("this.$L", fieldName);
    }

    /**
     * Generates and appends deserialization logic for generic collection properties.
     *
     * <p>For a {@link List}, generates:
     *
     * <pre>
     *     return CollectionsUtils.deserializeList(itemsFromMap, context, ctx0 -> ...);
     * </pre>
     *
     * @param builder the method spec builder
     * @param property the property being processed, used to generate the variable name (e.g. {@code
     *     itemsFromMap})
     * @param wrappedElementType the wrapped property type mirror
     * @param elementType the wrapped property type element
     * @return an optional method spec if handled successfully
     */
    public Optional<MethodSpec> handleGenericType(
            MethodSpec.Builder builder,
            TypeElement dtoType,
            Property property,
            TypeMirrorWrapper wrappedElementType,
            TypeElementWrapper elementType) {

        String canonicalName = wrappedElementType.erasure().getQualifiedName();
        ElementWrapper.wrap(property.source().element())
                .validate()
                .asError()
                .check($ -> AptkCoreMatchers.BY_RAW_TYPE
                        .getValidator()
                        .hasOneOf(elementType.unwrap(), List.class, Set.class, Map.class, Optional.class))
                .validateAndIssueMessages();

        final String fromMapName = property.name() + "FromMap";

        if (canonicalName.equals(List.class.getName())) {
            return handleListType(builder, dtoType, property, wrappedElementType, fromMapName);
        } else if (canonicalName.equals(Set.class.getName())) {
            return handleSetType(builder, dtoType, property, wrappedElementType, fromMapName);
        } else if (canonicalName.equals(Map.class.getName())) {
            return handleMapType(builder, dtoType, property, wrappedElementType, fromMapName);
        } else if (canonicalName.equals(Optional.class.getName())) {
            return handleOptionalType(builder, dtoType, property, wrappedElementType, fromMapName);
        } else {
            throw new IllegalStateException("Unexpected generic type: " + canonicalName);
        }
    }

    /**
     * Recursively generates a {@link CodeBlock} representing a {@link
     * DeserializationFunction} for the given type. Maps collections
     * recursively and falls back to object mapper mapping for basic types.
     *
     * <p>Generates lambdas like:
     *
     * <pre>
     *     ctx0 -> CollectionsUtils.deserializeList(ctx0.getData(), ctx0, ctx1 -> ...)
     * </pre>
     *
     * @param type the type to generate a deserialization function for
     * @param depth the current nesting depth, used to generate unique context variable names (e.g.
     *     {@code ctx0}, {@code ctx1})
     * @return a {@link CodeBlock} lambda expression {@code ctx -> ...}
     */
    private CodeBlock getDeserializationFunction(TypeElement dtoType, Property property, TypeMirror type, int depth) {
        TypeMirrorWrapper wrapped = TypeMirrorWrapper.wrap(type);

        // Custom Deserializer
        Optional<CustomDeserializerInfo> customDeserializerOptional = customDeserializers.getCustomInfo(type);
        if (customDeserializerOptional.isPresent()) {
            return getDeserializationFunctionReference(customDeserializerOptional.get());
        }

        // Config type
        if (typesUtil.isConfigType(type)) {
            String loaderField = configurationClassNameGenerator.getDeserializerProviderFieldName(type);
            return CodeBlock.of("this.$L.get()", loaderField);
        }

        // 3. Generic collections (List, Map)
        if (wrapped.hasTypeArguments()) {
            String canonicalName = wrapped.erasure().getQualifiedName();
            String ctxVar = "ctx" + depth;
            if (canonicalName.equals(List.class.getName())) {
                TypeMirror elementType = wrapped.getTypeArguments().getFirst();
                CodeBlock innerFunction = getDeserializationFunction(dtoType, property, elementType, depth + 1);
                return CodeBlock.of(
                        "$L -> $T.deserializeList($L.getData(), $L, $L)",
                        ctxVar,
                        CollectionsUtils.class,
                        ctxVar,
                        ctxVar,
                        innerFunction);
            } else if (canonicalName.equals(Set.class.getName())) {
                TypeMirror elementType = wrapped.getTypeArguments().getFirst();
                CodeBlock innerFunction = getDeserializationFunction(dtoType, property, elementType, depth + 1);
                return CodeBlock.of(
                        "$L -> $T.deserializeSet($L.getData(), $L, $L)",
                        ctxVar,
                        CollectionsUtils.class,
                        ctxVar,
                        ctxVar,
                        innerFunction);
            } else if (canonicalName.equals(Map.class.getName())) {
                var arguments = wrapped.getTypeArguments();
                TypeMirror keyType = arguments.get(0);
                TypeMirror valueType = arguments.get(1);
                CodeBlock innerFunction = getDeserializationFunction(dtoType, property, valueType, depth + 1);
                return CodeBlock.of(
                        "$L -> $T.deserializeMap($T.class, $L.getData(), $L, $L)",
                        ctxVar,
                        CollectionsUtils.class,
                        typesUtil.getSafeType(keyType),
                        ctxVar,
                        ctxVar,
                        innerFunction);
            } else if (canonicalName.equals(Optional.class.getName())) {
                TypeMirror elementType = wrapped.getTypeArguments().getFirst();
                CodeBlock innerFunction = getDeserializationFunction(dtoType, property, elementType, depth + 1);
                return CodeBlock.builder()
                        .add("($L) -> {\n", ctxVar)
                        .indent()
                        .beginControlFlow(
                                "if ($1L.getData() == null || $1L.getData() instanceof $2T)",
                                ctxVar,
                                DataTree.DataTreeNull.class)
                        .addStatement("return $T.ok($T.empty())", Result.class, Optional.class)
                        .endControlFlow()
                        .addStatement(
                                "return (($T) $L).apply($L).map($T::ofNullable)",
                                DeserializationFunction.class,
                                innerFunction,
                                ctxVar,
                                Optional.class)
                        .unindent()
                        .add("}")
                        .build();
            }
        }

        // 4. Basic fallback using ObjectMapper mapping
        String ctxVar = "ctx" + depth;
        MethodSpec.Builder innerMethod = MethodSpec.methodBuilder("temp");
        TypeName safeType = configurationClassNameGenerator.publicPropertyClassName(typesUtil.getBoxedType(type));
        boolean handled = nonGenericTypeDeserializerGenerator.handleNonGenericType(
                innerMethod, property, dtoType, type, wrapped, ctxVar + ".getData()", safeType);

        if (handled) {
            CodeBlock code = innerMethod.build().code();
            return CodeBlock.builder()
                    .add("$L -> {\n", ctxVar)
                    .add(code)
                    .add("}")
                    .build();
        }

        return CodeBlock.of(
                "$L -> $L.getMapper().map($L.getData(), new $T<$T>(){})",
                ctxVar,
                ctxVar,
                ctxVar,
                TypeToken.class,
                typesUtil.getBoxedType(type));
    }

    /**
     * Generates deserialization logic for a {@link List} property.
     *
     * <p>Generates:
     *
     * <pre>
     *     return CollectionsUtils.deserializeList(fromMap, context, ctx0 -> ...);
     * </pre>
     *
     * @param builder the method spec builder
     * @param wrappedElementType the wrapped property type mirror
     * @param fromMapName the name of the variable containing the raw data (e.g. {@code fromMap})
     * @return an optional method spec
     */
    private Optional<MethodSpec> handleListType(
            MethodSpec.Builder builder,
            TypeElement dtoType,
            Property property,
            TypeMirrorWrapper wrappedElementType,
            String fromMapName) {
        var listType = wrappedElementType.getTypeArguments().getFirst();
        CodeBlock deserializationFunction = getDeserializationFunction(dtoType, property, listType, 0);
        builder.addCode(
                "return $T.deserializeList($L, context, $L);\n",
                CollectionsUtils.class,
                fromMapName,
                deserializationFunction);
        return Optional.of(builder.build());
    }

    /**
     * Generates deserialization logic for a {@link Map} property.
     *
     * <p>Generates:
     *
     * <pre>
     *     return CollectionsUtils.deserializeMap(KeyType.class, fromMap, context, ctx0 -> ...);
     * </pre>
     *
     * @param builder the method spec builder
     * @param wrappedElementType the wrapped property type mirror
     * @param fromMapName the name of the variable containing the raw data (e.g. {@code fromMap})
     * @return an optional method spec
     */
    private Optional<MethodSpec> handleMapType(
            MethodSpec.Builder builder,
            TypeElement dtoType,
            Property property,
            TypeMirrorWrapper wrappedElementType,
            String fromMapName) {
        var arguments = wrappedElementType.getTypeArguments();
        var keyType = arguments.get(0);
        var valueType = arguments.get(1);

        CodeBlock deserializationFunction = getDeserializationFunction(dtoType, property, valueType, 0);
        builder.addCode(
                "return $T.deserializeMap($T.class, $L, context, $L);\n",
                CollectionsUtils.class,
                typesUtil.getSafeType(keyType),
                fromMapName,
                deserializationFunction);
        return Optional.of(builder.build());
    }

    private Optional<MethodSpec> handleSetType(
            MethodSpec.Builder builder,
            TypeElement dtoType,
            Property property,
            TypeMirrorWrapper wrappedElementType,
            String fromMapName) {
        var setType = wrappedElementType.getTypeArguments().getFirst();
        CodeBlock deserializationFunction = getDeserializationFunction(dtoType, property, setType, 0);
        builder.addCode(
                "return $T.deserializeSet($L, context, $L);\n",
                CollectionsUtils.class,
                fromMapName,
                deserializationFunction);
        return Optional.of(builder.build());
    }

    private Optional<MethodSpec> handleOptionalType(
            MethodSpec.Builder builder,
            TypeElement dtoType,
            Property property,
            TypeMirrorWrapper wrappedElementType,
            String fromMapName) {
        var optionalType = wrappedElementType.getTypeArguments().getFirst();
        CodeBlock deserializationFunction = getDeserializationFunction(dtoType, property, optionalType, 0);

        if (property.settings().hasDefaultValue()) {
            builder.beginControlFlow("if ($L instanceof $T)", fromMapName, Optional.class);
            builder.addStatement("return $T.ok(($T) $L)", Result.class, Optional.class, fromMapName);
            builder.endControlFlow();
        }

        builder.beginControlFlow("if ($L instanceof $T)", fromMapName, DataTree.DataTreeNull.class);
        builder.addStatement("return $T.ok($T.empty())", Result.class, Optional.class);
        builder.endControlFlow();

        String treeVar = property.name() + "Tree";
        if (property.settings().hasDefaultValue()) {
            builder.addStatement(
                    "$T $L = $T.loadFrom($L)", DataTree.class, treeVar, DataTreeTransforms.class, fromMapName);
        } else {
            builder.addStatement("$T $L = $L", DataTree.class, treeVar, fromMapName);
        }

        builder.addCode(
                "$T<$T> innerResult = $L.apply(context.withData($L));\n",
                Result.class,
                typesUtil.getBoxedType(optionalType),
                deserializationFunction,
                treeVar);
        builder.addStatement("return innerResult.map($T::ofNullable)", Optional.class);
        return Optional.of(builder.build());
    }
}
