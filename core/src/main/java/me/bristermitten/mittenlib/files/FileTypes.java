package me.bristermitten.mittenlib.files;

import me.bristermitten.mittenlib.files.json.JSONFileType;
import me.bristermitten.mittenlib.files.yaml.YamlFileType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Stores all the known {@link FileType} classes,
 * used for registration in the {@link FileTypeModule}
 */
public class FileTypes {
    private final Set<Class<? extends FileType>> types;

    /**
     * Create a new FileTypes instance
     *
     * @param types the types to register
     */
    public FileTypes(Set<Class<? extends FileType>> types) {
        this.types = types;
    }

    /**
     * @return A {@link FileTypes} containing all the default {@link FileType}s.
     * These are currently {@link JSONFileType} and {@link YamlFileType}
     */
    public static FileTypes defaultTypes() {
        return new FileTypes(new HashSet<>(Arrays.asList(
                JSONFileType.class,
                YamlFileType.class
        )));
    }

    /**
     * Register a new {@link FileType} class
     *
     * @param type the class to register
     * @return this
     */
    public FileTypes addType(Class<? extends FileType> type) {
        types.add(type);
        return this;
    }

    /**
     * @return the set of {@link FileType} classes registered
     */
    public Set<Class<? extends FileType>> getTypes() {
        return types;
    }
}
