package me.bristermitten.mittenlib.annotations.compile;

import com.google.inject.Inject;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.WildcardTypeName;
import io.toolisticon.aptk.tools.TypeMirrorWrapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import me.bristermitten.mittenlib.annotations.ast.AbstractConfigStructure;
import me.bristermitten.mittenlib.annotations.ast.CustomSerializerInfo;
import me.bristermitten.mittenlib.annotations.ast.Property;
import me.bristermitten.mittenlib.annotations.parser.CustomSerializers;
import me.bristermitten.mittenlib.annotations.util.NewtypeUtil;
import me.bristermitten.mittenlib.annotations.util.TypesUtil;
import me.bristermitten.mittenlib.config.CollectionsUtils;
import me.bristermitten.mittenlib.config.SerializationContext;
import me.bristermitten.mittenlib.config.extension.UseObjectMapperSerialization;
import me.bristermitten.mittenlib.config.tree.DataTree;
import me.bristermitten.mittenlib.config.tree.DataTreeTransforms;
import me.bristermitten.mittenlib.util.Strings;

/**
 * Generates serialization code for configuration classes. This class is responsible for creating
 * methods that convert from strongly typed configuration objects to {@link DataTree}
 * representations.
 */
public class SerializationCodeGenerator {

    private final ConfigurationClassNameGenerator configurationClassNameGenerator;
    private final TypesUtil typesUtil;
    private final CustomSerializers customSerializers;
    private final MethodNames methodNames;

    @Inject
    public SerializationCodeGenerator(
            ConfigurationClassNameGenerator configurationClassNameGenerator,
            TypesUtil typesUtil,
            CustomSerializers customSerializers,
            MethodNames methodNames) {
        this.configurationClassNameGenerator = configurationClassNameGenerator;
        this.typesUtil = typesUtil;
        this.customSerializers = customSerializers;
        this.methodNames = methodNames;
    }

