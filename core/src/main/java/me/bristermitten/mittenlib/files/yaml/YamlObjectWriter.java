package me.bristermitten.mittenlib.files.yaml;

import static me.bristermitten.mittenlib.util.Result.runCatching;

import com.google.inject.Inject;
import java.io.Writer;
import me.bristermitten.mittenlib.config.tree.DataTree;
import me.bristermitten.mittenlib.config.tree.DataTreeTransforms;
import me.bristermitten.mittenlib.config.writer.ObjectWriter;
import me.bristermitten.mittenlib.util.Result;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.Yaml;

/** Responsible for writing DataTree objects to YAML format. */
public class YamlObjectWriter implements ObjectWriter {
    private final Yaml yaml;

    @Inject
    public YamlObjectWriter(Yaml yaml) {
        this.yaml = yaml;
    }

    /**
     * Writes a DataTree to the given writer as YAML. This method does not close the writer.
     *
     * @param tree The DataTree to write
     * @param writer The writer to write to
     * @return A Result indicating success or failure
     */
    @Override
    public @NotNull Result<Void> write(@NotNull DataTree tree, @NotNull Writer writer) {
        return runCatching(() -> {
            Object pojo = DataTreeTransforms.toPOJO(tree);
            yaml.dump(pojo, writer);
            writer.flush();
            return null;
        });
    }
}
