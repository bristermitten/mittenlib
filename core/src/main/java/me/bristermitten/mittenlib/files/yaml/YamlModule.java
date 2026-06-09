package me.bristermitten.mittenlib.files.yaml;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.OptionalBinder;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class YamlModule extends AbstractModule {
    @Override
    protected void configure() {
        OptionalBinder.newOptionalBinder(binder(), DumperOptions.class)
                .setDefault()
                .toProvider(DefaultDumperOptionsProvider.class);
        bind(Yaml.class).toProvider(YamlProvider.class);
    }
}
