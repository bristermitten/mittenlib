package me.bristermitten.mittenlib.annotations.compile;

import com.squareup.javapoet.*;
import io.toolisticon.aptk.tools.MessagerUtils;
import io.toolisticon.aptk.tools.TypeMirrorWrapper;
import me.bristermitten.mittenlib.annotations.ast.AbstractConfigStructure;
import me.bristermitten.mittenlib.annotations.ast.ConfigTypeSource;
import me.bristermitten.mittenlib.annotations.ast.Property;
import me.bristermitten.mittenlib.annotations.parser.CustomSerializers;
import me.bristermitten.mittenlib.annotations.ast.CustomSerializerInfo;
import me.bristermitten.mittenlib.annotations.parser.CustomDeserializers;
import me.bristermitten.mittenlib.annotations.util.TypesUtil;
import me.bristermitten.mittenlib.config.SerializationContext;
import java.util.Optional;
import me.bristermitten.mittenlib.config.extension.UseObjectMapperSerialization;
import me.bristermitten.mittenlib.config.reader.ObjectMapper;
import me.bristermitten.mittenlib.config.tree.DataTree;
import me.bristermitten.mittenlib.config.tree.DataTreeTransforms;
import me.bristermitten.mittenlib.util.Strings;

import javax.inject.Inject;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates serialization code for configuration classes.
 * This class is responsible for creating methods that convert from strongly typed
 * configuration objects to {@link DataTree} representations.
 */
public class SerializationCodeGenerator {
    /**
     * The prefix for all generated serialization methods.
     * For example, a method to serialize a field called "test" would be called serializeTest
     */
    public static final String SERIALIZE_METHOD_PREFIX = "serialize";

    private final ConfigurationClassNameGenerator configurationClassNameGenerator;
    private final TypesUtil typesUtil;
    private final CustomSerializers customSerializers;

    @Inject
    public SerializationCodeGenerator(
            ConfigurationClassNameGenerator configurationClassNameGenerator,
            TypesUtil typesUtil,
            CustomSerializers customSerializers) {
        this.configurationClassNameGenerator = configurationClassNameGenerator;
        this.typesUtil = typesUtil;
        this.customSerializers = customSerializers;
    }

    /**
     * Checks if serialization is fully supported for the given configuration.
     * Serialization is supported if all properties either:
     * - Are natively supported types
     * - Are @Config types
     * - Have @UseObjectMapperSerialization annotation
     * <p>
     * Properties with CustomDeserializers that don't have serialization support will prevent
     * full serialization from being generated.
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
     * Gets a list of properties that cannot be serialized.
     * Useful for generating helpful error/warning messages.
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

    /**
     * Checks if a single property can be serialized.
     */
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

