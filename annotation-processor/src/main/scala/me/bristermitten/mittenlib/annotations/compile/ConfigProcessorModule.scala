package me.bristermitten.mittenlib.annotations.compile

import com.google.inject.AbstractModule
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

/** Guice module for the configuration annotation processor. This module binds
  * the necessary components for the annotation processor, including the
  * ProcessingEnvironment and its utility classes.
  */
class ConfigProcessorModule(
    private val processingEnvironment: ProcessingEnvironment
) extends AbstractModule:

  /** Configures the Guice bindings for this module. Binds the
    * ProcessingEnvironment and its utility classes (Elements and Types) for use
    * by other components in the annotation processor.
    */
  override def configure(): Unit =
    bind(classOf[ProcessingEnvironment]).toInstance(processingEnvironment)
    bind(classOf[Elements]).toInstance(processingEnvironment.getElementUtils)
    bind(classOf[Types]).toInstance(processingEnvironment.getTypeUtils)
