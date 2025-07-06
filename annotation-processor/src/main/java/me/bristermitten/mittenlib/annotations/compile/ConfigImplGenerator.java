package me.bristermitten.mittenlib.annotations.compile;

import com.squareup.javapoet.*;
import me.bristermitten.mittenlib.annotations.ast.AbstractConfigStructure;
import me.bristermitten.mittenlib.annotations.ast.ConfigTypeSource;
import me.bristermitten.mittenlib.annotations.ast.Property;
import me.bristermitten.mittenlib.annotations.config.ToStringGenerator;
import me.bristermitten.mittenlib.config.GeneratedConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;

public class ConfigImplGenerator {


    private final AccessorGenerator accessorGenerator;
    private final DeserializationCodeGenerator deserializationCodeGenerator;
    private final ToStringGenerator toStringGenerator;
    private final EqualsHashCodeGenerator equalsHashCodeGenerator;
    private final ConfigurationClassNameGenerator configurationClassNameGenerator;
    private final ConfigNameCache configNameCache;
    private final MethodNames methodNames;

    @Inject
    public ConfigImplGenerator(AccessorGenerator accessorGenerator, DeserializationCodeGenerator deserializationCodeGenerator, ToStringGenerator toStringGenerator, EqualsHashCodeGenerator equalsHashCodeGenerator, ConfigurationClassNameGenerator configurationClassNameGenerator, ConfigNameCache configNameCache, MethodNames methodNames) {
        this.accessorGenerator = accessorGenerator;
        this.deserializationCodeGenerator = deserializationCodeGenerator;
        this.toStringGenerator = toStringGenerator;
        this.equalsHashCodeGenerator = equalsHashCodeGenerator;
        this.configurationClassNameGenerator = configurationClassNameGenerator;
        this.configNameCache = configNameCache;
        this.methodNames = methodNames;
    }

