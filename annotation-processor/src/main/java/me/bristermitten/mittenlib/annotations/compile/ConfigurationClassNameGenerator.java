package me.bristermitten.mittenlib.annotations.compile;

import com.google.inject.Inject;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import io.toolisticon.aptk.tools.wrapper.TypeElementWrapper;
import java.util.List;
import java.util.function.Function;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import me.bristermitten.mittenlib.annotations.ast.ASTParentReference;
import me.bristermitten.mittenlib.annotations.ast.AbstractConfigStructure;
import me.bristermitten.mittenlib.annotations.ast.ConfigTypeSource;
import me.bristermitten.mittenlib.annotations.ast.Property;
import me.bristermitten.mittenlib.config.Config;
import me.bristermitten.mittenlib.util.Strings;
import org.jspecify.annotations.Nullable;

/**
 * Responsible for generating proper class names for configuration classes. This class handles the
 * conversion between DTO class names and their corresponding implementation class names, taking
 * into account nesting, package names, and custom naming specified in annotations.
 */
public class ConfigurationClassNameGenerator {

    public static final String DESERIALIZER_SUFFIX = "Deserializer";
    public static final String SERIALIZER_SUFFIX = "Serializer";
    public static final String VALIDATOR_SUFFIX = "Validator";
    public static final String PROVIDER_SUFFIX = "Provider";
    public static final String DEFAULT_METHOD_ACCESS_SUFFIX = "DefaultMethodAccess";
    public static final String CONFIG_LOADER_MODULE_NAME = "ConfigLoaderModule";
    private final ConfigNameCache configNameCache;

    @Inject
    public ConfigurationClassNameGenerator(ConfigNameCache configNameCache) {
        this.configNameCache = configNameCache;
    }

    /**
     * Creates a class name for the implementation of a DTO class. If the class name ends with "DTO",
     * it removes that suffix. Otherwise, it appends "Impl" to the class name.
     *
     * @param dtoClassName The original DTO class name
     * @return The implementation class name
     */
    public static ClassName translateConfigClassName(ClassName dtoClassName) {
        var implName = dtoClassName.simpleName().endsWith("DTO")
                ? dtoClassName
                        .simpleName()
                        .substring(0, dtoClassName.simpleName().length() - 3)
                : dtoClassName.simpleName() + "Impl";
        return dtoClassName.peerClass(implName);
    }

    private static String findConfigClassName(TypeElement dtoType) {

        final Config annotation = dtoType.getAnnotation(Config.class);
        if (annotation == null) {
            return ClassName.get(dtoType).simpleName();
        }
        if (!annotation.className().isEmpty()) {
            return annotation.className();
        }

        return translateConfigClassName(ClassName.get(dtoType)).simpleName();
    }

    private static NamingNode node(AbstractConfigStructure ast) {
        return new NamingNode(
                ast.name(),
                ast.settings().config().className(),
                ast.enclosedIn() == null ? null : node(ast.enclosedIn()),
                ast.source() instanceof ConfigTypeSource.InterfaceConfigTypeSource);
    }

    private static NamingNode node(ASTParentReference parent) {
        return new NamingNode(
                parent.parentClassName(),
                parent.manualClassName(),
                parent.parent() == null ? null : node(parent.parent()),
                parent.isInterface());
    }

    private static String getCleanSimpleName(ClassName name) {
        String simpleName = name.simpleName();
        if (simpleName.endsWith("DTO")) {
            return simpleName.substring(0, simpleName.length() - 3);
        }
        return simpleName;
    }

    private ClassName getRecursiveName(NamingNode node, Function<NamingNode, String> simpleNameSelector) {
        if (node.parent() != null) {
            return getRecursiveName(node.parent(), simpleNameSelector).nestedClass(simpleNameSelector.apply(node));
        }
        return node.name().peerClass(simpleNameSelector.apply(node));
    }

    private String getCleanSimpleName(NamingNode node) {
        return getCleanSimpleName(node.name());
    }

    private ClassName getImplClassName(NamingNode node) {
        ClassName baseName =
                node.manualClassName() == null || node.manualClassName().isBlank()
                        ? translateConfigClassName(node.name())
                        : ClassName.bestGuess(node.manualClassName());

        if (node.parent() != null) {
            return getImplClassName(node.parent()).nestedClass(baseName.simpleName());
        }
        return baseName;
    }

    /**
     * Creates a class name for the implementation of a configuration structure. This method handles
     * nested classes by checking if the structure is enclosed in another structure.
     *
     * @param ast The abstract configuration structure
     * @return The implementation class name, properly nested if necessary
     */
    public ClassName translateConfigClassName(AbstractConfigStructure ast) {
        return getImplClassName(node(ast));
    }

