package fun.pozzoo.quickwaystones.managers;

import fun.pozzoo.quickwaystones.QuickWaystones;
import fun.pozzoo.quickwaystones.Utils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class CraftManager {
    private final FileConfiguration config;
    private final NamespacedKey persistentDataKey;

    public CraftManager(QuickWaystones plugin) {
        this.config = plugin.getConfig();
        this.persistentDataKey = new NamespacedKey(QuickWaystones.getInstance(), "waystone_name");
    }

    public NamespacedKey getPersistentDataKey() {
        return persistentDataKey;
    }

    public void registerRecipes() {
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(QuickWaystones.getInstance(), "waypoint"), createWaystoneItem(null));
        recipe.shape(config.getStringList("Item.Recipe.Layout").toArray(new String[0]));
        ConfigurationSection ingredients = config.getConfigurationSection("Item.Recipe.Ingredients");
        if (ingredients == null)
            return;
        for (String key : ingredients.getKeys(false)) {
            char token = key.charAt(0);
            Material type = Material.valueOf(ingredients.getString(key, "BEDROCK").toUpperCase(Locale.ROOT));
            recipe.setIngredient(token, type);
        }
        QuickWaystones.getInstance().getServer().addRecipe(recipe);
    }

    public ItemStack createWaystoneItem(String name) {
        ItemStack item = new ItemStack(Material.valueOf(config.getString("Item.Material", Material.LODESTONE.name()).toUpperCase(Locale.ROOT)));
        ItemMeta meta = item.getItemMeta();
        meta.setEnchantmentGlintOverride(config.getBoolean("Item.EnchantmentGlint", true));
        meta.displayName(Utils.formatItemName(config.getString("Item.DisplayName")));
        meta.setRarity(ItemRarity.valueOf(config.getString("Item.Rarity", ItemRarity.UNCOMMON.name()).toUpperCase(Locale.ROOT)));
        meta.lore(config.getConfigurationSection("Item")
                .getStringList(name == null ? "Lore" : "LoreWithName")
                .stream()
                .map(x -> x.replace("{name}", name == null ? "<unknown>" : name))
                .map(Utils::formatString)
                .toList());
        meta.getPersistentDataContainer().set(persistentDataKey, PersistentDataType.STRING, name == null ? "" : name);
        item.setItemMeta(meta);
        return item;
    }
}
