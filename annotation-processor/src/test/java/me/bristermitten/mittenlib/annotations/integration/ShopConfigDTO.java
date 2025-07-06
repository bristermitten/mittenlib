package me.bristermitten.mittenlib.annotations.integration;

import me.bristermitten.mittenlib.config.Config;
import me.bristermitten.mittenlib.config.generate.GenerateToString;
import me.bristermitten.mittenlib.config.names.NamingPattern;
import me.bristermitten.mittenlib.config.names.NamingPatterns;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

@Config
@NamingPattern(NamingPatterns.LOWER_KEBAB_CASE)
@GenerateToString
public class ShopConfigDTO {
    String title;
    int rows;
    Map<String, ShopItemConfigDTO> items;


    @Config
    static class ShopItemConfigDTO extends ItemConfigDTO {
        int medianStock;
        @Nullable Integer slot;
    }

    @Config
    @NamingPattern(NamingPatterns.LOWER_KEBAB_CASE)
    public static class ItemConfigDTO {
        Material type;
        @Nullable String name;
        @Nullable List<String> lore;

        @Nullable String player;

        boolean glow = false;

        @Nullable String dyeColor;

        @Nullable List<ItemFlag> flags;

        @Nullable List<EnchantmentDTO> enchantments;

        boolean unbreakable = false;

        @Config
        static class EnchantmentDTO {
            Enchantment type;
            int level = 1;
        }
    }

}