package me.bristermitten.mittenlib.annotations.config.stage

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.TypeSpec
import me.bristermitten.mittenlib.annotations.config.ConfigClassBuildSettings
import me.bristermitten.mittenlib.annotations.util.TypesUtil
import me.bristermitten.mittenlib.annotations.util.shouldGenerateRecord
import me.bristermitten.mittenlib.config.Config
import me.bristermitten.mittenlib.config.GeneratedConfig
import me.bristermitten.mittenlib.util.Strings
import javax.inject.Inject
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

/**
 * Stage to create the typespec builder and set up its basic properties (name, access modifiers, superclass)
 */
class InitialiseBuilderStage @Inject internal constructor(
    private val typesUtil: TypesUtil
) :
    BuildStage<ConfigClassBuildSettings, InitialiseBuilderStage.Output> {
    override fun name(): String {
        return "initialise-builder"
    }

    private fun getDTOSuperclass(dtoType: TypeElement): TypeMirror? {
        val superClass = dtoType.superclass
        if (superClass.kind == TypeKind.NONE || superClass.toString() == "java.lang.Object") { // reached the top, no more superclasses
            return null
        }
        require(
            isConfigType(superClass)
        ) { "Superclass of @Config class must be a @Config class, was $superClass" }

        return superClass
    }

    private fun isConfigType(mirror: TypeMirror): Boolean {
        return mirror is DeclaredType &&
                mirror.asElement().getAnnotation(Config::class.java) != null
    }

    private fun getSuperFieldName(superClass: TypeMirror): String {
        val configName = typesUtil.getConfigClassName(superClass)
        return "parent" + Strings.capitalize(typesUtil.getSimpleName(configName))
    }

    private fun createClassOrRecord(name: ClassName, record: Boolean) =
        if (record) TypeSpec.recordBuilder(name) else TypeSpec.classBuilder(name)

    override fun apply(generateFrom: TypeElement, input: ConfigClassBuildSettings): Output {
        val typeSpecBuilder =
            createClassOrRecord(input.generatedClassName, typesUtil.shouldGenerateRecord(generateFrom, input))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(
                    AnnotationSpec.builder(GeneratedConfig::class.java)
                        .addMember("source", "\$T.class", ClassName.get(generateFrom))
                        .build()
                )

        val superClass = getDTOSuperclass(generateFrom)
        /*
        If the dto class c has a superclass s, which is also a dto class, then gen(s) is a superclass of gen(c).
         */
        if (superClass != null) {
            // Store the super instance
            val superclassName = typesUtil.getConfigClassName(superClass, generateFrom)
            typeSpecBuilder.superclass(superclassName)
            val superParamName = getSuperFieldName(superClass)
            typeSpecBuilder.addField(
                FieldSpec.builder(superclassName, superParamName)
                    .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                    .build()
            )
        }


        return Output(typeSpecBuilder, superClass, input)
    }

    data class Output(val builder: TypeSpec.Builder, val superclass: TypeMirror?, val rest: ConfigClassBuildSettings)
}
