package me.bristermitten.mittenlib.files;

import me.bristermitten.mittenlib.config.reader.ObjectLoader;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public interface FileType {
    boolean matches(Path path);

    /**
     * @return an Object Loader for this file type
     * It is assumed that the loader will only work (i.e not produce an error)
     * for paths for which {@link FileType#matches(Path)} returns true
     */
    @NotNull ObjectLoader loader();
}
