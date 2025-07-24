package fun.pozzoo.quickwaystones.gui;

import fun.pozzoo.quickwaystones.QuickWaystones;
import fun.pozzoo.quickwaystones.Utils;
import fun.pozzoo.quickwaystones.WaystoneSound;
import fun.pozzoo.quickwaystones.data.WaystoneData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import java.util.*;

public abstract class DialogGUI {
    public static final String KEY_LAST_WAYSTONE = "last_waystone";

    protected final QuickWaystones plugin;
    private long lastSaved;

    public DialogGUI(QuickWaystones plugin) {
        this.plugin = plugin;
    }

    public abstract void register();
    public abstract void unregister();

    public void cleanupPlayerCache(UUID uuid) {
        // Nothing by default
    }

    public void showWaystoneDestroyedNoticeDialog(Player player, WaystoneData previousClickedWaystone, WaystoneData clickedWaystone) {
        showWaystoneDestroyedNoticeDialog(player, player, previousClickedWaystone, clickedWaystone);
    }
    public void showListDialog(Player player, WaystoneData clickedWaystone) {
        showListDialog(player, player, clickedWaystone);
    }

    public abstract void showWaystoneDestroyedNoticeDialog(Player player, Player viewer, WaystoneData previousClickedWaystone, WaystoneData clickedWaystone);
    public abstract void showListDialog(Player player, Player viewer, WaystoneData clickedWaystone);

    protected List<WaystoneData> getSortedWaystones(Player player, WaystoneData clickedWaystone) {
        Map<Location, WaystoneData> waystones = plugin.getWaystonesMap();
        return waystones.values().stream()
                .filter(x ->
                        (clickedWaystone == null && x.getOwnerUniqueId().equals(player.getUniqueId())) ||
                                (clickedWaystone != null && (x.isGloballyAccessible()
                                        || x.getAddedPlayers().contains(player.getUniqueId())
                                        || x.getOwnerUniqueId().equals(player.getUniqueId())
                                )))
                .sorted(Comparator.comparing(WaystoneData::getCreatedAt))
                .toList();
    }

    protected void doTeleport(UUID uuid, boolean isViewer, WaystoneData waystone, WaystoneData clickedWaystone) {
        Player dialogViewer = Bukkit.getPlayer(uuid);
        if (dialogViewer == null)
            return;
        if (plugin.isWaystoneDestroyed(waystone.getBlock())) {
            showWaystoneDestroyedNoticeDialog(dialogViewer, clickedWaystone, waystone);
            return;
        }
        plugin.playWaystoneSound(null, dialogViewer.getLocation(), WaystoneSound.TELEPORTED);
        Location location = waystone.getTeleportLocation();
        dialogViewer.getWorld().spawnParticle(Particle.PORTAL, dialogViewer.getLocation(), 5);
        dialogViewer.teleport(location);
        dialogViewer.getWorld().spawnParticle(Particle.PORTAL, location, 5);
        plugin.playWaystoneSound(null, location, WaystoneSound.TELEPORTED);
        dialogViewer.setNoDamageTicks(10);
        if (!isViewer) {
            dialogViewer.setMetadata(KEY_LAST_WAYSTONE, new FixedMetadataValue(plugin, waystone.getID()));
            waystone.markLastUsed();
            long current = System.currentTimeMillis();
            if (lastSaved + 1000L <= current) {
                plugin.getDataManager().saveWaystoneData();
                lastSaved = current;
            }
        }
        if (!Utils.isBedrockPlayer(uuid)) {
            long delayAfter = plugin.getConfig().getLong("Teleportation.DelayAfter");
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                dialogViewer.closeInventory();
                dialogViewer.setMetadata("teleported_at", new FixedMetadataValue(plugin, System.currentTimeMillis()));
            }, Math.max(0, delayAfter) * 20);
        } // TODO Support Bedrock
    }

    protected void fillPlaceholders(Map<String, String> placeholders, Player player, WaystoneData waystone, WaystoneData clickedWaystone) {
        if (waystone != null)
            fillPlaceholders(placeholders, player, waystone, "");
        if (clickedWaystone != null)
            fillPlaceholders(placeholders, player, clickedWaystone, "current_");
    }

    private void fillPlaceholders(Map<String, String> placeholders, Player player, WaystoneData waystone, String prefix) {
        placeholders.put(prefix + "name", waystone.getName());
        placeholders.put(prefix + "id", waystone.getID());
        placeholders.put(prefix + "owner", waystone.getOwner());
        placeholders.put(prefix + "x", Integer.toString(waystone.getLocation().getBlockX()));
        placeholders.put(prefix + "y", Integer.toString(waystone.getLocation().getBlockY()));
        placeholders.put(prefix + "z", Integer.toString(waystone.getLocation().getBlockZ()));
        placeholders.put(prefix + "world", plugin.getConfig()
                .getString("Messages.WorldReplacedNames." + waystone.getLocation().getWorld().getName(),
                        waystone.getLocation().getWorld().getName()
                ));
        placeholders.put(prefix + "world_name", waystone.getLocation().getWorld().getName());

        List<String> attributes = new ArrayList<>();
        attributes.add(plugin.getConfig().getString("Messages.WaystoneAttributes." + (waystone.isGloballyAccessible() ? "Public" : "Private")));
        if (plugin.isWaystoneDestroyed(waystone.getLocation().getBlock()))
            attributes.add(plugin.getConfig().getString("Messages.WaystoneAttributes.Destroyed"));
        if (player.hasMetadata(KEY_LAST_WAYSTONE)) {
            List<MetadataValue> metadata = player.getMetadata(KEY_LAST_WAYSTONE);
            if (!metadata.isEmpty() && waystone.getID().equals(player.getMetadata(KEY_LAST_WAYSTONE).getFirst().asString()))
                attributes.add(plugin.getConfig().getString("Messages.WaystoneAttributes.LastlyUsed"));
        }
        placeholders.put(prefix + "attributes", String.join(plugin.getConfig().getString("Messages.WaystoneAttributes.Separator", ", "), attributes));
    }
}
