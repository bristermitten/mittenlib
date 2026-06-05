package me.bristermitten.mittenlib.annotations.compile;

import com.google.gson.reflect.TypeToken;
import com.squareup.javapoet.*;
import io.toolisticon.aptk.tools.TypeMirrorWrapper;
import io.toolisticon.aptk.tools.wrapper.TypeElementWrapper;
import me.bristermitten.mittenlib.annotations.ast.AbstractConfigStructure;
import me.bristermitten.mittenlib.annotations.ast.ConfigTypeSource;
import me.bristermitten.mittenlib.annotations.ast.Property;
import me.bristermitten.mittenlib.annotations.compile.deserializer.GenericTypeDeserializerGenerator;
import me.bristermitten.mittenlib.annotations.compile.deserializer.NonGenericTypeDeserializerGenerator;
import me.bristermitten.mittenlib.annotations.parser.CustomDeserializers;
import me.bristermitten.mittenlib.annotations.util.TypesUtil;
import me.bristermitten.mittenlib.config.DeserializationContext;
import me.bristermitten.mittenlib.config.exception.ConfigLoadingErrors;
import me.bristermitten.mittenlib.config.tree.DataTree;
import me.bristermitten.mittenlib.util.Result;
import me.bristermitten.mittenlib.util.Strings;
import org.jspecify.annotations.Nullable;

import javax.inject.Inject;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Optional;

/**
 * Generates deserialization code for configuration classes.
 * This class is responsible for creating methods that convert from JSON/YAML data
 * to strongly typed configuration objects.
 */
public class DeserializationCodeGenerator {
    /**
     * The prefix for all generated deserialization methods.
     * For example, a method to deserialize a field called "test" would be called deserializeTest
     */
    public static final String DESERIALIZE_METHOD_PREFIX = "deserialize";
    public static final ClassName RESULT_CLASS_NAME = ClassName.get(Result.class);
    final TypesUtil typesUtil;
    private final FieldNameGenerator fieldNameGenerator;
    private final ConfigurationClassNameGenerator configurationClassNameGenerator;
    private final MethodNames methodNames;
    private final GenericTypeDeserializerGenerator genericTypeDeserializerGenerator;
    private final NonGenericTypeDeserializerGenerator nonGenericTypeDeserializerGenerator;

    @Inject
    public DeserializationCodeGenerator(
            TypesUtil typesUtil,
            FieldNameGenerator fieldNameGenerator,
            ConfigurationClassNameGenerator configurationClassNameGenerator,
            MethodNames methodNames,
            GenericTypeDeserializerGenerator genericTypeDeserializerGenerator,
            NonGenericTypeDeserializerGenerator nonGenericTypeDeserializerGenerator) {
        this.typesUtil = typesUtil;
        this.fieldNameGenerator = fieldNameGenerator;
        this.configurationClassNameGenerator = configurationClassNameGenerator;
        this.methodNames = methodNames;
        this.genericTypeDeserializerGenerator = genericTypeDeserializerGenerator;
        this.nonGenericTypeDeserializerGenerator = nonGenericTypeDeserializerGenerator;
    }

