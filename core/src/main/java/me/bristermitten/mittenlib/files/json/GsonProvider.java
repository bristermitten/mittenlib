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
    private final Provider<GsonBuilder> gsonBuilderProvider;

    @Inject
    GsonProvider(Set<TypeAdapterFactory> typeAdapterFactories, Set<ExtraTypeAdapter<?>> extraTypeAdapters, Provider<GsonBuilder> gsonBuilderProvider) {
        this.typeAdapterFactories = typeAdapterFactories;
        this.extraTypeAdapters = extraTypeAdapters;
        this.gsonBuilderProvider = gsonBuilderProvider;
    }

    @Override
    public Gson get() {
        final GsonBuilder gsonBuilder = gsonBuilderProvider.get();
        typeAdapterFactories.forEach(gsonBuilder::registerTypeAdapterFactory);
        extraTypeAdapters.forEach(eta -> gsonBuilder.registerTypeAdapter(eta.type(), eta));
        return gsonBuilder.create();
    }
}
