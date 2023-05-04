package me.bristermitten.mittenlib.annotations.config

import com.google.gson.reflect.TypeToken
import com.squareup.javapoet.*
import me.bristermitten.mittenlib.annotations.util.ElementsFinder
import me.bristermitten.mittenlib.annotations.util.Stringify
import me.bristermitten.mittenlib.annotations.util.TypesUtil
import me.bristermitten.mittenlib.config.*
import me.bristermitten.mittenlib.config.generate.GenerateToString
import me.bristermitten.mittenlib.util.Result
import me.bristermitten.mittenlib.util.Strings
import org.jetbrains.annotations.Contract
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import java.util.*
import java.util.function.Consumer
import javax.inject.Inject
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Types

/**
 * Does most of the processing for the config annotation processor.
 * Turns a @Config annotated class into a new config class.
 */
class ConfigClassBuilder @Inject internal constructor(
    private val elementsFinder: ElementsFinder,
    private val types: Types,
    private val methodNames: MethodNames,
    private val typesUtil: TypesUtil,
    private val classNameGenerator: ConfigurationClassNameGenerator,
    private val toStringGenerator: ToStringGenerator,
    private val fieldClassNameGenerator: FieldClassNameGenerator, private val generatedTypeCache: GeneratedTypeCache
) {
    private fun createFieldSpec(element: VariableElement?): FieldSpec {
        return FieldSpec.builder(
            getConfigClassName(element!!.asType(), element),
            element.simpleName.toString()
        ).addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .build()
    }

    private fun createParameterSpec(element: VariableElement?): ParameterSpec {
        return ParameterSpec.builder(
            getConfigClassName(element!!.asType(), element),
            element.simpleName.toString()
        ).addModifiers(Modifier.FINAL)
            .build()
    }

    private fun addInnerClasses(typeSpecBuilder: TypeSpec.Builder, classType: TypeElement) {
        classType.enclosedElements
            .filterIsInstance<TypeElement>()
            .filter { element: TypeElement ->
                element.getAnnotation(
                    Config::class.java
                ) != null
            }
            .forEach { typeElement: TypeElement ->
                var configClass = createConfigClass(
                    typeElement,
                    elementsFinder.getApplicableVariableElements(typeElement)
                )
                configClass = configClass.toBuilder().addModifiers(Modifier.STATIC).build()
                typeSpecBuilder.addType(configClass)
            }
    }

    private fun getConfigClassName(typeMirror: TypeMirror?, source: Element?): TypeName {
        return typesUtil.getConfigClassName(typeMirror, source)
    }

    private fun createGetterMethod(typeSpecBuilder: TypeSpec.Builder, element: VariableElement, field: FieldSpec) {
        val safeName = getFieldAccessorName(element)
        val builder = MethodSpec.methodBuilder(safeName)
            .addModifiers(Modifier.PUBLIC)
            .returns(field.type)
            .addStatement("return " + field.name)
        if (typesUtil.isNullable(element)) {
            builder.addAnnotation(Nullable::class.java)
        } else {
            builder.addAnnotation(NotNull::class.java)
        }
        builder.addAnnotation(
            AnnotationSpec.builder(Contract::class.java)
                .addMember("pure", CodeBlock.of("true")).build()
        )
        typeSpecBuilder.addMethod(builder.build())
    }

    private fun createConfigClass(
        classType: TypeElement,
        variableElements: List<VariableElement>
    ): TypeSpec {
        val className = classNameGenerator.generateConfigurationClassName(classType)
            ?: throw IllegalArgumentException("Cannot determine name for @Config class " + classType.qualifiedName)
        return createConfigClass(classType, variableElements, className)
    }

    private fun getDTOSuperclass(dtoType: TypeElement): TypeMirror? {
        var superClass = dtoType.superclass
        if (superClass!!.kind == TypeKind.NONE || superClass.toString() == "java.lang.Object") {
            superClass = null
        }
        require(!(superClass != null && !isConfigType(superClass))) { "Superclass of @Config class must be a @Config class, was $superClass" }
        return superClass
    }

    private fun getSuperFieldName(superClass: TypeMirror): String {
        val configName = typesUtil.getConfigClassName(superClass)
        return "parent" + Strings.capitalize(typesUtil.getSimpleName(configName))
    }

    private fun createConfigClass(
        classType: TypeElement,
        variableElements: List<VariableElement>,
        className: ClassName?
    ): TypeSpec {
        val typeSpecBuilder = TypeSpec.classBuilder(className)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(
                AnnotationSpec.builder(GeneratedConfig::class.java)
                    .addMember("source", "\$T.class", ClassName.get(classType))
                    .build()
            )
        val superClass = getDTOSuperclass(classType)
        if (superClass != null) {
            // Store the super instance
            val superclassName = getConfigClassName(superClass, classType)
            typeSpecBuilder.superclass(superclassName)
            val superParamName = getSuperFieldName(superClass)
            typeSpecBuilder.addField(
                FieldSpec.builder(superclassName, superParamName)
                    .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                    .build()
            )
        }
        createConfigurationField(classType, className, typeSpecBuilder)
        val fieldSpecs: Map<VariableElement, FieldSpec> = variableElements.associateWith { createFieldSpec(it) }

        fieldSpecs.values.forEach(Consumer { fieldSpec: FieldSpec? -> typeSpecBuilder.addField(fieldSpec) })
        addInnerClasses(typeSpecBuilder, classType)
        addAllArgsConstructor(variableElements, fieldSpecs.values, typeSpecBuilder, superClass)
        createDeserializeMethod(typeSpecBuilder, classType, className, variableElements)

        // Generate getter methods
        fieldSpecs.forEach { (elem: VariableElement, field: FieldSpec) ->
            createGetterMethod(
                typeSpecBuilder,
                elem,
                field
            )
        }

        // Generate copy setter methods
        fieldSpecs.values.forEach(Consumer<FieldSpec> { field: FieldSpec ->
            // Create a string representing the constructor parameters
            var constructorParams = Strings.joinWith(
                fieldSpecs.values,
                { f2: FieldSpec ->
                    if (f2.name == field.name) {
                        return@joinWith f2.name // we'll use the version from the parameter
                    }
                    "this." + f2.name
                }, ", "
            )
            if (superClass != null) {
                val joiner = StringJoiner(",")
                    .add(getSuperFieldName(superClass))
                if (!constructorParams.isEmpty()) {
                    joiner.add(constructorParams)
                }
                constructorParams = joiner.toString()
            }
            typeSpecBuilder.addMethod(
                MethodSpec.methodBuilder("with" + Strings.capitalize(field.name))
                    .addModifiers(Modifier.PUBLIC)
                    .returns(className)
                    .addParameter(ParameterSpec.builder(field.type, field.name).addModifiers(Modifier.FINAL).build())
                    .addStatement("return new \$T($constructorParams)", className)
                    .build()
            )
        })
        val generateToString = typesUtil.getAnnotation(classType, GenerateToString::class.java)
        if (generateToString != null) {
            val toString = toStringGenerator.generateToString(typeSpecBuilder, className)
            typeSpecBuilder.addMethod(toString)
        }
        return typeSpecBuilder.build()
    }

    /**
     * Create a Java source file that can deserialize data described in a given DTO class
     *
     * @param classType The DTO class
     * @return A [JavaFile] representing the generated source file
     */
    fun createConfigFile(classType: TypeElement): JavaFile {
        val className = classNameGenerator.generateConfigurationClassName(classType)
            ?: throw IllegalArgumentException("Cannot determine name for @Config class " + classType.qualifiedName)
        val matchingFields = elementsFinder.getApplicableVariableElements(classType)
        val configClass = createConfigClass(classType, matchingFields, className)
        val file = JavaFile.builder(className.packageName(), configClass).build()
        generatedTypeCache.generatedSpecs[classType] = Stringify.stringify(file)
        return file
    }

    private fun addAllArgsConstructor(
        variableElements: List<VariableElement?>?,
        fieldSpecs: Collection<FieldSpec>,
        typeSpecBuilder: TypeSpec.Builder,
        superclass: TypeMirror?
    ) {
        val constructorBuilder = MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
        if (superclass != null) {
            val superclassName = getConfigClassName(
                superclass,
                variableElements!!.stream().findFirst().map { obj: VariableElement? -> obj!!.enclosingElement }
                    .orElse(null))
            val superParamName = getSuperFieldName(superclass)
            val parameter = ParameterSpec.builder(superclassName, superParamName)
                .addModifiers(Modifier.FINAL)
                .build()
            constructorBuilder.addParameter(parameter)
            val superElements = elementsFinder.getApplicableVariableElements(superclass)
            var collect = superElements.stream()
                .map { obj: VariableElement? -> VariableElement::class.java.cast(obj) }
                .map { variableElement: VariableElement -> superParamName + "." + getFieldAccessorName(variableElement) + "()" }
                .toList()
            if (getDTOSuperclass(types.asElement(superclass) as TypeElement) != null) {
                val newCollect = ArrayList(collect)
                newCollect.add(0, superParamName)
                collect = newCollect
            }
            constructorBuilder.addStatement("super(\$L)", java.lang.String.join(", ", collect))
            constructorBuilder.addStatement("this.\$L = \$L", superParamName, superParamName)
        }
        constructorBuilder.addParameters(
            variableElements!!.stream()
                .map { element: VariableElement? -> createParameterSpec(element) }
                .toList())
        fieldSpecs.forEach(Consumer { field: FieldSpec ->
            constructorBuilder.addStatement(
                "this.\$N = \$N",
                field,
                field.name
            )
        })
        typeSpecBuilder.addMethod(constructorBuilder.build())
    }

    private fun createConfigurationField(
        classType: TypeElement,
        className: ClassName?,
        typeSpecBuilder: TypeSpec.Builder
    ) {
        val annotation = classType.getAnnotation(
            Source::class.java
        ) ?: return

        // Create Configuration type
        val type: TypeName = ParameterizedTypeName.get(ClassName.get(Configuration::class.java), className)
        val configField = FieldSpec.builder(type, "CONFIG")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
            .initializer(
                "new \$T<>(\$S, \$T.class, \$T::\$L)",
                Configuration::class.java,
                annotation.value,
                className,
                className,
                getDeserializeMethodName(className)
            )
            .build()
        typeSpecBuilder.addField(configField)
    }

    private fun getFieldAccessorName(variableElement: VariableElement): String {
        return methodNames.safeMethodName(variableElement, variableElement.enclosingElement as TypeElement)
    }

    private fun createDeserializeMethodFor(dtoType: TypeElement, element: VariableElement): MethodSpec {
        val typeMirror = element.asType()
        val elementType = getConfigClassName(typeMirror, dtoType)
        val variableName = element.simpleName
        val boxedType = typesUtil.getBoxedType(typeMirror)
        val boxedTypeName = getConfigClassName(boxedType, dtoType)
        val builder = MethodSpec.methodBuilder(DESERIALIZE + Strings.capitalize(variableName.toString()))
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .returns(ParameterizedTypeName.get(RESULT_CLASS_NAME, boxedTypeName))
            .addParameter(ParameterSpec.builder(DeserializationContext::class.java, "context").build())
            .addParameter(ParameterSpec.builder(ClassName.get(dtoType), "dao").build())
        builder.addStatement("\$T $\$data = context.getData()", MAP_STRING_OBJ_NAME)
        builder.addStatement("\$T \$L", elementType, variableName)
        val fromMapName = variableName.toString() + "FromMap"
        val key = fieldClassNameGenerator.getConfigFieldName(element)
        val defaultName = element.simpleName
        builder.addStatement("Object \$L = $\$data.getOrDefault(\$S, dao.\$L)", fromMapName, key, defaultName)
        if (typesUtil.isNullable(element)) {
            // Short circuit the null rather than trying any deserialization
            builder.beginControlFlow("if (\$L == null)", fromMapName)
            builder.addStatement("return \$T.ok(null)", Result::class.java)
            builder.endControlFlow()
        } else {
            builder.beginControlFlow("if (\$L == null)", fromMapName)
            builder.addStatement(
                "return \$T.fail(\$T.throwNotFound(\$S, \$S, \$T.class, \$L))",
                Result::class.java, ConfigMapLoader::class.java, variableName, element, dtoType, fromMapName
            )
            builder.endControlFlow()
        }
        if (elementType !is ParameterizedTypeName) {
            /*
             Construct a simple check that does
               if (fromMap instanceof X) return fromMap; NOSONAR this is not code you stupid program
             Useful when the type is a primitive or String
             This is only safe to do with non-parameterized types, type erasure and all
            */
            val safeType = typesUtil.getSafeType(typeMirror)
            val safeTypeName = getConfigClassName(safeType, dtoType)
            builder.beginControlFlow("if (\$L instanceof \$T)", fromMapName, safeTypeName)
            builder.addStatement("return \$T.ok((\$T) \$L)", Result::class.java, elementType, fromMapName)
            builder.endControlFlow()
        } else if (typeMirror is DeclaredType) {
            /*
             This is really cursed, but it's the only real way
             When the type is a List<T> or Map<_, T> then we need to first load it as a C<Map<String, Object>> then
             apply the deserialize function to each element. Otherwise, gson would try to deserialize it without
             using the generated deserialization method and produce inconsistent results.

             However, we can't just blindly do this for every type - there's no way of knowing how to convert a
             Blah<A> into a Blah<B> without some knowledge of the underlying structure.
             Map and List are the most common collection types, but this could really use some extensibility.
            */
            val canonicalName = types.erasure(typeMirror).toString()
            if (canonicalName == JAVA_UTIL_LIST) {
                val listType: TypeMirror = typeMirror.typeArguments[0]
                if (isConfigType(listType)) {
                    val listTypeName = getConfigClassName(listType, null)
                    builder.addStatement(
                        "return \$T.deserializeList(\$L, context, \$T::\$L)", CollectionsUtils::class.java,
                        fromMapName,
                        listTypeName,
                        getDeserializeMethodName(listTypeName)
                    )
                    return builder.build()
                }
            }
            if (canonicalName == JAVA_UTIL_MAP) {
                val mapType: TypeMirror = typeMirror.typeArguments[1]
                val keyType: TypeMirror = typeMirror.typeArguments[0]
                if (isConfigType(mapType)) {
                    val mapTypeName = getConfigClassName(mapType, null)
                    builder.addStatement(
                        "return \$T.deserializeMap(\$T.class, \$L, context, \$T::\$L)",
                        CollectionsUtils::class.java,
                        getConfigClassName(typesUtil.getSafeType(keyType), null),
                        fromMapName,
                        mapTypeName,
                        getDeserializeMethodName(mapTypeName)
                    )
                    return builder.build()
                }
            }
        }
        if (isConfigType(typeMirror)) {
            builder
                .beginControlFlow("if (\$L instanceof \$T)", fromMapName, MutableMap::class.java)
                .addStatement("$1T mapData = ($1T) $2L", MAP_STRING_OBJ_NAME, fromMapName)
                .addStatement(
                    "return \$T.\$L(context.withData(mapData))",
                    elementType,
                    getDeserializeMethodName(elementType)
                )
                .endControlFlow()
        }


        // If no shortcuts work, pass it to the context and do some dynamic-ish deserialization
        builder.addStatement(
            "return context.getMapper().map(\$N, new \$T<\$T>(){})",
            fromMapName,
            TypeToken::class.java,
            boxedTypeName
        )
        return builder.build()
    }

    private fun isConfigType(mirror: TypeMirror): Boolean {
        return mirror is DeclaredType &&
                mirror.asElement().getAnnotation(Config::class.java) != null
    }

    private fun getDeserializeMethodName(name: TypeName?): String {
        return if (name is ClassName) {
            DESERIALIZE + name.simpleName()
        } else DESERIALIZE + name
    }

    private fun createDeserializeMethod(
        typeSpecBuilder: TypeSpec.Builder,
        dtoType: TypeElement,
        className: ClassName?,
        variableElements: List<VariableElement?>?
    ) {
        val builder = MethodSpec.methodBuilder(getDeserializeMethodName(className))
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(ParameterizedTypeName.get(RESULT_CLASS_NAME, className))
            .addParameter(
                ParameterSpec.builder(DeserializationContext::class.java, "context").addModifiers(Modifier.FINAL)
                    .build()
            )
        builder.addStatement("$1T dto = new $1T()", dtoType.asType())
        val deserializeMethods = variableElements!!.stream()
            .map { variableElement: VariableElement? -> createDeserializeMethodFor(dtoType, variableElement!!) }
            .toList()
        deserializeMethods.forEach(Consumer { methodSpec: MethodSpec? -> typeSpecBuilder.addMethod(methodSpec) })
        val expressionBuilder = CodeBlock.builder()
        expressionBuilder.add("return ")
        var i = 0
        val superClass = getDTOSuperclass(dtoType)

        // Add the superclass deserialization first, if it exists
        if (superClass != null) {
            val superConfigName = getConfigClassName(superClass, dtoType)
            expressionBuilder.add("\$T.\$L", superConfigName, getDeserializeMethodName(superConfigName))
            expressionBuilder.add("(context).flatMap(var\$L -> \n", i++)
        }
        for (deserializeMethod in deserializeMethods) {
            expressionBuilder.add("\$N(context, dto).flatMap(var\$L -> \n", deserializeMethod, i++)
        }
        expressionBuilder.add("\$T.ok(new \$T(", Result::class.java, className)
        for (i1 in 0 until i) {
            expressionBuilder.add("var\$L", i1)
            if (i1 != i - 1) {
                expressionBuilder.add(", ")
            }
        }
        expressionBuilder.add("))") // Close ok and new parens
        expressionBuilder.add(")".repeat(Math.max(0, i))) // close all the flatMap parens
        builder.addStatement(expressionBuilder.build())
        typeSpecBuilder.addMethod(builder.build())
    }

    companion object {
        const val DESERIALIZE = "deserialize"
        private val MAP_STRING_OBJ_NAME: TypeName =
            ParameterizedTypeName.get(MutableMap::class.java, String::class.java, Any::class.java)
        private val RESULT_CLASS_NAME = ClassName.get(Result::class.java)
        const val JAVA_UTIL_LIST = "java.util.List"
        const val JAVA_UTIL_MAP = "java.util.Map"
    }
}
