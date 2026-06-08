package me.bristermitten.mittenlib.annotations.compile;

import com.google.inject.Inject;
import com.squareup.javapoet.*;
import me.bristermitten.mittenlib.annotations.ast.ASTSettings;
import me.bristermitten.mittenlib.annotations.ast.AbstractConfigStructure;
import me.bristermitten.mittenlib.annotations.ast.ConfigTypeSource;
import me.bristermitten.mittenlib.annotations.ast.Property;
import me.bristermitten.mittenlib.annotations.config.ConfigProcessor;
import me.bristermitten.mittenlib.annotations.util.Nullity;
import me.bristermitten.mittenlib.config.Configuration;
import me.bristermitten.mittenlib.config.GeneratedConfig;
import me.bristermitten.mittenlib.config.Source;
import me.bristermitten.mittenlib.config.exception.ConfigLoadingErrors;

import javax.annotation.processing.Generated;
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
    public ConfigImplGenerator(
            AccessorGenerator accessorGenerator,
            ToStringGenerator toStringGenerator,
            EqualsHashCodeGenerator equalsHashCodeGenerator,
            ConfigurationClassNameGenerator configurationClassNameGenerator,
            ConfigNameCache configNameCache,
            MethodNames methodNames) {
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
        ClassName configImplClassName =
                configurationClassNameGenerator.generateConfigurationClassName(ast.source().element());
        TypeSpec.Builder source = TypeSpec.classBuilder(configImplClassName);

        emitInto(ast, source);

        return JavaFile.builder(configImplClassName.packageName(), source.build()).build();
    }

    /**
     * Adds all necessary elements to the {@link TypeSpec.Builder} to create a complete implementation
     * class.
     *
     * @param ast    The abstract configuration structure to generate an implementation for
     * @param source The {@link TypeSpec.Builder} to add elements to
     */
    private void emitInto(AbstractConfigStructure ast, TypeSpec.Builder source) {
        ClassName configImplClassName =
                configurationClassNameGenerator.generateConfigurationClassName(ast.source().element());
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
     * Adds the {@code CONFIG} static field to the class if a {@link Source} is defined.
     *
     * <p>Generates:
     *
     * <pre>{@code
     * public static final Configuration<MyConfig> CONFIG = new Configuration<>("config.yml", MyConfig.class);
     * }</pre>
     *
     * @param ast     the configuration structure, used to determine the public class name (e.g. {@code
     *                MyConfig}) and source (e.g. {@code "config.yml"})
     * @param builder the class builder
     */
    private void addSourceElement(AbstractConfigStructure ast, TypeSpec.Builder builder) {
        if (ast.settings().source() != null) {
            ClassName publicClassName = configurationClassNameGenerator.getPublicClassName(ast);
            ClassName implementationClassName =
                    configurationClassNameGenerator.translateConfigClassName(ast);

            FieldSpec.Builder configFieldBuilder =
                    FieldSpec.builder(
                                    ParameterizedTypeName.get(ClassName.get(Configuration.class), publicClassName),
                                    "CONFIG")
                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);

            configFieldBuilder.initializer(
                    "new $T<>($S, $T.class, $T.class)",
                    Configuration.class,
                    ast.settings().source().value(),
                    publicClassName,
                    implementationClassName);

            builder.addField(configFieldBuilder.build());
        }
    }

    /**
     * Adds inheritance information to the generated implementation class.
     *
     * <p>Generates:
     *
     * <pre>{@code
     * public class MyConfigImpl extends BaseConfigImpl implements MyConfig
     * }</pre>
     *
     * @param ast    the configuration structure, used to determine the parent class (e.g. {@code
     *               BaseConfigImpl}) and interfaces (e.g. {@code MyConfig})
     * @param source the class builder
     */
    private void addInheritance(AbstractConfigStructure ast, TypeSpec.Builder source) {
        if (ast.source() instanceof ConfigTypeSource.InterfaceConfigTypeSource) {
            source.addSuperinterface(ast.name());
        }
        if (ast.source() instanceof ConfigTypeSource.ClassConfigTypeSource classParent) {
            classParent
                    .parent()
                    .flatMap(configNameCache::lookupAST)
                    .ifPresent(
                            parent ->
                                    source.superclass(
                                            configurationClassNameGenerator.translateConfigClassName(parent)));
        }
    }

    /**
     * Adds {@link GeneratedConfig} and {@link Generated} annotations to the class.
     *
     * @param ast    the configuration structure
     * @param source the class builder
     */
    private void addGeneratedConfigAnnotations(AbstractConfigStructure ast, TypeSpec.Builder source) {
        final List<String> unserializableProperties =
                ast.properties().stream()
                        .filter(p -> !p.settings().hasDefaultValue() && !p.settings().isNullable())
                        .map(Property::name)
                        .toList();

        AnnotationSpec.Builder generatedConfigBuilder =
                AnnotationSpec.builder(GeneratedConfig.class)
                        .addMember("source", "$T.class", ast.name())
                        .addMember("isDynamicallyInitializable", "$L", ast.isDynamicallyInitializable());

        for (String property : unserializableProperties) {
            generatedConfigBuilder.addMember("uninitializableProperties", "$S", property);
        }

        source.addAnnotation(generatedConfigBuilder.build());

        source.addAnnotation(
                AnnotationSpec.builder(Generated.class)
                        .addMember("value", "$S", ConfigProcessor.class.getName())
                        .addMember("comments", "$S", "Generated by MittenLib Annotation Processor")
                        .addMember(
                                "date",
                                "$S",
                                ZonedDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_INSTANT))
                        .build());
    }

    /**
     * Ensures nested classes are marked as {@code static}.
     *
     * @param ast    the configuration structure
     * @param source the class builder
     */
    private void addNestedClassModifiers(AbstractConfigStructure ast, TypeSpec.Builder source) {
        // if it's enclosed in a class, make sure it's a nested class rather than an inner class
        if (ast.enclosedIn() != null) {
            source.addModifiers(Modifier.STATIC);
        }
    }

    /**
     * Adds all properties as fields and accessors to the implementation class.
     *
     * @param ast    the configuration structure
     * @param source the class builder
     */
    private void addProperties(AbstractConfigStructure ast, TypeSpec.Builder source) {
        for (Property property : ast.properties()) {
            addProperty(property, source);
        }
    }

    /**
     * Adds {@code equals} and {@code hashCode} methods, plus {@code toString} if {@link
     * ASTSettings.ConfigASTSettings#generateToString()} is true
     *
     * @param ast                 the configuration structure
     * @param configImplClassName the name of the implementation class
     * @param source              the class builder
     */
    private void addStandardObjectMethods(
            AbstractConfigStructure ast, ClassName configImplClassName, TypeSpec.Builder source) {
        if (ast.settings().generateToString()) {
            var toString = toStringGenerator.generateToString(ast.properties(), configImplClassName);
            source.addMethod(toString);
        }

        source.addMethod(equalsHashCodeGenerator.generateEquals(configImplClassName, ast.properties()));
        source.addMethod(equalsHashCodeGenerator.generateHashCode(ast.properties()));
    }

    /**
     * Recursively adds implementation classes for enclosed configuration structures. For each
     * enclosed structure, we create a subclass builder and call {@link
     * #emitInto(AbstractConfigStructure, TypeSpec.Builder)} to add the subclass config values.
     *
     * @param ast    the parent configuration structure
     * @param source the parent class builder
     */
    private void addChildClasses(AbstractConfigStructure ast, TypeSpec.Builder source) {
        for (AbstractConfigStructure child : ast.enclosed()) {
            var childClassName = configurationClassNameGenerator.translateConfigClassName(child);
            TypeSpec.Builder childBuilder = TypeSpec.classBuilder(childClassName);
            emitInto(child, childBuilder);
            source.addType(childBuilder.build());
        }
    }

    /**
     * Adds a single property as a private final field and its corresponding getter.
     *
     * <p>Generates:
     *
     * <pre>{@code
     * private final String name;
     *
     * @Override
     * public String name() {
     *     return this.name;
     * }
     * }</pre>
     *
     * @param property the property to add, used for the field name and getter (e.g. {@code name})
     * @param source   the class builder
     */
    private void addProperty(Property property, TypeSpec.Builder source) {
        FieldSpec field =
                FieldSpec.builder(
                                configurationClassNameGenerator
                                        .publicPropertyClassName(property)
                                        .annotated(Nullity.getNullityAnnotationSpec(property)),
                                property.name(),
                                Modifier.FINAL,
                                Modifier.PRIVATE)
                        .build();

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
        return configNameCache.lookupAST(ast).flatMap(this::getSuperClass);
    }

    /**
     * Adds an all-argument constructor to the implementation class.
     *
     * <p>Generates:
     *
     * <pre>{@code
     * public MyConfigImpl(String name, int age) {
     *     this.name = name;
     *     this.age = age;
     * }
     * }</pre>
     *
     * @param source the class builder
     * @param ast the configuration structure, used to determine constructor parameters (e.g. {@code
     *     name}, {@code age})
     */
    private void addAllArgsConstructor(TypeSpec.Builder source, AbstractConfigStructure ast) {
        MethodSpec.Builder constructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);

        addSuperClassParameter(ast, constructor);
        addPropertyParameters(ast, constructor);

        source.addMethod(constructor.build());
    }

    /**
     * Adds a parameter to the constructor for the parent configuration class, if applicable.
     *
     * <p>Generates:
     *
     * <pre>{@code
     * public MyConfigImpl(BaseConfigImpl parent, String name) {
     *     super(parent.a(), parent.b());
     *     this.parent = parent;
     *     ... // see addPropertyParameters
     * }
     * }</pre>
     *
     * @param ast         the configuration structure, used to find the parent configuration
     * @param constructor the constructor builder to add the parameter and super call to
     */
    private void addSuperClassParameter(AbstractConfigStructure ast, MethodSpec.Builder constructor) {
        // when we have a super_class_
        // we accept an instance of it as a parent
        // and then call `super(parent.a(), parent.b(), ...)`
        var parentMirror = getSuperClass(ast);

        parentMirror.ifPresent(
                parent -> {
                    var parentConfig =
                            configNameCache
                                    .lookupAST(parent)
                                    .orElseThrow(
                                            () ->
                                                    new IllegalStateException(
                                                            "could not determine a config for parent class " + parent));
                    ClassName parentName =
                            configurationClassNameGenerator.translateConfigClassName(parentConfig);

                    String superParameterName = "parent";
                    constructor.addParameter(
                            ParameterSpec.builder(parentName, superParameterName, Modifier.FINAL).build());

                    List<String> parentParams =
                            buildSuperConstructorParams(parent, parentConfig, superParameterName);
                    constructor.addStatement("super($L)", String.join(", ", parentParams));
                    constructor.addStatement("this.parent = parent");
                });
    }

    /**
     * Adds a field to the implementation class to store the parent configuration instance.
     *
     * <p>Generates:
     *
     * <pre>{@code
     * private final BaseConfigImpl parent;
     * }</pre>
     *
     * @param ast the configuration structure, used to find the parent configuration
     * @param builder the class builder
     */
    private void addSuperClassField(AbstractConfigStructure ast, TypeSpec.Builder builder) {
        var parentMirror = getSuperClass(ast);

        parentMirror.ifPresent(
                parent -> {
                    var parentConfig =
                            configNameCache
                                    .lookupAST(parent)
                                    .orElseThrow(
                                            () ->
                                                    new IllegalStateException(
                                                            "could not determine a config for parent class " + parent));
                    ClassName parentName =
                            configurationClassNameGenerator.translateConfigClassName(parentConfig);
                    FieldSpec.Builder field =
                            FieldSpec.builder(parentName, "parent", Modifier.PRIVATE, Modifier.FINAL);

                    builder.addField(field.build());
                });
    }

    /**
     * Builds the list of parameters to be passed to the super constructor.
     *
     * <p>Generates:
     *
     * <pre>{@code
     * parent.a(), parent.b()
     * }</pre>
     *
     * @param parent             the type mirror of the parent class
     * @param parentConfig       the configuration structure of the parent
     * @param superParameterName the name of the parent parameter (e.g. {@code parent})
     * @return a list of code strings for the super constructor parameters
     */
    private List<String> buildSuperConstructorParams(
            TypeMirror parent, AbstractConfigStructure parentConfig, String superParameterName) {
        var parentParams =
                parentConfig.properties().stream()
                        .map(
                                variableElement ->
                                        superParameterName + "." + methodNames.safeMethodName(variableElement) + "()")
                        .toList();

        Optional<TypeMirror> parentParent = getSuperClass(parent);
        // when the superclass has a superclass, we need to also pass the superclass to the parent
        // constructor
        if (parentParent.isPresent()) {
            var newParentParams = new ArrayDeque<>(parentParams);
            newParentParams.addFirst(superParameterName);
            return List.copyOf(newParentParams);
        }

        return parentParams;
    }

    /**
     * Adds constructor parameters and initialization statements for all properties.
     *
     * <p>Generates:
     *
     * <pre>{@code
     * public MyConfigImpl(String name) {
     *     this.name = name;
     * }
     * }</pre>
     *
     * @param ast         the configuration structure
     * @param constructor the constructor builder
     */
    private void addPropertyParameters(AbstractConfigStructure ast, MethodSpec.Builder constructor) {
        for (Property property : ast.properties()) {
            ParameterSpec parameter = createPropertyParameter(property);
            constructor.addParameter(parameter);
            constructor.addStatement("this.$N = $N", property.name(), property.name());
        }
    }

    private ParameterSpec createPropertyParameter(Property property) {
        var nullityAnnotation = Nullity.getNullityAnnotation(property);
        ParameterSpec.Builder builder =
                ParameterSpec.builder(
                                configurationClassNameGenerator
                                        .publicPropertyClassName(property)
                                        .annotated(AnnotationSpec.builder(nullityAnnotation).build()),
                                property.name())
                        .addModifiers(Modifier.FINAL);

        return builder.build();
    }

    /**
     * Create a dummy interface named "DefaultMethodAccess" which implements the config interface, but
     * leaves any non-default methods empty. This essentially gives us an easy way to access the
     * default method of an interface. Any non-default method will be given an implementation that
     * throws {@link ConfigLoadingErrors#defaultValueProxyException(Class, String)}.
     *
     * <p>We only need to generate this interface if the given <code>ast</code> is interface and has
     * any properties with default values.
     *
     * @param typeSpecBuilder The {@link TypeSpec.Builder} to add elements to. The generated interface
     *     will be added here, if it should be generated.
     * @param ast the {@link AbstractConfigStructure} that we are generating a config from.
     * @return the {@link ClassName} of the generated <code>DefaultMethodAccess</code> interface,
     *     <i>if and only if</i> it was generated, otherwise an empty {@link Optional}.
     */
    private Optional<ClassName> addInnerDefaultMethodImpl(
            TypeSpec.Builder typeSpecBuilder, AbstractConfigStructure ast) {
        if (!(ast.source() instanceof ConfigTypeSource.InterfaceConfigTypeSource)) {
            return Optional.empty(); // nothing to do
        }

        boolean hasAnyDefaultValue =
                ast.properties().stream().anyMatch(property -> property.settings().hasDefaultValue());
        // if no properties have default values, there's nothing to do
        if (!hasAnyDefaultValue) {
            return Optional.empty();
        }

        ClassName concreteConfigClassName =
                configurationClassNameGenerator.getConcreteConfigClassName(ast);
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
              .addStatement(
                  "throw $T.defaultValueProxyException($T.class, $S)",
                  ConfigLoadingErrors.class,
                  concreteConfigClassName,
                  property.name())
              .build());
    }

    typeSpecBuilder.addType(innerBuilder.build());
    return Optional.of(innerName);
  }
}
