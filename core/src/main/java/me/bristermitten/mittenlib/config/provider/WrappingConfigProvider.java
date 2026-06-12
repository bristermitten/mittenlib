package me.bristermitten.mittenlib.config.provider;

import me.bristermitten.mittenlib.config.tree.DataTree;
import me.bristermitten.mittenlib.util.Result;
import org.jetbrains.annotations.NotNull;

/** Generic interface for a {@link ConfigProvider} that wraps another {@link ConfigProvider}. */
public interface WrappingConfigProvider<T> extends SaveableConfigProvider<T> {
    /**
     * Get the wrapped {@link ConfigProvider}.
     *
     * @return the wrapped {@link ConfigProvider}
     */
    @NotNull ConfigProvider<T> getWrapped();

    @Override
    default Result<DataTree> save(T instance, boolean overrideExisting) {
        ConfigProvider<T> wrapped = getWrapped();
        if (wrapped instanceof SaveableConfigProvider) {
            Result<DataTree> result = ((SaveableConfigProvider<T>) wrapped).save(instance, overrideExisting);
            if (result.isSuccess()) {
                clearCache();
            }
            return result;
        }
        return Result.fail(new UnsupportedOperationException(
                "Wrapped provider " + wrapped.getClass().getName() + " is not saveable"));
    }
}
