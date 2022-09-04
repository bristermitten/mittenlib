package me.bristermitten.mittenlib.files;

import com.google.gson.Gson;
import com.google.gson.TypeAdapterFactory;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import me.bristermitten.mittenlib.config.reader.ObjectMapper;
import me.bristermitten.mittenlib.files.json.ExtraTypeAdapter;
import me.bristermitten.mittenlib.files.json.GsonObjectMapper;
import me.bristermitten.mittenlib.files.json.GsonProvider;

/**
 * Module handling registration of an {@link me.bristermitten.mittenlib.config.reader.ObjectMapper}, {@link me.bristermitten.mittenlib.files.FileType}s,
 * and a {@link com.google.gson.Gson} instance.
 * TODO move the Gson instance to a separate module
 */

public class FileTypeModule extends AbstractModule {
    private final FileTypes types;
    private final Class<? extends ObjectMapper> objectMapper;

    /**
     * Create a new FileTypeModule, using {@link FileTypes#defaultTypes()} and {@link GsonObjectMapper}
     **/

    public FileTypeModule() {
        this(FileTypes.defaultTypes(), GsonObjectMapper.class);
    }

    /**
     * Create a new FileTypeModule, using a provided {@link FileTypes} and {@link ObjectMapper} class
     *
     * @param types        the {@link FileTypes} to register
     * @param objectMapper the {@link ObjectMapper} class to register
     */
    public FileTypeModule(FileTypes types, Class<? extends ObjectMapper> objectMapper) {
        this.types = types;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void configure() {
        final Multibinder<FileType> fileTypeMultibinder = Multibinder.newSetBinder(binder(), FileType.class);
        for (Class<? extends FileType> type : types.getTypes()) {
            fileTypeMultibinder.addBinding().to(type);
        }

        bind(ObjectMapper.class).to(objectMapper);
        bind(Gson.class).toProvider(GsonProvider.class);
        // This means Guice won't complain even if there aren't any custom type adapters
        Multibinder.newSetBinder(binder(), new TypeLiteral<ExtraTypeAdapter<?>>() {
        });
        Multibinder.newSetBinder(binder(), TypeAdapterFactory.class);
    }
}
