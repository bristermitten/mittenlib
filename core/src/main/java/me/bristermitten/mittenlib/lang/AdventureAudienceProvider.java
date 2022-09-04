package me.bristermitten.mittenlib.lang;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.plugin.Plugin;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * A provider for {@link BukkitAudiences} that uses the {@link Plugin}
 */
public class AdventureAudienceProvider implements Provider<BukkitAudiences> {
    private final Plugin plugin;

    @Inject
    AdventureAudienceProvider(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public BukkitAudiences get() {
        return BukkitAudiences.create(plugin);
    }
}
