package me.bristermitten.mittenlib.docs.tutorial;

import com.google.inject.Guice;
import com.google.inject.Injector;
import me.bristermitten.mittenlib.MittenLib;
import org.bukkit.plugin.java.JavaPlugin;

public class MainPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        // Initialize MittenLib with some default modules
        MittenLib<JavaPlugin> mittenLib = MittenLib.withDefaults(this);

        // Setup the Guice injector
        Injector injector = mittenLib.setup();

        // Instantiate your main application logic
        MyService service = injector.getInstance(MyService.class);
        service.start();
    }
}
