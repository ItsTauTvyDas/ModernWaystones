package fun.pozzoo.quickwaystones.managers;

import fun.pozzoo.quickwaystones.QuickWaystones;
import fun.pozzoo.quickwaystones.data.WaystoneData;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.util.*;

public class WaystoneDataManager extends DataManagerBase {
    public WaystoneDataManager(QuickWaystones plugin) throws IOException {
        super(plugin, "waystones");
    }

    @Override
    public void loadData() throws Exception {
        load();

        for (String key : keys) {
            WaystoneData waystoneData = new WaystoneData(key,
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
            getPlugin().getWaystonesMap().put(waystoneData.getLocation(), waystoneData);
        }
    }

    @Override
    public void saveData() {
        Collection<WaystoneData> waystones = getPlugin().getWaystonesMap().values();
        config = new YamlConfiguration();

        for (WaystoneData waystone : waystones) {
            config.set(waystone.getUniqueId() + ".name", waystone.getName());
            config.set(waystone.getUniqueId() + ".location", waystone.getLocation());
            config.set(waystone.getUniqueId() + ".owner", waystone.getOwner());
            config.set(waystone.getUniqueId() + ".ownerId", waystone.getOwnerUniqueId().toString());
            config.set(waystone.getUniqueId() + ".global", waystone.isInternal() || waystone.isGloballyAccessible());
            config.set(waystone.getUniqueId() + ".createdAt", waystone.getCreatedAt());
            if (waystone.isInternal())
                config.set(waystone.getUniqueId() + ".internal", waystone.isInternal());
            if (!waystone.getAddedPlayers().isEmpty())
                config.set(waystone.getUniqueId() + ".addedPlayers", waystone.getAddedPlayers()
                        .stream()
                        .map(UUID::toString)
                        .toList());
        }

        save();
    }
}
