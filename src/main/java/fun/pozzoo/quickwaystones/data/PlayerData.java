package fun.pozzoo.quickwaystones.data;

import fun.pozzoo.quickwaystones.QuickWaystones;
import fun.pozzoo.quickwaystones.enums.PlayerSortType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class PlayerData {
    private final QuickWaystones plugin;
    private final UUID uniqueId;
    private final PlayerSortType sortType;
    private Set<String> sortedWaystones;

    private Comparator<String> comparator;

    public PlayerData(QuickWaystones plugin, UUID uniqueId, PlayerSortType sortType, Collection<String> sortedWaystones) {
        this.plugin = plugin;
        this.uniqueId = uniqueId;
        this.sortType = sortType;

        sort();
        this.sortedWaystones.addAll(sortedWaystones);
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public Player getOnlinePlayer() {
        return Bukkit.getPlayer(uniqueId);
    }

    public PlayerSortType getSortType() {
        return sortType;
    }

    public Set<String> getSortedWaystones() {
        return sortedWaystones;
    }

    public void add(Collection<WaystoneData> waystones) {
        sort();
        sortedWaystones.addAll(waystones.stream().map(WaystoneData::getUniqueId).toList());
    }

    public void add(WaystoneData waystone) {
        sort();
        sortedWaystones.add(waystone.getUniqueId());
    }

    private void sort() {
        if (sortType == PlayerSortType.MANUAL) {
            sortedWaystones = new LinkedHashSet<>();
        } else {
            sortedWaystones = new TreeSet<>((id1, id2) -> {
                WaystoneData w1 = plugin.getWaystones(uniqueId).get(id1);
                WaystoneData w2 = plugin.getWaystones(uniqueId).get(id2);
                if (w1 == null || w2 == null) return 0;

                int result = switch (sortType) {
                    case CREATED_AT -> Long.compare(w1.getCreatedAt(), w2.getCreatedAt());
                    case LAST_USED_AT -> Long.compare(w1.getLastUsedAt(), w2.getLastUsedAt());
                    case NAME -> w1.getName().compareToIgnoreCase(w2.getName());
                    case OWNER -> w1.getOwner().compareToIgnoreCase(w2.getOwner());
                    default -> 0;
                };

                if (result == 0) result = id1.compareTo(id2); // avoid collision
                return result;
            });
        }
    }
}
