package me.bristermitten.mittenlib;

import java.io.File;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;

public class TestMittenLib extends JavaPlugin {
    public TestMittenLib() {}

    public TestMittenLib(JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder, File other) {
        super(loader, description, dataFolder, other);
    }
}
