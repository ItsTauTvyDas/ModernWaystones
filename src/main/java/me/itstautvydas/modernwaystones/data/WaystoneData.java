package me.itstautvydas.modernwaystones.data;

import me.itstautvydas.modernwaystones.ModernWaystones;
import org.bukkit.Bukkit;
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

    private boolean internal;

    private final long createdAt;
    private final String uniqueId;
    private String owner;
    private final UUID ownerUniqueId;
    private final Location location;
    private final Set<UUID> addedPlayers = new HashSet<>();

    public WaystoneData(Location location, String owner, UUID ownerUniqueId) {
        String waystoneDefaultName = ModernWaystones.config().getString("DefaultWaystone.Name", "Waystone #{id}");
        this.uniqueId = UUID.randomUUID().toString();
        this.name = waystoneDefaultName.replace("{id}", Integer.toString(ModernWaystones.getAndIncrementLastWaystoneID()));
        this.location = location;
        this.owner = owner;
        this.globallyAccessible = ModernWaystones.config().getBoolean("DefaultWaystone.GloballyAccessible", true);
        this.createdAt = System.currentTimeMillis();
        this.ownerUniqueId = ownerUniqueId;
    }

    public WaystoneData(String id, String name, Location location, String owner, UUID ownerUniqueId, long createdAt) {
        this.uniqueId = id;
        this.name = name;
        this.location = location;
        this.owner = owner;
        this.globallyAccessible = ModernWaystones.config().getBoolean("DefaultWaystone.GloballyAccessible", true);
        this.createdAt = createdAt;
        this.ownerUniqueId = ownerUniqueId;
    }

    public String getUniqueId() {
        return uniqueId;
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

    public void setInternal(boolean internal) {
        this.internal = internal;
    }

    public boolean isInternal() {
        return internal;
    }

    public boolean addPlayer(UUID player) {
        return this.addedPlayers.add(player);
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

    public void updateOwnerName() {
        Player player = Bukkit.getPlayer(uniqueId);
        if (player != null)
            this.owner = player.getName();
    }

    @Override
    public String toString() {
        return "WaystoneData{" +
                "id=" + uniqueId +
                ", name='" + name + '\'' +
                ", location=" + location +
                ", owner='" + owner + '\'' +
                ", globallyAccessible=" + globallyAccessible +
                ", createdAt=" + createdAt +
                ", internal=" + internal +
                ", addedPlayers=" + addedPlayers +
                '}';
    }
}