    /**
     * Creates a deserialization method for a specific field in a DTO class.
     *
     * @param dtoType     The DTO class type
     * @param property    The field element to create a deserialization method for
     * @param propertyAST The AST representation of the property
     * @param daoName     The DAO class name, if applicable (can be null)
     * @return A method spec for the deserialization method
     */
    public MethodSpec createDeserializeMethodFor(TypeElement dtoType,
                                                 AbstractConfigStructure propertyAST,
                                                 Property property,
                                                 @Nullable ClassName daoName) {
        TypeMirror elementType = property.propertyType();
        TypeName elementTypeName = TypeName.get(elementType).withoutAnnotations();
        var elementResultType = configurationClassNameGenerator.publicPropertyClassName(
                typesUtil.getBoxedType(property.propertyType())
        );

        final MethodSpec.Builder builder = createDeserializeMethodBuilder(property, elementResultType, daoName);
        setupInitialStatements(builder, propertyAST, property, daoName);
        handleNullChecks(builder, property, dtoType, elementTypeName);

        TypeMirrorWrapper wrappedElementType = TypeMirrorWrapper.wrap(elementType);
        boolean isGenericType = wrappedElementType.hasTypeArguments();
        Optional<TypeElementWrapper> typeElementOpt = wrappedElementType.getTypeElement();

        if (isGenericType && typeElementOpt.isPresent()) {
            Optional<MethodSpec> methodSpec = genericTypeDeserializerGenerator.handleGenericType(builder, property, wrappedElementType, typeElementOpt.get());
            if (methodSpec.isPresent()) {
                return methodSpec.get();
            }
        } else if (!isGenericType) {
            if (nonGenericTypeDeserializerGenerator.handleNonGenericType(builder, property, dtoType, elementType, wrappedElementType)) {
                return builder.build();
            }
        }

        // If no shortcuts work, pass it to the context and do some dynamic-ish deserialization
        String fromMapName = property.name() + "FromMap";
        builder.addStatement("return context.getMapper().map($N, new $T<$T>(){})", fromMapName, TypeToken.class,
                elementResultType
        );
        return builder.build();
    }

