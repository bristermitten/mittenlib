package me.bristermitten.mittenlib.annotations.compile;

import com.squareup.javapoet.*;
import me.bristermitten.mittenlib.annotations.ast.AbstractConfigStructure;
import me.bristermitten.mittenlib.annotations.ast.ConfigTypeSource;
import me.bristermitten.mittenlib.annotations.ast.Property;
import me.bristermitten.mittenlib.annotations.parser.CustomSerializers;
import me.bristermitten.mittenlib.annotations.util.TypesUtil;
import me.bristermitten.mittenlib.config.SerializationContext;
import me.bristermitten.mittenlib.config.SerializationFunction;
import me.bristermitten.mittenlib.config.reader.ObjectMapper;
import me.bristermitten.mittenlib.config.tree.DataTree;
import me.bristermitten.mittenlib.util.Strings;

import java.util.Optional;
import javax.lang.model.element.TypeElement;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigSaverGenerator {
    private final ConfigurationClassNameGenerator classNameGenerator;
    private final SerializationCodeGenerator serializationCodeGenerator;
    private final MethodNames methodNames;
    private final TypesUtil typesUtil;
    private final FieldNameGenerator fieldNameGenerator;
    private final ConfigNameCache configNameCache;
    private final CustomSerializers customSerializers;

    @Inject
    public ConfigSaverGenerator(
            ConfigurationClassNameGenerator classNameGenerator,
            SerializationCodeGenerator serializationCodeGenerator,
            MethodNames methodNames,
            TypesUtil typesUtil,
            FieldNameGenerator fieldNameGenerator,
            ConfigNameCache configNameCache,
            CustomSerializers customSerializers) {
        this.classNameGenerator = classNameGenerator;
        this.serializationCodeGenerator = serializationCodeGenerator;
        this.methodNames = methodNames;
        this.typesUtil = typesUtil;
        this.fieldNameGenerator = fieldNameGenerator;
        this.configNameCache = configNameCache;
        this.customSerializers = customSerializers;
    }

    public JavaFile emit(AbstractConfigStructure ast) {
        ClassName saverClassName = classNameGenerator.getSaverClassName(ast);
        TypeSpec.Builder builder = createSaverBuilder(ast);

        return JavaFile.builder(saverClassName.packageName(), builder.build()).build();
    }

    private TypeSpec.Builder createSaverBuilder(AbstractConfigStructure ast) {
        ClassName publicClassName = classNameGenerator.getPublicClassName(ast);
        ClassName saverClassName = classNameGenerator.getSaverClassName(ast);

        TypeSpec.Builder builder = TypeSpec.classBuilder(saverClassName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ParameterizedTypeName.get(ClassName.get(SerializationFunction.class), publicClassName));

        MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder()
                .addAnnotation(Inject.class)
                .addModifiers(Modifier.PUBLIC);

        // Add child savers as dependencies recursively
        for (Property property : ast.properties()) {
            collectSaverDependencies(property.propertyType(), builder, constructorBuilder);
        }

        // Add custom serializers as dependencies recursively
        java.util.Set<TypeName> injectedTypes = new java.util.LinkedHashSet<>();
        java.util.Map<TypeName, String> injectedFieldNames = new java.util.LinkedHashMap<>();
        for (Property property : ast.properties()) {
            collectCustomSerializers(property.propertyType(), injectedTypes, injectedFieldNames);
        }

        for (TypeName typeName : injectedTypes) {
            String fieldName = injectedFieldNames.get(typeName);
            TypeName providerType = ParameterizedTypeName.get(ClassName.get(Provider.class), typeName);
            builder.addField(FieldSpec.builder(providerType, fieldName, Modifier.PRIVATE, Modifier.FINAL).build());
            constructorBuilder.addParameter(providerType, fieldName);
            constructorBuilder.addStatement("this.$L = $L", fieldName, fieldName);
        }

        builder.addMethod(constructorBuilder.build());

        // Implement apply method
        MethodSpec.Builder applyMethod = MethodSpec.methodBuilder("apply")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(DataTree.class)
                .addParameter(publicClassName, "config")
                .addParameter(SerializationContext.class, "context");

        applyMethod.addStatement("$T<$T, $T> map = new $T<>()",
                Map.class, DataTree.class, DataTree.class, LinkedHashMap.class);

        for (Property property : ast.properties()) {
            String key = fieldNameGenerator.getConfigFieldName(property);
            String serializeMethodName = SerializationCodeGenerator.SERIALIZE_METHOD_PREFIX + Strings.capitalize(property.name());

            // Get the property value based on source type
            CodeBlock propertyAccess = switch (ast.source()) {
                case ConfigTypeSource.InterfaceConfigTypeSource ignored -> CodeBlock.of("config.$L()", property.name());
                case ConfigTypeSource.ClassConfigTypeSource ignored ->
                        CodeBlock.of("config.$L()", methodNames.safeMethodName(property));
            };

            applyMethod.addStatement("map.put($T.string($S), this.$L($L, context))",
                    DataTree.class,
                    key,
                    serializeMethodName,
                    propertyAccess);
        }

        applyMethod.addStatement("return $T.map(map)", DataTree.class);
        builder.addMethod(applyMethod.build());

        // Add private serialize methods for each property (copied from SerializationCodeGenerator but adapted)
        serializationCodeGenerator.addSerializeMethodsToSaver(builder, ast);

        // Add nested classes
        for (AbstractConfigStructure enclosed : ast.enclosed()) {
            builder.addType(createSaverBuilder(enclosed).addModifiers(Modifier.STATIC).build());
        }

        return builder;
    }

    private void addSaverDependency(TypeSpec.Builder builder, MethodSpec.Builder constructorBuilder, TypeMirror type) {
        AbstractConfigStructure ast = configNameCache.lookupAST(type).orElse(null);
        if (ast == null) return;
        
        ClassName publicChildClassName = classNameGenerator.getPublicClassName(ast);
        String fieldName = classNameGenerator.getSaverFieldName(type) + "Provider";

        if (builder.fieldSpecs.stream().anyMatch(f -> f.name.equals(fieldName))) {
            return;
        }

        builder.addField(ParameterizedTypeName.get(ClassName.get(Provider.class),
                ParameterizedTypeName.get(ClassName.get(SerializationFunction.class), publicChildClassName)),
                fieldName, Modifier.PRIVATE, Modifier.FINAL);

        constructorBuilder.addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class),
                ParameterizedTypeName.get(ClassName.get(SerializationFunction.class), publicChildClassName)),
                fieldName);
        constructorBuilder.addStatement("this.$L = $L", fieldName, fieldName);
    }

    /**
     * Recursively traverses generic type arguments of a property's type to discover
     * configuration savers that need to be injected into the generated saver as dependencies.
     *
     * @param type               the property type (or component/argument type) to inspect
     * @param builder            the TypeSpec builder of the saver class
     * @param constructorBuilder the constructor builder of the saver class
     */
    private void collectSaverDependencies(TypeMirror type, TypeSpec.Builder builder, MethodSpec.Builder constructorBuilder) {
        if (typesUtil.isConfigType(type)) {
            addSaverDependency(builder, constructorBuilder, type);
            return;
        }

        io.toolisticon.aptk.tools.TypeMirrorWrapper wrapped = io.toolisticon.aptk.tools.TypeMirrorWrapper.wrap(type);
        if (wrapped.hasTypeArguments()) {
            for (TypeMirror arg : wrapped.getTypeArguments()) {
                collectSaverDependencies(arg, builder, constructorBuilder);
            }
        }
    }

    /**
     * Recursively traverses generic type arguments of a property's type to discover
     * non-static custom serializers that need to be injected into the generated saver.
     *
     * @param type               the property type (or component/argument type) to inspect
     * @param injectedTypes      the set of already registered injected Types to add to
     * @param injectedFieldNames the mapping of injected types to their corresponding field names
     */
    private void collectCustomSerializers(TypeMirror type, java.util.Set<TypeName> injectedTypes, java.util.Map<TypeName, String> injectedFieldNames) {
        customSerializers.getCustomSerializer(type).ifPresent(info -> {
            if (!info.isStatic()) {
                TypeElement serializerClass = info.serializerClass();
                ClassName serializerClassName = ClassName.get(serializerClass);
                String fieldName = me.bristermitten.mittenlib.util.Strings.uncapitalize(serializerClass.getSimpleName().toString()) + "Provider";
                if (injectedTypes.add(serializerClassName)) {
                    injectedFieldNames.put(serializerClassName, fieldName);
                }
            }
        });

        io.toolisticon.aptk.tools.TypeMirrorWrapper wrapped = io.toolisticon.aptk.tools.TypeMirrorWrapper.wrap(type);
        if (wrapped.hasTypeArguments()) {
            for (TypeMirror arg : wrapped.getTypeArguments()) {
                collectCustomSerializers(arg, injectedTypes, injectedFieldNames);
            }
        }
    }
}
