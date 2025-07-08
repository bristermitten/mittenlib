package me.bristermitten.mittenlib.files.yaml;

import me.bristermitten.mittenlib.config.reader.ObjectLoader;
import me.bristermitten.mittenlib.config.tree.DataTree;
import me.bristermitten.mittenlib.config.tree.DataTreeTransforms;
import me.bristermitten.mittenlib.util.Result;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.Yaml;

import javax.inject.Inject;
import java.io.Reader;

import static me.bristermitten.mittenlib.util.Result.runCatching;

public class YamlObjectLoader implements ObjectLoader {
    private final Yaml yaml;

    @Inject
    public YamlObjectLoader(Yaml yaml) {

        this.yaml = yaml;
    }

    @Override
    public @NotNull Result<@NotNull DataTree> load(@NotNull Reader source) {
        return runCatching(() -> {

            Object obj = yaml.load(source);

            return DataTreeTransforms.loadFrom(obj);
        });
    }
}
