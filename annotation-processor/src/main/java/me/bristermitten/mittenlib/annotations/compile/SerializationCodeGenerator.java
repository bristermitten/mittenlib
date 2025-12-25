package me.bristermitten.mittenlib.annotations.compile;

import com.google.gson.reflect.TypeToken;
import com.squareup.javapoet.*;
import io.toolisticon.aptk.tools.MessagerUtils;
import io.toolisticon.aptk.tools.TypeMirrorWrapper;
import me.bristermitten.mittenlib.annotations.ast.AbstractConfigStructure;
import me.bristermitten.mittenlib.annotations.ast.ConfigTypeSource;
import me.bristermitten.mittenlib.annotations.ast.Property;
import me.bristermitten.mittenlib.annotations.util.TypesUtil;
import me.bristermitten.mittenlib.config.extension.UseObjectMapperSerialization;
import me.bristermitten.mittenlib.config.tree.DataTree;
import me.bristermitten.mittenlib.config.tree.DataTreeTransforms;
import me.bristermitten.mittenlib.util.Strings;

import javax.inject.Inject;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;
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

    @Inject
    public SerializationCodeGenerator(
            FieldNameGenerator fieldNameGenerator,
            ConfigurationClassNameGenerator configurationClassNameGenerator,
            MethodNames methodNames,
            TypesUtil typesUtil) {
        this.fieldNameGenerator = fieldNameGenerator;
        this.configurationClassNameGenerator = configurationClassNameGenerator;
        this.methodNames = methodNames;
        this.typesUtil = typesUtil;
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
        boolean useObjectMapper = property.source().element().getAnnotation(UseObjectMapperSerialization.class) != null;

        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(DataTree.class)
                .addParameter(ParameterSpec.builder(propertyType, "value").build());
        
        // Add ObjectMapper parameter if needed
        if (useObjectMapper) {
            builder.addParameter(ParameterSpec.builder(
                    ClassName.get("me.bristermitten.mittenlib.config.reader", "ObjectMapper"), 
                    "mapper").build());
        }

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
            // Unknown generic type without @UseObjectMapperSerialization
            MessagerUtils.error(property.source().element(),
                    "Cannot serialize generic type '" + canonicalName + "'. " +
                    "Only List and Map are supported by default. " +
                    "Use @UseObjectMapperSerialization to explicitly opt-in to ObjectMapper serialization.");
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
            // Unknown type without @UseObjectMapperSerialization
            MessagerUtils.error(property.source().element(),
                    "Cannot serialize type '" + propertyTypeMirror + "'. " +
                    "Supported types are: primitives, String, Boolean, Number types, enums, @Config types, Map, and List. " +
                    "Use @UseObjectMapperSerialization to explicitly opt-in to ObjectMapper serialization for custom types.");
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
        // Primitives
        if (wrappedType.getTypeElement().isEmpty() && wrappedType.getTypeMirror().getKind().isPrimitive()) {
            return true;
        }

        // Check for basic types that DataTreeTransforms can handle
        String qualifiedName = wrappedType.getQualifiedName();
        if (qualifiedName != null) {
            // String, Boolean, and boxed primitives
            if (qualifiedName.equals(String.class.getName()) ||
                qualifiedName.equals(Boolean.class.getName()) ||
                qualifiedName.equals(Integer.class.getName()) ||
                qualifiedName.equals(Long.class.getName()) ||
                qualifiedName.equals(Short.class.getName()) ||
                qualifiedName.equals(Byte.class.getName()) ||
                qualifiedName.equals(Float.class.getName()) ||
                qualifiedName.equals(Double.class.getName()) ||
                qualifiedName.equals(Character.class.getName())) {
                return true;
            }
        }

        // Enums
        if (wrappedType.isEnum()) {
            return true;
        }

        return false;
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
            builder.addStatement("array[i] = $T.$L(value.get(i))", configClassName, serializeMethodName);
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
            builder.addStatement("map.put($T.loadFrom(entry.getKey()), $T.$L(($T) entry.getValue()))", 
                    DataTreeTransforms.class, configClassName, serializeMethodName, configClassName);
            builder.endControlFlow();
            builder.addStatement("return $T.map(map)", DataTree.class);
        } else {
            // Map with primitive values - use DataTreeTransforms
            builder.addStatement("return $T.loadFrom(value)", DataTreeTransforms.class);
        }
    }

    private void handleConfigTypeSerialization(MethodSpec.Builder builder, TypeMirror configType) {
        // Direct config object - call its serialize method
        TypeName configClassName = configurationClassNameGenerator.getConfigClassName(configType, null);
        String serializeMethodName = methodNames.getSerializeMethodName(configClassName);
        builder.addStatement("return $T.$L(value)", configClassName, serializeMethodName);
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

        // Check if any property needs ObjectMapper
        boolean needsObjectMapper = ast.properties().stream()
                .anyMatch(p -> p.source().element().getAnnotation(UseObjectMapperSerialization.class) != null);

        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(DataTree.class)
                .addParameter(ParameterSpec.builder(publicClassName, "config", Modifier.FINAL).build());

        // Add ObjectMapper parameter if any property needs it
        if (needsObjectMapper) {
            builder.addParameter(ParameterSpec.builder(
                    ClassName.get("me.bristermitten.mittenlib.config.reader", "ObjectMapper"), 
                    "mapper", Modifier.FINAL).build());
        }

        // Create a map to hold the serialized values
        builder.addStatement("$T<$T, $T> map = new $T<>()",
                Map.class, DataTree.class, DataTree.class, LinkedHashMap.class);

        // Serialize each property
        for (Property property : ast.properties()) {
            String key = fieldNameGenerator.getConfigFieldName(property);
            String serializeMethodName = SERIALIZE_METHOD_PREFIX + Strings.capitalize(property.name());
            boolean propertyUsesObjectMapper = property.source().element().getAnnotation(UseObjectMapperSerialization.class) != null;
            
            // Get the property value based on source type
            CodeBlock propertyAccess = switch (ast.source()) {
                case ConfigTypeSource.InterfaceConfigTypeSource ignored ->
                        CodeBlock.of("config.$L()", property.name());
                case ConfigTypeSource.ClassConfigTypeSource ignored ->
                        CodeBlock.of("config.$L()", methodNames.safeMethodName(property));
            };

            // Call the serialize method with or without ObjectMapper parameter
            if (propertyUsesObjectMapper) {
                builder.addStatement("map.put($T.string($S), $L($L, mapper))",
                        DataTree.class,
                        key,
                        serializeMethodName,
                        propertyAccess);
            } else {
                builder.addStatement("map.put($T.string($S), $L($L))",
                        DataTree.class,
                        key,
                        serializeMethodName,
                        propertyAccess);
            }
        }

        builder.addStatement("return $T.map(map)", DataTree.class);

        return builder.build();
    }
}
