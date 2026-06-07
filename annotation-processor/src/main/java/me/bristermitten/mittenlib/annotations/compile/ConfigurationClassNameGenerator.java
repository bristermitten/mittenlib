package me.bristermitten.mittenlib.annotations.compile;

import com.google.inject.Inject;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import io.toolisticon.aptk.tools.MessagerUtils;
import io.toolisticon.aptk.tools.TypeMirrorWrapper;
import io.toolisticon.aptk.tools.wrapper.TypeElementWrapper;
import me.bristermitten.mittenlib.annotations.ast.ASTParentReference;
import me.bristermitten.mittenlib.annotations.ast.AbstractConfigStructure;
import me.bristermitten.mittenlib.annotations.ast.ConfigTypeSource;
import me.bristermitten.mittenlib.annotations.ast.Property;
import me.bristermitten.mittenlib.annotations.exception.DTOReferenceException;
import me.bristermitten.mittenlib.config.Config;
import me.bristermitten.mittenlib.config.GeneratedConfig;
import me.bristermitten.mittenlib.util.Null;
import me.bristermitten.mittenlib.util.Strings;
import org.jspecify.annotations.Nullable;

import javax.lang.model.element.Element;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Responsible for generating proper class names for configuration classes.
 * This class handles the conversion between DTO class names and their corresponding
 * implementation class names, taking into account nesting, package names, and
 * custom naming specified in annotations.
 */
public class ConfigurationClassNameGenerator {

    private final ConfigNameCache configNameCache;
    private final GeneratedTypeCache generatedTypeCache;
    private final Elements elements;

    @Inject
    ConfigurationClassNameGenerator(ConfigNameCache configNameCache, GeneratedTypeCache generatedTypeCache, Elements elements) {
        this.configNameCache = configNameCache;
        this.generatedTypeCache = generatedTypeCache;
        this.elements = elements;
    }


    /**
     * Creates a class name for the implementation of a DTO class.
     * If the class name ends with "DTO", it removes that suffix.
     * Otherwise, it appends "Impl" to the class name.
     *
     * @param dtoClassName The original DTO class name
     * @return The implementation class name
     */
    public static ClassName translateConfigClassName(ClassName dtoClassName) {
        var implName = dtoClassName.simpleName().endsWith("DTO") ?
                dtoClassName.simpleName().substring(0, dtoClassName.simpleName().length() - 3) :
                dtoClassName.simpleName() + "Impl";
        return dtoClassName.peerClass(implName);
    }

    /**
     * Translates a {@link TypeElement} into its non-DTO name by
     * reading a {@link Config#className()} or removing the suffix.
     *
     * @param dtoType the DTO type
     * @return the non-DTO name, if possible, or the unchanged name
     */
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

    /**
     * Creates a class name for the implementation of a configuration structure.
     * This method handles nested classes by checking if the structure is enclosed in another structure.
     *
     * @param ast The abstract configuration structure
     * @return The implementation class name, properly nested if necessary
     */
    public ClassName translateConfigClassName(AbstractConfigStructure ast) {

        ClassName implName = ast.settings().config().className().isBlank() ?
                translateConfigClassName(ast.name())
                : ClassName.bestGuess(ast.settings().config().className());
        if (ast.enclosedIn() != null) {
            return translateConfigClassName(ast.enclosedIn())
                    .nestedClass(implName.simpleName());
        }
        return implName;
    }

    /**
     * Get the <i>public</i> class name for a config structure.
     * "Public" is defined as the type that the user should primarily interact with.
     * For interfaces, this is the interface itself, and for classes, this is the generated implementation class
     * (as the DTO class becomes mostly useless after code generation).
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
     * Get the concrete class name for a config structure.
     * This is the actual implementation class that will be instantiated.
     * For classes, this is the original DTO class, and for interfaces, this is the generated implementation class.
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
     * Creates a class name for the implementation of a DTO class, taking into account parent references.
     * This method handles nested classes by recursively processing parent references.
     *
     * @param parentReference The parent reference containing information about the class hierarchy
     * @return The implementation class name, properly nested if necessary
     */
    private ClassName translateConfigClassName(ASTParentReference parentReference) {
        Optional<AbstractConfigStructure> abstractConfigStructure = configNameCache.lookupAST(parentReference.parentClassName());
        ClassName implName =
                abstractConfigStructure.map(this::translateConfigClassName)
                        .orElseGet(() -> translateConfigClassName(parentReference.parentClassName()));

        if (parentReference.parent() != null) {
            var parentName = translateConfigClassName(parentReference.parent());
            return parentName.nestedClass(implName.simpleName());
        }
        return implName;
    }

