package me.itstautvydas.modernwaystones.managers;

import me.itstautvydas.modernwaystones.ModernWaystones;
import me.itstautvydas.modernwaystones.data.WaystoneData;
import org.bukkit.Location;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class WaystoneDataManager extends DataManagerBase<Map<Location, WaystoneData>> {
    private final Map<Location, WaystoneData> waystonesMap = new HashMap<>();

    public WaystoneDataManager(ModernWaystones plugin) throws IOException {
        super(plugin, "waystones");
    }

    @Override
    public void loadData() throws IOException, InvalidConfigurationException {
        load();
        for (String key : keys) {
            WaystoneData waystoneData = new WaystoneData(
                    key,
                    config.getString(key + ".name"),
                    config.getLocation(key + ".location"),
                    config.getString(key + ".owner"),
                    UUID.fromString(Objects.requireNonNull(config.getString(key + ".ownerId"))),
                    config.getLong(key + ".createdAt")
            );
            waystoneData.setInternal(config.getBoolean(key + ".internal"));
            waystoneData.setGloballyAccessible(config.getBoolean(key + ".global"));
            for (String uuidString : config.getStringList(key + ".addedPlayers")) {
                UUID uuid = UUID.fromString(uuidString);
                waystoneData.addPlayer(uuid);
            }
            waystonesMap.put(waystoneData.getLocation(), waystoneData);
        }
    }

    @Override
    public void saveData() {
        config = new YamlConfiguration();
        for (WaystoneData waystone : waystonesMap.values()) {
            config.set(waystone.getUniqueId() + ".name", waystone.getName());
            config.set(waystone.getUniqueId() + ".location", waystone.getLocation());
            config.set(waystone.getUniqueId() + ".owner", waystone.getOwner());
            config.set(waystone.getUniqueId() + ".ownerId", waystone.getOwnerUniqueId().toString());
            config.set(waystone.getUniqueId() + ".global", waystone.isInternal() || waystone.isGloballyAccessible());
            config.set(waystone.getUniqueId() + ".createdAt", waystone.getCreatedAt());
            if (waystone.isInternal() || getPlugin().shouldDefaultDataBeSaved())
                config.set(waystone.getUniqueId() + ".internal", waystone.isInternal());
            if (!waystone.getAddedPlayers().isEmpty())
                config.set(waystone.getUniqueId() + ".addedPlayers", waystone.getAddedPlayers()
                        .stream()
                        .map(UUID::toString)
                        .toList());
        }
        save();
    }

    @Override
    public Map<Location, WaystoneData> getData() {
        return waystonesMap;
    }

    public void updatePlayerName(UUID uuid) {
        waystonesMap.values()
                .stream()
                .filter(x -> x.isOwner(uuid))
                .forEach(WaystoneData::updateOwnerName);
    }
}
