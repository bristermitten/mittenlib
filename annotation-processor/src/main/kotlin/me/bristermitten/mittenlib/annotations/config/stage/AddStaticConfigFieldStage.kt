package me.bristermitten.mittenlib.annotations.config.stage

import com.squareup.javapoet.FieldSpec
import me.bristermitten.mittenlib.annotations.util.asTypeName
import me.bristermitten.mittenlib.annotations.util.getDeserializeMethodName
import me.bristermitten.mittenlib.annotations.util.withTypeArguments
import me.bristermitten.mittenlib.config.Configuration
import me.bristermitten.mittenlib.config.Source
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement

/**
 * Creates a `public static final Configuration<T> CONFIG` field for types with a [Source] annotation.
 */
class AddStaticConfigFieldStage : BuildStage<InitialiseBuilderStage.Output, InitialiseBuilderStage.Output> {
    override fun name(): String {
        return "add-static-config-field"
    }

    override fun apply(
        generateFrom: TypeElement,
        input: InitialiseBuilderStage.Output
    ): InitialiseBuilderStage.Output {
        val annotation = generateFrom.getAnnotation(
            Source::class.java
        ) ?: return input // do nothing if there's no @Source annotation

        val generatedClassName = input.rest.generatedClassName

        val configurationType =
            Configuration::class.asTypeName()
                .withTypeArguments(generatedClassName) // Configuration<T>

        val configField = FieldSpec.builder(configurationType, "CONFIG")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
            .initializer(
                "new \$T<>(\$S, \$T.class, \$T::\$L)",
                Configuration::class.java,
                annotation.value,
                generatedClassName,
                generatedClassName,
                getDeserializeMethodName(generatedClassName)
            )
            .build()

        input.builder.addField(configField)
        return input
    }
}