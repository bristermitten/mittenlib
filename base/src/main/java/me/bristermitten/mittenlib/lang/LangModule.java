package me.bristermitten.mittenlib.lang;

import com.google.gson.TypeAdapterFactory;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

public class LangModule extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder.newSetBinder(binder(), TypeAdapterFactory.class)
                .addBinding().to(LangMessageTypeAdapterFactory.class);
    }
}