    /**
     * Generates a JavaFile containing the implementation class for the given configuration structure.
     *
     * @param ast The abstract configuration structure to generate an implementation for
     * @return A JavaFile containing the generated implementation class
     */
    public @NotNull JavaFile emit(@NotNull AbstractConfigStructure ast) {
        ClassName configImplClassName = ConfigurationClassNameGenerator.createConfigImplClassName(ast.name());
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
    private void emitInto(@NotNull AbstractConfigStructure ast, TypeSpec.@NotNull Builder source) {
        ClassName configImplClassName = ConfigurationClassNameGenerator.createConfigImplClassName(ast);

        if (ast instanceof AbstractConfigStructure.Union) {
            source.addModifiers(Modifier.ABSTRACT);
        }
        addInheritance(ast, source);
        addGeneratedConfigAnnotation(ast, source);
        addNestedClassModifiers(ast, source);
        addProperties(ast, source);
        addSuperClassField(ast, source);
        accessorGenerator.createWithMethods(source, ast);
        addAllArgsConstructor(source, ast);
        addDeserializationMethods(ast, source);
        addStandardObjectMethods(ast, configImplClassName, source);
        addChildClasses(ast, source);
    }

    private void addInheritance(@NotNull AbstractConfigStructure ast, TypeSpec.@NotNull Builder source) {
        if (ast.source() instanceof ConfigTypeSource.InterfaceConfigTypeSource) {
            source.addSuperinterface(ast.name());
        }
        if (ast.source() instanceof ConfigTypeSource.ClassConfigTypeSource classParent) {
            classParent.parent()
                    .flatMap(configNameCache::lookupAST)
                    .ifPresent(parent ->
                            source.superclass(ConfigurationClassNameGenerator.createConfigImplClassName(parent)));
        }
    }

    private void addGeneratedConfigAnnotation(@NotNull AbstractConfigStructure ast, TypeSpec.@NotNull Builder source) {
        source.addAnnotation(AnnotationSpec.builder(GeneratedConfig.class)
                .addMember("source", "$T.class", ast.name())
                .build());
    }

    private void addNestedClassModifiers(@NotNull AbstractConfigStructure ast, TypeSpec.@NotNull Builder source) {
        // if it's enclosed in a class, make sure it's a nested class rather than an inner class
        if (ast.enclosedIn() != null) {
            source.addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        }
    }

    private void addProperties(@NotNull AbstractConfigStructure ast, TypeSpec.@NotNull Builder source) {
        for (Property property : ast.properties()) {
            addProperty(property, source);
        }
    }

    private void addDeserializationMethods(@NotNull AbstractConfigStructure ast, TypeSpec.@NotNull Builder source) {
        deserializationCodeGenerator.createDeserializeMethods(source,
                ast
        );
    }

    private void addStandardObjectMethods(@NotNull AbstractConfigStructure ast, ClassName configImplClassName, @NotNull TypeSpec.Builder source) {
        if (ast.settings().generateToString()) {
            var toString = toStringGenerator.generateToString(ast.properties(), configImplClassName);
            source.addMethod(toString);
        }

        source.addMethod(equalsHashCodeGenerator.generateEquals(configImplClassName, ast.properties()));
        source.addMethod(equalsHashCodeGenerator.generateHashCode(ast.properties()));
    }

    private void addChildClasses(@NotNull AbstractConfigStructure ast, TypeSpec.@NotNull Builder source) {
        for (AbstractConfigStructure child : ast.enclosed()) {
            var childClassName = ConfigurationClassNameGenerator.createConfigImplClassName(child);
            TypeSpec.Builder childBuilder = TypeSpec.classBuilder(childClassName);
            emitInto(child, childBuilder);
            source.addType(childBuilder.build());
        }
    }

    private void addProperty(@NotNull Property property, TypeSpec.@NotNull Builder source) {
        FieldSpec field = FieldSpec.builder(
                configurationClassNameGenerator.publicPropertyClassName(property),
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

    private Optional<TypeMirror> getSuperClass(@NotNull AbstractConfigStructure ast) {
        if (ast.source() instanceof ConfigTypeSource.ClassConfigTypeSource classParent) {
            return classParent.parent();
        }
        return Optional.empty();
    }

    private @NotNull Optional<TypeMirror> getSuperClass(@NotNull TypeMirror ast) {
        return configNameCache
                .lookupAST(ast)
                .flatMap(this::getSuperClass);
    }

    private void addAllArgsConstructor(TypeSpec.@NotNull Builder source, @NotNull AbstractConfigStructure ast) {
        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC);

        addSuperClassParameter(ast, constructor);
        addPropertyParameters(ast, constructor);

        source.addMethod(constructor.build());
    }

    private void addSuperClassParameter(@NotNull AbstractConfigStructure ast, MethodSpec.@NotNull Builder constructor) {
        // when we have a super_class_
        // we accept an instance of it as a parent
        // and then call `super(parent.a(), parent.b(), ...)`
        var parentMirror = getSuperClass(ast);

        parentMirror.ifPresent(parent -> {
            var parentConfig = configNameCache.lookupAST(parent)
                    .orElseThrow(() -> new IllegalStateException("could not determine a config for parent class " + parent));
            ClassName parentName = ConfigurationClassNameGenerator.createConfigImplClassName(parentConfig);

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

    private void addSuperClassField(@NotNull AbstractConfigStructure ast, TypeSpec.Builder builder) {
        var parentMirror = getSuperClass(ast);

        parentMirror.ifPresent(parent -> {
            var parentConfig = configNameCache.lookupAST(parent)
                    .orElseThrow(() -> new IllegalStateException("could not determine a config for parent class " + parent));
            ClassName parentName = ConfigurationClassNameGenerator.createConfigImplClassName(parentConfig);
            FieldSpec.Builder field = FieldSpec.builder(parentName, "parent", Modifier.PRIVATE, Modifier.FINAL);

            builder.addField(field.build());
        });
    }

    private @NotNull List<String> buildSuperConstructorParams(@NotNull TypeMirror parent, @NotNull AbstractConfigStructure parentConfig, @NotNull String superParameterName) {
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

    private void addPropertyParameters(@NotNull AbstractConfigStructure ast, MethodSpec.@NotNull Builder constructor) {
        for (Property property : ast.properties()) {
            ParameterSpec parameter = createPropertyParameter(property);
            constructor.addParameter(parameter);
            constructor.addStatement("this.$N = $N", property.name(), property.name());
        }
    }

    private @NotNull ParameterSpec createPropertyParameter(@NotNull Property property) {
        ParameterSpec.Builder builder = ParameterSpec.builder(
                configurationClassNameGenerator.publicPropertyClassName(property),
                property.name()
        ).addModifiers(Modifier.FINAL);

        if (property.settings().isNullable()) {
            builder.addAnnotation(AnnotationSpec.builder(Nullable.class).build());
        } else {
            builder.addAnnotation(AnnotationSpec.builder(NotNull.class).build());
        }

        return builder.build();
    }
}
