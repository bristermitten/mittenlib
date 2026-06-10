package me.bristermitten.mittenlib.docs.tutorial;

import com.google.inject.Guice;
import com.google.inject.Injector;
import me.bristermitten.mittenlib.MittenLib;
// Assuming this is generated in your package:
// import com.yourplugin.config.ConfigLoaderModule;
import org.bukkit.plugin.java.JavaPlugin;

public class PluginInitExample extends JavaPlugin {
    @Override
    public void onEnable() {
        MittenLib<PluginInitExample> mittenLib = MittenLib.withDefaults(this)
                // Register the generated config module which binds all your configs
                .config(new ConfigLoaderModule());

        Injector injector = mittenLib.setup();
        
        // Get your class that depends on the ConfigProvider
        UsageExample example = injector.getInstance(UsageExample.class);
        example.doSomething();
    }
}
