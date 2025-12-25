package me.bristermitten.mittenlib.config.provider;

import me.bristermitten.mittenlib.config.Configuration;
import me.bristermitten.mittenlib.config.reader.ConfigReader;
import me.bristermitten.mittenlib.config.tree.DataTree;
import me.bristermitten.mittenlib.files.yaml.YamlObjectWriter;
import me.bristermitten.mittenlib.util.Result;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A {@link ConfigProvider} that reads from a file, using a {@link ConfigReader}
 *
 * @param <T> the type of the config
 */
public class ReadingConfigProvider<T> implements ConfigProvider<T> {
    private final Configuration<T> config;
    private final ConfigReader reader;
    private final Path path;
    private final YamlObjectWriter writer;

    /**
     * Create a new ReadingConfigProvider
     *
     * @param path   the path to read from
     * @param config the configuration to read
     * @param reader the reader to use
     * @param writer the writer to use for saving
     */
    public ReadingConfigProvider(Path path, Configuration<T> config, ConfigReader reader, YamlObjectWriter writer) {
        this.path = path;
        this.config = config;
        this.reader = reader;
        this.writer = writer;
    }

    @Override
    public T get() {
        return reader.load(config.getType(), path, config.getDeserializeFunction()).getOrThrow();
    }

    @Override
    public Optional<Path> path() {
        return Optional.of(path);
    }

    @Override
    public void clearCache() {
        //no-op
    }

    /**
     * Saves the given config instance back to the file.
     * This can be used to save default values for missing fields.
     * By default, this only adds missing fields and does not override existing ones.
     *
     * @param instance the config instance to save
     * @return a Result indicating success or failure
     */
    public Result<Void> save(T instance) {
        return save(instance, false);
    }

    /**
     * Saves the given config instance back to the file.
     * This can be used to save default values for missing fields.
     *
     * @param instance         the config instance to save
     * @param overrideExisting if true, overwrites the entire file; if false, only adds missing fields
     * @return a Result indicating success or failure
     */
    public Result<Void> save(T instance, boolean overrideExisting) {
        if (config.getSerializeFunction() == null) {
            return Result.fail(new UnsupportedOperationException("No serialization function provided for " + config.getType()));
        }

        // Get the ObjectMapper from the reader
        DataTree serializedTree = config.getSerializeFunction().apply(instance, reader.getMapper());

        if (overrideExisting) {
            return writer.write(serializedTree, path);
        }
        // Read existing file and merge with new values
        return reader.load(DataTree.class, path, x -> Result.ok(x.getData()))
                .map(existingTree -> mergeDataTrees(existingTree, serializedTree))
                .flatMap(mergedTree -> writer.write(mergedTree, path))
                .flatMapException(error -> {
                    // If file doesn't exist or can't be read, just write the new config
                    return writer.write(serializedTree, path);
                });
    }

    /**
     * Merges two DataTrees, with existing values taking precedence.
     * Only adds fields from newTree that don't exist in existingTree.
     *
     * @param existingTree the existing data tree (takes precedence)
     * @param newTree      the new data tree with default values
     * @return the merged data tree
     */
    private DataTree mergeDataTrees(DataTree existingTree, DataTree newTree) {
        if (!(existingTree instanceof DataTree.DataTreeMap) || !(newTree instanceof DataTree.DataTreeMap)) {
            return existingTree;
        }

        DataTree.DataTreeMap existingMap = (DataTree.DataTreeMap) existingTree;
        DataTree.DataTreeMap newMap = (DataTree.DataTreeMap) newTree;

        Map<DataTree, DataTree> mergedValues = new LinkedHashMap<>(existingMap.values());

        for (Map.Entry<DataTree, DataTree> entry : newMap.values().entrySet()) {
            if (!mergedValues.containsKey(entry.getKey())) {
                mergedValues.put(entry.getKey(), entry.getValue());
            }
        }

        return new DataTree.DataTreeMap(mergedValues);
    }
}

