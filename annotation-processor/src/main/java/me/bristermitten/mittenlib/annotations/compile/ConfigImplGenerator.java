package me.bristermitten.mittenlib.annotations.compile;

import com.squareup.javapoet.*;
import io.toolisticon.aptk.tools.MessagerUtils;
import me.bristermitten.mittenlib.annotations.ast.AbstractConfigStructure;
import me.bristermitten.mittenlib.annotations.ast.ConfigTypeSource;
import me.bristermitten.mittenlib.annotations.ast.Property;
import me.bristermitten.mittenlib.annotations.config.ConfigProcessor;
import me.bristermitten.mittenlib.annotations.util.Nullity;
import me.bristermitten.mittenlib.annotations.util.TypesUtil;
import me.bristermitten.mittenlib.config.Config;
import me.bristermitten.mittenlib.config.Configuration;
import me.bristermitten.mittenlib.config.GeneratedConfig;
import me.bristermitten.mittenlib.config.exception.ConfigLoadingErrors;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import javax.annotation.processing.Generated;
import javax.inject.Inject;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;

public class ConfigImplGenerator {


    private final AccessorGenerator accessorGenerator;
    private final DeserializationCodeGenerator deserializationCodeGenerator;
    private final SerializationCodeGenerator serializationCodeGenerator;
    private final ToStringGenerator toStringGenerator;
    private final EqualsHashCodeGenerator equalsHashCodeGenerator;
    private final ConfigurationClassNameGenerator configurationClassNameGenerator;
    private final ConfigNameCache configNameCache;
    private final MethodNames methodNames;
    private final TypesUtil typesUtil;

    @Inject
    public ConfigImplGenerator(AccessorGenerator accessorGenerator, DeserializationCodeGenerator deserializationCodeGenerator, SerializationCodeGenerator serializationCodeGenerator, ToStringGenerator toStringGenerator, EqualsHashCodeGenerator equalsHashCodeGenerator, ConfigurationClassNameGenerator configurationClassNameGenerator, ConfigNameCache configNameCache, MethodNames methodNames, TypesUtil typesUtil) {
        this.accessorGenerator = accessorGenerator;
        this.deserializationCodeGenerator = deserializationCodeGenerator;
        this.serializationCodeGenerator = serializationCodeGenerator;
        this.toStringGenerator = toStringGenerator;
        this.equalsHashCodeGenerator = equalsHashCodeGenerator;
        this.configurationClassNameGenerator = configurationClassNameGenerator;
        this.configNameCache = configNameCache;
        this.methodNames = methodNames;
        this.typesUtil = typesUtil;
    }

    private static void makeAbstractIfUnion(AbstractConfigStructure ast, TypeSpec.Builder source) {
        if (ast instanceof AbstractConfigStructure.Union) {
            source.addModifiers(Modifier.ABSTRACT);
        }
    }


    /**
     * Generates a JavaFile containing the implementation class for the given configuration structure.
     *
     * @param ast The abstract configuration structure to generate an implementation for
     * @return A JavaFile containing the generated implementation class
     */
    public @NonNull JavaFile emit(@NonNull AbstractConfigStructure ast) {
        ClassName configImplClassName = configurationClassNameGenerator.generateConfigurationClassName(ast.source().element());
        TypeSpec.Builder source = TypeSpec.classBuilder(configImplClassName);

        emitInto(ast, source);

        return JavaFile.builder(configImplClassName.packageName(), source.build()).build();
    }

