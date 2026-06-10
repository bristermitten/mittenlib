package me.bristermitten.mittenlib.annotations.parser;

import com.google.inject.Inject;
import com.squareup.javapoet.ClassName;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.Trees;
import io.toolisticon.aptk.compilermessage.api.DeclareCompilerMessage;
import io.toolisticon.aptk.tools.MessagerUtils;
import io.toolisticon.aptk.tools.TypeMirrorWrapper;
import io.toolisticon.aptk.tools.corematcher.AptkCoreMatchers;
import io.toolisticon.aptk.tools.wrapper.ElementWrapper;
import io.toolisticon.aptk.tools.wrapper.TypeElementWrapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import me.bristermitten.mittenlib.annotations.ast.*;
import me.bristermitten.mittenlib.annotations.compile.ConfigNameCache;
import me.bristermitten.mittenlib.annotations.compile.ConfigurationClassNameGenerator;
import me.bristermitten.mittenlib.annotations.compile.GeneratedTypeCache;
import me.bristermitten.mittenlib.annotations.util.ElementsFinder;
import me.bristermitten.mittenlib.annotations.util.TypesUtil;
import me.bristermitten.mittenlib.config.*;
import me.bristermitten.mittenlib.config.generate.GenerateToString;
import me.bristermitten.mittenlib.config.names.ConfigName;
import me.bristermitten.mittenlib.config.names.NamingPattern;
import me.bristermitten.mittenlib.util.Null;
import org.jspecify.annotations.Nullable;

public class ConfigClassParser {

    private final TypesUtil typesUtil;
    private final ElementsFinder elementsFinder;
    private final ConfigNameCache configNameCache;
    private final GeneratedTypeCache generatedTypeCache;
    private final ConfigurationClassNameGenerator classNameGenerator;
    private final @Nullable Trees trees;

    @Inject
    public ConfigClassParser(
            TypesUtil typesUtil,
            ElementsFinder elementsFinder,
            ConfigNameCache configNameCache,
            GeneratedTypeCache generatedTypeCache,
            ConfigurationClassNameGenerator classNameGenerator,
            ProcessingEnvironment processingEnv) {
        this.typesUtil = typesUtil;
        this.elementsFinder = elementsFinder;
        this.configNameCache = configNameCache;
        this.generatedTypeCache = generatedTypeCache;
        this.classNameGenerator = classNameGenerator;
        Trees t;
        try {
            t = Trees.instance(processingEnv);
        } catch (IllegalArgumentException e) {
            t = null;
        }
        this.trees = t;
    }

    @SuppressWarnings("TypeParameterUnusedInFormals") // ok as it always returns bottom
    private static <T> T throwInvalidConfigError() {
        throw new IllegalArgumentException("Invalid config, impossible?");
    }

    private ConfigTypeSource getSource(TypeElement element) {
        var wrapped = TypeElementWrapper.wrap(element);

        List<TypeMirror> parents = Stream.concat(Stream.of(element.getSuperclass()), element.getInterfaces().stream())
                .filter(c -> c.getKind() != TypeKind.NONE)
                .filter(c -> !ClassName.get(c).equals(ClassName.OBJECT))
                .toList();

        if (wrapped.isClass()) {
            if (parents.size() > 1) {
                return throwInvalidConfigError();
            }
            Optional<TypeMirror> parent = parents.isEmpty() ? Optional.empty() : Optional.of(parents.getFirst());

            return new ConfigTypeSource.ClassConfigTypeSource(element, parent);
        } else if (wrapped.isInterface()) {
            return new ConfigTypeSource.InterfaceConfigTypeSource(element, parents);
        } else {
            return throwInvalidConfigError();
        }
    }

