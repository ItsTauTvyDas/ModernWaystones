package me.itstautvydas.modernwaystones.managers;

import me.itstautvydas.modernwaystones.ModernWaystones;
import me.itstautvydas.modernwaystones.Utils;
import me.itstautvydas.modernwaystones.data.WaystoneData;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.util.*;

public class WaystoneDataManager extends DataManagerBase<Map<Location, WaystoneData>> {
    private final Map<Location, WaystoneData> waystonesMap = new HashMap<>();

    public WaystoneDataManager(ModernWaystones plugin) throws IOException {
        super(plugin, "waystones");
    }

    @Override
    public void loadData() {
        load();
        for (String key : keys) {
            try {
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
            } catch (Exception ex) {
                getPlugin().getLogger().severe("Failed to load data for a waystone with an ID of" + key + ", skipping");
                Utils.printStackTrace(getPlugin(), ex);
            }
        }
    }

    @Override
    public void saveData() {
        YamlConfiguration oldConfig = config;
        boolean revertConfig = false;
        boolean canRevert = getPlugin().getConfig().getBoolean("DataConfigurations.RevertWaystonesConfigurationIfOneErrored");
        config = new YamlConfiguration();
        for (WaystoneData waystone : waystonesMap.values()) {
            try {
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
            } catch (Exception ex) {
                config = oldConfig;
                getPlugin().getLogger().severe("Failed to save waystone with an ID of " + waystone.getUniqueId());
                Utils.printStackTrace(getPlugin(), ex);
                if (canRevert) {
                    getPlugin().getLogger().warning("Waystones configuration is going to be reverted to previous version!");
                    revertConfig = true;
                } else {
                    getPlugin().getLogger().warning("This waystone is going to be skipped...");
                }
                break;
            }
        }
        if (revertConfig)
            config = oldConfig;
        save();
    }

    @Override
    public Map<Location, WaystoneData> getData() {
        return waystonesMap;
    }

    public void deleteExpiredWaystones() {
        getData().entrySet().removeIf(entry ->
                        entry.getValue().getMarkedForDeletionTime() >= System.currentTimeMillis());
    }

    public List<WaystoneData> filterPlayerWaystones(UUID playerUUID) {
        return getData().values().stream()
                .filter(waystone -> {
                    Utils.loadChunkIfNeeded(waystone.getLocation());
                    if (getPlugin().isWaystoneDestroyed(waystone.getBlock()) && !waystone.isOwner(playerUUID))
                        return false;
                    if (waystone.isOwner(playerUUID) || waystone.isInternal() || waystone.isGloballyAccessible())
                        return true;
                    if (waystone.getAddedPlayers().contains(playerUUID)) {
                        return getPlugin().isWaystoneFriendsBlock(waystone.getLocation().clone().add(0, -1, 0).getBlock());
                    }
                    return false;
                }).toList();
    }

    public void updatePlayerName(UUID uuid) {
        waystonesMap.values()
                .stream()
                .filter(x -> x.isOwner(uuid))
                .forEach(WaystoneData::updateOwnerName);
    }

    public WaystoneData findById(String uuid) {
        return waystonesMap.values()
                .stream()
                .filter(x -> Objects.equals(x.getUniqueId(), uuid))
                .findFirst()
                .orElse(null);
    }
}