    /**
     * Recursively transforms DTO type parameters into their corresponding configuration class names.
     * For example, converts {@code List<UserDTO>} to {@code List<User>}.
     *
     * @param mirror             The type mirror to transform
     * @param getConfigClassName A function that maps a type mirror to its configuration class name
     * @return The transformed type name with DTO parameters replaced by their configuration counterparts
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
        List<TypeName> properArguments = typeArguments.stream()
                .map(getConfigClassName)
                .toList();

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
     * @param mirror          The type mirror
     * @param astMapper       The function to map an AbstractConfigStructure to a ClassName
     * @param recursiveMapper The function to map a TypeMirror to a TypeName (used for recursive calls)
     * @return The property class name
     */
    private TypeName getPropertyClassName(TypeMirror mirror,
                                          Function<AbstractConfigStructure, ClassName> astMapper,
                                          Function<TypeMirror, TypeName> recursiveMapper) {
        return configNameCache.lookupAST(mirror)
                .map(astMapper)
                .map(TypeName.class::cast)
                .orElse(translateDTOParameters(mirror, recursiveMapper));
    }


    /**
     * Get a suitable configuration class name for the given type mirror,
     * performing edge case checks for primitives, unnamed packages, and generated config references.
     * This method throws exceptions for error types, already generated configs, and types in unnamed packages.
     *
     * @param typeMirror The type mirror to get the configuration class name for
     * @param source     The source element that references the typeMirror (used exclusively for error messages)
     * @return The configuration class name for the given type mirror
     * @throws RuntimeException         if the type is an error type or already generated
     * @throws IllegalArgumentException if the type is not a declared type or is in an unnamed package
     */
    public TypeName getConfigClassName(TypeMirror typeMirror, @Nullable Element source) {
        if (typeMirror.getKind().isPrimitive()) {
            return TypeName.get(typeMirror);
        }
        GeneratedConfig annotation = typeMirror.getAnnotation(GeneratedConfig.class); // if the type is already generated by us
        if (typeMirror.getKind() == TypeKind.ERROR || annotation != null) {
            MessagerUtils.error(source,
                    new DTOReferenceException(typeMirror, generatedTypeCache,
                            Null.map(annotation, GeneratedConfig::source), source).getMessage());
            throw new RuntimeException();
        }

        final TypeElement element = TypeMirrorWrapper.wrap(typeMirror).getTypeElement()
                .map(TypeElementWrapper::unwrap)
                .orElseThrow(() -> new IllegalArgumentException(typeMirror + " must be a declared type"));

        final PackageElement packageElement = elements.getPackageOf(element);
        if (packageElement.isUnnamed()) {
            throw new IllegalArgumentException("Unnamed packages are not supported");
        }
        return generateConfigurationClassName(element);
    }

    /**
     * Generates a ClassName for the actual generated configuration class from a given DTO, using the package
     * of the given {@link TypeElement}. If the given type is not a config, its unchanged name is returned.
     * The returned class name will be the same as the given type, but with the suffix removed.
     * It can also be manually specified in the Config annotation with {@link Config#className()}
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

    public static final String DESERIALIZER_SUFFIX = "Deserializer";
    public static final String SERIALIZER_SUFFIX = "Serializer";
    public static final String VALIDATOR_SUFFIX = "Validator";
    public static final String PROVIDER_SUFFIX = "Provider";
    public static final String DEFAULT_METHOD_ACCESS_SUFFIX = "DefaultMethodAccess";
    public static final String CONFIG_LOADER_MODULE_NAME = "ConfigLoaderModule";

    public String getLoaderProviderFieldName(TypeMirror type) {
        return getLoaderFieldName(type) + PROVIDER_SUFFIX;
    }

    public String getSaverProviderFieldName(TypeMirror type) {
        return getSaverFieldName(type) + PROVIDER_SUFFIX;
    }

    public String getValidatorFieldName(Property property) {
        return property.name() + VALIDATOR_SUFFIX;
    }

    public String getValidatorErrorFieldName(Property property) {
        return property.name() + "ValidationError";
    }

    public ClassName getDefaultMethodAccessClassName(AbstractConfigStructure ast) {
        ClassName concreteConfigClassName = getConcreteConfigClassName(ast);
        return concreteConfigClassName.nestedClass(ast.name().simpleName() + DEFAULT_METHOD_ACCESS_SUFFIX);
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
     * Gets the ClassName of the loader for a given configuration structure.
     * For MyConfigImpl, the loader is MyConfigImplLoader.
     * For nested class OuterConfigImpl.InnerConfigImpl, the loader is OuterConfigImplLoader.InnerConfigImplLoader.
     */
    public ClassName getLoaderClassName(AbstractConfigStructure ast) {
        ClassName implName = translateConfigClassName(ast);
        if (ast.enclosedIn() != null) {
            return getLoaderClassName(ast.enclosedIn()).nestedClass(implName.simpleName() + DESERIALIZER_SUFFIX);
        }
        return implName.peerClass(implName.simpleName() + DESERIALIZER_SUFFIX);
    }