    /**
     * Get the <i>public</i> class name for a config structure. "Public" is defined as the type that
     * the user should primarily interact with. For interfaces, this is the interface itself, and for
     * classes, this is the generated implementation class (as the DTO class becomes mostly useless
     * after code generation).
     *
     * @param ast The abstract configuration structure
     * @return The public class name that should be used for interaction with this configuration
     */
    public ClassName getPublicClassName(AbstractConfigStructure ast) {
        return switch (ast.source()) {
            case ConfigTypeSource.InterfaceConfigTypeSource ignored -> ast.name();
            case ConfigTypeSource.ClassConfigTypeSource ignored -> translateConfigClassName(ast);
        };
    }

    /**
     * Get the concrete class name for a config structure. This is the actual implementation class
     * that will be instantiated. For classes, this is the original DTO class, and for interfaces,
     * this is the generated implementation class.
     *
     * @param ast The abstract configuration structure
     * @return The concrete class name that will be instantiated
     */
    public ClassName getConcreteConfigClassName(AbstractConfigStructure ast) {
        return switch (ast.source()) {
            case ConfigTypeSource.ClassConfigTypeSource ignored -> ast.name();
            case ConfigTypeSource.InterfaceConfigTypeSource ignored -> translateConfigClassName(ast);
        };
    }

    /**
     * Recursively transforms DTO type parameters into their corresponding configuration class names.
     * For example, converts {@code List<UserDTO>} to {@code List<User>}.
     *
     * @param mirror The type mirror to transform
     * @param getConfigClassName A function that maps a type mirror to its configuration class name
     * @return The transformed type name with DTO parameters replaced by their configuration
     *     counterparts
     */
    private TypeName translateDTOParameters(TypeMirror mirror, Function<TypeMirror, TypeName> getConfigClassName) {
        if (!(mirror instanceof DeclaredType declaredType)) {
            return TypeName.get(mirror);
        }
        TypeElement element = (TypeElement) declaredType.asElement();
        List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
        if (typeArguments.isEmpty()) {
            return TypeName.get(mirror);
        }
        List<TypeName> properArguments =
                typeArguments.stream().map(getConfigClassName).toList();

        return ParameterizedTypeName.get(ClassName.get(element), properArguments.toArray(new TypeName[0]));
    }

    /**
     * Get the config property class name for a type mirror.
     *
     * @param mirror The type mirror
     * @return The config property class name
     */
    public TypeName getConfigPropertyClassName(TypeMirror mirror) {
        return getPropertyClassName(mirror, this::translateConfigClassName, this::getConfigPropertyClassName);
    }

    /**
     * Get the public property class name for a property.
     *
     * @param p The property
     * @return The public property class name
     */
    public TypeName publicPropertyClassName(Property p) {
        return publicPropertyClassName(p.propertyType());
    }

    /**
     * Get the public property class name for a type mirror.
     *
     * @param mirror The type mirror
     * @return The public property class name
     */
    public TypeName publicPropertyClassName(TypeMirror mirror) {
        return getPropertyClassName(mirror, this::getPublicClassName, this::publicPropertyClassName);
    }

    /**
     * Helper method to get a property class name based on a type mirror and a mapping function.
     *
     * @param mirror The type mirror
     * @param astMapper The function to map an AbstractConfigStructure to a ClassName
     * @param recursiveMapper The function to map a TypeMirror to a TypeName (used for recursive
     *     calls)
     * @return The property class name
     */
    private TypeName getPropertyClassName(
            TypeMirror mirror,
            Function<AbstractConfigStructure, ClassName> astMapper,
            Function<TypeMirror, TypeName> recursiveMapper) {
        return configNameCache
                .lookupAST(mirror)
                .map(astMapper)
                .map(TypeName.class::cast)
                .orElse(translateDTOParameters(mirror, recursiveMapper));
    }

    /**
     * Generates a ClassName for the actual generated configuration class from a given DTO, using the
     * package of the given {@link TypeElement}. If the given type is not a config, its unchanged name
     * is returned. The returned class name will be the same as the given type, but with the suffix
     * removed. It can also be manually specified in the Config annotation with {@link
     * Config#className()}
     *
     * @param configDTOType The DTO type
     * @return The generated ClassName
     */
    public ClassName generateConfigurationClassName(TypeElement configDTOType) {
        if (configDTOType.getNestingKind() == NestingKind.MEMBER) {
            /*
            If the type is a nested class, then we first translate the enclosedConfigs class name (which may do nothing),
            then create a nested class name.
             */

            final var enclosingElement = configDTOType.getEnclosingElement();
            return generateConfigurationClassName((TypeElement) enclosingElement)
                    .nestedClass(findConfigClassName(configDTOType));
        }

        final String packageName = TypeElementWrapper.wrap(configDTOType).getPackageName();
        return ClassName.get(packageName, findConfigClassName(configDTOType));
    }

    public String getDeserializerProviderFieldName(TypeMirror type) {
        return getDeserializerFieldName(type) + PROVIDER_SUFFIX;
    }

    public String getSerializerProviderFieldName(TypeMirror type) {
        return getSerializerFieldName(type) + PROVIDER_SUFFIX;
    }

    public String getValidatorFieldName(Property property) {
        return property.name() + VALIDATOR_SUFFIX;
    }

