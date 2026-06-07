package me.bristermitten.mittenlib.annotations.compile;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.ProvidesIntoSet;
import com.squareup.javapoet.*;
import me.bristermitten.mittenlib.annotations.ast.AbstractConfigStructure;
import me.bristermitten.mittenlib.config.ConfigModule;
import me.bristermitten.mittenlib.config.Configuration;
import me.bristermitten.mittenlib.config.DeserializationFunction;
import me.bristermitten.mittenlib.config.SerializationFunction;
import me.bristermitten.mittenlib.config.provider.ConfigProvider;
import me.bristermitten.mittenlib.config.provider.construct.ConfigProviderFactory;
import me.bristermitten.mittenlib.config.provider.construct.ConfigProviderImprover;

import javax.annotation.processing.Generated;
import javax.inject.Inject;
import javax.lang.model.element.Modifier;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

public class ConfigLoaderModuleGenerator {
    private final ConfigurationClassNameGenerator classNameGenerator;

    @Inject
    public ConfigLoaderModuleGenerator(ConfigurationClassNameGenerator classNameGenerator) {
        this.classNameGenerator = classNameGenerator;
    }

    public JavaFile emit(List<AbstractConfigStructure> asts) {
        if (asts.isEmpty()) {
            throw new IllegalArgumentException("asts list cannot be empty");
        }

        ClassName moduleClassName = classNameGenerator.getLoaderModuleClassName(
                classNameGenerator.getPublicClassName(asts.getFirst()).packageName()
        );

        TypeSpec.Builder builder = TypeSpec.classBuilder(moduleClassName)
                .addModifiers(Modifier.PUBLIC)
                .superclass(AbstractModule.class)
                .addAnnotation(AnnotationSpec.builder(Generated.class)
                        .addMember("value", "$S", "me.bristermitten.mittenlib.annotations.config.ConfigProcessor")
                        .addMember("date", "$S", ZonedDateTime.now(ZoneId.of("UTC")).toString())
                        .build());

        MethodSpec.Builder configureMethod = MethodSpec.methodBuilder("configure")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .addStatement("install(new $T())", ConfigModule.class);

        // Bindings for functions
        for (AbstractConfigStructure ast : asts) {
            addFunctionBindings(configureMethod, ast);
        }

        builder.addMethod(configureMethod.build());

        // Provides methods for providers and configs
        for (AbstractConfigStructure ast : asts) {
            addProvidesMethods(builder, ast);
        }

        return JavaFile.builder(moduleClassName.packageName(), builder.build()).build();
    }

    private void addFunctionBindings(MethodSpec.Builder configureMethod, AbstractConfigStructure ast) {
        ClassName publicClassName = classNameGenerator.getPublicClassName(ast);
        ClassName loaderClassName = classNameGenerator.getLoaderClassName(ast);
        ClassName saverClassName = classNameGenerator.getSaverClassName(ast);

        // Bind DeserializationFunction<Public> -> Loader
        configureMethod.addStatement(
                "bind(new $1T<$2T<$3T>>() {}).to($4T.class)",
                TypeLiteral.class, DeserializationFunction.class, publicClassName, loaderClassName
        );

        // Bind SerializationFunction<Public> -> Saver
        configureMethod.addStatement(
                "bind(new $1T<$2T<$3T>>() {}).to($4T.class)",
                TypeLiteral.class, SerializationFunction.class, publicClassName, saverClassName
        );

        if (ast.needsValidation()) {
            ClassName validatorClassName = classNameGenerator.getValidatorClassName(ast);
            configureMethod.addStatement("bind($T.class)", validatorClassName);
        }

        for (AbstractConfigStructure enclosed : ast.enclosed()) {
            addFunctionBindings(configureMethod, enclosed);
        }
    }

    private void addProvidesMethods(TypeSpec.Builder builder, AbstractConfigStructure ast) {
        if (ast.settings().source() == null) {
             for (AbstractConfigStructure enclosed : ast.enclosed()) {
                addProvidesMethods(builder, enclosed);
            }
            return;
        }

        ClassName publicClassName = classNameGenerator.getPublicClassName(ast);
        ClassName implClassName = classNameGenerator.translateConfigClassName(ast);
        String name = publicClassName.simpleName();

        // @Provides ConfigProvider<Public>
        MethodSpec.Builder providerMethod = MethodSpec.methodBuilder(classNameGenerator.getProvidesProviderMethodName(name))
                .addAnnotation(Provides.class)
                .addAnnotation(Singleton.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(ConfigProvider.class), publicClassName))
                .addParameter(ConfigProviderFactory.class, "factory")
                .addParameter(ConfigProviderImprover.class, "improver")
                .addParameter(ParameterizedTypeName.get(ClassName.get(DeserializationFunction.class), publicClassName), "deserializer")
                .addParameter(ParameterizedTypeName.get(ClassName.get(SerializationFunction.class), publicClassName), "serializer")
                .addStatement("return improver.improve(factory.createProvider($T.CONFIG, deserializer, serializer).getOrThrow())",
                        implClassName);

        builder.addMethod(providerMethod.build());

        // @Provides Public
        MethodSpec.Builder configMethod = MethodSpec.methodBuilder(classNameGenerator.getProvidesMethodName(name))
                .addAnnotation(Provides.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(publicClassName)
                .addParameter(ParameterizedTypeName.get(ClassName.get(ConfigProvider.class), publicClassName), "provider")
                .addStatement("return provider.get()");

        builder.addMethod(configMethod.build());

        // Multibinder registrations
        
        MethodSpec.Builder configMultibinder = MethodSpec.methodBuilder(classNameGenerator.getProvidesToConfigSetMethodName(name))
                .addAnnotation(ProvidesIntoSet.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Configuration.class), WildcardTypeName.subtypeOf(Object.class)))
                .addStatement("return $T.CONFIG", implClassName);
        builder.addMethod(configMultibinder.build());

        MethodSpec.Builder providerMultibinder = MethodSpec.methodBuilder(classNameGenerator.getProvidesToProviderSetMethodName(name))
                .addAnnotation(ProvidesIntoSet.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(ConfigProvider.class), WildcardTypeName.subtypeOf(Object.class)))
                .addParameter(ParameterizedTypeName.get(ClassName.get(ConfigProvider.class), publicClassName), "provider")
                .addStatement("return provider");
        builder.addMethod(providerMultibinder.build());

        for (AbstractConfigStructure enclosed : ast.enclosed()) {
            addProvidesMethods(builder, enclosed);
        }
    }
}
