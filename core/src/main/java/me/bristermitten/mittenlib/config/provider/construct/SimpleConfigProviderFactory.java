package me.bristermitten.mittenlib.config.provider.construct;

import me.bristermitten.mittenlib.config.Configuration;
import me.bristermitten.mittenlib.config.paths.ConfigInitializationStrategy;
import me.bristermitten.mittenlib.config.paths.ConfigPathResolver;
import me.bristermitten.mittenlib.config.provider.ConfigProvider;
import me.bristermitten.mittenlib.config.provider.ReadingConfigProvider;
import me.bristermitten.mittenlib.config.provider.StringReadingConfigProvider;
import me.bristermitten.mittenlib.config.reader.ConfigReader;
import me.bristermitten.mittenlib.files.FileType;
import me.bristermitten.mittenlib.util.Result;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.nio.file.Path;

public class SimpleConfigProviderFactory implements ConfigProviderFactory {

    private final ConfigReader reader;
    private final ConfigInitializationStrategy initializationStrategy;
    private final ConfigPathResolver pathResolver;

    @Inject
    public SimpleConfigProviderFactory(ConfigReader reader, ConfigInitializationStrategy initializationStrategy, ConfigPathResolver pathResolver) {
        this.reader = reader;
        this.initializationStrategy = initializationStrategy;
        this.pathResolver = pathResolver;
    }

    @Override
    public <T> @NotNull Result<ConfigProvider<T>> createProvider(Configuration<T> configuration) {
        return initializationStrategy.initializeConfig(configuration.getFileName())
                .map(unit -> {
                    final Path configPath = pathResolver.getConfigPath(configuration.getFileName());
                    return new ReadingConfigProvider<>(configPath, configuration, reader);
                });

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