    /**
     * Adds all necessary elements to the TypeSpec.Builder to create a complete implementation class.
     *
     * @param ast    The abstract configuration structure to generate an implementation for
     * @param source The TypeSpec.Builder to add elements to
     */
    private void emitInto(@NonNull AbstractConfigStructure ast, TypeSpec.@NonNull Builder source) {
        ClassName configImplClassName = configurationClassNameGenerator.generateConfigurationClassName(ast.source().element());
        source.addModifiers(Modifier.PUBLIC);
        makeAbstractIfUnion(ast, source);
        addSourceElement(ast, source);
        addInheritance(ast, source);
        Optional<ClassName> innerDaoName = addInnerDefaultMethodImpl(source, ast);
        addGeneratedConfigAnnotations(ast, source);
        addNestedClassModifiers(ast, source);
        addProperties(ast, source);
        addSuperClassField(ast, source);
        accessorGenerator.createWithMethods(source, ast);
        addAllArgsConstructor(source, ast);
        addDeserializationMethods(ast, source, switch (ast.source()) {
            case ConfigTypeSource.InterfaceConfigTypeSource ignored -> innerDaoName.orElse(null);
            case ConfigTypeSource.ClassConfigTypeSource ignored -> ast.name();
        });
        addSerializationMethods(ast, source);
        addStandardObjectMethods(ast, configImplClassName, source);
        addChildClasses(ast, source);

    }