    private List<Property> getPropertiesIn(TypeElement element, @Nullable NamingPattern namingPattern) {
        var wrapped = TypeElementWrapper.wrap(element);
        List<? extends Element> elements;
        if (wrapped.isClass()) {
            elements = elementsFinder.getApplicableVariableElements(element);
        } else if (wrapped.isInterface()) {
            elements = elementsFinder.getPropertyMethods(element);
        } else {
            return throwInvalidConfigError();
        }
        return elements.stream()
                .map(propertyElement -> {
                    var propertySource =
                            switch (propertyElement) {
                                case ExecutableElement e -> new Property.PropertySource.MethodSource(e);
                                case VariableElement e -> new Property.PropertySource.FieldSource(e);
                                default ->
                                    throw new IllegalStateException("Unexpected value: " + propertyElement.getKind());
                            };
                    var propertyType = propertyElement instanceof ExecutableElement executableElement
                            ? executableElement.getReturnType()
                            : propertyElement.asType();
                    var configName = typesUtil.getAnnotation(propertyElement, ConfigName.class);
                    var namingPatternSub =
                            Null.orElse(typesUtil.getAnnotation(propertyElement, NamingPattern.class), namingPattern);

                    var isNullable = typesUtil.isNullable(propertyElement);

                    var enumParsingScheme = typesUtil.getAnnotation(propertyElement, EnumParsingScheme.class);
                    if (enumParsingScheme != null
                            && propertyElement.getAnnotation(EnumParsingScheme.class)
                                    != null // if the annotation is precisely present on the property
                            && !TypeMirrorWrapper.wrap(propertyType).isEnum()) {
                        MessagerUtils.warning(propertyElement, ConfigVerificationErrors.ENUM_PARSING_SCHEME_NOT_ENUM);
                    }

                    var hasDefault =
                            switch (propertySource) {
                                case Property.PropertySource.MethodSource(var m) -> m.isDefault();
                                case Property.PropertySource.FieldSource(var f) -> {
                                    if (trees != null) {
                                        var path = trees.getPath(f);
                                        if (path != null) {
                                            var tree = (VariableTree) path.getLeaf();
                                            yield tree.getInitializer() != null;
                                        }
                                    }
                                    yield true; // can't tell so assume true
                                }
                            };

                    List<ValidationConstraint> constraints = parseConstraints(propertyElement);
                    List<ValidationConstraint> elementConstraints = List.of();
                    List<ValidationConstraint> keyConstraints = List.of();

                    if (propertyType instanceof DeclaredType declaredType) {
                        List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
                        if (typesUtil.isCollection(propertyType)) {
                            if (!typeArguments.isEmpty()) {
                                elementConstraints = parseConstraints(typeArguments.get(0));
                            }
                        } else if (typesUtil.isMap(propertyType)) {
                            if (typeArguments.size() >= 2) {
                                keyConstraints = parseConstraints(typeArguments.get(0));
                                elementConstraints = parseConstraints(typeArguments.get(1));
                            }
                        }
                    }

                    return new Property(
                            propertyElement.getSimpleName().toString(),
                            propertyType,
                            propertySource,
                            new ASTSettings.PropertyASTSettings(
                                    namingPatternSub,
                                    configName,
                                    enumParsingScheme == null
                                            ? EnumParsingSchemes.EXACT_MATCH
                                            : enumParsingScheme.value(),
                                    isNullable,
                                    hasDefault,
                                    constraints,
                                    elementConstraints,
                                    keyConstraints));
                })
                .toList();
    }

    @DeclareCompilerMessage(
            enumValueName = "NO_CONFIG_ANNOTATION",
            message = "Element ${0} does not have a @Config annotation!")
    private ASTSettings.ConfigASTSettings getSettings(TypeElement element) {
        var namingPattern = typesUtil.getAnnotation(element, NamingPattern.class);
        GenerateToString generateToString = typesUtil.getAnnotation(element, GenerateToString.class);
        Source source = typesUtil.getAnnotation(element, Source.class);
        Config config = typesUtil.getAnnotation(element, Config.class);
        if (config == null) {
            MessagerUtils.error(element, ConfigClassParserCompilerMessages.NO_CONFIG_ANNOTATION, element);
            throw new IllegalStateException("Config " + element.getSimpleName() + " is missing @Config annotation");
        }

        return new ASTSettings.ConfigASTSettings(namingPattern, source, config, generateToString != null);
    }

    private AbstractConfigStructure parseAbstract(TypeElement element, @Nullable ASTParentReference parentReference) {
        TypeElementWrapper wrapper = TypeElementWrapper.wrap(element);

        Optional<TypeElementWrapper> enclosingType = wrapper.getEnclosingElement()
                .filter(ElementWrapper::isTypeElement)
                .map(ElementWrapper::toTypeElement);

        List<ClassName> parents = Stream.concat(Stream.of(element.getSuperclass()), element.getInterfaces().stream())
                .map(TypeMirrorWrapper::wrap)
                .<TypeElementWrapper>mapMulti((a, b) -> a.getTypeElement().ifPresent(b))
                .map(TypeElementWrapper::unwrap)
                .map(ClassName::get)
                .filter(c -> !c.equals(ClassName.OBJECT))
                .toList();

        ClassName enclosingName = enclosingType
                .map(TypeElementWrapper::unwrap)
                .map(ClassName::get)
                .orElse(null);

        boolean isEnclosingInterface =
                enclosingType.map(TypeElementWrapper::isInterface).orElse(false);
        String manualClassName = enclosingType
                .map(TypeElementWrapper::unwrap)
                .map(e -> e.getAnnotation(Config.class))
                .map(Config::className)
                .orElse(null);

        var thisParentReference = enclosingName == null
                ? null
                : new ASTParentReference(enclosingName, isEnclosingInterface, manualClassName, parentReference);

        var enclosedConfigs = wrapper
                .filterEnclosedElements()
                .applyFilter(AptkCoreMatchers.IS_TYPE_ELEMENT)
                .applyFilter(AptkCoreMatchers.BY_ELEMENT_KIND)
                .filterByOneOf(ElementKind.CLASS, ElementKind.INTERFACE)
                .getResult()
                .stream()
                .filter(e -> typesUtil.getAnnotation(e, Config.class) != null)
                .map((e) -> parseAbstract(e, thisParentReference))
                .toList();

        var namingPattern = typesUtil.getAnnotation(element, NamingPattern.class);
        List<Property> properties = getPropertiesIn(element, namingPattern);

        var source = getSource(element);
        if (wrapper.hasAnnotation(ConfigUnion.class)) {
            return putInCache(new AbstractConfigStructure.Union(
                    ClassName.get(element),
                    source,
                    getSettings(element),
                    thisParentReference,
                    parents,
                    enclosedConfigs,
                    properties));
        }
        if (parents.isEmpty()) {
            return putInCache(new AbstractConfigStructure.Atomic(
                    ClassName.get(element),
                    source,
                    getSettings(element),
                    enclosedConfigs,
                    thisParentReference,
                    properties));
        }

        return putInCache(new AbstractConfigStructure.Intersection(
                ClassName.get(element),
                source,
                getSettings(element),
                thisParentReference,
                enclosedConfigs,
                parents,
                properties));
    }

