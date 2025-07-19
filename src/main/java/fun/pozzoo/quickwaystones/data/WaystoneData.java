package fun.pozzoo.quickwaystones.data;

import fun.pozzoo.quickwaystones.QuickWaystones;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.*;

public class WaystoneData {
    private String name;
    private boolean globallyAccessible;

    private final long createdAt;
    private long lastUsedAt;

    private final String id;
    private final String owner;
    private final UUID ownerUniqueId;
    private final Location location;
    private final Set<UUID> addedPlayers = new HashSet<>();

    public WaystoneData(Location location, String owner, UUID ownerUniqueId) {
        String waystoneDefaultName = QuickWaystones.config().getString("DefaultWaystone.Name", "Waystone #{id}");
        this.id = UUID.randomUUID().toString();
        this.name = waystoneDefaultName.replace("{id}", Integer.toString(QuickWaystones.getAndIncrementLastWaystoneID()));
        this.location = location;
        this.owner = owner;
        this.globallyAccessible = QuickWaystones.config().getBoolean("DefaultWaystone.GloballyAccessible", true);
        this.createdAt = new Date().getTime();
        this.ownerUniqueId = ownerUniqueId;
    }

    public WaystoneData(String id, String name, Location location, String owner, UUID ownerUniqueId, long createdAt) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.owner = owner;
        this.globallyAccessible = QuickWaystones.config().getBoolean("DefaultWaystone.GloballyAccessible", true);;
        this.createdAt = createdAt;
        this.ownerUniqueId = ownerUniqueId;
    }

    public String getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public UUID getOwnerUniqueId() {
        return ownerUniqueId;
    }

    public Location getLocation() {
        return location;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(long timestamp) {
        this.lastUsedAt = timestamp;
    }

    public void addPlayer(Player player) {
        this.addedPlayers.add(player.getUniqueId());
    }

    public void removePlayer(Player player) {
        this.addedPlayers.remove(player.getUniqueId());
    }

    public Set<UUID> getAddedPlayers() {
        return addedPlayers;
    }

    public boolean isGloballyAccessible() {
        return globallyAccessible;
    }

    public void setGloballyAccessible(boolean globallyAccessible) {
        this.globallyAccessible = globallyAccessible;
    }

    public Block getBlock() {
        return getLocation().getBlock();
    }

    public Location getTeleportLocation() {
        Block block = getBlock();
        Location location = getLocation().toCenterLocation().add(0, 0.5, 0);
        if (block.getRelative(BlockFace.UP).getType().isOccluding())
            location.add(0, 1, 0);
        return location;
    }

    @Override
    public String toString() {
        return "WaystoneData{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", location=" + location +
                ", owner='" + owner + '\'' +
                ", globallyAccessible='" + globallyAccessible + '\'' +
                ", createdAt='" + createdAt + '\'' +
                ", lastUsedAt='" + lastUsedAt + '\'' +
                '}';
    }
}