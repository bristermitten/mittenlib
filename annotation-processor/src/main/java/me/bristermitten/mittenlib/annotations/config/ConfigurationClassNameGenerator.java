package me.bristermitten.mittenlib.annotations.config;

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
import me.bristermitten.mittenlib.annotations.util.TypesUtil;
import me.bristermitten.mittenlib.config.Config;
import me.bristermitten.mittenlib.config.GeneratedConfig;
import me.bristermitten.mittenlib.util.Null;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.inject.Inject;
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
import java.util.regex.Pattern;

/**
 * Responsible for generating proper class names for config classes
 */
public class ConfigurationClassNameGenerator {
    private static final Pattern SUFFIX_PATTERN = Pattern.compile("(.+)(DTO|Config)");

    private final ProcessingEnvironment environment;

    private final ConfigNameCache configNameCache;
    private final TypesUtil typesUtil;
    private final GeneratedTypeCache generatedTypeCache;
    private final Elements elements;

    @Inject
    ConfigurationClassNameGenerator(ProcessingEnvironment environment, ConfigNameCache configNameCache, TypesUtil typesUtil, GeneratedTypeCache generatedTypeCache, Elements elements) {
        this.environment = environment;
        this.configNameCache = configNameCache;
        this.typesUtil = typesUtil;
        this.generatedTypeCache = generatedTypeCache;
        this.elements = elements;
    }


    public static ClassName createConfigImplClassName(ClassName dtoClassName) {
        var implName = dtoClassName.simpleName().endsWith("DTO") ?
                dtoClassName.simpleName().substring(0, dtoClassName.simpleName().length() - 3) :
                dtoClassName.simpleName() + "Impl";
        return dtoClassName.peerClass(implName);
    }

    private static ClassName createConfigImplClassName(ASTParentReference parentReference) {
        ClassName implName = createConfigImplClassName(parentReference.parentClassName());
        if (parentReference.parent() != null) {
            var parentName = createConfigImplClassName(parentReference.parent());
            return parentName.nestedClass(implName.simpleName());
        }
        return implName;
    }

    public static ClassName createConfigImplClassName(AbstractConfigStructure ast) {
        ClassName implName = createConfigImplClassName(ast.name());
        if (ast.enclosedIn() != null) {
            return createConfigImplClassName(ast.enclosedIn())
                    .nestedClass(implName.simpleName());
        }
        return implName;
    }

    /**
     * Get the <i>public </i>class name for a config structure.
     * "public" is defined as the type that the user should primarily interact with.
     * For interfaces, this is the interface, and for classes, this is the generated class (as the dto class becomes mostly useless)
     *
     * @param ast
     * @return
     */
    public static ClassName getPublicClassName(AbstractConfigStructure ast) {
        return switch (ast.source()) {
            case ConfigTypeSource.InterfaceConfigTypeSource iface -> ast.name();
            case ConfigTypeSource.ClassConfigTypeSource clazz -> createConfigImplClassName(ast);
        };
    }

    public static ClassName getConcreteConfigClassName(AbstractConfigStructure ast) {
        return switch (ast.source()) {
            case ConfigTypeSource.ClassConfigTypeSource clazz -> ast.name();
            case ConfigTypeSource.InterfaceConfigTypeSource iface -> createConfigImplClassName(ast);
        };
    }

    public static Optional<ClassName> findConfigClassName(@NotNull TypeMirror mirror) {
        TypeMirrorWrapper wrap = TypeMirrorWrapper.wrap(mirror);
        if (!wrap.isDeclared()) {
            return Optional.empty();
        }
        DeclaredType declaredType = wrap.getDeclaredType();

        ClassName nameFor = createConfigImplClassName(ClassName.bestGuess(wrap.getQualifiedName()));
        return Optional.of(findConfigClassName(declaredType.getEnclosingType())
                .map(enclosing ->
                        enclosing.peerClass(nameFor.simpleName())
                )
                .orElse(nameFor));
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
            return (annotation.className());
        }

        return createConfigImplClassName(ClassName.get(dtoType)).simpleName();
    }

    /**
     * Recursively turns any DTO types into their non-DTO counterparts,
     * i.e. {@code List<BlahDTO> -> List<Blah>}
     *
     * @param mirror The type to convert
     * @return The converted type
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


    public TypeName getConfigPropertyClassName(Property p) {
        return getConfigPropertyClassName(p.propertyType());
    }

    public TypeName getConfigPropertyClassName(TypeMirror mirror) {
        return configNameCache.lookupAST(mirror)
                .map(ConfigurationClassNameGenerator::createConfigImplClassName)
                .map(TypeName.class::cast)
                .orElse(translateDTOParameters(mirror, this::getConfigPropertyClassName));
    }

    public TypeName publicPropertyClassName(Property p) {
        var propertyType = p.propertyType();
        return publicPropertyClassName(propertyType);
    }

    public TypeName publicPropertyClassName(TypeMirror mirror) {
        return configNameCache.lookupAST(mirror)
                .map(ConfigurationClassNameGenerator::getPublicClassName)
                .map(TypeName.class::cast)
                .orElse(translateDTOParameters(mirror, this::publicPropertyClassName));
    }

    public TypeName concretePropertyClassName(TypeMirror mirror) {
        return configNameCache.lookupAST(mirror)
                .map(ConfigurationClassNameGenerator::getConcreteConfigClassName)
                .map(TypeName.class::cast)
                .orElse(translateDTOParameters((mirror), this::concretePropertyClassName));
    }

    public TypeName concretePropertyClassName(Property p) {
        var propertyType = p.propertyType();
        return concretePropertyClassName(propertyType);
    }

    public TypeName getConfigClassName(TypeMirror mirror) {
        return getConfigClassName(mirror, null);
    }

    /**
     * Get a suitable configuration class name for the given type mirror,
     * performing edge case checks for primitives, unnamed packages, and generated config references
     *
     * @param typeMirror
     * @param source     The source element that references the typeMirror. Used exclusively for error messages
     * @return
     */
    public @NotNull TypeName getConfigClassName(TypeMirror typeMirror, @Nullable Element source) {
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
}
