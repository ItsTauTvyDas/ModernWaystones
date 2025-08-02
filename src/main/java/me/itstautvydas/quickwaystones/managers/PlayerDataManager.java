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
                    config.getBoolean(key + ".sorting.inverted", false),
                    config.getBoolean(key + ".sorting.showNumbers", getPlugin().getConfig().getBoolean("WaystoneScreen.ShowNumbers")),
                    config.getBoolean(key + ".sorting.hideLocation", getPlugin().getConfig().getBoolean("UncategorizedPlayerData.VisuallyHideLocations"))
            );
            playerDataMap.put(uuid, playerData);
        }
    }

    @Override
    public void saveData() {
        config = new YamlConfiguration();
        for (PlayerData playerData : playerDataMap.values()) {
            config.set(playerData.getUniqueId().toString() + ".sorting.type", playerData.getSortType().toString());
            if (playerData.isSortingInverted() || getPlugin().shouldDefaultDataBeSaved())
                config.set(playerData.getUniqueId().toString() + ".sorting.inverted", playerData.isSortingInverted());
            if (playerData.getShowNumbers() || getPlugin().shouldDefaultDataBeSaved())
                config.set(playerData.getUniqueId().toString() + ".sorting.showNumbers", playerData.getShowNumbers());
            if (playerData.getHideLocation() || getPlugin().shouldDefaultDataBeSaved())
                config.set(playerData.getUniqueId().toString() + ".sorting.hideLocation", playerData.getHideLocation());
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
                getPlugin().getConfig().getBoolean("WaystoneScreen.DefaultPlayerSorting.Inverted"),
                getPlugin().getConfig().getBoolean("WaystoneScreen.ShowNumbers"),
                getPlugin().getConfig().getBoolean("UncategorizedPlayerData.VisuallyHideLocations")
        );
        if (save) {
            getPlugin().getPlayerDataMap().put(player.getUniqueId(), data);
            saveData();
        }
        return data;
    }
}
