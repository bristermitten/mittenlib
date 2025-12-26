package me.bristermitten.mittenlib.files.json;

import com.google.common.io.Files;
import me.bristermitten.mittenlib.config.reader.ObjectLoader;
import me.bristermitten.mittenlib.config.writer.ObjectWriter;
import me.bristermitten.mittenlib.files.FileType;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.nio.file.Path;

public class JSONFileType implements FileType {
    private static final String JSON_EXTENSION = "json";
    private final GsonObjectLoader gsonObjectLoader;
    private final GsonObjectWriter gsonObjectWriter;

    @Inject
    public JSONFileType(GsonObjectLoader gsonObjectLoader, GsonObjectWriter gsonObjectWriter) {
        this.gsonObjectLoader = gsonObjectLoader;
        this.gsonObjectWriter = gsonObjectWriter;
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

    @Override
    public @NotNull ObjectWriter writer() {
        return gsonObjectWriter;
    }
}