    /**
     * When the element has a @{@link me.bristermitten.mittenlib.config.Source} marked, turn it into a {@link Configuration} field
     */
    private void addSourceElement(AbstractConfigStructure ast, TypeSpec.Builder builder) {
        if (ast.settings().source() != null) {
            ClassName publicClassName = configurationClassNameGenerator.getPublicClassName(ast);
            ClassName implClassName = configurationClassNameGenerator.translateConfigClassName(ast);
            
            // Check if serialization is fully supported for this config
            boolean serializationSupported = serializationCodeGenerator.isSerializationSupported(ast);
            
            FieldSpec.Builder configFieldBuilder = FieldSpec.builder(
                            ParameterizedTypeName.get(ClassName.get(Configuration.class), publicClassName),
                            "CONFIG"
                    )
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);
            
            if (serializationSupported) {
                // Include both deserialize and serialize functions
                configFieldBuilder.initializer(
                        "new $T<>($S, $T.class, $T::$L, $T::$L)", Configuration.class,
                        ast.settings().source().value(),
                        publicClassName,
                        implClassName,
                        methodNames.getDeserializeMethodName(ast),
                        implClassName,
                        methodNames.getSerializeMethodName(ast)
                );
            } else {
                // Only include deserialize function (serialize is null)
                configFieldBuilder.initializer(
                        "new $T<>($S, $T.class, $T::$L)", Configuration.class,
                        ast.settings().source().value(),
                        publicClassName,
                        implClassName,
                        methodNames.getDeserializeMethodName(ast)
                );
            }
            
            builder.addField(configFieldBuilder.build());
        }
    }

    private void addInheritance(AbstractConfigStructure ast, TypeSpec.Builder source) {
        if (ast.source() instanceof ConfigTypeSource.InterfaceConfigTypeSource) {
            source.addSuperinterface(ast.name());
        }
        if (ast.source() instanceof ConfigTypeSource.ClassConfigTypeSource classParent) {
            classParent.parent()
                    .flatMap(configNameCache::lookupAST)
                    .ifPresent(parent ->
                            source.superclass(configurationClassNameGenerator.translateConfigClassName(parent)));
        }
    }

    private void addGeneratedConfigAnnotations(AbstractConfigStructure ast, TypeSpec.Builder source) {
        source.addAnnotation(AnnotationSpec.builder(GeneratedConfig.class)
                .addMember("source", "$T.class", ast.name())
                .build());


        source.addAnnotation(AnnotationSpec.builder(Generated.class)
                .addMember("value", "$S", ConfigProcessor.class.getName())
                .addMember("comments", "$S", "Generated by MittenLib Annotation Processor")
                .addMember("date", "$S", ZonedDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_INSTANT))
                .build()
        );
    }

    private void addNestedClassModifiers(AbstractConfigStructure ast, TypeSpec.Builder source) {
        // if it's enclosed in a class, make sure it's a nested class rather than an inner class
        if (ast.enclosedIn() != null) {
            source.addModifiers(Modifier.STATIC);
        }
    }

    private void addProperties(AbstractConfigStructure ast, TypeSpec.Builder source) {
        for (Property property : ast.properties()) {
            addProperty(property, source);
        }
    }

    private void addDeserializationMethods(AbstractConfigStructure ast,
                                           TypeSpec.Builder source,
                                           @Nullable ClassName daoName
    ) {
        deserializationCodeGenerator.createDeserializeMethods(source, ast, daoName);
    }

    private void addSerializationMethods(AbstractConfigStructure ast, TypeSpec.Builder source) {
        boolean serializationSupported = serializationCodeGenerator.isSerializationSupported(ast);
        
        if (serializationSupported) {
            serializationCodeGenerator.createSerializeMethods(source, ast);
        } else {
            List<String> unsupportedProperties = serializationCodeGenerator.getUnsupportedSerializationProperties(ast);
            
            // Check if serialization is required
            Config configAnnotation = ast.source().element().getAnnotation(Config.class);
            boolean requireSerialization = configAnnotation != null && configAnnotation.requireSerialization();
            
            String message = "Serialization cannot be generated for config '" + ast.name().simpleName() + "'. " +
                    "The following properties do not support serialization: " + String.join(", ", unsupportedProperties) + ". " +
                    "Consider adding @UseObjectMapperSerialization to these properties or providing CustomSerializers.";
            
            if (requireSerialization) {
                MessagerUtils.error(ast.source().element(), message);
            } else {
                MessagerUtils.warning(ast.source().element(), message);
            }
        }
    }

    private void addStandardObjectMethods(AbstractConfigStructure ast,
                                          ClassName configImplClassName,
                                          TypeSpec.Builder source) {
        if (ast.settings().generateToString()) {
            var toString = toStringGenerator.generateToString(ast.properties(), configImplClassName);
            source.addMethod(toString);
        }

        source.addMethod(equalsHashCodeGenerator.generateEquals(configImplClassName, ast.properties()));
        source.addMethod(equalsHashCodeGenerator.generateHashCode(ast.properties()));
    }

    private void addChildClasses(@NonNull AbstractConfigStructure ast, TypeSpec.@NonNull Builder source) {
        for (AbstractConfigStructure child : ast.enclosed()) {
            var childClassName = configurationClassNameGenerator.translateConfigClassName(child);
            TypeSpec.Builder childBuilder = TypeSpec.classBuilder(childClassName);
            emitInto(child, childBuilder);
            source.addType(childBuilder.build());
        }
    }

    private void addProperty(@NonNull Property property, TypeSpec.@NonNull Builder source) {
        FieldSpec field = FieldSpec.builder(
                configurationClassNameGenerator.publicPropertyClassName(property)
                        .annotated(Nullity.getNullityAnnotationSpec(property)),
                property.name(),
                Modifier.FINAL, Modifier.PRIVATE
        ).build();

        source.addField(field);

        switch (property.source()) {
            case Property.PropertySource.FieldSource(var element) ->
                    accessorGenerator.createGetterMethod(source, element, field);
            case Property.PropertySource.MethodSource(var method) ->
                    accessorGenerator.createGetterMethodOverriding(source, method, field);
        }
    }

    private Optional<TypeMirror> getSuperClass(@NonNull AbstractConfigStructure ast) {
        if (ast.source() instanceof ConfigTypeSource.ClassConfigTypeSource classParent) {
            return classParent.parent();
        }
        return Optional.empty();
    }

    private @NonNull Optional<TypeMirror> getSuperClass(@NonNull TypeMirror ast) {
        return configNameCache
                .lookupAST(ast)
                .flatMap(this::getSuperClass);
    }

    private void addAllArgsConstructor(TypeSpec.@NonNull Builder source, @NonNull AbstractConfigStructure ast) {
        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC);

        addSuperClassParameter(ast, constructor);
        addPropertyParameters(ast, constructor);

        source.addMethod(constructor.build());
    }

    private void addSuperClassParameter(@NonNull AbstractConfigStructure ast, MethodSpec.@NonNull Builder constructor) {
        // when we have a super_class_
        // we accept an instance of it as a parent
        // and then call `super(parent.a(), parent.b(), ...)`
        var parentMirror = getSuperClass(ast);

        parentMirror.ifPresent(parent -> {
            var parentConfig = configNameCache.lookupAST(parent)
                    .orElseThrow(() -> new IllegalStateException("could not determine a config for parent class " + parent));
            ClassName parentName = configurationClassNameGenerator.translateConfigClassName(parentConfig);

            String superParameterName = "parent";
            constructor.addParameter(
                    ParameterSpec.builder(
                            parentName, superParameterName,
                            Modifier.FINAL
                    ).build()
            );

            List<String> parentParams = buildSuperConstructorParams(parent, parentConfig, superParameterName);
            constructor.addStatement("super($L)", String.join(", ", parentParams));
            constructor.addStatement("this.parent = parent");
        });
    }

    private void addSuperClassField(@NonNull AbstractConfigStructure ast, TypeSpec.Builder builder) {
        var parentMirror = getSuperClass(ast);

        parentMirror.ifPresent(parent -> {
            var parentConfig = configNameCache.lookupAST(parent)
                    .orElseThrow(() -> new IllegalStateException("could not determine a config for parent class " + parent));
            ClassName parentName = configurationClassNameGenerator.translateConfigClassName(parentConfig);
            FieldSpec.Builder field = FieldSpec.builder(parentName, "parent", Modifier.PRIVATE, Modifier.FINAL);

            builder.addField(field.build());
        });
    }

    private @NonNull List<String> buildSuperConstructorParams(@NonNull TypeMirror parent, @NonNull AbstractConfigStructure parentConfig, @NonNull String superParameterName) {
        var parentParams = parentConfig.properties().stream()
                .map(variableElement -> superParameterName + "." + methodNames.safeMethodName(variableElement) + "()")
                .toList();

        Optional<TypeMirror> parentParent = getSuperClass(parent);
        // when the superclass has a superclass, we need to also pass the superclass to the parent constructor
        if (parentParent.isPresent()) {
            var newParentParams = new ArrayDeque<>(parentParams);
            newParentParams.addFirst(superParameterName);
            return List.copyOf(newParentParams);
        }

        return parentParams;
    }

    private void addPropertyParameters(@NonNull AbstractConfigStructure ast, MethodSpec.@NonNull Builder constructor) {
        for (Property property : ast.properties()) {
            ParameterSpec parameter = createPropertyParameter(property);
            constructor.addParameter(parameter);
            constructor.addStatement("this.$N = $N", property.name(), property.name());
        }
    }

    private @NonNull ParameterSpec createPropertyParameter(@NonNull Property property) {
        var nullityAnnotation = Nullity.getNullityAnnotation(property);
        ParameterSpec.Builder builder = ParameterSpec.builder(
                configurationClassNameGenerator.publicPropertyClassName(property)
                        .annotated(AnnotationSpec.builder(nullityAnnotation).build()),
                property.name()
        ).addModifiers(Modifier.FINAL);


        return builder.build();
    }

    private Optional<ClassName> addInnerDefaultMethodImpl(TypeSpec.@NonNull Builder typeSpecBuilder, @NonNull AbstractConfigStructure ast) {
        if (!(ast.source() instanceof ConfigTypeSource.InterfaceConfigTypeSource)) {
            return Optional.empty(); // nothing to do
        }


        ClassName concreteConfigClassName = configurationClassNameGenerator.getConcreteConfigClassName(ast);
        var innerName = concreteConfigClassName.nestedClass(ast.name().simpleName() + "DefaultMethodAccess");


        var innerBuilder = TypeSpec.classBuilder(innerName);
        innerBuilder.addModifiers(Modifier.PRIVATE, Modifier.STATIC);
        innerBuilder.addSuperinterface(ast.name());

        for (Property property : ast.properties()) {
            if (property.settings().hasDefaultValue()) {
                continue;
            }

            innerBuilder.addMethod(
                    MethodSpec.methodBuilder(property.name())
                            .addModifiers(Modifier.PUBLIC)
                            .addAnnotation(Override.class)
                            .returns(configurationClassNameGenerator.publicPropertyClassName(property))
                            .addStatement("throw $T.defaultValueProxyException($T.class, $S)",
                                    ConfigLoadingErrors.class,
                                    concreteConfigClassName,
                                    property.name())
                            .build()
            );
        }

        if (innerBuilder.methodSpecs.isEmpty()) {
            return Optional.empty(); // we've effectively done nothing
        }
        typeSpecBuilder.addType(innerBuilder.build());
        return Optional.of(innerName);
    }
}