    private AbstractConfigStructure putInCache(AbstractConfigStructure configStructure) {
        configNameCache.put(configStructure);
        generatedTypeCache.put(
                configStructure.source().element(),
                classNameGenerator
                        .generateConfigurationClassName(configStructure.source().element())
                        .reflectionName());
        return configStructure;
    }

    public AbstractConfigStructure parseAbstract(TypeElement element) {
        var ast = parseAbstract(element, null);
        configNameCache.put(ast);
        return ast;
    }

    private List<ValidationConstraint> parseConstraints(AnnotatedConstruct element) {
        List<ValidationConstraint> constraints = new ArrayList<>();
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            String qName = ((TypeElement) mirror.getAnnotationType().asElement())
                    .getQualifiedName()
                    .toString();
            switch (qName) {
                case "me.bristermitten.mittenlib.config.validation.Positive":
                case "jakarta.validation.constraints.Positive":
                case "javax.validation.constraints.Positive":
                    constraints.add(new ValidationConstraint.Positive());
                    break;
                case "me.bristermitten.mittenlib.config.validation.Negative":
                case "jakarta.validation.constraints.Negative":
                case "javax.validation.constraints.Negative":
                    constraints.add(new ValidationConstraint.Negative());
                    break;
                case "me.bristermitten.mittenlib.config.validation.Min":
                case "jakarta.validation.constraints.Min":
                case "javax.validation.constraints.Min":
                    constraints.add(new ValidationConstraint.Min(getDoubleAttributeValue(mirror, "value")));
                    break;
                case "me.bristermitten.mittenlib.config.validation.Max":
                case "jakarta.validation.constraints.Max":
                case "javax.validation.constraints.Max":
                    constraints.add(new ValidationConstraint.Max(getDoubleAttributeValue(mirror, "value")));
                    break;
                case "me.bristermitten.mittenlib.config.validation.NotBlank":
                case "jakarta.validation.constraints.NotBlank":
                case "javax.validation.constraints.NotBlank":
                    constraints.add(new ValidationConstraint.NotBlank());
                    break;
                case "me.bristermitten.mittenlib.config.validation.Range":
                case "org.hibernate.validator.constraints.Range":
                    constraints.add(new ValidationConstraint.Range(
                            getDoubleAttributeValue(mirror, "min"), getDoubleAttributeValue(mirror, "max")));
                    break;
                case "me.bristermitten.mittenlib.config.validation.ValidateWith":
                    TypeMirror validatorType = getTypeAttributeValue(mirror, "value");
                    if (validatorType != null) {
                        constraints.add(new ValidationConstraint.Custom(
                                ClassName.get((TypeElement) ((DeclaredType) validatorType).asElement())));
                    }
                    break;
            }
        }
        return constraints;
    }

    private double getDoubleAttributeValue(AnnotationMirror mirror, String name) {
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
                mirror.getElementValues().entrySet()) {
            if (entry.getKey().getSimpleName().toString().equals(name)) {
                Object val = entry.getValue().getValue();
                if (val instanceof Number num) {
                    return num.doubleValue();
                }
            }
        }
        return 0.0;
    }

    private TypeMirror getTypeAttributeValue(AnnotationMirror mirror, String name) {
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
                mirror.getElementValues().entrySet()) {
            if (entry.getKey().getSimpleName().toString().equals(name)) {
                Object val = entry.getValue().getValue();
                if (val instanceof TypeMirror typeMirror) {
                    return typeMirror;
                }
            }
        }
        return null;
    }
}
