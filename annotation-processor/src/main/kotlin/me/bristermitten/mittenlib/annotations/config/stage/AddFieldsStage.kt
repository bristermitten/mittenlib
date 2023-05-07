package me.bristermitten.mittenlib.annotations.config.stage

import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.ParameterSpec
import me.bristermitten.mittenlib.annotations.util.TypesUtil
import javax.inject.Inject
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement

/**
 * Adds the appropriate fields for deserialized attributes
 */
class AddFieldsStage @Inject constructor(private val typesUtil: TypesUtil) :
    BuildStage<InitialiseBuilderStage.Output, InitialiseBuilderStage.Output> {
    override fun name(): String {
        return "add-fields"
    }

    private fun createFieldSpec(element: VariableElement): FieldSpec {
        return FieldSpec.builder(
            typesUtil.getConfigClassName(element.asType(), element),
            element.simpleName.toString()
        ).addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .build()
    }

    private fun createRecordSpec(element: VariableElement): ParameterSpec {
        return ParameterSpec.builder(
            typesUtil.getConfigClassName(element.asType(), element),
            element.simpleName.toString()
        ).addModifiers(Modifier.PRIVATE)
            .build()
    }

    override fun apply(
        generateFrom: TypeElement,
        input: InitialiseBuilderStage.Output
    ): InitialiseBuilderStage.Output {
        if (input.rest.generateRecords) {
            input.rest.fields.forEach {
                input.builder.addRecordComponent(createRecordSpec(it))
            }
        } else {
            val fieldSpecs = input.rest.fields.associateWith { createFieldSpec(it) }

            fieldSpecs.values.forEach {
                input.builder.addField(it)
            }
        }
    }
}