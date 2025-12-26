package me.bristermitten.mittenlib.annotations.compile;

import com.squareup.javapoet.*;
import io.toolisticon.aptk.tools.MessagerUtils;
import io.toolisticon.aptk.tools.TypeMirrorWrapper;
import me.bristermitten.mittenlib.annotations.ast.AbstractConfigStructure;
import me.bristermitten.mittenlib.annotations.ast.ConfigTypeSource;
import me.bristermitten.mittenlib.annotations.ast.Property;
import me.bristermitten.mittenlib.annotations.parser.CustomDeserializers;
import me.bristermitten.mittenlib.annotations.util.TypesUtil;
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

    private final FieldNameGenerator fieldNameGenerator;
    private final ConfigurationClassNameGenerator configurationClassNameGenerator;
    private final MethodNames methodNames;
    private final TypesUtil typesUtil;
    private final CustomDeserializers customDeserializers;

    @Inject
    public SerializationCodeGenerator(
            FieldNameGenerator fieldNameGenerator,
            ConfigurationClassNameGenerator configurationClassNameGenerator,
            MethodNames methodNames,
            TypesUtil typesUtil,
            CustomDeserializers customDeserializers) {
        this.fieldNameGenerator = fieldNameGenerator;
        this.configurationClassNameGenerator = configurationClassNameGenerator;
        this.methodNames = methodNames;
        this.typesUtil = typesUtil;
        this.customDeserializers = customDeserializers;
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
            return false; // we only support List and Map generics
        }

        // Config types are always serializable
        if (typesUtil.isConfigType(propertyTypeMirror)) {
            return false;
        }

        if (isKnownSerializableType(wrappedType)) {
            return false;
        }

        // Unknown type - only serializable if CustomDeserializer is present
        return customDeserializers.getCustomDeserializer(propertyTypeMirror).isEmpty();
    }

    /**
     * Creates serialization methods for a config class.
     *
     * @param typeSpecBuilder The builder for the config class
     * @param ast             The abstract configuration structure
     */
    public void createSerializeMethods(TypeSpec.Builder typeSpecBuilder, AbstractConfigStructure ast) {
        // Generate serialize methods for each property
        for (Property property : ast.properties()) {
            MethodSpec serializeMethod = createSerializeMethodFor(property);
            typeSpecBuilder.addMethod(serializeMethod);
        }

        // Generate main serialize method
        MethodSpec mainSerializeMethod = createMainSerializeMethod(ast);
        typeSpecBuilder.addMethod(mainSerializeMethod);
    }

    /**
     * Creates a serialization method for a specific property.
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
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(DataTree.class)
                .addParameter(ParameterSpec.builder(propertyType, "value").build())
                .addParameter(ParameterSpec.builder(ObjectMapper.class, "mapper").build());


        // Handle null values
        if (property.settings().isNullable()) {
            builder.beginControlFlow("if (value == null)");
            builder.addStatement("return $T.null_()", DataTree.class);
            builder.endControlFlow();
        }

        TypeMirror propertyTypeMirror = property.propertyType();

        if (useObjectMapper) {
            // Use ObjectMapper for serialization
            handleObjectMapperSerialization(builder);
            return builder.build();
        }

        TypeMirrorWrapper wrappedType = TypeMirrorWrapper.wrap(propertyTypeMirror);

        // Check if it's a generic type (List, Map, etc.)
        if (wrappedType.hasTypeArguments()) {
            String canonicalName = wrappedType.erasure().getQualifiedName();

            if (canonicalName.equals(List.class.getName())) {
                handleListSerialization(builder, wrappedType);
                return builder.build();
            } else if (canonicalName.equals(Map.class.getName())) {
                handleMapSerialization(builder, wrappedType);
                return builder.build();
            }
            // Unknown generic type - check if it has a CustomDeserializer
            if (customDeserializers.getCustomDeserializer(propertyTypeMirror).isEmpty()) {
                // No CustomDeserializer and no @UseObjectMapperSerialization
                MessagerUtils.error(property.source().element(),
                        "Cannot serialize generic type '" + canonicalName + "'. " +
                                "Only List and Map are supported by default. " +
                                "Use @UseObjectMapperSerialization to explicitly opt-in to ObjectMapper serialization, " +
                                "or provide a CustomDeserializer with serialization support.");
            }
            // Generate a fallback to prevent further compilation errors
            builder.addStatement("throw new $T($S)", UnsupportedOperationException.class,
                    "Serialization not supported for type: " + canonicalName);
            return builder.build();
        }

        // Check if it's a config type
        if (typesUtil.isConfigType(propertyTypeMirror)) {
            handleConfigTypeSerialization(builder, propertyTypeMirror);
            return builder.build();
        }

        // Check if it's a known serializable type
        if (!isKnownSerializableType(wrappedType)) {

            if (customDeserializers.getCustomDeserializer(propertyTypeMirror).isEmpty()) {
                // Unknown type without @UseObjectMapperSerialization and without CustomDeserializer
                MessagerUtils.error(property.source().element(),
                        "Cannot serialize type '" + propertyTypeMirror + "'. " +
                                "Supported types are: primitives, String, Boolean, Number types, enums, @Config types, Map, and List. " +
                                "Use @UseObjectMapperSerialization to explicitly opt-in to ObjectMapper serialization for custom types, " +
                                "or provide a CustomDeserializer with serialization support.");
            }
            // If it has a CustomDeserializer, skip serialization generation for this property
            // The serialization will be skipped at the config level
            // Generate a fallback to prevent further compilation errors
            builder.addStatement("throw new $T($S)", UnsupportedOperationException.class,
                    "Serialization not supported for type: " + propertyTypeMirror);
            return builder.build();
        }

        // For primitive types, strings, enums, and other basic types, use DataTreeTransforms.loadFrom
        // This only handles POJOs: primitives, String, Boolean, Number, Map, List, enums
        builder.addStatement("return $T.loadFrom(value)", DataTreeTransforms.class);

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
        builder.addStatement("return $T.loadFrom(mapper.map(value))", DataTreeTransforms.class);
    }

    private void handleListSerialization(MethodSpec.Builder builder, TypeMirrorWrapper wrappedType) {
        var elementType = wrappedType.getTypeArguments().getFirst();

        if (typesUtil.isConfigType(elementType)) {
            TypeName configClassName = configurationClassNameGenerator.getConfigClassName(elementType, null);
            String serializeMethodName = methodNames.getSerializeMethodName(configClassName);

            builder.addStatement("$T[] array = new $T[value.size()]", DataTree.class, DataTree.class);
            builder.beginControlFlow("for (int i = 0; i < value.size(); i++)");
            builder.addStatement("array[i] = $T.$L(value.get(i), mapper)", configClassName, serializeMethodName);
            builder.endControlFlow();
            builder.addStatement("return $T.array(array)", DataTree.class);
        } else {
            builder.addStatement("return $T.loadFrom(value)", DataTreeTransforms.class);
        }
    }

    private void handleMapSerialization(MethodSpec.Builder builder, TypeMirrorWrapper wrappedType) {
        var typeArguments = wrappedType.getTypeArguments();
        var valueType = typeArguments.get(1);

        if (typesUtil.isConfigType(valueType)) {
            // Map with config object values - need to serialize each value
            TypeName configClassName = configurationClassNameGenerator.getConfigClassName(valueType, null);
            String serializeMethodName = methodNames.getSerializeMethodName(configClassName);

            builder.addStatement("$T<$T, $T> map = new $T<>()",
                    Map.class, DataTree.class, DataTree.class, LinkedHashMap.class);
            builder.beginControlFlow("for ($T<?, ?> entry : value.entrySet())", Map.Entry.class);
            builder.addStatement("map.put($T.loadFrom(entry.getKey()), $T.$L(($T) entry.getValue(), mapper))",
                    DataTreeTransforms.class, configClassName, serializeMethodName, configClassName);
            builder.endControlFlow();
            builder.addStatement("return $T.map(map)", DataTree.class);
        } else {
            // Map with primitive values - use DataTreeTransforms
            builder.addStatement("return $T.loadFrom(value)", DataTreeTransforms.class);
        }
    }

    private void handleConfigTypeSerialization(MethodSpec.Builder builder, TypeMirror configType) {
        TypeName configClassName = configurationClassNameGenerator.getConfigClassName(configType, null);
        String serializeMethodName = methodNames.getSerializeMethodName(configClassName);
        builder.addStatement("return $T.$L(value, mapper)", configClassName, serializeMethodName);
    }

    /**
     * Creates the main serialization method that serializes the entire config to a DataTree.
     *
     * @param ast The configuration structure
     * @return A method spec for the main serialization method
     */
    private MethodSpec createMainSerializeMethod(AbstractConfigStructure ast) {
        String methodName = methodNames.getSerializeMethodName(ast);
        ClassName publicClassName = configurationClassNameGenerator.getPublicClassName(ast);


        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(DataTree.class)
                .addParameter(ParameterSpec.builder(publicClassName, "config", Modifier.FINAL).build())
                .addParameter(ParameterSpec.builder(ObjectMapper.class, "mapper", Modifier.FINAL).build());


        // Create a map to hold the serialized values
        builder.addStatement("$T<$T, $T> map = new $T<>()",
                Map.class, DataTree.class, DataTree.class, LinkedHashMap.class);

        // Serialize each property
        for (Property property : ast.properties()) {
            String key = fieldNameGenerator.getConfigFieldName(property);
            String serializeMethodName = SERIALIZE_METHOD_PREFIX + Strings.capitalize(property.name());

            // Get the property value based on source type
            CodeBlock propertyAccess = switch (ast.source()) {
                case ConfigTypeSource.InterfaceConfigTypeSource ignored -> CodeBlock.of("config.$L()", property.name());
                case ConfigTypeSource.ClassConfigTypeSource ignored ->
                        CodeBlock.of("config.$L()", methodNames.safeMethodName(property));
            };

            builder.addStatement("map.put($T.string($S), $L($L, mapper))",
                    DataTree.class,
                    key,
                    serializeMethodName,
                    propertyAccess);
        }

        builder.addStatement("return $T.map(map)", DataTree.class);

        return builder.build();
    }
}
