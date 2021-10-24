package me.bristermitten.mittenlib.files;

import me.bristermitten.mittenlib.config.reader.ObjectLoader;

import java.nio.file.Path;

public interface FileType {
    boolean matches(Path path);

    ObjectLoader loader();
}
