package me.bristermitten.mittenlib.config.provider.construct;

import me.bristermitten.mittenlib.config.Configuration;
import me.bristermitten.mittenlib.config.paths.ConfigInitializationStrategy;
import me.bristermitten.mittenlib.config.paths.ConfigPathResolver;
import me.bristermitten.mittenlib.config.provider.ConfigProvider;
import me.bristermitten.mittenlib.config.provider.ReadingConfigProvider;
import me.bristermitten.mittenlib.config.provider.StringReadingConfigProvider;
import me.bristermitten.mittenlib.config.reader.ConfigReader;
import me.bristermitten.mittenlib.files.FileType;
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
    public <T> @NotNull ConfigProvider<T> createProvider(Configuration<T> configuration) {
        initializationStrategy.initializeConfig(configuration.getFileName());
        final Path configPath = pathResolver.getConfigPath(configuration.getFileName());
        return new ReadingConfigProvider<>(configPath, configuration, reader);
    }


    @Override
    public @NotNull <T> ConfigProvider<T> createStringReaderProvider(String data, Configuration<T> configuration) {
        return new StringReadingConfigProvider<>(data, configuration, reader);
    }

    @Override
    @NotNull
    public <T> ConfigProvider<T> createStringReaderProvider(FileType type, String data, Configuration<T> configuration) {
        return new StringReadingConfigProvider<>(data, configuration, reader.withLoader(type.loader()));
    }
}
