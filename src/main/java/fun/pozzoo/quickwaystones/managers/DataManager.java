package fun.pozzoo.quickwaystones.managers;

import fun.pozzoo.quickwaystones.QuickWaystones;
import fun.pozzoo.quickwaystones.data.WaystoneData;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DataManager {
    private final QuickWaystones plugin;

    private File file;
    private YamlConfiguration config;
    private YamlConfiguration configOverwrite;
    private Set<String> keys;

    public DataManager(QuickWaystones plugin) {
        this.plugin = plugin;
        keys = new HashSet<>();
        checkFile();
    }

    private void checkFile() {
        file = new File(QuickWaystones.getInstance().getDataFolder(), "waystones.yml");

        if (!file.exists()) {
            QuickWaystones.getInstance().getLogger().info("Creating waystones.yml");
            QuickWaystones.getInstance().saveResource("waystones.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(file);
        config.options().parseComments(true);

        if (config.getKeys(true).isEmpty()) return;

        keys = Objects.requireNonNull(config.getConfigurationSection("Waystones.")).getKeys(false);
    }

    public void loadWaystonesData() {
        try {
            config.load(file);

            for (String key : keys) {
                WaystoneData waystoneData = new WaystoneData(key,
                        config.getString("Waystones." + key + ".name"),
                        config.getLocation("Waystones." + key + ".location"),
                        config.getString("Waystones." + key + ".owner"),
                        UUID.fromString(Objects.requireNonNull(config.getString("Waystones." + key + ".ownerId"))),
                        config.getLong("Waystones." + key + ".createdAt")
                );
                waystoneData.setLastUsedAt(config.getLong("Waystones." + key + ".lastUsedAt"));
                waystoneData.setInternal(config.getBoolean("Waystones." + key + ".internal"));
                waystoneData.setGloballyAccessible(config.getBoolean("Waystones." + key + ".global"));
                for (String uuidString : config.getStringList("Waystones." + key + ".addedPlayers")) {
                    UUID uuid = UUID.fromString(uuidString);
                    waystoneData.addPlayer(uuid);
                }
                plugin.getWaystonesMap().put(waystoneData.getLocation(), waystoneData);
            }
        } catch (InvalidConfigurationException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveWaystoneData(Collection<WaystoneData> waystones) {
        configOverwrite = new YamlConfiguration();

        for (WaystoneData waystone : waystones) {
            configOverwrite.set("Waystones." + waystone.getID() + ".name", waystone.getName());
            configOverwrite.set("Waystones." + waystone.getID() + ".location", waystone.getLocation());
            configOverwrite.set("Waystones." + waystone.getID() + ".owner", waystone.getOwner());
            configOverwrite.set("Waystones." + waystone.getID() + ".ownerId", waystone.getOwnerUniqueId().toString());
            configOverwrite.set("Waystones." + waystone.getID() + ".global", waystone.isInternal() || waystone.isGloballyAccessible());
            configOverwrite.set("Waystones." + waystone.getID() + ".createdAt", waystone.getCreatedAt());
            configOverwrite.set("Waystones." + waystone.getID() + ".lastUsedAt", waystone.getLastUsedAt());
            if (waystone.isInternal())
                configOverwrite.set("Waystones." + waystone.getID() + ".internal", waystone.isInternal());
            if (!waystone.getAddedPlayers().isEmpty())
                configOverwrite.set("Waystones." + waystone.getID() + ".addedPlayers", waystone.getAddedPlayers()
                        .stream()
                        .map(UUID::toString)
                        .toList());
        }

        save();
    }

    public void saveWaystoneData() {
        saveWaystoneData(plugin.getWaystonesMap().values());
    }

    public void save() {
        QuickWaystones.getInstance().saveResource("waystones.yml", true);

        try {
            configOverwrite.save(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
