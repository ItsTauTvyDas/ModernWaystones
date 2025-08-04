package me.itstautvydas.modernwaystones.managers;

import me.itstautvydas.modernwaystones.ModernWaystones;
import me.itstautvydas.modernwaystones.Utils;
import me.itstautvydas.modernwaystones.data.PlayerData;
import me.itstautvydas.modernwaystones.data.WaystoneData;
import me.itstautvydas.modernwaystones.enums.PlayerSortType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class PlayerDataManager extends DataManagerBase<Map<UUID, PlayerData>> {
    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();

    public PlayerDataManager(ModernWaystones plugin) throws IOException {
        super(plugin, "players");
    }

    @Override
    public void loadData() throws IOException, InvalidConfigurationException {
        load();
        Map<String, WaystoneData> map = getPlugin().getSimpleWaystonesMap();
        for (String key : keys) {
            try {
                UUID uuid = UUID.fromString(key);
                PlayerData playerData = new PlayerData(
                        uuid,
                        PlayerSortType.valueOf(config.getString(key + ".sorting.type")),
                        config.getStringList(key + ".sorting.list").stream().map(map::get).toList(),
                        config.getBoolean(key + ".sorting.inverted", getPlugin().getConfig().getBoolean("WaystoneScreen.DefaultPlayerSorting.Inverse"))
                );
                playerData.setShowNumbers(config.getBoolean(key + ".waystonesListScreen.showNumbers", getPlugin().getConfig().getBoolean("WaystoneScreen.ShowNumbers")));
                playerData.setHideLocation(config.getBoolean(key + ".waystonesListScreen.hideLocation", getPlugin().getConfig().getBoolean("DefaultPlayerData.VisuallyHideLocations")));
                playerData.setWaystoneScreenColumns(config.getInt(key + ".waystonesListScreen.columns", getPlugin().getConfig().getInt("DefaultPlayerData.WaystoneScreenColumns.Default")));
                playerData.setWaystoneButtonWidth(config.getInt(key + ".waystonesListScreen.buttonWidth", getPlugin().getConfig().getInt("DefaultPlayerData.WaystoneButtonWidth.Default")));
                playerData.setShowAttributes(config.getBoolean(key + ".waystonesListScreen.showAttributes", getPlugin().getConfig().getBoolean("DefaultPlayerData.ShowAttributes")));
                playerDataMap.put(uuid, playerData);
            } catch (Exception ex) {
                getPlugin().getLogger().severe("Failed to load data for " + key + ": " + ex.getMessage());
                Utils.printStackTrace(getPlugin(), ex);
            }
        }
    }

    @Override
    public void saveData() {
        config = new YamlConfiguration();
        for (PlayerData playerData : playerDataMap.values()) {
            try {
                config.set(playerData.getUniqueId().toString() + ".sorting.type", playerData.getSortType().toString());
                if (playerData.isSortingInverted() != getPlugin().getConfig().getBoolean("WaystoneScreen.DefaultPlayerSorting.Inverse")
                        || getPlugin().shouldDefaultDataBeSaved())
                    config.set(playerData.getUniqueId().toString() + ".sorting.inverted", playerData.isSortingInverted());

                if (playerData.getShowNumbers() != getPlugin().getConfig().getBoolean("WaystoneScreen.ShowNumbers")
                        || getPlugin().shouldDefaultDataBeSaved())
                    config.set(playerData.getUniqueId().toString() + ".waystonesListScreen.showNumbers", playerData.getShowNumbers());

                if (playerData.getHideLocation() != getPlugin().getConfig().getBoolean("DefaultPlayerData.VisuallyHideLocations")
                        || getPlugin().shouldDefaultDataBeSaved())
                    config.set(playerData.getUniqueId().toString() + ".waystonesListScreen.hideLocation", playerData.getHideLocation());

                if (playerData.getWaystoneScreenColumns() != getPlugin().getConfig().getInt("DefaultPlayerData.WaystoneScreenColumns.Default")
                        || getPlugin().shouldDefaultDataBeSaved())
                    config.set(playerData.getUniqueId().toString() + ".waystonesListScreen.columns", playerData.getWaystoneScreenColumns());

                if (playerData.getWaystoneButtonWidth() != getPlugin().getConfig().getInt("DefaultPlayerData.WaystoneButtonWidth.Default")
                        || getPlugin().shouldDefaultDataBeSaved())
                    config.set(playerData.getUniqueId().toString() + ".waystonesListScreen.buttonWidth", playerData.getWaystoneButtonWidth());

                if (playerData.getShowAttributes() != getPlugin().getConfig().getBoolean("DefaultPlayerData.ShowAttributes")
                        || getPlugin().shouldDefaultDataBeSaved())
                    config.set(playerData.getUniqueId().toString() + ".waystonesListScreen.showAttributes", playerData.getShowAttributes());

                config.set(playerData.getUniqueId().toString() + ".sorting.list", playerData.getSortedWaystones()
                        .stream()
                        .map(WaystoneData::getUniqueId)
                        .toList());
            } catch (Exception ex) {
                getPlugin().getLogger().severe("Failed to save data for " + playerData.getUniqueId() + ": " + ex.getMessage());
                Utils.printStackTrace(getPlugin(), ex);
            }
        }
        save();
    }

    @Override
    public Map<UUID, PlayerData> getData() {
        return playerDataMap;
    }

    public PlayerData createData(UUID playerUUID, boolean save) {
        PlayerData data = new PlayerData(
                playerUUID,
                PlayerSortType.valueOf(getPlugin().getConfig().getString("WaystoneScreen.DefaultPlayerSorting.Type")),
                getPlugin().getWaystoneDataManager().filterPlayerWaystones(playerUUID),
                getPlugin().getConfig().getBoolean("WaystoneScreen.DefaultPlayerSorting.Inverted")
        );
        data.setHideLocation(getPlugin().getConfig().getBoolean("DefaultPlayerData.VisuallyHideLocations"));
        data.setShowNumbers(getPlugin().getConfig().getBoolean("WaystoneScreen.ShowNumbers"));
        data.setShowAttributes(getPlugin().getConfig().getBoolean("DefaultPlayerData.ShowAttributes"));
        data.setWaystoneScreenColumns(getPlugin().getConfig().getInt("DefaultPlayerData.WaystoneScreenColumns.Default"));
        data.setWaystoneButtonWidth(getPlugin().getConfig().getInt("DefaultPlayerData.WaystoneButtonWidth.Default"));
        if (save) {
            getData().put(playerUUID, data);
            saveData();
        }
        return data;
    }

    public void updatePlayersAccesses(WaystoneData waystone, boolean visibilityChange, boolean addedPlayersChange, boolean save) {
        Set<UUID> set = new HashSet<>();
        if (addedPlayersChange)
            set.addAll(waystone.getAddedPlayers());
        if (visibilityChange)
            set.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getUniqueId).collect(Collectors.toSet()));
        for (UUID playerUUID : set) {
            PlayerData data = playerDataMap.get(playerUUID);
            if (data == null) {
                getData().put(playerUUID, createData(playerUUID, false));
                continue;
            }
            data.setWaystones(getPlugin().getWaystoneDataManager().filterPlayerWaystones(playerUUID));
        }
        if (save)
            saveData();
    }
}
