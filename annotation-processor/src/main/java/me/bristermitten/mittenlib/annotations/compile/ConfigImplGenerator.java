package me.bristermitten.mittenlib.annotations.compile;

import com.squareup.javapoet.*;
import me.bristermitten.mittenlib.annotations.ast.AbstractConfigStructure;
import me.bristermitten.mittenlib.annotations.ast.ConfigTypeSource;
import me.bristermitten.mittenlib.annotations.ast.Property;
import me.bristermitten.mittenlib.annotations.config.*;
import me.bristermitten.mittenlib.annotations.util.TypesUtil;
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
    private final FieldClassNameGenerator fieldClassNameGenerator;
    private final TypesUtil typesUtil;
    private final DeserializationCodeGenerator deserializationCodeGenerator;
    private final ToStringGenerator toStringGenerator;
    private final EqualsHashCodeGenerator equalsHashCodeGenerator;
    private final ConfigurationClassNameGenerator configurationClassNameGenerator;
    private final ConfigNameCache configNameCache;
    private final MethodNames methodNames;

    @Inject
    public ConfigImplGenerator(AccessorGenerator accessorGenerator, FieldClassNameGenerator fieldClassNameGenerator, TypesUtil typesUtil, DeserializationCodeGenerator deserializationCodeGenerator, ToStringGenerator toStringGenerator, EqualsHashCodeGenerator equalsHashCodeGenerator, ConfigurationClassNameGenerator configurationClassNameGenerator, ConfigNameCache configNameCache, MethodNames methodNames) {
        this.accessorGenerator = accessorGenerator;
        this.fieldClassNameGenerator = fieldClassNameGenerator;
        this.typesUtil = typesUtil;
        this.deserializationCodeGenerator = deserializationCodeGenerator;
        this.toStringGenerator = toStringGenerator;
        this.equalsHashCodeGenerator = equalsHashCodeGenerator;
        this.configurationClassNameGenerator = configurationClassNameGenerator;
        this.configNameCache = configNameCache;
        this.methodNames = methodNames;
    }

    public JavaFile emit(AbstractConfigStructure ast) {
        ClassName configImplClassName = ConfigurationClassNameGenerator.createConfigImplClassName(ast.name());

        TypeSpec.Builder source = TypeSpec.classBuilder(configImplClassName);

        emitInto(ast, source);


        JavaFile.Builder builder = JavaFile.builder(
                configImplClassName.packageName(),
                source.build()
        );
        return builder.build();
    }


    private void emitInto(AbstractConfigStructure ast, TypeSpec.Builder source) {
        ClassName configImplClassName = ConfigurationClassNameGenerator.createConfigImplClassName(ast);
        if (ast.source() instanceof ConfigTypeSource.InterfaceConfigTypeSource) {
            source.addSuperinterface(ast.name());
        }
        if (ast.source() instanceof ConfigTypeSource.ClassConfigTypeSource classParent) {
            classParent.parent()
                    .flatMap(configNameCache::lookupAST)
                    .ifPresent(parent -> {
                        source.superclass(ConfigurationClassNameGenerator.createConfigImplClassName(parent));
                    });
        }

//        MessagerUtils.error(ast.source().element(), ast.name().toString());
        source.addAnnotation(AnnotationSpec.builder(GeneratedConfig.class)
                .addMember("source", "$T.class", ast.name())
                .build());

        // if it's enclosed in a class, make sure it's a nested class rather than an inner class
        if (ast.enclosedIn() != null) {
            source.addModifiers(Modifier.STATIC);
        }

        for (Property property : ast.properties()) {
            addProperty(property, source);
        }
        addAllArgsConstructor(source, ast);

        deserializationCodeGenerator.createDeserializeMethods(source,
                ast,
                ast.source().element(),
                ast.properties(),
                v -> null
        );

        if (ast.settings().generateToString()) {
            var toString = toStringGenerator.generateToString(ast.properties(), configImplClassName);
            source.addMethod(toString);
        }

        source.addMethod(equalsHashCodeGenerator.generateEquals(configImplClassName, ast.properties()));
        source.addMethod(equalsHashCodeGenerator.generateHashCode(ast.properties()));

        for (AbstractConfigStructure child : ast.enclosed()) {
            var childClassName = ConfigurationClassNameGenerator.createConfigImplClassName(child);

            TypeSpec.Builder childBuilder = TypeSpec.classBuilder(childClassName);
            emitInto(child, childBuilder);


            source.addType(childBuilder.build());

        }

    }

    private void addProperty(Property property, TypeSpec.Builder source) {
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

            var parentParams = parentConfig.properties().stream()
                    .map(variableElement -> superParameterName + "." + methodNames.safeMethodName(variableElement) + "()")
                    .toList();

            Optional<TypeMirror> parentParent = getSuperClass(parent);
            // when the superclass has a superclass, we need to also pass the superclass to the parent constructor
            if (parentParent.isPresent()) {
                var newParentParams = new ArrayDeque<>(parentParams);
                newParentParams.addFirst(superParameterName);
                parentParams = List.copyOf(newParentParams);
            }

            constructor.addStatement("super($L)", String.join(", ", parentParams));
        });


        for (Property property : ast.properties()) {
            ParameterSpec.Builder builder = ParameterSpec.builder(
                    configurationClassNameGenerator.publicPropertyClassName(property),
                    property.name()
            ).addModifiers(Modifier.FINAL);

            if (property.settings().isNullable()) {
                builder.addAnnotation(AnnotationSpec.builder(Nullable.class).build());
            } else {
                builder.addAnnotation(AnnotationSpec.builder(NotNull.class).build());
            }

            constructor.addParameter(builder.build());
            constructor.addStatement("this.$N = $N", property.name(), property.name());
        }

        source.addMethod(constructor.build());
    }
}
