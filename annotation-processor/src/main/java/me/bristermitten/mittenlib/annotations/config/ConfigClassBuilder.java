package me.bristermitten.mittenlib.annotations.config;

import com.squareup.javapoet.*;
import me.bristermitten.mittenlib.annotations.util.ElementsFinder;
import me.bristermitten.mittenlib.annotations.util.Stringify;
import me.bristermitten.mittenlib.annotations.util.TypesUtil;
import me.bristermitten.mittenlib.config.Config;
import me.bristermitten.mittenlib.config.Configuration;
import me.bristermitten.mittenlib.config.GeneratedConfig;
import me.bristermitten.mittenlib.config.Source;
import me.bristermitten.mittenlib.config.generate.GenerateToString;
import me.bristermitten.mittenlib.util.Result;
import me.bristermitten.mittenlib.util.Strings;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Does most of the processing for the config annotation processor.
 * Turns a @Config annotated class into a new config class.
 */
public class ConfigClassBuilder {
    /**
     * The prefix for all generated deserialization methods.
     * For example, a method to deserialize a field called "test" would be called deserializeTest
     */
    public static final String DESERIALIZE_METHOD_PREFIX = "deserialize";
    private static final TypeName MAP_STRING_OBJ_NAME = ParameterizedTypeName.get(Map.class, String.class, Object.class);
    private static final ClassName RESULT_CLASS_NAME = ClassName.get(Result.class);
    private final ElementsFinder elementsFinder;
    private final Types types;

    private final MethodNames methodNames;
    private final TypesUtil typesUtil;
    private final ConfigurationClassNameGenerator classNameGenerator;
    private final ToStringGenerator toStringGenerator;

    private final FieldClassNameGenerator fieldClassNameGenerator;

    private final GeneratedTypeCache generatedTypeCache;

    private final DeserializationCodeGenerator deserializationCodeGenerator;
    private final ConstructorGenerator constructorGenerator;
    private final AccessorGenerator accessorGenerator;

    @Inject
    ConfigClassBuilder(ElementsFinder elementsFinder,
                       Types types,
                       MethodNames methodNames,
                       TypesUtil typesUtil,
                       ConfigurationClassNameGenerator classNameGenerator,
                       ToStringGenerator toStringGenerator,
                       FieldClassNameGenerator fieldClassNameGenerator,
                       GeneratedTypeCache generatedTypeCache,
                       DeserializationCodeGenerator deserializationCodeGenerator,
                       ConstructorGenerator constructorGenerator,
                       AccessorGenerator accessorGenerator) {
        this.elementsFinder = elementsFinder;
        this.types = types;
        this.methodNames = methodNames;
        this.typesUtil = typesUtil;
        this.classNameGenerator = classNameGenerator;
        this.toStringGenerator = toStringGenerator;
        this.fieldClassNameGenerator = fieldClassNameGenerator;
        this.generatedTypeCache = generatedTypeCache;
        this.deserializationCodeGenerator = deserializationCodeGenerator;
        this.constructorGenerator = constructorGenerator;
        this.accessorGenerator = accessorGenerator;
    }

    private FieldSpec createFieldSpec(VariableElement element) {
        return FieldSpec.builder(
                        getConfigClassName(element.asType(), element),
                        element.getSimpleName().toString()
                ).addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .build();
    }

    private ParameterSpec createParameterSpec(VariableElement element) {
        return ParameterSpec.builder(
                        getConfigClassName(element.asType(), element),
                        element.getSimpleName().toString()
                ).addModifiers(Modifier.FINAL)
                .build();
    }

    private void addInnerClasses(TypeSpec.Builder typeSpecBuilder, TypeElement classType) {
        classType.getEnclosedElements()
                .stream()
                .filter(TypeElement.class::isInstance)
                .map(TypeElement.class::cast)
                .filter(element -> element.getAnnotation(Config.class) != null)
                .forEach(typeElement -> {
                    final TypeSpec configClass = createConfigClass(typeElement,
                            elementsFinder.getApplicableVariableElements(typeElement))
                            .toBuilder()
                            .addModifiers(Modifier.STATIC)
                            .build();
                    typeSpecBuilder.addType(configClass);
                });
    }

    private TypeName getConfigClassName(TypeMirror typeMirror, @Nullable Element source) {
        return typesUtil.getConfigClassName(typeMirror, source);
    }

    private TypeSpec createConfigClass(TypeElement classType,
                                       List<VariableElement> variableElements) {
        final ClassName className =
                classNameGenerator.generateConfigurationClassName(classType)
                        .orElseThrow(() -> new IllegalArgumentException("Cannot determine name for @Config class " + classType.getQualifiedName()));

        return createConfigClass(classType, variableElements, className);
    }

    private @Nullable TypeMirror getDTOSuperclass(TypeElement dtoType) {
        var superClass = dtoType.getSuperclass();
        if (superClass.getKind() == TypeKind.NONE || superClass.toString().equals("java.lang.Object")) {
            superClass = null;
        }
        if (superClass != null && !isConfigType(superClass)) {
            throw new IllegalArgumentException("Superclass of @Config class must be a @Config class, was " + superClass);
        }
        return superClass;
    }

