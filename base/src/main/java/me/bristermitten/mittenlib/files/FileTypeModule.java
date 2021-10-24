package me.bristermitten.mittenlib.files;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import me.bristermitten.mittenlib.config.reader.ObjectMapper;
import me.bristermitten.mittenlib.files.json.GsonObjectMapper;

public class FileTypeModule extends AbstractModule {
    private final FileTypes types;
    private final Class<? extends ObjectMapper> objectMapper;

    public FileTypeModule() {
        this(FileTypes.defaultTypes(), GsonObjectMapper.class);
    }

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
    }
}
