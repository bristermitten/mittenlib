package me.bristermitten.mittenlib.config.provider.construct;

import me.bristermitten.mittenlib.config.Configuration;
import me.bristermitten.mittenlib.config.paths.ConfigInitializationStrategy;
import me.bristermitten.mittenlib.config.paths.ConfigPathResolver;
import me.bristermitten.mittenlib.config.provider.ConfigProvider;
import me.bristermitten.mittenlib.config.provider.ReadingConfigProvider;
import me.bristermitten.mittenlib.config.provider.StringReadingConfigProvider;
import me.bristermitten.mittenlib.config.reader.ConfigReader;
import me.bristermitten.mittenlib.config.writer.ObjectWriter;
import me.bristermitten.mittenlib.files.FileType;
import me.bristermitten.mittenlib.util.Result;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.nio.file.Path;

public class SimpleConfigProviderFactory implements ConfigProviderFactory {

    private final ConfigReader reader;
    private final ConfigInitializationStrategy initializationStrategy;
    private final ConfigPathResolver pathResolver;
    private final ObjectWriter objectWriter;

    @Inject
    public SimpleConfigProviderFactory(ConfigReader reader, ConfigInitializationStrategy initializationStrategy, ConfigPathResolver pathResolver, ObjectWriter objectWriter) {
        this.reader = reader;
        this.initializationStrategy = initializationStrategy;
        this.pathResolver = pathResolver;
        this.objectWriter = objectWriter;
    }

    @Override
    public <T> @NotNull Result<ConfigProvider<T>> createProvider(Configuration<T> configuration) {
        final Path configPath = pathResolver.getConfigPath(configuration.getFileName());

        return initializationStrategy.initializeConfig(configuration.getFileName())
                .map(unit -> new ReadingConfigProvider<>(configPath, configuration, reader, objectWriter));

    }


    @Override
    public @NotNull <T> Result<ConfigProvider<T>> createStringReaderProvider(String data, Configuration<T> configuration) {
        return Result.ok(new StringReadingConfigProvider<>(data, configuration, reader));
    }

    @Override
    @NotNull
    public <T> Result<ConfigProvider<T>> createStringReaderProvider(FileType type, String data, Configuration<T> configuration) {
        return Result.ok(new StringReadingConfigProvider<>(data, configuration, reader.withLoader(type.loader())));
    }
}
