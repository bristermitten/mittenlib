package me.bristermitten.mittenlib.files;

import me.bristermitten.mittenlib.files.json.JSONFileType;
import me.bristermitten.mittenlib.files.yaml.YamlFileType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class FileTypes {
    private final Set<Class<? extends FileType>> types;

    public FileTypes(Set<Class<? extends FileType>> types) {
        this.types = types;
    }

    public static FileTypes defaultTypes() {
        return new FileTypes(new HashSet<>(Arrays.asList(
                JSONFileType.class,
                YamlFileType.class
        )));
    }

    public FileTypes addType(Class<? extends FileType> type) {
        types.add(type);
        return this;
    }

    public Set<Class<? extends FileType>> getTypes() {
        return types;
    }
}
