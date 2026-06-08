package me.bristermitten.mittenlib.docs.tutorial;

import com.google.inject.Inject;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Logger;

public class MyService {
    private final JavaPlugin plugin;
    private final Logger logger;

    @Inject
    public MyService(JavaPlugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    public void start() {
        logger.info("MyService started successfully for " + plugin.getName() + "!");
    }
}
