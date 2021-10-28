package me.bristermitten.mittenlib.lang;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.plugin.Plugin;

import javax.inject.Inject;
import javax.inject.Provider;

public class AdventureAudienceProvider implements Provider<BukkitAudiences> {
    private final Plugin plugin;

    @Inject
    public AdventureAudienceProvider(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public BukkitAudiences get() {
        return BukkitAudiences.create(plugin);
    }
}
