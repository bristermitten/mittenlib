package me.bristermitten.mittenlib.files.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Set;

/**
 * Standard provider for {@link Gson} instances.
 * This class will automatically register all {@link TypeAdapterFactory} and {@link ExtraTypeAdapter} instances in the Guice context.
 */
public class GsonProvider implements Provider<Gson> {
    private final Set<TypeAdapterFactory> typeAdapterFactories;
    private final Set<ExtraTypeAdapter<?>> extraTypeAdapters;

    @Inject
    GsonProvider(Set<TypeAdapterFactory> typeAdapterFactories, Set<ExtraTypeAdapter<?>> extraTypeAdapters) {
        this.typeAdapterFactories = typeAdapterFactories;
        this.extraTypeAdapters = extraTypeAdapters;
    }

    @Override
    public Gson get() {
        final GsonBuilder gsonBuilder = new GsonBuilder();
        typeAdapterFactories.forEach(gsonBuilder::registerTypeAdapterFactory);
        extraTypeAdapters.forEach(eta -> gsonBuilder.registerTypeAdapter(eta.type(), eta));
        return gsonBuilder.create();
    }
}