    private ClassName getLoaderClassName(ASTParentReference parent) {
        ClassName implName = translateConfigClassName(parent);
        if (parent.parent() != null) {
            return getLoaderClassName(parent.parent()).nestedClass(implName.simpleName() + DESERIALIZER_SUFFIX);
        }
        return implName.peerClass(implName.simpleName() + DESERIALIZER_SUFFIX);
    }

    /**
     * Gets the ClassName of the loader for a given TypeMirror.
     */
    public ClassName getLoaderClassName(TypeMirror type) {
        AbstractConfigStructure ast = configNameCache.lookupAST(type)
                .orElseThrow(() -> new IllegalStateException("Not a config type: " + type));
        return getLoaderClassName(ast);
    }

    /**
     * Gets the field name for a loader instance based on the public config type.
     * E.g., for InterfaceConfig, it returns "interfaceConfigLoader".
     */
    public String getLoaderFieldName(TypeMirror type) {
        AbstractConfigStructure ast = configNameCache.lookupAST(type)
                .orElseThrow(() -> new IllegalStateException("Not a config type: " + type));
        ClassName publicName = getPublicClassName(ast);
        String safePkg = publicName.packageName().replace('.', '_');
        String prefix = safePkg.isEmpty() ? "" : safePkg + "_";
        return Strings.uncapitalize(prefix + publicName.simpleName()) + DESERIALIZER_SUFFIX;
    }

    /**
     * Gets the ClassName of the saver for a given configuration structure.
     * For MyConfigImpl, the saver is MyConfigImplSaver.
     * For nested class OuterConfigImpl.InnerConfigImpl, the saver is OuterConfigImplSaver.InnerConfigImplSaver.
     */
    public ClassName getSaverClassName(AbstractConfigStructure ast) {
        ClassName implName = translateConfigClassName(ast);
        if (ast.enclosedIn() != null) {
            return getSaverClassName(ast.enclosedIn()).nestedClass(implName.simpleName() + SERIALIZER_SUFFIX);
        }
        return implName.peerClass(implName.simpleName() + SERIALIZER_SUFFIX);
    }

    private ClassName getSaverClassName(ASTParentReference parent) {
        ClassName implName = translateConfigClassName(parent);
        if (parent.parent() != null) {
            return getSaverClassName(parent.parent()).nestedClass(implName.simpleName() + SERIALIZER_SUFFIX);
        }
        return implName.peerClass(implName.simpleName() + SERIALIZER_SUFFIX);
    }

    /**
     * Gets the field name for a saver instance based on the public config type.
     * E.g., for InterfaceConfig, it returns "interfaceConfigSaver".
     */
    public String getSaverFieldName(TypeMirror type) {
        AbstractConfigStructure ast = configNameCache.lookupAST(type)
                .orElseThrow(() -> new IllegalStateException("Not a config type: " + type));
        ClassName publicName = getPublicClassName(ast);
        String safePkg = publicName.packageName().replace('.', '_');
        String prefix = safePkg.isEmpty() ? "" : safePkg + "_";
        return Strings.uncapitalize(prefix + publicName.simpleName()) + SERIALIZER_SUFFIX;
    }

    public ClassName getValidatorClassName(AbstractConfigStructure ast) {
        ClassName implName = translateConfigClassName(ast);
        if (ast.enclosedIn() != null) {
            return getValidatorClassName(ast.enclosedIn()).nestedClass(implName.simpleName() + VALIDATOR_SUFFIX);
        }
        return implName.peerClass(implName.simpleName() + VALIDATOR_SUFFIX);
    }

    private ClassName getValidatorClassName(ASTParentReference parent) {
        ClassName implName = translateConfigClassName(parent);
        if (parent.parent() != null) {
            return getValidatorClassName(parent.parent()).nestedClass(implName.simpleName() + VALIDATOR_SUFFIX);
        }
        return implName.peerClass(implName.simpleName() + VALIDATOR_SUFFIX);
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
}