    private MethodSpec.Builder createDeserializeMethodBuilder(Property property,
                                                               TypeName elementResultType,
                                                               @Nullable ClassName daoName) {
        final MethodSpec.Builder builder = MethodSpec.methodBuilder(DESERIALIZE_METHOD_PREFIX + Strings.capitalize(property.name()))
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Result.class), elementResultType))
                .addParameter(ParameterSpec.builder(DeserializationContext.class, "context").build());

        // add the dao as a parameter if necessary
        if (daoName != null) {
            builder.addParameter(ParameterSpec.builder(daoName, "dao", Modifier.FINAL).build());
        }

        return builder;
    }

    private void setupInitialStatements(MethodSpec.Builder builder,
                                        AbstractConfigStructure propertyAST,
                                        Property property,
                                        @Nullable ClassName daoName) {
        builder.addStatement("$T $$data = context.getData()", DataTree.class);
        final String key = fieldNameGenerator.getConfigFieldName(property);
        final String fromMapName = property.name() + "FromMap";
        if (property.settings().hasDefaultValue()) {
            if (daoName == null) {
                throw new IllegalStateException(String.format(
                        "Property %s in %s has a default value, but no DAO class name was provided to resolve it.",
                        property.name(), propertyAST.name()
                ));
            }

            var defaultString = switch (propertyAST.source()) {
                case ConfigTypeSource.InterfaceConfigTypeSource ignored -> CodeBlock.of("dao.$L()", property.name());
                case ConfigTypeSource.ClassConfigTypeSource ignored -> CodeBlock.of("dao.$L", property.name());
            };

            builder.addStatement("Object $L = $$data.getOrDefault($S, $L)", fromMapName, key, defaultString);
        } else {
            builder.addStatement("$T $L = $$data.get($S)", DataTree.class, fromMapName, key);
        }
    }

    private void handleNullChecks(MethodSpec.Builder builder,
                                  Property property,
                                  TypeElement dtoType,
                                  TypeName elementTypeName) {
        final String key = fieldNameGenerator.getConfigFieldName(property);
        final String fromMapName = property.name() + "FromMap";

        if (property.settings().isNullable()) {
            // Short circuit the null rather than trying any deserialization
            builder.beginControlFlow("if ($L == null)", fromMapName);
            builder.addStatement("return $T.ok(null)", Result.class);
            builder.endControlFlow();
        } else {
            builder.beginControlFlow("if ($L == null)", fromMapName);
            builder.addStatement("return $T.fail($T.notFoundException($S, $S, $T.class, $S))",
                    Result.class,
                    ConfigLoadingErrors.class,
                    property.name(),
                    elementTypeName,
                    dtoType,
                    key);
            builder.endControlFlow();
        }
    }

    /**
     * Creates the main deserialization method for a config class.
     *
     * @param typeSpecBuilder The builder for the config class
     */
    public void createDeserializeMethods(TypeSpec.Builder typeSpecBuilder,
                                         AbstractConfigStructure ast,
                                         @Nullable ClassName daoName
    ) {

        final MethodSpec.Builder builder = MethodSpec.methodBuilder(methodNames.getDeserializeMethodName(ast))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ParameterizedTypeName.get(RESULT_CLASS_NAME, configurationClassNameGenerator.getPublicClassName(ast)))
                .addParameter(
                        ParameterSpec.builder(DeserializationContext.class, "context", Modifier.FINAL).build()
                );

        if (ast instanceof AbstractConfigStructure.Union union) {
            // union deserialization is very different
            CodeBlock.Builder deserialiseBuilder = CodeBlock.builder();
            deserialiseBuilder.add("return ");
            for (AbstractConfigStructure alternative : union.alternatives()) {
                ClassName alternativeClassName = configurationClassNameGenerator.translateConfigClassName(alternative);
                String deserializeMethodName = methodNames.getDeserializeMethodName(alternativeClassName);

                deserialiseBuilder.add("$T.$L(context).map($T.class::cast).orElse(() -> \n",
                        alternativeClassName,
                        deserializeMethodName,
                        configurationClassNameGenerator.getPublicClassName(ast));
                deserialiseBuilder.indent();
            }
            deserialiseBuilder.add("$T.fail($T.noUnionMatch())", Result.class, ConfigLoadingErrors.class);
            deserialiseBuilder.add(")".repeat(union.alternatives().size())); // close all the flatMap parens

            builder.addStatement(deserialiseBuilder.build());

            typeSpecBuilder.addMethod(builder.build());
            return;
        }
        var dtoType = ast.source().element();

        if (daoName != null) {
            builder.addStatement("$1T dao = new $1T()", daoName);
        }

        final List<MethodSpec> deserializeMethods = ast.properties().stream()
                .map(variableElement -> createDeserializeMethodFor(dtoType, ast, variableElement, daoName))
                .toList();

        deserializeMethods.forEach(typeSpecBuilder::addMethod);

        final CodeBlock.Builder expressionBuilder = CodeBlock.builder();

        expressionBuilder.add("return ");
        int i = 0;

        var superClass = switch (ast.source()) {
            case ConfigTypeSource.ClassConfigTypeSource c -> c.parent();
            case ConfigTypeSource.InterfaceConfigTypeSource ignored -> Optional.<TypeMirror>empty();//for now
        };

        // Add the superclass deserialization first, if it exists
        if (superClass.isPresent()) {
            var superConfigName = getConfigClassName(superClass.get(), dtoType);
            expressionBuilder.add("$T.$L", superConfigName, methodNames.getDeserializeMethodName(superConfigName));
            expressionBuilder.add("(context).flatMap(var$L -> \n", i++);
        }
        var deserialiseMethodArguments = (daoName != null) ? "context, dao" : "context";
        for (MethodSpec deserializeMethod : deserializeMethods) {
            expressionBuilder.add("$N($L).flatMap(var$L -> \n", deserializeMethod, deserialiseMethodArguments, i++);
        }
        expressionBuilder.add("$T.ok(new $T(", Result.class, configurationClassNameGenerator.translateConfigClassName(ast));
        for (int i1 = 0; i1 < i; i1++) {
            expressionBuilder.add("var$L", i1);
            if (i1 != i - 1) {
                expressionBuilder.add(", ");
            }
        }
        expressionBuilder.add("))"); // Close ok and new parens
        expressionBuilder.add(")".repeat(Math.max(0, i))); // close all the flatMap parens

        builder.addStatement(expressionBuilder.build());

        typeSpecBuilder.addMethod(builder.build());
    }

    /**
     * Gets the config class name for a type.
     *
     * @param typeMirror The type
     * @param source     The source element (can be null)
     * @return The config class name
     */
    private TypeName getConfigClassName(TypeMirror typeMirror, @Nullable Element source) {
        return configurationClassNameGenerator.getConfigClassName(typeMirror, source);
    }

}

