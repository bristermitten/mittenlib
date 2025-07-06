package me.bristermitten.mittenlib.annotations.parser;

import com.squareup.javapoet.ClassName;
import io.toolisticon.aptk.tools.TypeMirrorWrapper;
import io.toolisticon.aptk.tools.corematcher.AptkCoreMatchers;
import io.toolisticon.aptk.tools.wrapper.ElementWrapper;
import io.toolisticon.aptk.tools.wrapper.TypeElementWrapper;
import me.bristermitten.mittenlib.annotations.ast.*;
import me.bristermitten.mittenlib.annotations.compile.ConfigNameCache;
import me.bristermitten.mittenlib.annotations.util.ElementsFinder;
import me.bristermitten.mittenlib.annotations.util.TypesUtil;
import me.bristermitten.mittenlib.config.ConfigUnion;
import me.bristermitten.mittenlib.config.Source;
import me.bristermitten.mittenlib.config.generate.GenerateToString;
import me.bristermitten.mittenlib.config.names.ConfigName;
import me.bristermitten.mittenlib.config.names.NamingPattern;
import me.bristermitten.mittenlib.util.Null;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class ConfigClassParser {

    private final TypesUtil typesUtil;
    private final ElementsFinder elementsFinder;
    private final ConfigNameCache configNameCache;

    @Inject
    public ConfigClassParser(TypesUtil typesUtil, ElementsFinder elementsFinder, ConfigNameCache configNameCache) {
        this.typesUtil = typesUtil;
        this.elementsFinder = elementsFinder;
        this.configNameCache = configNameCache;
    }

    private static <T> T throwInvalidConfigError() {
        throw new IllegalArgumentException("Invalid config, impossible?");
    }

    private @NotNull ConfigTypeSource getSource(@NotNull TypeElement element) {
        var wrapped = TypeElementWrapper.wrap(element);

        List<TypeMirror> parents = Stream.concat(
                        Stream.of(element.getSuperclass()),
                        element.getInterfaces().stream()
                )
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

    private @NotNull List<Property> getPropertiesIn(@NotNull TypeElement element, @Nullable NamingPattern namingPattern) {
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
                    var propertySource = switch (propertyElement) {
                        case ExecutableElement e -> new Property.PropertySource.MethodSource(e);
                        case VariableElement e -> new Property.PropertySource.FieldSource(e);
                        default -> throw new IllegalStateException("Unexpected value: " + propertyElement.getKind());
                    };
                    var propertyType =
                            propertyElement instanceof ExecutableElement ? ((ExecutableElement) propertyElement).getReturnType() :
                                    propertyElement.asType();
                    var configName = typesUtil.getAnnotation(propertyElement, ConfigName.class);
                    var namingPatternSub =
                            Null.orElse(typesUtil.getAnnotation(propertyElement, NamingPattern.class), namingPattern);

                    var isNullable = typesUtil.isNullable(propertyElement);
                    return new Property(propertyElement.getSimpleName().toString(),
                            propertyType,
                            propertySource,
                            new ASTSettings.PropertyASTSettings(namingPatternSub, configName, isNullable));
                })
                .toList();
    }


    private ASTSettings.@NotNull ConfigASTSettings getSettings(@NotNull TypeElement element) {
        var namingPattern = typesUtil.getAnnotation(element, NamingPattern.class);
        GenerateToString generateToString = typesUtil.getAnnotation(element, GenerateToString.class);
        Source source = typesUtil.getAnnotation(element, Source.class);

        return new ASTSettings.ConfigASTSettings(namingPattern, source, generateToString != null);
    }

    private @NotNull AbstractConfigStructure parseAbstract(@NotNull TypeElement element, @Nullable ASTParentReference parentReference) {
        TypeElementWrapper wrapper = TypeElementWrapper.wrap(element);

        Optional<TypeElementWrapper> enclosingType = wrapper.getEnclosingElement()
                .filter(ElementWrapper::isTypeElement)
                .map(ElementWrapper::toTypeElement);


        List<ClassName> parents = Stream.concat(
                        Stream.of(element.getSuperclass()),
                        element.getInterfaces().stream()
                )
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

        var thisParentReference = enclosingName == null ? null : new ASTParentReference(enclosingName, parentReference);
        var enclosedConfigs = wrapper.filterEnclosedElements()
                .applyFilter(AptkCoreMatchers.IS_TYPE_ELEMENT)
                .applyFilter(AptkCoreMatchers.BY_ELEMENT_KIND)
                .filterByOneOf(ElementKind.CLASS, ElementKind.INTERFACE)
                .getResult()
                .stream()
                .map((e) -> parseAbstract(e, thisParentReference))
                .toList();

        var namingPattern = typesUtil.getAnnotation(element, NamingPattern.class);
        List<Property> properties = getPropertiesIn(element, namingPattern);


        var source = getSource(element);
        if (wrapper.hasAnnotation(ConfigUnion.class)) {
            return putInCache(new AbstractConfigStructure.Union(ClassName.get(element), source, getSettings(element), thisParentReference, parents, enclosedConfigs, properties));
        }
        if (parents.isEmpty()) {
            return putInCache(new AbstractConfigStructure.Atomic(ClassName.get(element), source, getSettings(element), enclosedConfigs, thisParentReference, properties));
        }

        return putInCache(new AbstractConfigStructure.Intersection(ClassName.get(element), source, getSettings(element), thisParentReference, enclosedConfigs, parents, properties));
    }

    private AbstractConfigStructure putInCache(@NotNull AbstractConfigStructure configStructure) {
        configNameCache.put(configStructure);
        return configStructure;
    }

    public @NotNull AbstractConfigStructure parseAbstract(@NotNull TypeElement element) {
        var ast = parseAbstract(element, null);
        configNameCache.put(ast);
        return ast;
    }
}
