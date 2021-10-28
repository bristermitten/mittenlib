package me.bristermitten.mittenlib.files.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Set;

public class GsonProvider implements Provider<Gson> {
    private final Set<TypeAdapterFactory> typeAdapterFactories;

    @Inject
    public GsonProvider(Set<TypeAdapterFactory> typeAdapterFactories) {
        this.typeAdapterFactories = typeAdapterFactories;
    }

    @Override
    public Gson get() {
        final GsonBuilder gsonBuilder = new GsonBuilder();
        typeAdapterFactories.forEach(gsonBuilder::registerTypeAdapterFactory);
        return gsonBuilder.create();
    }
}