    /**
     * Checks if serialization is fully supported for the given configuration. Serialization is
     * supported if all properties either: - Are natively supported types - Are @Config types -
     * Have @UseObjectMapperSerialization annotation
     *
     * <p>Properties with CustomDeserializers that don't have serialization support will prevent full
     * serialization from being generated.
     *
     * @param ast The configuration structure to check
     * @return true if serialization can be fully generated, false otherwise
     */
    public boolean isSerializationSupported(AbstractConfigStructure ast) {
        for (Property property : ast.properties()) {
            if (propertyIsUnserializable(property)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Gets a list of properties that cannot be serialized. Useful for generating helpful
     * error/warning messages.
     *
     * @param ast The configuration structure to check
     * @return List of property names that cannot be serialized
     */
    public List<String> getUnsupportedSerializationProperties(AbstractConfigStructure ast) {
        List<String> unsupported = new ArrayList<>();
        for (Property property : ast.properties()) {
            if (propertyIsUnserializable(property)) {
                unsupported.add(property.name() + " (" + property.propertyType() + ")");
            }
        }
        return unsupported;
    }

    /** Checks if a single property can be serialized. */
    private boolean propertyIsUnserializable(Property property) {
        // explicitly marked as using ObjectMapper serialization - always serializable
        if (typesUtil.getAnnotation(property.source().element(), UseObjectMapperSerialization.class) != null) {
            return false;
        }

        TypeMirror propertyTypeMirror = property.propertyType();
        TypeMirrorWrapper wrappedType = TypeMirrorWrapper.wrap(propertyTypeMirror);

        // Check generic types
        if (wrappedType.hasTypeArguments()) {
            String canonicalName = wrappedType.erasure().getQualifiedName();

            if (typesUtil.isCollection(propertyTypeMirror) || canonicalName.equals(Map.class.getName())) {
                var typeArguments = wrappedType.getTypeArguments();
                for (TypeMirror typeArgument : typeArguments) {
                    if (propertyIsUnserializable(
                            new Property(property.name(), typeArgument, property.source(), property.settings()))) {
                        return true;
                    }
                }
                return false;
            }
            // Unknown generic type: treat as unserializable unless explicitly opted in via UseObjectMapperSerialization
            return true;
        }

        // Config types are always serializable
        if (typesUtil.isConfigType(propertyTypeMirror)) {
            return false;
        }

        // Newtypes are serializable if their underlying type is serializable
        if (typesUtil.isNewtype(propertyTypeMirror)) {
            TypeElement typeElement = (TypeElement) ((DeclaredType) propertyTypeMirror).asElement();
            TypeMirror underlyingType = NewtypeUtil.getUnderlyingType(typeElement);
            return propertyIsUnserializable(
                    new Property(property.name(), underlyingType, property.source(), property.settings()));
        }

        if (isKnownSerializableType(wrappedType)) {
            return false;
        }

        // Unknown type - only serializable if CustomSerializer is present
        return customSerializers.getCustomInfo(propertyTypeMirror).isEmpty();
    }

    /**
     * Adds private serialization methods for each property of a configuration structure to its saver
     * class.
     *
     * @param typeSpecBuilder the builder for the saver class
     * @param ast the configuration structure
     */
    public void addSerializeMethodsToSaver(TypeSpec.Builder typeSpecBuilder, AbstractConfigStructure ast) {
        // Generate serialize methods for each property
        for (Property property : ast.properties()) {
            MethodSpec serializeMethod = createSerializeMethodFor(property);
            typeSpecBuilder.addMethod(serializeMethod);
        }
    }

    /**
     * Get the parameter type for a serialization method. This method adds wildcards to collection
     * types to allow for both the public and implementation types.
     *
     * <p>For example, a property with type {@code List<T>} will be mapped to {@code List<? extends
     * T>}
     */
    private TypeName getSerializeParameterType(Property property) {
        TypeName typeName = configurationClassNameGenerator.publicPropertyClassName(property);
        if (typeName instanceof ParameterizedTypeName parameterizedTypeName) {
            ClassName rawType = parameterizedTypeName.rawType();
            if (rawType.equals(ClassName.get(List.class))
                    || rawType.equals(ClassName.get(Set.class))
                    || rawType.equals(ClassName.get(Map.class))) {
                List<TypeName> typeArguments = parameterizedTypeName.typeArguments().stream()
                        .map(arg -> (TypeName) WildcardTypeName.subtypeOf(arg))
                        .toList();
                return ParameterizedTypeName.get(rawType, typeArguments.toArray(new TypeName[0]));
            }
        }
        return typeName;
    }

    /**
     * Creates a private serialization method for a specific property.
     *
     * <p>Generates:
     *
     * <pre>{@code
     * private DataTree serializeCount(Integer value, SerializationContext context) {
     *     if (value == null) return DataTree.null_();
     *     DataTree result;
     *     result = DataTreeTransforms.loadFrom(value);
     *     return result;
     * }
     * }</pre>
     *
     * @param property the property to create a serialization method for, used for the method name
     *     (e.g. {@code serializeCount}) and return type
     * @return a method spec for the serialization method
     */
    private MethodSpec createSerializeMethodFor(Property property) {
        String methodName = methodNames.getSerializeMethodName(property);
        TypeName propertyType = getSerializeParameterType(property);

        // Check if the property is annotated with @UseObjectMapperSerialization
        boolean useObjectMapper =
                typesUtil.getAnnotation(property.source().element(), UseObjectMapperSerialization.class) != null;

        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addJavadoc("""
                        Serializes the {@code $L} property into a {@link $T}.

                        @param value the value to serialize
                        @param context the serialization context
                        @return the serialized DataTree representation
                        """, property.name(), DataTree.class)
                .addModifiers(Modifier.PRIVATE)
                .returns(DataTree.class)
                .addParameter(ParameterSpec.builder(propertyType, "value").build())
                .addParameter(ParameterSpec.builder(SerializationContext.class, "context")
                        .build());

        // Handle null values
        if (property.settings().isNullable()) {
            builder.beginControlFlow("if (value == null)");
            builder.addStatement("return $T.null_()", DataTree.class);
            builder.endControlFlow();
        }

        if (useObjectMapper) {
            // Use ObjectMapper for serialization
            handleObjectMapperSerialization(builder);
            return builder.build();
        }

        builder.addStatement("$T result", DataTree.class);
        builder.addCode(generateSerialization(property.propertyType(), "value", "result", 0));
        builder.addStatement("return result");

        return builder.build();
    }

    /** Checks if a type is a known serializable type (primitives, String, Boolean, Number, enums). */
    private boolean isKnownSerializableType(TypeMirrorWrapper wrappedType) {
        if (wrappedType.getTypeElement().isEmpty() && wrappedType.isPrimitive()) {
            return true;
        }

        if (typesUtil.getDataTreeType(TypeName.get(wrappedType.unwrap())).isPresent()) {
            return true;
        }

        if (wrappedType
                .getQualifiedName()
                .equals(Character.class.getName())) { // TODO: do we actually know how to support Character?
            return true;
        }
        return wrappedType.isEnum();
    }

    /**
     * Handles serialization by delegating to an {@link
     * me.bristermitten.mittenlib.config.reader.ObjectMapper}.
     *
     * <p>Generates:
     *
     * <pre>{@code
     * return DataTreeTransforms.loadFrom(context.getMapper().map(value));
     * }</pre>
     *
     * @param builder the method builder
     */
    private void handleObjectMapperSerialization(MethodSpec.Builder builder) {
        // Use ObjectMapper to map the value and then load it as DataTree
        // We use mapper.map(value) which returns an Object (likely a Map or List)
        // then pass that to DataTreeTransforms.loadFrom
        builder.addStatement("return $T.loadFrom(context.getMapper().map(value))", DataTreeTransforms.class);
    }

    /**
     * Recursively generates serialization {@link CodeBlock} statements for a given type, converting
     * it to a {@link DataTree} representation. Supports custom serializers, nested config types,
     * collections ({@link List}, {@link Map}), and primitive/built-in serializable types.
     *
     * <p>For a {@link List}, generates:
     *
     * <pre>{@code
     * DataTree[] arr0 = new DataTree[value.size()];
     * for (int i0 = 0; i0 < value.size(); i0++) {
     *     String el0 = (String) value.get(i0);
     *     arr0[i0] = DataTreeTransforms.loadFrom(el0);
     * }
     * result = DataTree.array(arr0);
     * }</pre>
     *
     * @param type the type of the element being serialized
     * @param inputVar the name of the local variable holding the value to serialize (e.g. {@code
     *     value})
     * @param targetExpression the code expression to assign the resulting {@link DataTree} to (e.g.
     *     {@code result} or {@code arr0[i0]})
     * @param depth the current recursion depth, used to generate unique variable names and avoid
     *     local variable scope clashes (e.g. {@code arr0}, {@code i0}, {@code el0} vs {@code arr1},
     *     {@code i1}, {@code el1})
     * @return a {@link CodeBlock} containing the serialization logic statements
     */
    private CodeBlock generateSerialization(TypeMirror type, String inputVar, String targetExpression, int depth) {
        CodeBlock.Builder builder = CodeBlock.builder();
        generateSerialization(type, inputVar, targetExpression, depth, builder);
        return builder.build();
    }

    private void generateSerialization(
            TypeMirror type, String inputVar, String targetExpression, int depth, CodeBlock.Builder builder) {
        TypeMirrorWrapper wrappedType = TypeMirrorWrapper.wrap(type);

        // Custom Serializer
        Optional<CustomSerializerInfo> customSerializerOptional = customSerializers.getCustomInfo(type);
        if (customSerializerOptional.isPresent()) {
            CustomSerializerInfo info = customSerializerOptional.get();
            TypeName publicTypeName = configurationClassNameGenerator.publicPropertyClassName(type);
            if (info.isStatic()) {
                builder.addStatement(
                        "$L = $T.serialize(($T) $L, context)",
                        targetExpression,
                        info.serializerClass(),
                        publicTypeName,
                        inputVar);
            } else {
                String fieldName = Strings.uncapitalize(
                                info.serializerClass().getSimpleName().toString())
                        + ConfigurationClassNameGenerator.PROVIDER_SUFFIX;
                builder.addStatement(
                        "$L = this.$L.get().apply(($T) $L, context)",
                        targetExpression,
                        fieldName,
                        publicTypeName,
                        inputVar);
            }
            return;
        }

        // Newtype
        if (typesUtil.isNewtype(type)) {
            TypeElement typeElement = (TypeElement) ((DeclaredType) type).asElement();
            TypeMirror underlyingType = NewtypeUtil.getUnderlyingType(typeElement);
            String accessor = NewtypeUtil.getAccessorName(typeElement);
            String unwrappedVar = inputVar + "_" + depth + "_unwrapped";
            builder.addStatement("$T $L = $L.$L", underlyingType, unwrappedVar, inputVar, accessor);
            generateSerialization(underlyingType, unwrappedVar, targetExpression, depth + 1, builder);
            return;
        }

        // Config type
        if (typesUtil.isConfigType(type)) {
            String saverFieldName = configurationClassNameGenerator.getSerializerProviderFieldName(type);
            builder.addStatement(
                    "$L = this.$L.get().apply(($T) $L, context)",
                    targetExpression,
                    saverFieldName,
                    configurationClassNameGenerator.publicPropertyClassName(type),
                    inputVar);
            return;
        }

        // Generic collections (List, Map)
        if (wrappedType.hasTypeArguments()) {
            String canonicalName = wrappedType.erasure().getQualifiedName();
            if (canonicalName.equals(List.class.getName()) || canonicalName.equals(Set.class.getName())) {
                TypeMirror elementType = wrappedType.getTypeArguments().getFirst();
                String serializeHelper = canonicalName.equals(List.class.getName()) ? "serializeList" : "serializeSet";
                String elVar = "el" + depth;
                String ctxVar = "ctx" + depth;
                String elementTarget = "res" + depth;

                builder.beginControlFlow(
                        "$L = $T.$L($L, context, ($L, $L) ->",
                        targetExpression,
                        CollectionsUtils.class,
                        serializeHelper,
                        inputVar,
                        elVar,
                        ctxVar);
                builder.addStatement("$T $L", DataTree.class, elementTarget);
                generateSerialization(elementType, elVar, elementTarget, depth + 1, builder);
                builder.addStatement("return $L", elementTarget);
                builder.endControlFlow();
                builder.addStatement(")");
                return;
            } else if (canonicalName.equals(Map.class.getName())) {
                var typeArguments = wrappedType.getTypeArguments();
                TypeMirror valueType = typeArguments.get(1);
                String valVar = "val" + depth;
                String ctxVar = "ctx" + depth;
                String valTarget = "mapVal" + depth;

                builder.beginControlFlow(
                        "$L = $T.serializeMap($L, context, ($L, $L) ->",
                        targetExpression,
                        CollectionsUtils.class,
                        inputVar,
                        valVar,
                        ctxVar);
                builder.addStatement("$T $L", DataTree.class, valTarget);
                generateSerialization(valueType, valVar, valTarget, depth + 1, builder);
                builder.addStatement("return $L", valTarget);
                builder.endControlFlow();
                builder.addStatement(")");
                return;
            }
        }

        // Enum type
        if (wrappedType.isEnum()) {
            builder.addStatement(
                    "$L = $T.loadFrom($L == null ? null : $L.name())",
                    targetExpression,
                    DataTreeTransforms.class,
                    inputVar,
                    inputVar);
            return;
        }

        // Basic known serializable types or fallback
        builder.addStatement("$L = $T.loadFrom($L)", targetExpression, DataTreeTransforms.class, inputVar);
    }
}
