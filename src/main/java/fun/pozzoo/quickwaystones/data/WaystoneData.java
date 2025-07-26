package fun.pozzoo.quickwaystones.data;

import fun.pozzoo.quickwaystones.QuickWaystones;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class WaystoneData {
    private String name;
    private boolean globallyAccessible;

    private long lastUsedAt;
    private boolean internal;

    private final long createdAt;
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
        this.createdAt = System.currentTimeMillis();
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

    public String getLastUsedAtFormatted() {
        long totalSeconds = (System.currentTimeMillis() - getLastUsedAt()) / 1000;
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d");
            if (hours > 0) sb.append(" ").append(hours).append("h");
        } else if (hours > 0) {
            sb.append(hours).append("h");
            if (minutes > 0) sb.append(" ").append(minutes).append("m");
        } else if (minutes > 0) {
            sb.append(minutes).append("m");
            if (seconds > 0) sb.append(" ").append(seconds).append("s");
        } else {
            sb.append(seconds).append("s");
        }
        return sb.toString();
    }

    public void setLastUsedAt(long timestamp) {
        this.lastUsedAt = timestamp;
    }

    public void markLastUsed() {
        setLastUsedAt(System.currentTimeMillis());
    }

    public void setInternal(boolean internal) {
        this.internal = internal;
    }

    public boolean isInternal() {
        return internal;
    }

    public boolean addPlayer(UUID player) {
        if (!player.equals(ownerUniqueId))
            return this.addedPlayers.add(player);
        return false;
    }

    public boolean removePlayer(UUID player) {
        return this.addedPlayers.remove(player);
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

    public boolean isOwner(Player player) {
        return player.getUniqueId().equals(ownerUniqueId);
    }

    public boolean isOwner(UUID playerUniqueId) {
        return playerUniqueId.equals(ownerUniqueId);
    }

    @Override
    public String toString() {
        return "WaystoneData{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", location=" + location +
                ", owner='" + owner + '\'' +
                ", globallyAccessible=" + globallyAccessible +
                ", createdAt=" + createdAt +
                ", lastUsedAt=" + lastUsedAt +
                ", internal=" + internal +
                ", addedPlayers=" + addedPlayers +
                '}';
    }
}