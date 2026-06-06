package me.bristermitten.mittenlib.config.provider.construct;

import me.bristermitten.mittenlib.config.Configuration;
import me.bristermitten.mittenlib.config.DeserializationFunction;
import me.bristermitten.mittenlib.config.SerializationFunction;
import me.bristermitten.mittenlib.config.paths.ConfigInitializationStrategy;
import me.bristermitten.mittenlib.config.paths.ConfigPathResolver;
import me.bristermitten.mittenlib.config.provider.ConfigProvider;
import me.bristermitten.mittenlib.config.provider.ReadingConfigProvider;
import me.bristermitten.mittenlib.config.provider.StringReadingConfigProvider;
import me.bristermitten.mittenlib.config.reader.ConfigReader;
import me.bristermitten.mittenlib.config.writer.ObjectWriter;
import me.bristermitten.mittenlib.config.writer.ConfigSaver;
import me.bristermitten.mittenlib.files.FileType;
import me.bristermitten.mittenlib.util.Result;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.nio.file.Path;

public class SimpleConfigProviderFactory implements ConfigProviderFactory {

    private final ConfigReader reader;
    private final ConfigSaver saver;
    private final ConfigInitializationStrategy initializationStrategy;
    private final ConfigPathResolver pathResolver;
    private final ObjectWriter objectWriter;

    @Inject
    public SimpleConfigProviderFactory(ConfigReader reader, ConfigSaver saver, ConfigInitializationStrategy initializationStrategy, ConfigPathResolver pathResolver, ObjectWriter objectWriter) {
        this.reader = reader;
        this.saver = saver;
        this.initializationStrategy = initializationStrategy;
        this.pathResolver = pathResolver;
        this.objectWriter = objectWriter;
    }

    @Override
    public <T> @NotNull Result<ConfigProvider<T>> createProvider(Configuration<T> configuration, DeserializationFunction<T> deserializer, SerializationFunction<T> serializer) {
        final Path configPath = pathResolver.getConfigPath(configuration.getFileName());

        return initializationStrategy.initializeConfig(configuration.getFileName())
                .map(unit -> new ReadingConfigProvider<>(configPath, configuration, reader, deserializer, saver, serializer, objectWriter));

    }

    @Override
    public <T> @NotNull Result<ConfigProvider<T>> createProvider(Configuration<T> configuration) {
        final Path configPath = pathResolver.getConfigPath(configuration.getFileName());
        final Class<T> type = configuration.getType();

        return initializationStrategy.initializeConfig(configuration.getFileName())
                .map(unit -> new ReadingConfigProvider<>(configPath, configuration, reader,
                        ctx -> (Result<T>) reader.load(type, configPath),
                        saver,
                        (val, ctx) -> saver.serialize(val, type).getOrThrow(),
                        objectWriter));
    }

    @Override
    public <T> @NotNull Result<ConfigProvider<T>> createStringReaderProvider(FileType type, String data, Configuration<T> configuration, DeserializationFunction<T> deserializer, SerializationFunction<T> serializer) {
        return Result.ok(new StringReadingConfigProvider<>(data, configuration, reader.withLoader(type.loader()), deserializer, saver, serializer));
    }

    @Override
    public <T> @NotNull Result<ConfigProvider<T>> createStringReaderProvider(FileType type, String data, Configuration<T> configuration) {
        final Class<T> typeClass = configuration.getType();
        return Result.ok(new StringReadingConfigProvider<>(data, configuration, reader.withLoader(type.loader()),
                ctx -> (Result<T>) reader.load(typeClass, data),
                saver,
                (val, ctx) -> saver.serialize(val, typeClass).getOrThrow()));
    }

    @Override
    @Deprecated
    public <T> @NotNull Result<ConfigProvider<T>> createStringReaderProvider(String data, Configuration<T> configuration, DeserializationFunction<T> deserializer, SerializationFunction<T> serializer) {
        return Result.ok(new StringReadingConfigProvider<>(data, configuration, reader, deserializer, saver, serializer));
    }

    @Override
    @Deprecated
    public <T> @NotNull Result<ConfigProvider<T>> createStringReaderProvider(String data, Configuration<T> configuration) {
        final Class<T> typeClass = configuration.getType();
        return Result.ok(new StringReadingConfigProvider<>(data, configuration, reader,
                ctx -> (Result<T>) reader.load(typeClass, data),
                saver,
                (val, ctx) -> saver.serialize(val, typeClass).getOrThrow()));
    }
}
