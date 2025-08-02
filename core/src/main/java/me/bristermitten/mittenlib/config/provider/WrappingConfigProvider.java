package me.bristermitten.mittenlib.config.provider;

import org.jetbrains.annotations.NotNull;

/**
 * Generic interface for a {@link ConfigProvider} that wraps another {@link ConfigProvider}.
 */
public interface WrappingConfigProvider<T> {
    /**
     * Get the wrapped {@link ConfigProvider}.
     * @return the wrapped {@link ConfigProvider}
     */
    @NotNull ConfigProvider<T> getWrapped();
}