    public String getValidatorElementFieldName(Property property) {
        return property.name() + "Element" + VALIDATOR_SUFFIX;
    }

    public String getValidatorKeyFieldName(Property property) {
        return property.name() + "Key" + VALIDATOR_SUFFIX;
    }

    public String getValidatorErrorFieldName(Property property) {
        return property.name() + "ValidationError";
    }

    public String getValidatorElementErrorFieldName(Property property) {
        return property.name() + "ElementValidationError";
    }

    public String getValidatorKeyErrorFieldName(Property property) {
        return property.name() + "KeyValidationError";
    }

    public ClassName getDefaultMethodAccessClassName(AbstractConfigStructure ast) {
        ClassName concreteConfigClassName = getConcreteConfigClassName(ast);
        return concreteConfigClassName.nestedClass(getCleanSimpleName(ast.name()) + DEFAULT_METHOD_ACCESS_SUFFIX);
    }

    public ClassName getLoaderModuleClassName(String packageName) {
        return ClassName.get(packageName, CONFIG_LOADER_MODULE_NAME);
    }

    public String getProvidesProviderMethodName(String simpleName) {
        return "provide" + simpleName + PROVIDER_SUFFIX;
    }

    public String getProvidesMethodName(String simpleName) {
        return "provide" + simpleName;
    }

    public String getProvidesToConfigSetMethodName(String simpleName) {
        return "provide" + simpleName + "ToConfigSet";
    }

    public String getProvidesToProviderSetMethodName(String simpleName) {
        return "provide" + simpleName + "ToProviderSet";
    }

    /**
     * Gets the ClassName of the deserializer for a given configuration structure. For {@code
     * MyConfig}, the deserializer is {@code MyConfigImpl}. For nested class OuterConfig.InnerConfig,
     * the loader is OuterConfigDeserializer.InnerConfigDeserializer.
     */
    public ClassName getDeserializerClassName(AbstractConfigStructure ast) {
        return getRecursiveName(node(ast), n -> getCleanSimpleName(n) + DESERIALIZER_SUFFIX);
    }

    /** Gets the ClassName of the loader for a given TypeMirror. */
    public ClassName getDeserializerClassName(TypeMirror type) {
        AbstractConfigStructure ast = configNameCache
                .lookupAST(type)
                .orElseThrow(() -> new IllegalStateException("Not a config type: " + type));
        return getDeserializerClassName(ast);
    }

    private String getFieldName(TypeMirror type, String suffix) {
        AbstractConfigStructure ast = configNameCache
                .lookupAST(type)
                .orElseThrow(() -> new IllegalStateException("Not a config type: " + type));
        ClassName publicName = getPublicClassName(ast);
        String safePkg = publicName.packageName().replace('.', '_');
        String prefix = safePkg.isEmpty() ? "" : safePkg + "_";
        return Strings.uncapitalize(prefix + getCleanSimpleName(publicName)) + suffix;
    }

    /**
     * Gets the field name for a deserializer field based on the public config type. E.g., for {@code
     * InterfaceConfig}, it returns {@code interfaceConfigDeserializer}.
     */
    public String getDeserializerFieldName(TypeMirror type) {
        return getFieldName(type, DESERIALIZER_SUFFIX);
    }

    /**
     * Gets the {@link ClassName} of the serializer for a given configuration structure. For {@code
     * MyConfigImpl}, the saver is {@code MyConfigImplSerializer}. For nested class {@code
     * OuterConfigImpl.InnerConfigImpl}, the saver is {@code
     * OuterConfigImplSerializer.InnerConfigImplSerializer}.
     */
    public ClassName getSerializerClassName(AbstractConfigStructure ast) {
        return getRecursiveName(node(ast), n -> getCleanSimpleName(n) + SERIALIZER_SUFFIX);
    }

    /**
     * Gets the field name for a serializer instance based on the public config type. E.g., for {@code
     * InterfaceConfig}, it returns {@code "interfaceConfigSerializer"}.
     */
    public String getSerializerFieldName(TypeMirror type) {
        return getFieldName(type, SERIALIZER_SUFFIX);
    }

    public ClassName getValidatorClassName(AbstractConfigStructure ast) {
        return getRecursiveName(node(ast), n -> getCleanSimpleName(n) + VALIDATOR_SUFFIX);
    }

    public @Nullable ClassName getInnerDaoName(AbstractConfigStructure ast) {
        if (!(ast.source() instanceof ConfigTypeSource.InterfaceConfigTypeSource)) {
            return null;
        }
        boolean hasAnyDefaultValue = ast.properties().stream()
                .anyMatch(property -> property.settings().hasDefaultValue());
        if (!hasAnyDefaultValue) {
            return null;
        }
        return getDefaultMethodAccessClassName(ast);
    }

    private record NamingNode(
            ClassName name,
            @Nullable String manualClassName,
            @Nullable NamingNode parent,
            boolean isInterface) {}
}
