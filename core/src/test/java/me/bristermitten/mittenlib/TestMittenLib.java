package me.bristermitten.mittenlib;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;

import java.io.File;

public class TestMittenLib extends JavaPlugin {
    public TestMittenLib() {
    }

    public TestMittenLib(JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder, File other) {
        super(loader, description, dataFolder, other);
    }

}
