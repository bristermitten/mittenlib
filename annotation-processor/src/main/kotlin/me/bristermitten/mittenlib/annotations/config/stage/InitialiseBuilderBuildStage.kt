package me.bristermitten.mittenlib.annotations.config.stage

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.TypeSpec
import me.bristermitten.mittenlib.annotations.config.ConfigClassBuildSettings
import me.bristermitten.mittenlib.annotations.config.ConfigurationClassNameGenerator
import me.bristermitten.mittenlib.annotations.util.ElementsFinder
import me.bristermitten.mittenlib.annotations.util.TypesUtil
import me.bristermitten.mittenlib.config.Config
import me.bristermitten.mittenlib.config.GeneratedConfig
import javax.inject.Inject
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

class InitialiseBuilderBuildStage @Inject internal constructor(
    private val classNameGenerator: ConfigurationClassNameGenerator,
    private val typesUtil: TypesUtil,
    private val elementsFinder: ElementsFinder
) :
    BuildStage<ConfigClassBuildSettings, ConfigClassBuildSettings> {
    override fun name(): String {
        return "initialise-builder"
    }

    private fun getDTOSuperclass(dtoType: TypeElement): TypeMirror? {
        var superClass = dtoType.superclass
        if (superClass!!.kind == TypeKind.NONE || superClass.toString() == "java.lang.Object") {
            superClass = null
        }
        require(!(superClass != null && !isConfigType(superClass))) { "Superclass of @Config class must be a @Config class, was $superClass" }
        return superClass
    }

    private fun isConfigType(mirror: TypeMirror): Boolean {
        return mirror is DeclaredType &&
                mirror.asElement().getAnnotation(Config::class.java) != null
    }


    override fun apply(generateFrom: TypeElement, input: ConfigClassBuildSettings): ConfigClassBuildSettings {
        val typeSpecBuilder = TypeSpec.classBuilder(input.generatedClassName)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(
                AnnotationSpec.builder(GeneratedConfig::class.java)
                    .addMember("source", "\$T.class", ClassName.get(generateFrom))
                    .build()
            )

        val superClass = getDTOSuperclass(generateFrom)
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
    }
}
