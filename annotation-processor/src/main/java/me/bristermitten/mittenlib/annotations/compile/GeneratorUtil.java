package me.bristermitten.mittenlib.annotations.compile;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import me.bristermitten.mittenlib.annotations.ast.AbstractConfigStructure;
import me.bristermitten.mittenlib.annotations.ast.ConfigTypeSource;
import me.bristermitten.mittenlib.annotations.ast.Property;
import org.jspecify.annotations.Nullable;

/** Utility class for shared code generation logic. */
public final class GeneratorUtil {
    private GeneratorUtil() {}

    /**
     * Resolves the DAO name for a configuration structure. For interfaces, this is the generated
     * DefaultMethodAccess class. For classes, this is the original class itself.
     *
     * @param ast The configuration structure
     * @param classNameGenerator The class name generator
     * @return The DAO class name, or null if it's an interface without any defaults
     */
    public static @Nullable ClassName getDaoName(
            AbstractConfigStructure ast, ConfigurationClassNameGenerator classNameGenerator) {
        return switch (ast.source()) {
            case ConfigTypeSource.InterfaceConfigTypeSource ignored -> classNameGenerator.getInnerDaoName(ast);
            case ConfigTypeSource.ClassConfigTypeSource ignored -> ast.name();
        };
    }

    /**
     * Adds a DAO instantiation statement to a method builder if the configuration has any default
     * values.
     *
     * @param ast The configuration structure
     * @param methodBuilder The method builder to add the statement to
     * @param daoName The DAO class name
     */
    public static void addDaoInstantiationIfNecessary(
            AbstractConfigStructure ast, MethodSpec.Builder methodBuilder, @Nullable ClassName daoName) {
        boolean hasAnyDefault =
                ast.properties().stream().anyMatch(p -> p.settings().hasDefaultValue());

        if (daoName != null && hasAnyDefault) {
            methodBuilder.addStatement("$T dao = new $T()", daoName, daoName);
        }
    }

    /**
     * Generates a {@link CodeBlock} for accessing a property on a given variable.
     *
     * @param ast The configuration structure
     * @param property The property to access
     * @param variableName The name of the variable to access the property on
     * @param methodNames The method names generator (used for safe method names in classes)
     * @param useGetters Whether to use getter methods (true) or direct field access (false) for
     *     classes
     * @return A {@link CodeBlock} representing the property access
     */
    public static CodeBlock getPropertyAccess(
            AbstractConfigStructure ast,
            Property property,
            String variableName,
            MethodNames methodNames,
            boolean useGetters) {
        return switch (ast.source()) {
            case ConfigTypeSource.InterfaceConfigTypeSource ignored ->
                CodeBlock.of("$L.$L()", variableName, property.name());
            case ConfigTypeSource.ClassConfigTypeSource ignored ->
                useGetters
                        ? CodeBlock.of("$L.$L()", variableName, methodNames.safeMethodName(property))
                        : CodeBlock.of("$L.$L", variableName, property.name());
        };
    }
}
