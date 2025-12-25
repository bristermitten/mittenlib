package me.bristermitten.mittenlib.annotations.compile;

import com.squareup.javapoet.*;
import me.bristermitten.mittenlib.annotations.ast.AbstractConfigStructure;
import me.bristermitten.mittenlib.annotations.ast.ConfigTypeSource;
import me.bristermitten.mittenlib.annotations.ast.Property;
import me.bristermitten.mittenlib.config.tree.DataTree;
import me.bristermitten.mittenlib.config.tree.DataTreeTransforms;
import me.bristermitten.mittenlib.util.Strings;

import javax.inject.Inject;
import javax.lang.model.element.Modifier;
import java.util.LinkedHashMap;
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

    @Inject
    public SerializationCodeGenerator(
            FieldNameGenerator fieldNameGenerator,
            ConfigurationClassNameGenerator configurationClassNameGenerator,
            MethodNames methodNames) {
        this.fieldNameGenerator = fieldNameGenerator;
        this.configurationClassNameGenerator = configurationClassNameGenerator;
        this.methodNames = methodNames;
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
            MethodSpec serializeMethod = createSerializeMethodFor(property, ast);
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
     * @param ast      The configuration structure
     * @return A method spec for the serialization method
     */
    private MethodSpec createSerializeMethodFor(Property property, AbstractConfigStructure ast) {
        String methodName = SERIALIZE_METHOD_PREFIX + Strings.capitalize(property.name());
        TypeName propertyType = configurationClassNameGenerator.publicPropertyClassName(property);

        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(DataTree.class)
                .addParameter(ParameterSpec.builder(propertyType, "value").build());

        // Handle null values
        if (property.settings().isNullable()) {
            builder.beginControlFlow("if (value == null)");
            builder.addStatement("return $T.null_()", DataTree.class);
            builder.endControlFlow();
        }

        // For now, use DataTreeTransforms.loadFrom for all types
        // This handles primitives, strings, collections, and will convert
        // nested config objects to maps via their toString or natural serialization
        builder.addStatement("return $T.loadFrom(value)", DataTreeTransforms.class);

        return builder.build();
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
                .addParameter(ParameterSpec.builder(publicClassName, "config", Modifier.FINAL).build());

        // Create a map to hold the serialized values
        builder.addStatement("$T<$T, $T> map = new $T<>()",
                Map.class, DataTree.class, DataTree.class, LinkedHashMap.class);

        // Serialize each property
        for (Property property : ast.properties()) {
            String key = fieldNameGenerator.getConfigFieldName(property);
            String serializeMethodName = SERIALIZE_METHOD_PREFIX + Strings.capitalize(property.name());
            
            // Get the property value based on source type
            CodeBlock propertyAccess = switch (ast.source()) {
                case ConfigTypeSource.InterfaceConfigTypeSource ignored ->
                        CodeBlock.of("config.$L()", property.name());
                case ConfigTypeSource.ClassConfigTypeSource ignored ->
                        CodeBlock.of("config.$L()", methodNames.safeMethodName(property));
            };

            builder.addStatement("map.put($T.string($S), $L($L))",
                    DataTree.class,
                    key,
                    serializeMethodName,
                    propertyAccess);
        }

        builder.addStatement("return $T.map(map)", DataTree.class);

        return builder.build();
    }
}
