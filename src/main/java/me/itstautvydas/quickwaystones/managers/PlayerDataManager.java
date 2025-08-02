package me.itstautvydas.quickwaystones.managers;

import me.itstautvydas.quickwaystones.QuickWaystones;
import me.itstautvydas.quickwaystones.data.PlayerData;
import me.itstautvydas.quickwaystones.data.WaystoneData;
import me.itstautvydas.quickwaystones.enums.PlayerSortType;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager extends DataManagerBase<Map<UUID, PlayerData>> {
    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();

    public PlayerDataManager(QuickWaystones plugin) throws IOException {
        super(plugin, "players");
    }

    @Override
    public void loadData() throws IOException, InvalidConfigurationException {
        load();
        Map<String, WaystoneData> map = getPlugin().getSimpleWaystonesMap();
        for (String key : keys) {
            UUID uuid = UUID.fromString(key);
            PlayerData playerData = new PlayerData(
                    uuid,
                    PlayerSortType.valueOf(config.getString(key + ".sorting.type")),
                    config.getStringList(key + ".sorting.list").stream().map(map::get).toList(),
                    config.getBoolean(key + ".sorting.inverted", getPlugin().getConfig().getBoolean("WaystoneScreen.DefaultPlayerSorting.Inverse"))
            );
            playerData.setShowNumbers(config.getBoolean(key + ".sorting.showNumbers", getPlugin().getConfig().getBoolean("WaystoneScreen.ShowNumbers")));
            playerData.setHideLocation(config.getBoolean(key + ".waystonesListScreen.hideLocation", getPlugin().getConfig().getBoolean("DefaultPlayerData.VisuallyHideLocations")));
            playerData.setWaystoneScreenColumns(config.getInt(key + ".waystonesListScreen.columns", getPlugin().getConfig().getInt("DefaultPlayerData.WaystoneScreenColumns.Default")));
            playerData.setWaystoneButtonWidth(config.getInt(key + ".waystonesListScreen.buttonWidth", getPlugin().getConfig().getInt("DefaultPlayerData.WaystoneButtonWidth.Default")));
            playerDataMap.put(uuid, playerData);
        }
    }

    @Override
    public void saveData() {
        config = new YamlConfiguration();
        for (PlayerData playerData : playerDataMap.values()) {
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
                config.set(playerData.getUniqueId().toString() + ".waystonesListScreen.columns", playerData.getHideLocation());

            if (playerData.getWaystoneButtonWidth() != getPlugin().getConfig().getInt("DefaultPlayerData.WaystoneButtonWidth.Default")
                    || getPlugin().shouldDefaultDataBeSaved())
                config.set(playerData.getUniqueId().toString() + ".waystonesListScreen.buttonWidth", playerData.getHideLocation());

            if (playerData.getShowAttributes() != getPlugin().getConfig().getBoolean("DefaultPlayerData.ShowAttributes")
                    || getPlugin().shouldDefaultDataBeSaved())
                config.set(playerData.getUniqueId().toString() + ".waystonesListScreen.showAttributes", playerData.getShowAttributes());

            config.set(playerData.getUniqueId().toString() + ".sorting.list", playerData.getSortedWaystones()
                    .stream()
                    .map(WaystoneData::getUniqueId)
                    .toList());
        }
        save();
    }

    @Override
    public Map<UUID, PlayerData> getData() {
        return playerDataMap;
    }

    public PlayerData createData(Player player, boolean save) {
        PlayerData data = new PlayerData(
                player.getUniqueId(),
                PlayerSortType.valueOf(getPlugin().getConfig().getString("WaystoneScreen.DefaultPlayerSorting.Type")),
                getPlugin().getWaystonesMap().values().stream()
                        .filter(x -> x.isOwner(player) ||
                                x.getAddedPlayers().contains(player.getUniqueId()) ||
                                x.isInternal() ||
                                x.isGloballyAccessible())
                        .toList(),
                getPlugin().getConfig().getBoolean("WaystoneScreen.DefaultPlayerSorting.Inverted")
        );
        data.setHideLocation(getPlugin().getConfig().getBoolean("DefaultPlayerData.VisuallyHideLocations"));
        data.setShowNumbers(getPlugin().getConfig().getBoolean("DefaultPlayerData.ShowNumbers"));
        data.setShowAttributes(getPlugin().getConfig().getBoolean("DefaultPlayerData.ShowAttributes"));
        data.setWaystoneScreenColumns(getPlugin().getConfig().getInt("DefaultPlayerData.WaystoneScreenColumns.Default"));
        data.setWaystoneButtonWidth(getPlugin().getConfig().getInt("DefaultPlayerData.WaystoneButtonWidth.Default"));
        if (save) {
            getPlugin().getPlayerDataMap().put(player.getUniqueId(), data);
            saveData();
        }
        return data;
    }
}
