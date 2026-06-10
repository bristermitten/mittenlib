package me.bristermitten.mittenlib.config.provider.construct;

import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import java.nio.file.Path;
import me.bristermitten.mittenlib.config.Configuration;
import me.bristermitten.mittenlib.config.DeserializationFunction;
import me.bristermitten.mittenlib.config.SerializationFunction;
import me.bristermitten.mittenlib.config.paths.ConfigInitializationStrategy;
import me.bristermitten.mittenlib.config.paths.ConfigPathResolver;
import me.bristermitten.mittenlib.config.provider.ConfigProvider;
import me.bristermitten.mittenlib.config.provider.FileBasedConfigProvider;
import me.bristermitten.mittenlib.config.provider.StringReadingConfigProvider;
import me.bristermitten.mittenlib.config.reader.ConfigReader;
import me.bristermitten.mittenlib.config.writer.ConfigWriter;
import me.bristermitten.mittenlib.config.writer.ObjectWriter;
import me.bristermitten.mittenlib.files.FileType;
import me.bristermitten.mittenlib.util.Result;
import org.jetbrains.annotations.NotNull;

public class SimpleConfigProviderFactory implements ConfigProviderFactory {

    private final ConfigReader reader;
    private final ConfigWriter saver;
    private final ConfigInitializationStrategy initializationStrategy;
    private final ConfigPathResolver pathResolver;
    private final ObjectWriter objectWriter;

    @Inject
    public SimpleConfigProviderFactory(
            ConfigReader reader,
            ConfigWriter saver,
            ConfigInitializationStrategy initializationStrategy,
            ConfigPathResolver pathResolver,
            ObjectWriter objectWriter) {
        this.reader = reader;
        this.saver = saver;
        this.initializationStrategy = initializationStrategy;
        this.pathResolver = pathResolver;
        this.objectWriter = objectWriter;
    }

    @Override
    public <T> @NotNull Result<ConfigProvider<T>> createProvider(
            Configuration<T> configuration,
            DeserializationFunction<T> deserializer,
            SerializationFunction<T> serializer) {
        final Result<Path> configPathResult = pathResolver.getConfigPath(configuration.getFileName());

        return configPathResult.flatMap(configPath -> initializationStrategy
                .initializeConfig(configuration.getFileName(), configuration.getImplementationType())
                .map(unit -> new FileBasedConfigProvider<>(
                        configPath, reader, deserializer, saver, serializer, objectWriter)));
    }

    @Override
    public <T> @NotNull Result<ConfigProvider<T>> createProvider(Configuration<T> configuration) {
        final Result<Path> configPathResult = pathResolver.getConfigPath(configuration.getFileName());
        final Class<T> type = configuration.getType();

        return configPathResult.flatMap(configPath -> initializationStrategy
                .initializeConfig(configuration.getFileName(), configuration.getImplementationType())
                .map(unit -> new FileBasedConfigProvider<>(
                        configPath,
                        reader,
                        ctx -> ctx.getMapper().map(ctx.getData(), TypeToken.get(type)),
                        saver,
                        (val, ctx) -> saver.serialize(val, type).getOrThrow(),
                        objectWriter)));
    }

    @Override
    public <T> @NotNull Result<ConfigProvider<T>> createStringReaderProvider(
            FileType type,
            String data,
            Configuration<T> configuration,
            DeserializationFunction<T> deserializer,
            SerializationFunction<T> serializer) {
        return Result.ok(new StringReadingConfigProvider<>(data, reader.withLoader(type.loader()), deserializer));
    }

    @Override
    public <T> @NotNull Result<ConfigProvider<T>> createStringReaderProvider(
            FileType type, String data, Configuration<T> configuration) {
        final Class<T> typeClass = configuration.getType();
        return Result.ok(new StringReadingConfigProvider<>(
                data, reader.withLoader(type.loader()), ctx -> reader.load(typeClass, data)));
    }

    @Override
    @Deprecated
    public <T> @NotNull Result<ConfigProvider<T>> createStringReaderProvider(
            String data,
            Configuration<T> configuration,
            DeserializationFunction<T> deserializer,
            SerializationFunction<T> serializer) {
        return Result.ok(new StringReadingConfigProvider<>(data, reader, deserializer));
    }

    @Override
    @Deprecated
    public <T> @NotNull Result<ConfigProvider<T>> createStringReaderProvider(
            String data, Configuration<T> configuration) {
        final Class<T> typeClass = configuration.getType();
        return Result.ok(new StringReadingConfigProvider<>(data, reader, ctx -> reader.load(typeClass, data)));
    }
}
