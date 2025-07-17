package fun.pozzoo.quickwaystones;

import fun.pozzoo.quickwaystones.data.WaystoneData;
import fun.pozzoo.quickwaystones.events.WaystoneEventsHandler;
import fun.pozzoo.quickwaystones.gui.WaystoneDialogs;
import fun.pozzoo.quickwaystones.managers.CraftManager;
import fun.pozzoo.quickwaystones.managers.DataManager;
import io.github.projectunified.unidialog.paper.PaperDialogManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class QuickWaystones extends JavaPlugin {
    private static QuickWaystones plugin;
    private DataManager dataManager;
    private CraftManager craftManager;
    private PaperDialogManager dialogManager;
    private WaystoneDialogs waystoneDialogs;
    private Material waystoneBlockType;
    private Map<Location, WaystoneData> waystonesMap;
    private static int lastWaystoneID = 0;
    private static Metrics metrics;

    @Override
    public void onEnable() {
        plugin = this;

        saveDefaultConfig();

        craftManager = new CraftManager(this);
        craftManager.registerRecipes();

        new WaystoneEventsHandler(this);

        waystonesMap = new HashMap<>();

        dataManager = new DataManager(this);
        dataManager.loadWaystonesData();

        dialogManager = new PaperDialogManager(this, "quickwaystones");
        dialogManager.register();

        waystoneDialogs = new WaystoneDialogs(this);
        waystoneDialogs.registerEvents();

        lastWaystoneID = waystonesMap.size();

        metrics = new Metrics(this, 22064);

        waystoneBlockType = Material.valueOf(Objects.requireNonNull(getConfig().getString("Item.Material")).toUpperCase(Locale.ROOT));
    }

    @Override
    public void onDisable() {
        dataManager.saveWaystoneData(waystonesMap.values());
        dialogManager.unregister();
        metrics.shutdown();
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public PaperDialogManager getDialogManager() {
        return dialogManager;
    }

    public boolean isWaystoneBlock(Block block) {
        return block.getType() == waystoneBlockType && waystonesMap.containsKey(block.getLocation());
    }

    public boolean isWaystoneItem(ItemStack item) {
        return craftManager.createWaystoneItem(null).isSimilar(item) &&
                item.getItemMeta().getPersistentDataContainer().has(craftManager.getPersistentDataKey(), PersistentDataType.STRING);
    }

    public Map<Location, WaystoneData> getWaystonesMap() {
        return waystonesMap;
    }

    public WaystoneDialogs getWaystoneDialogs() {
        return waystoneDialogs;
    }

    public static QuickWaystones getInstance() {
        return plugin;
    }

    public static int getAndIncrementLastWaystoneID() {
        lastWaystoneID++;
        return lastWaystoneID;
    }

    public static String multiMessage(String path) {
        path = "Messages." + path;
        if (config().isList(path))
            return String.join("\n<reset>", config().getStringList( path));
        return config().getString(path, "<message_not_found>");
    }

    public static Component message(String path) {
        return Utils.formatString(multiMessage(path));
    }

    public static Component message(String path, Map<String, String> placeholders) {
        String text = multiMessage(path);
        for (Map.Entry<String, String> entry : placeholders.entrySet())
            text = text.replace("{" + entry.getKey() + "}", entry.getValue());
        return Utils.formatString(text);
    }

    public static FileConfiguration config() {
        return getInstance().getConfig();
    }
}
