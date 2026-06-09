package me.bristermitten.mittenlib.files.yaml;

import com.google.inject.Provider;
import org.yaml.snakeyaml.DumperOptions;

public class DefaultDumperOptionsProvider implements Provider<DumperOptions> {
    @Override
    public DumperOptions get() {
        DumperOptions options = new DumperOptions();
        options.setIndent(4);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        return options;
    }
}