            if (canonicalName.equals(List.class.getName()) || canonicalName.equals(Map.class.getName())) {
                var typeArguments = wrappedType.getTypeArguments();
                for (TypeMirror typeArgument : typeArguments) {
                    if (propertyIsUnserializable(new Property(
                            property.name(),
                            typeArgument,
                            property.source(),
                            property.settings()
                    ))) {
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

        if (isKnownSerializableType(wrappedType)) {
            return false;
        }

        // Unknown type - only serializable if CustomSerializer is present
        return customSerializers.getCustomSerializer(propertyTypeMirror).isEmpty();
    }

    /**
     * Creates serialization methods for a config class in its Saver class.
     *
     * @param typeSpecBuilder The builder for the Saver class
     * @param ast             The abstract configuration structure
     */
    public void addSerializeMethodsToSaver(TypeSpec.Builder typeSpecBuilder, AbstractConfigStructure ast) {
        // Generate serialize methods for each property
        for (Property property : ast.properties()) {
            MethodSpec serializeMethod = createSerializeMethodFor(property);
            typeSpecBuilder.addMethod(serializeMethod);
        }
    }

    /**
     * Creates a serialization method for a specific property in a Saver class.
     *
     * @param property The property to create a serialization method for
     * @return A method spec for the serialization method
     */
    private MethodSpec createSerializeMethodFor(Property property) {
        String methodName = SERIALIZE_METHOD_PREFIX + Strings.capitalize(property.name());
        TypeName propertyType = configurationClassNameGenerator.publicPropertyClassName(property);

        // Check if the property is annotated with @UseObjectMapperSerialization
        boolean useObjectMapper = typesUtil.getAnnotation(property.source().element(), UseObjectMapperSerialization.class) != null;

        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PRIVATE)
                .returns(DataTree.class)
                .addParameter(ParameterSpec.builder(propertyType, "value").build())
                .addParameter(ParameterSpec.builder(SerializationContext.class, "context").build());

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

    /**
     * Checks if a type is a known serializable type (primitives, String, Boolean, Number, enums).
     */
    private boolean isKnownSerializableType(TypeMirrorWrapper wrappedType) {
        if (wrappedType.getTypeElement().isEmpty() && wrappedType.isPrimitive()) {
            return true;
        }

        if (typesUtil.getDataTreeType(TypeName.get(wrappedType.unwrap()))
                .isPresent()) {
            return true;
        }

        if (wrappedType.getQualifiedName().equals(Character.class.getName())) { // TODO: do we actually know how to support Character?
            return true;
        }
        return wrappedType.isEnum();
    }

    private void handleObjectMapperSerialization(MethodSpec.Builder builder) {
        // Use ObjectMapper to map the value and then load it as DataTree
        // We use mapper.map(value) which returns an Object (likely a Map or List)
        // then pass that to DataTreeTransforms.loadFrom
        builder.addStatement("return $T.loadFrom(context.getMapper().map(value))", DataTreeTransforms.class);
    }

    /**
     * Recursively generates serialization CodeBlock statements for a given type,
     * converting it to a {@link DataTree} representation.
     * Supports custom serializers, nested config types, collections (List, Map),
     * and primitive/built-in serializable types.
     *
     * @param type             the type of the element being serialized
     * @param inputVar         the name of the local variable holding the value to serialize
     * @param targetExpression the code expression to assign the resulting {@link DataTree} to
     * @param depth            the current recursion depth, used to generate unique variable names
     *                         and avoid local variable scope clashes (e.g. arr0, i0, el0 vs arr1, i1, el1)
     * @return a {@link CodeBlock} containing the serialization logic statements
     */
    private CodeBlock generateSerialization(TypeMirror type, String inputVar, String targetExpression, int depth) {
        CodeBlock.Builder builder = CodeBlock.builder();
        TypeMirrorWrapper wrappedType = TypeMirrorWrapper.wrap(type);

        // 1. Custom Serializer
        Optional<CustomSerializerInfo> customSerializerOptional = customSerializers.getCustomSerializer(type);
        if (customSerializerOptional.isPresent()) {
            CustomSerializerInfo info = customSerializerOptional.get();
            TypeName publicTypeName = configurationClassNameGenerator.publicPropertyClassName(type);
            if (info.isStatic()) {
                builder.addStatement("$L = $T.serialize(($T) $L, context)", targetExpression, info.serializerClass(), publicTypeName, inputVar);
            } else {
                String fieldName = Strings.uncapitalize(info.serializerClass().getSimpleName().toString()) + "Provider";
                builder.addStatement("$L = this.$L.get().apply(($T) $L, context)", targetExpression, fieldName, publicTypeName, inputVar);
            }
            return builder.build();
        }

        // 2. Config type
        if (typesUtil.isConfigType(type)) {
            String saverFieldName = configurationClassNameGenerator.getSaverFieldName(type) + "Provider";
            builder.addStatement("$L = this.$L.get().apply(($T) $L, context)", targetExpression, saverFieldName, configurationClassNameGenerator.publicPropertyClassName(type), inputVar);
            return builder.build();
        }

        // 3. Generic collections (List, Map)
        if (wrappedType.hasTypeArguments()) {
            String canonicalName = wrappedType.erasure().getQualifiedName();
            if (canonicalName.equals(List.class.getName())) {
                TypeMirror elementType = wrappedType.getTypeArguments().getFirst();
                TypeName elementTypeName = configurationClassNameGenerator.publicPropertyClassName(elementType);
                String arrayVar = "arr" + depth;
                String indexVar = "i" + depth;
                String elementVar = "el" + depth;
                String elementTarget = arrayVar + "[" + indexVar + "]";

                builder.addStatement("$T[] $L = new $T[$L.size()]", DataTree.class, arrayVar, DataTree.class, inputVar);
                builder.beginControlFlow("for (int $L = 0; $L < $L.size(); $L++)", indexVar, indexVar, inputVar, indexVar);
                builder.addStatement("$T $L = ($T) $L.get($L)", elementTypeName, elementVar, elementTypeName, inputVar, indexVar);
                builder.add(generateSerialization(elementType, elementVar, elementTarget, depth + 1));
                builder.endControlFlow();
                builder.addStatement("$L = $T.array($L)", targetExpression, DataTree.class, arrayVar);
                return builder.build();
            } else if (canonicalName.equals(Map.class.getName())) {
                var typeArguments = wrappedType.getTypeArguments();
                TypeMirror valueType = typeArguments.get(1);
                TypeName valueTypeName = configurationClassNameGenerator.publicPropertyClassName(valueType);
                String mapVar = "map" + depth;
                String entryVar = "entry" + depth;
                String valueVar = "val" + depth;
                String valTarget = "mapVal" + depth;

                builder.addStatement("$T<$T, $T> $L = new $T<>()", Map.class, DataTree.class, DataTree.class, mapVar, LinkedHashMap.class);
                builder.beginControlFlow("for ($T<?, ?> $L : $L.entrySet())", Map.Entry.class, entryVar, inputVar);
                builder.addStatement("$T $L = ($T) $L.getValue()", valueTypeName, valueVar, valueTypeName, entryVar);
                builder.addStatement("$T $L", DataTree.class, valTarget);
                builder.add(generateSerialization(valueType, valueVar, valTarget, depth + 1));
                builder.addStatement("$L.put($T.loadFrom($L.getKey()), $L)", mapVar, DataTreeTransforms.class, entryVar, valTarget);
                builder.endControlFlow();
                builder.addStatement("$L = $T.map($L)", targetExpression, DataTree.class, mapVar);
                return builder.build();
            }
        }

        // 4. Basic known serializable types or fallback
        builder.addStatement("$L = $T.loadFrom($L)", targetExpression, DataTreeTransforms.class, inputVar);
        return builder.build();
    }
}
