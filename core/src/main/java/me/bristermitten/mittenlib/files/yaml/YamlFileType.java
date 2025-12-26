package me.bristermitten.mittenlib.files.yaml;

import com.google.common.io.Files;
import me.bristermitten.mittenlib.config.reader.ObjectLoader;
import me.bristermitten.mittenlib.config.writer.ObjectWriter;
import me.bristermitten.mittenlib.files.FileType;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.nio.file.Path;

public class YamlFileType implements FileType {
    private static final String YAML_SHORT_EXTENSION = "yml";
    private static final String YAML_LONG_EXTENSION = "yaml";
    private final YamlObjectLoader yamlObjectLoader;
    private final YamlObjectWriter yamlObjectWriter;

    @Inject
    public YamlFileType(YamlObjectLoader objectLoader, YamlObjectWriter yamlObjectWriter) {
        this.yamlObjectLoader = objectLoader;
        this.yamlObjectWriter = yamlObjectWriter;
    }

    @Override
    public boolean matches(Path path) {
        //noinspection UnstableApiUsage
        final String fileExtension = Files.getFileExtension(path.toString());
        return YAML_SHORT_EXTENSION.equals(fileExtension) || YAML_LONG_EXTENSION.equals(fileExtension);
    }

    @Override
    public @NotNull ObjectLoader loader() {
        return yamlObjectLoader;
    }

    @Override
    public @NotNull ObjectWriter writer() {
        return yamlObjectWriter;
    }


}
