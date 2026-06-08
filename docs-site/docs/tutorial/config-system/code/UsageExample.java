package me.bristermitten.mittenlib.docs.tutorial;

import com.google.inject.Inject;
import me.bristermitten.mittenlib.config.provider.ConfigProvider;

public class UsageExample {
    private final ConfigProvider<AdvancedConfig> configProvider;

    @Inject
    public UsageExample(ConfigProvider<AdvancedConfig> configProvider) {
        this.configProvider = configProvider;
    }

    public void doSomething() {
        // Retrieve the currently loaded config
        AdvancedConfig config = configProvider.get();

        System.out.println("Connecting to " + config.host() + ":" + config.port());
        
        // Nested configs are just method calls
        String dbUser = config.database().username();
        System.out.println("Database user: " + dbUser);
    }
}
