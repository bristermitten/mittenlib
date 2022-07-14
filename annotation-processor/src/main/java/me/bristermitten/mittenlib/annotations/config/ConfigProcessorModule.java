package me.bristermitten.mittenlib.annotations.config;

import com.google.inject.AbstractModule;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public class ConfigProcessorModule extends AbstractModule {
    private final ProcessingEnvironment processingEnvironment;

    public ConfigProcessorModule(ProcessingEnvironment processingEnvironment) {
        this.processingEnvironment = processingEnvironment;
    }

    @Override
    protected void configure() {
        bind(ProcessingEnvironment.class).toInstance(processingEnvironment);
        bind(Elements.class).toInstance(processingEnvironment.getElementUtils());
        bind(Types.class).toInstance(processingEnvironment.getTypeUtils());
    }
}
