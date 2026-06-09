package me.bristermitten.mittenlib.files.yaml;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class YamlProvider implements Provider<Yaml> {
    private final Provider<DumperOptions> optionsProvider;

    @Inject
    YamlProvider(Provider<DumperOptions> optionsProvider) {
        this.optionsProvider = optionsProvider;
    }

    @Override
    public Yaml get() {
        return new Yaml(optionsProvider.get());
    }
}
