package me.bristermitten.mittenlib.annotations.compile;

import com.google.inject.*;
import com.google.inject.multibindings.ProvidesIntoSet;
import com.squareup.javapoet.*;
import io.toolisticon.aptk.tools.MessagerUtils;
import me.bristermitten.mittenlib.annotations.ast.AbstractConfigStructure;
import me.bristermitten.mittenlib.annotations.ast.Property;
import me.bristermitten.mittenlib.config.ConfigModule;
import me.bristermitten.mittenlib.config.Configuration;
import me.bristermitten.mittenlib.config.DeserializationFunction;
import me.bristermitten.mittenlib.config.SerializationFunction;
import me.bristermitten.mittenlib.config.provider.ConfigProvider;
import me.bristermitten.mittenlib.config.provider.construct.ConfigProviderFactory;
import me.bristermitten.mittenlib.config.provider.construct.ConfigProviderImprover;
import org.jspecify.annotations.Nullable;

import javax.annotation.processing.Generated;
import javax.lang.model.element.Modifier;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

public class ConfigLoaderModuleGenerator {
    private final ConfigurationClassNameGenerator classNameGenerator;
    private final MethodNames methodNames;

    @Inject
    public ConfigLoaderModuleGenerator(ConfigurationClassNameGenerator classNameGenerator, MethodNames methodNames) {
        this.classNameGenerator = classNameGenerator;
        this.methodNames = methodNames;
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
            addProvidesMethods(builder, ast, null, false);
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

    private void addProvidesMethods(TypeSpec.Builder builder, AbstractConfigStructure ast, @Nullable AbstractConfigStructure parent, boolean isParentProvided) {
        ClassName publicClassName = classNameGenerator.getPublicClassName(ast);
        boolean isCurrentProvided = false;

        if (ast.settings().source() != null) {
            isCurrentProvided = true;
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

            MethodSpec.Builder configMultiBinder = MethodSpec.methodBuilder(classNameGenerator.getProvidesToConfigSetMethodName(name))
                    .addAnnotation(ProvidesIntoSet.class)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(ParameterizedTypeName.get(ClassName.get(Configuration.class), WildcardTypeName.subtypeOf(Object.class)))
                    .addStatement("return $T.CONFIG", implClassName);
            builder.addMethod(configMultiBinder.build());

            MethodSpec.Builder providerMultiBinder = MethodSpec.methodBuilder(classNameGenerator.getProvidesToProviderSetMethodName(name))
                    .addAnnotation(ProvidesIntoSet.class)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(ParameterizedTypeName.get(ClassName.get(ConfigProvider.class), WildcardTypeName.subtypeOf(Object.class)))
                    .addParameter(ParameterizedTypeName.get(ClassName.get(ConfigProvider.class), publicClassName), "provider")
                    .addStatement("return provider");
            builder.addMethod(providerMultiBinder.build());
        } else if (isParentProvided && parent != null) {
            isCurrentProvided = addNestedProvidesMethod(builder, parent, ast);
        }

        for (AbstractConfigStructure enclosed : ast.enclosed()) {
            addProvidesMethods(builder, enclosed, ast, isCurrentProvided);
        }
    }

    private boolean addNestedProvidesMethod(TypeSpec.Builder builder, AbstractConfigStructure parent, AbstractConfigStructure child) {
        ClassName parentPublicName = classNameGenerator.getPublicClassName(parent);
        ClassName childPublicName = classNameGenerator.getPublicClassName(child);

        List<Property> matchingProperties = parent.properties().stream()
                .filter(property -> classNameGenerator.publicPropertyClassName(property).equals(childPublicName))
                .toList();

        if (matchingProperties.isEmpty()) {
            return false;
        }

        Property propertyToBind;
        if (matchingProperties.size() == 1) {
            propertyToBind = matchingProperties.get(0);
        } else {
            // Check for @BindProperty
            List<Property> explicitBindings = matchingProperties.stream()
                    .filter(p -> p.source().element().getAnnotation(me.bristermitten.mittenlib.config.BindProperty.class) != null)
                    .toList();

            if (explicitBindings.size() == 1) {
                propertyToBind = explicitBindings.get(0);
            } else {
                if (explicitBindings.size() > 1) {
                    for (Property explicitBinding : explicitBindings) {
                        MessagerUtils.error(explicitBinding.source().element(),
                                "Multiple properties of type " + childPublicName.simpleName() + " are marked with @BindProperty. Only one can be bound to the type in Guice.");
                    }
                }
                return false;
            }
        }

        String name = childPublicName.simpleName();
        MethodSpec.Builder configMethod = MethodSpec.methodBuilder(classNameGenerator.getProvidesMethodName(name))
                .addAnnotation(Provides.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(childPublicName)
                .addParameter(parentPublicName, "parent")
                .addStatement("return parent.$L()", methodNames.safeMethodName(propertyToBind));
        builder.addMethod(configMethod.build());
        return true;
    }
}
