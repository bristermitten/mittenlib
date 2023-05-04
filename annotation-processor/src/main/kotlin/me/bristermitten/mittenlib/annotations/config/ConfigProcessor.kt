package me.bristermitten.mittenlib.annotations.config

import com.google.auto.service.AutoService
import com.google.inject.Guice
import me.bristermitten.mittenlib.annotations.exception.ConfigProcessingException
import java.util.function.Consumer
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.*

/**
 * Annotation processor for generating config classes from DTO classes marked with [Config]
 */
@SupportedAnnotationTypes("me.bristermitten.mittenlib.config.Config")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(
    Processor::class
)
class ConfigProcessor
/**
 * Public constructor for the compiler
 */
    : AbstractProcessor() {
    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        val injector = Guice.createInjector(
            ConfigProcessorModule(processingEnv)
        )
        val types = annotations
            .flatMap { a: TypeElement? -> roundEnv.getElementsAnnotatedWith(a) }
            .filterIsInstance<TypeElement>()
            .filter { element: TypeElement -> element.nestingKind == NestingKind.TOP_LEVEL }
            .toList()
        val builder = injector.getInstance(ConfigClassBuilder::class.java)
        types.forEach(Consumer { clazz: TypeElement ->
            val fileContent = builder.createConfigFile(clazz)
            try {
                fileContent.writeTo(processingEnv.filer)
            } catch (e: Exception) {
                throw ConfigProcessingException("Could not create config file", e)
            }
        })
        return true
    }
}
