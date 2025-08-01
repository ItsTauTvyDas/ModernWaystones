package me.itstautvydas.quickwaystones.data;

import me.itstautvydas.quickwaystones.enums.PlayerSortType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class PlayerData {
    private final UUID uniqueId;
    private boolean inverted;
    private PlayerSortType sortType;
    private PlayerSortType oldSortType;
    private Set<WaystoneData> sortedWaystones;

    private final Comparator<WaystoneData> comparator;

    public PlayerData(UUID uniqueId, PlayerSortType sortType, boolean inverted, Collection<WaystoneData> waystones) {
        this.uniqueId = uniqueId;
        this.sortType = sortType;
        this.oldSortType = sortType;
        this.inverted = inverted;

        comparator = (w1, w2) -> switch (this.sortType) {
            case CREATED_AT -> Long.compare(w1.getCreatedAt(), w2.getCreatedAt());
            case NAME -> {
                int compared = w1.getName().compareToIgnoreCase(w2.getName());
                if (compared == 0)
                    yield w1.getName().compareToIgnoreCase(w2.getName() + w1.getName());
                yield 1;
            }
            case OWNER -> {
                int compared = w1.getOwner().compareToIgnoreCase(w2.getOwner());
                if (compared == 0)
                    yield w1.getOwner().compareToIgnoreCase(w2.getOwner() + w1.getOwner());
                yield 1;
            }
            default -> w1.getUniqueId().compareTo(w2.getUniqueId());
        };

        updateSorting(false);
        this.sortedWaystones.addAll(waystones);
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public boolean isSortingInverted() {
        return inverted;
    }

    public Player getOnlinePlayer() {
        return Bukkit.getPlayer(uniqueId);
    }

    public PlayerSortType getSortType() {
        return sortType;
    }

    public Set<WaystoneData> getSortedWaystones() {
        return sortedWaystones;
    }

    public boolean setSortType(PlayerSortType type, Boolean inverted, boolean updateSorting) {
        if (this.sortType == type && Boolean.valueOf(this.inverted).equals(inverted))
            return false;
        this.sortType = type;
        if (updateSorting)
            updateSorting(false);
        if (inverted != null) {
            if (this.inverted != inverted)
                invert();
            this.inverted = inverted;
        }
        return true;
    }

    public void setWaystones(Collection<WaystoneData> waystones) {
        updateSorting(false);
        sortedWaystones.clear();
        sortedWaystones.addAll(waystones);
    }

    public void addWaystone(WaystoneData waystone) {
        updateSorting(false);
        sortedWaystones.add(waystone);
    }

    private void move(WaystoneData waystone, int i) {
        setSortType(PlayerSortType.MANUAL, null, true);
        List<WaystoneData> ids = new ArrayList<>(sortedWaystones);
        int index = ids.indexOf(waystone);
        if (index == -1) return;
        Collections.swap(ids, index, index - i);
        sortedWaystones = new LinkedHashSet<>(ids);
    }

    public void moveUp(WaystoneData waystone) {
        move(waystone, 1);
    }

    public void moveDown(WaystoneData waystone) {
        move(waystone, -1);
    }

    public void exchange(WaystoneData waystone1, WaystoneData waystone2) {
        setSortType(PlayerSortType.MANUAL, null, true);
        List<WaystoneData> ids = new ArrayList<>(sortedWaystones);
        int index1 = ids.indexOf(waystone1);
        if (index1 < 0) return;
        int index2 = ids.indexOf(waystone2);
        if (index2 < 0) return;
        Collections.swap(ids, index1, index2);
        sortedWaystones = new LinkedHashSet<>(ids);
    }

    private void invert() {
        sortedWaystones = ((SequencedSet<WaystoneData>) sortedWaystones).reversed();
    }

    public void updateSorting(boolean forced) {
        if (!forced && oldSortType == sortType && sortedWaystones != null)
            return;
        System.out.println("--------------------------------- " + sortType);
        List<WaystoneData> oldItems = sortedWaystones != null ? new ArrayList<>(sortedWaystones) : List.of();
        if (sortType == PlayerSortType.MANUAL) {
            sortedWaystones = new LinkedHashSet<>(oldItems);
        } else {
            sortedWaystones = new TreeSet<>(comparator);
            sortedWaystones.addAll(oldItems);
        }
        System.out.println(sortedWaystones);
        oldSortType = sortType;
    }
}
