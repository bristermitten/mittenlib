package me.bristermitten.mittenlib.files.yaml;

import me.bristermitten.mittenlib.config.reader.ObjectLoader;
import me.bristermitten.mittenlib.util.Result;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.Yaml;

import javax.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static me.bristermitten.mittenlib.util.Result.runCatching;

public class YamlObjectLoader implements ObjectLoader {
    private final Yaml yaml;

    @Inject
    public YamlObjectLoader(Yaml yaml) {
        this.yaml = yaml;
    }

    @Override
    public @NotNull Result<Map<Object, Object>> load(@NotNull Path source) {
        //noinspection unchecked
        return runCatching(() ->
                (Map<Object, Object>) yaml.load(Files.newBufferedReader(source)));
    }
}
