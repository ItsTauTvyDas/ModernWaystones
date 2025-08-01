package me.itstautvydas.quickwaystones.managers;

import me.itstautvydas.quickwaystones.QuickWaystones;
import me.itstautvydas.quickwaystones.Utils;
import me.itstautvydas.quickwaystones.data.WaystoneData;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Locale;
import java.util.Objects;

public class CraftManager {
    private final QuickWaystones plugin;
    private final NamespacedKey persistentWaystoneNameKey;
    private final NamespacedKey persistentWaystoneVisibilityKey;
    private final NamespacedKey craftKey;

    public CraftManager(QuickWaystones plugin) {
        this.plugin = plugin;
        this.persistentWaystoneNameKey = new NamespacedKey(QuickWaystones.getInstance(), "name");
        this.persistentWaystoneVisibilityKey = new NamespacedKey(QuickWaystones.getInstance(), "visibility_attr");
        this.craftKey = new NamespacedKey(QuickWaystones.getInstance(), "recipe");
    }

    public NamespacedKey getPersistentWaystoneNameKey() {
        return persistentWaystoneNameKey;
    }

    public NamespacedKey getPersistentWaystoneVisibilityKey() {
        return persistentWaystoneVisibilityKey;
    }

    public void registerRecipes() {
        if (plugin.getServer().getRecipe(craftKey) != null)
            plugin.getServer().removeRecipe(craftKey, true);
        ShapedRecipe recipe = new ShapedRecipe(craftKey, createWaystoneItem(null, null));
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

    public ItemStack createWaystoneItem(WaystoneData waystone) {
        return createWaystoneItem(waystone.getName(), waystone.isGloballyAccessible());
    }

    public ItemStack createWaystoneItem(String name, Boolean visibility) {
        ItemStack item = new ItemStack(Material.valueOf(plugin.getConfig().getString("Item.Material", Material.LODESTONE.name()).toUpperCase(Locale.ROOT)));
        ItemMeta meta = item.getItemMeta();
        // For now, only visibility
        meta.getPersistentDataContainer().set(persistentWaystoneNameKey, PersistentDataType.STRING, name == null ? "" : name);
        if (visibility == null)
            visibility = plugin.getConfig().getBoolean("DefaultWaystone.GloballyAccessible");
        meta.getPersistentDataContainer().set(persistentWaystoneVisibilityKey, PersistentDataType.BOOLEAN, visibility);
        String visibilityString = plugin.getConfig().getString("Messages.WaystoneAttributes." + (visibility ? "Public" : "Private"));
        ConfigurationSection section = Objects.requireNonNull(plugin.getConfig().getConfigurationSection("Item"));
        meta.setMaxStackSize(section.getInt("MaxStackSize", 1));
        meta.setEnchantmentGlintOverride(section.getBoolean("EnchantmentGlint", true));
        meta.displayName(Utils.formatItemName(section.getString("DisplayName")));
        meta.setRarity(ItemRarity.valueOf(section.getString("Rarity", ItemRarity.UNCOMMON.name()).toUpperCase(Locale.ROOT)));
        meta.lore(section.getStringList(name == null ? "Lore" : "LoreWithData")
                .stream()
                .map(x -> Utils.formatItemName(
                        x.replace("{name}", name == null ? "<unknown>" : name).replace("{visibility}", Objects.requireNonNull(visibilityString))
                ))
                .toList());
        item.setItemMeta(meta);
        return item;
    }
}