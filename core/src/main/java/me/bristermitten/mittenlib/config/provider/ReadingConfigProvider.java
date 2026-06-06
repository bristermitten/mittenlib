package me.bristermitten.mittenlib.config.provider;

import me.bristermitten.mittenlib.config.Configuration;
import me.bristermitten.mittenlib.config.DeserializationFunction;
import me.bristermitten.mittenlib.config.SerializationFunction;
import me.bristermitten.mittenlib.config.reader.ConfigReader;
import me.bristermitten.mittenlib.config.tree.DataTree;
import me.bristermitten.mittenlib.config.writer.ConfigSaver;
import me.bristermitten.mittenlib.config.writer.ObjectWriter;
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
    private final ConfigReader reader;
    private final DeserializationFunction<T> deserializer;
    private final ConfigSaver saver;
    private final SerializationFunction<T> serializer;
    private final Path path;
    private final ObjectWriter writer; // TODO: merge into ConfigReader?

    /**
     * Create a new ReadingConfigProvider
     *
     * @param path         the path to read from
     * @param config       the configuration to read
     * @param reader       the reader to use
     * @param deserializer the deserialization function to use
     * @param saver        the saver to use
     * @param serializer   the serialization function to use
     * @param writer       the writer to use for saving
     */
    public ReadingConfigProvider(Path path, Configuration<T> config, ConfigReader reader, DeserializationFunction<T> deserializer, ConfigSaver saver, SerializationFunction<T> serializer, ObjectWriter writer) {
        this.path = path;
        this.reader = reader;
        this.deserializer = deserializer;
        this.saver = saver;
        this.serializer = serializer;
        this.writer = writer;
    }

    @Override
    public T get() {
        return reader.load(deserializer, path).getOrThrow();
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
        return saver.serialize(instance, serializer).flatMap(serializedTree -> {
            if (overrideExisting) {
                return writer.write(serializedTree, path);
            }
            // Read existing file and merge with new values
            return reader.load(ctx -> Result.ok(ctx.getData()), path)
                    .map(existingTree -> (DataTree) mergeDataTrees((DataTree) existingTree, serializedTree))
                    .flatMap(mergedTree -> writer.write(mergedTree, path))
                    .flatMapException(error -> {
                        // If file doesn't exist or can't be read, just write the new config
                        return writer.write(serializedTree, path);
                    });
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
            DataTree key = entry.getKey();
            DataTree newValue = entry.getValue();

            if (!mergedValues.containsKey(key)) {
                mergedValues.put(key, newValue);
            } else {
                DataTree existingValue = mergedValues.get(key);
                DataTree mergedValue = mergeDataTrees(existingValue, newValue);
                mergedValues.put(key, mergedValue);
            }
        }

        return new DataTree.DataTreeMap(mergedValues);
    }
}

