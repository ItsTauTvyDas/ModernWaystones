package fun.pozzoo.quickwaystones.managers;

import fun.pozzoo.quickwaystones.QuickWaystones;
import fun.pozzoo.quickwaystones.Utils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class CraftManager {
    private final QuickWaystones plugin;
    private final NamespacedKey persistentItemDataKey;
    private final NamespacedKey craftKey;

    public CraftManager(QuickWaystones plugin) {
        this.plugin = plugin;
        this.persistentItemDataKey = new NamespacedKey(QuickWaystones.getInstance(), "waystone_name");
        this.craftKey = new NamespacedKey(QuickWaystones.getInstance(), "waypoint_recipe");
    }

    public NamespacedKey getPersistentItemDataKey() {
        return persistentItemDataKey;
    }

    public void registerRecipes() {
        if (plugin.getServer().getRecipe(craftKey) != null)
            plugin.getServer().removeRecipe(craftKey, true);
        ShapedRecipe recipe = new ShapedRecipe(craftKey, createWaystoneItem(null));
        recipe.shape(plugin.getConfig().getStringList("Item.Recipe.Layout").toArray(new String[0]));
        ConfigurationSection ingredients = plugin.getConfig().getConfigurationSection("Item.Recipe.Ingredients");
        if (ingredients == null)
            return;
        for (String key : ingredients.getKeys(false)) {
            char token = key.charAt(0);
            Material type = Material.valueOf(ingredients.getString(key, "BEDROCK").toUpperCase(Locale.ROOT));
            recipe.setIngredient(token, type);
        }
        plugin.getServer().addRecipe(recipe, true);
        plugin.getLogger().info("Custom recipe registered!");
    }

    public ItemStack createWaystoneItem(String name) {
        ItemStack item = new ItemStack(Material.valueOf(plugin.getConfig().getString("Item.Material", Material.LODESTONE.name()).toUpperCase(Locale.ROOT)));
        ItemMeta meta = item.getItemMeta();
        meta.setMaxStackSize(1);
        meta.setEnchantmentGlintOverride(plugin.getConfig().getBoolean("Item.EnchantmentGlint", true));
        meta.displayName(Utils.formatItemName(plugin.getConfig().getString("Item.DisplayName")));
        meta.setRarity(ItemRarity.valueOf(plugin.getConfig().getString("Item.Rarity", ItemRarity.UNCOMMON.name()).toUpperCase(Locale.ROOT)));
        meta.lore(Objects.requireNonNull(plugin.getConfig().getConfigurationSection("Item"))
                .getStringList(name == null ? "Lore" : "LoreWithName")
                .stream()
                .map(x -> x.replace("{name}", name == null ? "<unknown>" : name))
                .map(Utils::formatItemName)
                .toList());
        meta.getPersistentDataContainer().set(persistentItemDataKey, PersistentDataType.STRING, name == null ? "" : name);
        item.setItemMeta(meta);
        return item;
    }
}