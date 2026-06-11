package me.bristermitten.mittenlib.config.provider;

import me.bristermitten.mittenlib.config.tree.DataTree;
import me.bristermitten.mittenlib.util.Result;

/**
 * A {@link ConfigProvider} that supports saving to the same source it reads from.
 * This interface should <i>only</i> be implemented if there is a meaningful notion of "saving"
 * for the source.
 */
public interface SaveableConfigProvider<T> extends ConfigProvider<T> {

    /**
     * Saves the given config instance back to the file. This can be used to save default values for
     * missing fields. By default, this only adds missing fields and does not override existing ones.
     *
     * @param instance the config instance to save
     * @return a {@link Result} indicating success or failure, containing the exact {@link DataTree} that was written
     */
    default Result<DataTree> save(T instance) {
        return save(instance, false);
    }

    /**
     * Saves the given config instance back to the file. This can be used to save default values for
     * missing fields.
     *
     * @param instance the config instance to save
     * @param overrideExisting if true, overwrites the entire file; if false, only adds missing fields
     * @return a {@link Result} indicating success or failure, containing the exact {@link DataTree} that was written
     */
    Result<DataTree> save(T instance, boolean overrideExisting);
}
