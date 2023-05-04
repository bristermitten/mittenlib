package me.bristermitten.mittenlib.annotations.config

import com.google.inject.AbstractModule
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

internal class ConfigProcessorModule(private val processingEnvironment: ProcessingEnvironment) : AbstractModule() {
    override fun configure() {
        bind(ProcessingEnvironment::class.java).toInstance(processingEnvironment)
        bind(Elements::class.java).toInstance(
            processingEnvironment.elementUtils
        )
        bind(Types::class.java).toInstance(processingEnvironment.typeUtils)
    }
}
