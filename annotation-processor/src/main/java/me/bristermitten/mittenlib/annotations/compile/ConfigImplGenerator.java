package me.bristermitten.mittenlib.annotations.compile;

import com.squareup.javapoet.*;
import me.bristermitten.mittenlib.annotations.ast.AbstractConfigStructure;
import me.bristermitten.mittenlib.annotations.ast.ConfigTypeSource;
import me.bristermitten.mittenlib.annotations.ast.Property;
import me.bristermitten.mittenlib.annotations.config.ConfigProcessor;
import me.bristermitten.mittenlib.annotations.util.Nullity;
import me.bristermitten.mittenlib.config.Configuration;
import me.bristermitten.mittenlib.config.GeneratedConfig;
import me.bristermitten.mittenlib.config.exception.ConfigLoadingErrors;

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
    private final ToStringGenerator toStringGenerator;
    private final EqualsHashCodeGenerator equalsHashCodeGenerator;
    private final ConfigurationClassNameGenerator configurationClassNameGenerator;
    private final ConfigNameCache configNameCache;
    private final MethodNames methodNames;

    @Inject
    public ConfigImplGenerator(AccessorGenerator accessorGenerator, ToStringGenerator toStringGenerator, EqualsHashCodeGenerator equalsHashCodeGenerator, ConfigurationClassNameGenerator configurationClassNameGenerator, ConfigNameCache configNameCache, MethodNames methodNames) {
        this.accessorGenerator = accessorGenerator;
        this.toStringGenerator = toStringGenerator;
        this.equalsHashCodeGenerator = equalsHashCodeGenerator;
        this.configurationClassNameGenerator = configurationClassNameGenerator;
        this.configNameCache = configNameCache;
        this.methodNames = methodNames;
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
    public JavaFile emit(AbstractConfigStructure ast) {
        ClassName configImplClassName = configurationClassNameGenerator.generateConfigurationClassName(ast.source().element());
        TypeSpec.Builder source = TypeSpec.classBuilder(configImplClassName);

        emitInto(ast, source);

        return JavaFile.builder(configImplClassName.packageName(), source.build()).build();
    }

    /**
     * Adds all necessary elements to the {@link TypeSpec.Builder} to create a complete implementation class.
     *
     * @param ast    The abstract configuration structure to generate an implementation for
     * @param source The {@link TypeSpec.Builder} to add elements to
     */
    private void emitInto(AbstractConfigStructure ast, TypeSpec.Builder source) {
        ClassName configImplClassName = configurationClassNameGenerator.generateConfigurationClassName(ast.source().element());
        source.addModifiers(Modifier.PUBLIC);
        makeAbstractIfUnion(ast, source);
        addSourceElement(ast, source);
        addInheritance(ast, source);
        addInnerDefaultMethodImpl(source, ast);
        addGeneratedConfigAnnotations(ast, source);
        addNestedClassModifiers(ast, source);
        addProperties(ast, source);
        addSuperClassField(ast, source);
        accessorGenerator.createWithMethods(source, ast);
        addAllArgsConstructor(source, ast);
        addStandardObjectMethods(ast, configImplClassName, source);
        addChildClasses(ast, source);

    }

    /**
     * When the element has a @{@link me.bristermitten.mittenlib.config.Source} marked, turn it into a {@link Configuration}
     * field, wiring in deserialization and, when supported for the type, serialization.
     */
    private void addSourceElement(AbstractConfigStructure ast, TypeSpec.Builder builder) {
        if (ast.settings().source() != null) {
            ClassName publicClassName = configurationClassNameGenerator.getPublicClassName(ast);

            FieldSpec.Builder configFieldBuilder = FieldSpec.builder(
                            ParameterizedTypeName.get(ClassName.get(Configuration.class), publicClassName),
                            "CONFIG"
                    )
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);

            configFieldBuilder.initializer(
                    "new $T<>($S, $T.class)", Configuration.class,
                    ast.settings().source().value(),
                    publicClassName
            );

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
        final List<String> unserializableProperties = ast.properties().stream()
                .filter(p -> !p.settings().hasDefaultValue() && !p.settings().isNullable())
                .map(Property::name)
                .toList();

        AnnotationSpec.Builder generatedConfigBuilder = AnnotationSpec.builder(GeneratedConfig.class)
                .addMember("source", "$T.class", ast.name())
                .addMember("isDynamicallyInitializable", "$L", ast.isDynamicallyInitializable());

        for (String property : unserializableProperties) {
            generatedConfigBuilder.addMember("unserializableProperties", "$S", property);
        }

        source.addAnnotation(generatedConfigBuilder.build());


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

    private void addChildClasses(AbstractConfigStructure ast, TypeSpec.Builder source) {
        for (AbstractConfigStructure child : ast.enclosed()) {
            var childClassName = configurationClassNameGenerator.translateConfigClassName(child);
            TypeSpec.Builder childBuilder = TypeSpec.classBuilder(childClassName);
            emitInto(child, childBuilder);
            source.addType(childBuilder.build());
        }
    }

    private void addProperty(Property property, TypeSpec.Builder source) {
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

    private Optional<TypeMirror> getSuperClass(AbstractConfigStructure ast) {
        if (ast.source() instanceof ConfigTypeSource.ClassConfigTypeSource classParent) {
            return classParent.parent();
        }
        return Optional.empty();
    }

    private Optional<TypeMirror> getSuperClass(TypeMirror ast) {
        return configNameCache
                .lookupAST(ast)
                .flatMap(this::getSuperClass);
    }

    private void addAllArgsConstructor(TypeSpec.Builder source, AbstractConfigStructure ast) {
        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC);

        addSuperClassParameter(ast, constructor);
        addPropertyParameters(ast, constructor);

        source.addMethod(constructor.build());
    }





    private void addSuperClassParameter(AbstractConfigStructure ast, MethodSpec.Builder constructor) {
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

    private void addSuperClassField(AbstractConfigStructure ast, TypeSpec.Builder builder) {
        var parentMirror = getSuperClass(ast);

        parentMirror.ifPresent(parent -> {
            var parentConfig = configNameCache.lookupAST(parent)
                    .orElseThrow(() -> new IllegalStateException("could not determine a config for parent class " + parent));
            ClassName parentName = configurationClassNameGenerator.translateConfigClassName(parentConfig);
            FieldSpec.Builder field = FieldSpec.builder(parentName, "parent", Modifier.PRIVATE, Modifier.FINAL);

            builder.addField(field.build());
        });
    }

    private List<String> buildSuperConstructorParams(TypeMirror parent, AbstractConfigStructure parentConfig, String superParameterName) {
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

    private void addPropertyParameters(AbstractConfigStructure ast, MethodSpec.Builder constructor) {
        for (Property property : ast.properties()) {
            ParameterSpec parameter = createPropertyParameter(property);
            constructor.addParameter(parameter);
            constructor.addStatement("this.$N = $N", property.name(), property.name());
        }
    }

    private ParameterSpec createPropertyParameter(Property property) {
        var nullityAnnotation = Nullity.getNullityAnnotation(property);
        ParameterSpec.Builder builder = ParameterSpec.builder(
                configurationClassNameGenerator.publicPropertyClassName(property)
                        .annotated(AnnotationSpec.builder(nullityAnnotation).build()),
                property.name()
        ).addModifiers(Modifier.FINAL);


        return builder.build();
    }

    /**
     * Create a dummy interface named "DefaultMethodAccess" which implements the config interface,
     * but leaves any non-default methods empty. This essentially gives us an easy way to access the default method of an interface.
     * Any non-default method will be given an implementation that throws {@link ConfigLoadingErrors#defaultValueProxyException(Class, String)}.
     * <p>
     * We only need to generate this interface if the given <code>ast</code> is interface and has any properties with default values.
     *
     * @param typeSpecBuilder The {@link TypeSpec.Builder} to add elements to. The generated interface will be added here, if it should be generated.
     * @param ast             the {@link AbstractConfigStructure} that we are generating a config from.
     * @return the {@link ClassName} of the generated <code>DefaultMethodAccess</code> interface, <i>if and only if</i> it was generated, otherwise an empty {@link Optional}.
     */
    private Optional<ClassName> addInnerDefaultMethodImpl(TypeSpec.Builder typeSpecBuilder, AbstractConfigStructure ast) {
        if (!(ast.source() instanceof ConfigTypeSource.InterfaceConfigTypeSource)) {
            return Optional.empty(); // nothing to do
        }

        boolean hasAnyDefaultValue = ast.properties().stream()
                .anyMatch(property -> property.settings().hasDefaultValue());
        // if no properties have default values, there's nothing to do
        if (!hasAnyDefaultValue) {
            return Optional.empty();
        }

        ClassName concreteConfigClassName = configurationClassNameGenerator.getConcreteConfigClassName(ast);
        var innerName = configurationClassNameGenerator.getDefaultMethodAccessClassName(ast);


        var innerBuilder = TypeSpec.classBuilder(innerName);
        innerBuilder.addModifiers(Modifier.PUBLIC, Modifier.STATIC);
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

        typeSpecBuilder.addType(innerBuilder.build());
        return Optional.of(innerName);
    }

}