    private TypeSpec createConfigClass(TypeElement classType,
                                       List<VariableElement> variableElements,
                                       ClassName className) {

        var typeSpecBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(
                        AnnotationSpec.builder(GeneratedConfig.class)
                                .addMember("source", "$T.class", ClassName.get(classType))
                                .build());


        var superClass = getDTOSuperclass(classType);
        if (superClass != null) {
            // Store the super instance
            var superclassName = getConfigClassName(superClass, classType);
            typeSpecBuilder.superclass(superclassName);

            var superParamName = constructorGenerator.getSuperFieldName(superClass);
            typeSpecBuilder.addField(FieldSpec.builder(superclassName, superParamName)
                    .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                    .build()
            );
        }

        createConfigurationField(classType, className, typeSpecBuilder);

        final Map<VariableElement, FieldSpec> fieldSpecs = variableElements.stream()
                .collect(Collectors.toMap(Function.identity(), this::createFieldSpec, (x, y) -> y, LinkedHashMap::new));

        fieldSpecs.values().forEach(typeSpecBuilder::addField);
        addInnerClasses(typeSpecBuilder, classType);
        addAllArgsConstructor(variableElements, fieldSpecs.values(), typeSpecBuilder, superClass);


        createDeserializeMethod(typeSpecBuilder, classType, className, variableElements);

        // Generate getter methods
        fieldSpecs.forEach((elem, field) -> accessorGenerator.createGetterMethod(typeSpecBuilder, elem, field));

        // Generate copy setter methods
        fieldSpecs.values().forEach(field -> {
            // Create a string representing the constructor parameters
            String constructorParams = Strings.joinWith(fieldSpecs.values(),
                    f2 -> {
                        if (f2.name.equals(field.name)) {
                            return f2.name; // we'll use the version from the parameter
                        }
                        return "this." + f2.name;
                    }, ", ");

            if (superClass != null) {
                var joiner = new StringJoiner(",")
                        .add(constructorGenerator.getSuperFieldName(superClass));
                if (!constructorParams.isEmpty()) {
                    joiner.add(constructorParams);
                }
                constructorParams = joiner.toString();
            }
            typeSpecBuilder.addMethod(
                    MethodSpec.methodBuilder("with" + Strings.capitalize(field.name))
                            .addModifiers(Modifier.PUBLIC)
                            .returns(className)
                            .addParameter(ParameterSpec.builder(field.type, field.name).addModifiers(Modifier.FINAL).build())
                            .addStatement("return new $T(" + constructorParams + ")", className)
                            .build());
        });

        GenerateToString generateToString = typesUtil.getAnnotation(classType, GenerateToString.class);
        if (generateToString != null) {
            var toString = toStringGenerator.generateToString(typeSpecBuilder, className);
            typeSpecBuilder.addMethod(toString);
        }

        return typeSpecBuilder.build();
    }

    /**
     * Create a Java source file that can deserialize data described in a given DTO class
     *
     * @param classType The DTO class
     * @return A {@link JavaFile} representing the generated source file
     */
    public JavaFile createConfigFile(TypeElement classType) {
        final ClassName className =
                classNameGenerator.generateConfigurationClassName(classType)
                        .orElseThrow(() -> new IllegalArgumentException("Cannot determine name for @Config class " + classType.getQualifiedName()));

        var matchingFields =
                elementsFinder.getApplicableVariableElements(classType);

        final TypeSpec configClass = createConfigClass(classType, matchingFields, className);
        var file = JavaFile.builder(className.packageName(), configClass).build();
        generatedTypeCache.getGeneratedSpecs().put(classType, Stringify.fullyQualifiedName(file));
        return file;
    }

    private void addAllArgsConstructor(List<VariableElement> variableElements,
                                       Collection<FieldSpec> fieldSpecs,
                                       TypeSpec.Builder typeSpecBuilder,
                                       @Nullable TypeMirror superclass) {
        constructorGenerator.addAllArgsConstructor(
                variableElements,
                fieldSpecs,
                typeSpecBuilder,
                superclass,
                typeMirror -> getConfigClassName(typeMirror, variableElements.stream().findFirst().map(VariableElement::getEnclosingElement).orElse(null)),
                this::getDTOSuperclass,
                this::getFieldAccessorName
        );
    }

    private void createConfigurationField(TypeElement classType, ClassName className, TypeSpec.Builder
            typeSpecBuilder) {
        final Source annotation = classType.getAnnotation(Source.class);
        if (annotation == null) {
            return;
        }

        // Create Configuration type
        final TypeName type = ParameterizedTypeName.get(ClassName.get(Configuration.class), className);
        final FieldSpec configField = FieldSpec.builder(type, "CONFIG")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("new $T<>($S, $T.class, $T::$L)", Configuration.class, annotation.value(), className, className, getDeserializeMethodName(className))
                .build();
        typeSpecBuilder.addField(configField);
    }

    private String getFieldAccessorName(VariableElement variableElement) {
        return methodNames.safeMethodName(variableElement, (TypeElement) variableElement.getEnclosingElement());
    }

    private MethodSpec createDeserializeMethodFor(TypeElement dtoType, VariableElement element) {
        return deserializationCodeGenerator.createDeserializeMethodFor(dtoType, element);
    }

    private boolean isConfigType(TypeMirror mirror) {
        return deserializationCodeGenerator.isConfigType(mirror);
    }


    private String getDeserializeMethodName(TypeName name) {
        return deserializationCodeGenerator.getDeserializeMethodName(name);
    }

    private void createDeserializeMethod(TypeSpec.Builder typeSpecBuilder,
                                         TypeElement dtoType,
                                         ClassName className,
                                         List<VariableElement> variableElements) {
        deserializationCodeGenerator.createDeserializeMethod(
                typeSpecBuilder,
                dtoType,
                className,
                variableElements,
                this::getDTOSuperclass
        );
    }

}
