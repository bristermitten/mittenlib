package me.bristermitten.mittenlib.config.provider;

import me.bristermitten.mittenlib.config.tree.DataTree;
import me.bristermitten.mittenlib.util.Result;

/**
 * A {@link ConfigProvider} that supports saving to the same source it reads from.
 * This interface may be implemented directly by providers that support saving,
 * or by wrappers that delegate to a saveable provider.
 * <p>
 * If the underlying source does not support saving, the {@link #save} method
 * should return a failed {@link Result} containing a {@link UnsupportedOperationException}.
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
