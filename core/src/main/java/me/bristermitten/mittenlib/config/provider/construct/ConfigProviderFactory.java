package me.bristermitten.mittenlib.config.provider.construct;

import me.bristermitten.mittenlib.config.Configuration;
import me.bristermitten.mittenlib.config.DeserializationFunction;
import me.bristermitten.mittenlib.config.SerializationFunction;
import me.bristermitten.mittenlib.config.provider.ConfigProvider;
import me.bristermitten.mittenlib.config.reader.SearchingObjectLoader;
import me.bristermitten.mittenlib.files.FileType;
import me.bristermitten.mittenlib.util.Result;
import org.jetbrains.annotations.NotNull;

/**
 * Creates {@link ConfigProvider}s for {@link Configuration}s
 */
public interface ConfigProviderFactory {
    /**
     * Creates the simplest {@link ConfigProvider} possible for the given {@link Configuration}.
     * This will generally not include fancy things like caching, auto reloading, etc.
     * See {@link ConfigProviderImprover} to add extra functionality.
     *
     * @param configuration the configuration to create a provider for
     * @param <T>           the type of the configuration
     * @return the created provider
     */
    @NotNull <T> Result<ConfigProvider<T>> createProvider(Configuration<T> configuration, DeserializationFunction<T> deserializer, SerializationFunction<T> serializer);

    @NotNull <T> Result<ConfigProvider<T>> createProvider(Configuration<T> configuration);


    /**
     * Creates a {@link ConfigProvider} that reads a given {@link String} for its data, rather than a file.
     * While implementations may differ, one can safely assume that the value of {@link Configuration#getFileName()}
     * will be irrelevant to the functionality of this method (aside from for "pure" things like debug logging).
     *
     * @param data          the data to read
     * @param configuration the configuration to create a provider for
     * @param <T>           the type of the configuration
     * @return the created provider
     * @deprecated This method makes use of {@link SearchingObjectLoader}, which cannot efficiently process Strings.
     * Use {@link #createStringReaderProvider(FileType, String, Configuration, DeserializationFunction, SerializationFunction)} instead to manually specify a file type
     */
    @Deprecated
    @NotNull <T> Result<ConfigProvider<T>> createStringReaderProvider(String data, Configuration<T> configuration, DeserializationFunction<T> deserializer, SerializationFunction<T> serializer);

    @Deprecated
    @NotNull <T> Result<ConfigProvider<T>> createStringReaderProvider(String data, Configuration<T> configuration);

    /**
     * Creates a {@link ConfigProvider} that reads a given String for its data, rather than a file.
     * While implementations may differ, one can safely assume that the value of {@link Configuration#getFileName()}
     * will be irrelevant.
     * <p>
     * This method also accepts a {@link FileType} whose {@link FileType#loader()} will be used to load the data.
     * If you don't know the file type, use {@link #createStringReaderProvider(String, Configuration, DeserializationFunction, SerializationFunction)} instead, but
     * be aware that performance may suffer from this approach, as it may have to check multiple {@link FileType}s.
     *
     * @param type          the file type to use
     * @param data          the data to read
     * @param configuration the configuration to create a provider for
     * @param <T>           the type of the configuration
     * @return the created provider
     */
    @NotNull <T> Result<ConfigProvider<T>> createStringReaderProvider(FileType type, String data, Configuration<T> configuration, DeserializationFunction<T> deserializer, SerializationFunction<T> serializer);

    @NotNull <T> Result<ConfigProvider<T>> createStringReaderProvider(FileType type, String data, Configuration<T> configuration);
}
