package me.bristermitten.mittenlib.files.json;

import com.google.common.io.Files;
import me.bristermitten.mittenlib.config.reader.ObjectLoader;
import me.bristermitten.mittenlib.files.FileType;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.nio.file.Path;

public class JSONFileType implements FileType {
    private static final String JSON_EXTENSION = "json";
    private final GsonObjectLoader gsonObjectLoader;

    @Inject
    public JSONFileType(GsonObjectLoader gsonObjectLoader) {
        this.gsonObjectLoader = gsonObjectLoader;
    }

    @Override
    public boolean matches(Path path) {
        //noinspection UnstableApiUsage
        return JSON_EXTENSION.equals(Files.getFileExtension(path.toString()));
    }

    @Override
    public @NotNull ObjectLoader loader() {
        return gsonObjectLoader;
    }
}
