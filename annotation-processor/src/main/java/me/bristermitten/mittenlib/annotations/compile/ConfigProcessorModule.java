package me.bristermitten.mittenlib.annotations.compile;

import com.google.inject.AbstractModule;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * Guice module for the configuration annotation processor.
 * This module binds the necessary components for the annotation processor,
 * including the ProcessingEnvironment and its utility classes.
 */
public class ConfigProcessorModule extends AbstractModule {
    private final ProcessingEnvironment processingEnvironment;

    /**
     * Creates a new ConfigProcessorModule with the given processing environment.
     *
     * @param processingEnvironment The processing environment to use for this module
     */
    public ConfigProcessorModule(ProcessingEnvironment processingEnvironment) {
        this.processingEnvironment = processingEnvironment;
    }

    /**
     * Configures the Guice bindings for this module.
     * Binds the ProcessingEnvironment and its utility classes (Elements and Types)
     * for use by other components in the annotation processor.
     */
    @Override
    protected void configure() {
        bind(ProcessingEnvironment.class).toInstance(processingEnvironment);
        bind(Elements.class).toInstance(processingEnvironment.getElementUtils());
        bind(Types.class).toInstance(processingEnvironment.getTypeUtils());
    }
}
