package me.itstautvydas.modernwaystones.managers;

import me.itstautvydas.modernwaystones.ModernWaystones;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public abstract class DataManagerBase<D> {
    private final ModernWaystones plugin;

    private File file;
    protected YamlConfiguration config;
    protected Set<String> keys;

    public DataManagerBase(ModernWaystones plugin, String filename) throws IOException {
        this.plugin = plugin;
        keys = new HashSet<>();
        checkFile(filename);
    }

    private void checkFile(String filename) throws IOException {
        file = new File(ModernWaystones.getInstance().getDataFolder(), filename + ".yml");

        if (!file.exists()) {
            ModernWaystones.getInstance().getLogger().info("Creating " + filename + ".yml");
            file.createNewFile();
        }

        config = YamlConfiguration.loadConfiguration(file);
        config.options().parseComments(true);

        keys = config.getKeys(false);
    }

    public final ModernWaystones getPlugin() {
        return plugin;
    }

    public final File getFile() {
        return file;
    }

    public final FileConfiguration getConfig() {
        return config;
    }

    public abstract void loadData() throws Exception;
    public abstract void saveData();
    public abstract D getData();

    protected void save() {
        try {
            config.save(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void load() throws IOException, InvalidConfigurationException {
        config.load(file);
    }
}
