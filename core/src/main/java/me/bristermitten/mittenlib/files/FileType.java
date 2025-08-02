package me.bristermitten.mittenlib.files;

import me.bristermitten.mittenlib.config.reader.ObjectLoader;
import me.bristermitten.mittenlib.util.Result;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * A specific type of file such as JSON or YAML which can be loaded into a Map-like data structure.
 * An instance of this class should generally only handle a single file type.
 */
public interface FileType {
    /**
     * If the given path matches this file type.
     * This is generally done by the file extension, but this is not a requirement.
     *
     * @param path the path to check
     * @return if the path matches this file type
     */
    boolean matches(Path path);

    /**
     * Returns an {@link ObjectLoader} for this file type.
     * It should be assumed that the loader will only work (i.e. not produce an {@link Result#fail(Exception)})
     * for paths for which {@link FileType#matches(Path)} returns true
     *
     * @return an Object Loader for this file type
     */
    @NotNull ObjectLoader loader();
}
