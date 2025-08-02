package me.itstautvydas.quickwaystones.gui;

import me.itstautvydas.quickwaystones.QuickWaystones;
import me.itstautvydas.quickwaystones.Utils;
import me.itstautvydas.quickwaystones.data.PlayerData;
import me.itstautvydas.quickwaystones.data.WaystoneData;
import me.itstautvydas.quickwaystones.enums.WaystoneSound;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class DialogGUI {
    public static final String KEY_LAST_WAYSTONE = "last_waystone";

    protected final QuickWaystones plugin;
//    private long lastSaved;

    private List<Consumer<Player>> runAfterClose;

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
    public abstract void showRenameDialog(Player player, Player viewer, WaystoneData clickedWaystone, String initialInput, boolean showNotice, boolean showError);
    public abstract void showSimpleNotice(Player viewer, Component title, Component text, Component button, Consumer<Player> action, boolean closeOnEscape);
    public abstract void showFriendsSettingsDialog(Player viewer, WaystoneData waystone, boolean canEdit);
    public abstract void showWaitingDialog(Player viewer, Component title, Function<Long, Component> text, Component cancelButton, long waitTicks, Consumer<Player> onClose, Consumer<Player> onFinish);
    public abstract void showSortSettingsDialog(Player viewer);
    public abstract void showWaystonePlayerSettingsDialog(Player viewer);

    public Collection<WaystoneData> getSortedWaystones(Player player) {
        PlayerData data = plugin.getPlayerData(player);
        if (data.getSortedWaystones().size() != plugin.getWaystonesMap().size())
            data.setWaystones(plugin.getWaystonesMap().values());
        return data.getSortedWaystones();
    }

    protected void doTeleport(Player dialogViewer, boolean isViewer, WaystoneData waystone) {
        Utils.loadChunkIfNeeded(waystone.getLocation());
        dialogViewer.setMetadata("teleported_at", new FixedMetadataValue(plugin, System.currentTimeMillis()));
        plugin.playWaystoneSound(null, dialogViewer.getLocation(), WaystoneSound.TELEPORTED);
        Location location = waystone.getTeleportLocation();
        dialogViewer.getWorld().spawnParticle(Particle.PORTAL, dialogViewer.getLocation(), 5);
        dialogViewer.teleport(location);
        dialogViewer.getWorld().spawnParticle(Particle.PORTAL, location, 5);
        plugin.playWaystoneSound(null, location, WaystoneSound.TELEPORTED);
        int noDamageTicks = plugin.getConfig().getInt("Teleportation.NoDamageTicksAfter");
        if (noDamageTicks > 0)
            dialogViewer.setNoDamageTicks(noDamageTicks);
        if (!isViewer) {
            dialogViewer.setMetadata(KEY_LAST_WAYSTONE, new FixedMetadataValue(plugin, waystone.getUniqueId()));
//            waystone.markLastUsed();
//            long current = System.currentTimeMillis();
//            if (lastSaved + 1000L <= current) {
//                plugin.getWaystoneDataManager().saveData();
//                lastSaved = current;
//            }
        }
    }

    protected void fillPlaceholders(Map<String, String> placeholders, Player player, PlayerData data, WaystoneData waystone, WaystoneData clickedWaystone) {
        if (waystone != null)
            fillPlaceholders(placeholders, player, data, waystone, "");
        if (clickedWaystone != null)
            fillPlaceholders(placeholders, player, data, clickedWaystone, "current_");
    }

    private void fillPlaceholders(Map<String, String> placeholders, Player player, PlayerData data, WaystoneData waystone, String prefix) {
        placeholders.put(prefix + "name", waystone.getName());
        placeholders.put(prefix + "id", waystone.getUniqueId());
        if (waystone.isInternal()) {
            placeholders.put(prefix + "owner", "Server");
            placeholders.put(prefix + "owner_id", "Server");
        } else {
            placeholders.put(prefix + "owner", waystone.getOwner());
            placeholders.put(prefix + "owner_id", waystone.getOwnerUniqueId().toString());
        }
        boolean hideLocation = data != null && data.getHideLocation();
        placeholders.put(prefix + "x", hideLocation ? "?" : Integer.toString(waystone.getLocation().getBlockX()));
        placeholders.put(prefix + "y", hideLocation ? "?" : Integer.toString(waystone.getLocation().getBlockY()));
        placeholders.put(prefix + "z", hideLocation ? "?" : Integer.toString(waystone.getLocation().getBlockZ()));
        placeholders.put(prefix + "world", plugin.getConfig()
                .getString("Messages.WorldReplacedNames." + waystone.getLocation().getWorld().getName(),
                        waystone.getLocation().getWorld().getName()
                ));
        placeholders.put(prefix + "world_name", waystone.getLocation().getWorld().getName());

        // TODO add for last_used
        placeholders.put(prefix + "created_ago", Utils.formatAgoTime(waystone.getCreatedAt()));

        if (data == null || data.getShowAttributes()) {
            List<String> attributes = new ArrayList<>();
            attributes.add(plugin.getConfig().getString("Messages.WaystoneAttributes." + (waystone.isGloballyAccessible() ? "Public" : "Private")));
            if (plugin.isWaystoneDestroyed(waystone.getLocation().getBlock()))
                attributes.add(plugin.getConfig().getString("Messages.WaystoneAttributes.Destroyed"));
            if (player != null) {
                if (player.hasMetadata(KEY_LAST_WAYSTONE)) {
                    List<MetadataValue> metadata = player.getMetadata(KEY_LAST_WAYSTONE);
                    if (!metadata.isEmpty() && waystone.getUniqueId().equals(player.getMetadata(KEY_LAST_WAYSTONE).getFirst().asString()))
                        attributes.add(plugin.getConfig().getString("Messages.WaystoneAttributes.LastlyUsed"));
                }
                if (!waystone.isGloballyAccessible() && waystone.getAddedPlayers().contains(player.getUniqueId()))
                    attributes.add(plugin.getConfig().getString("Messages.WaystoneAttributes.WasAdded"));
                String attrPrefix = plugin.getConfig().getString("Messages.WaystoneAttributes.Prefix");
                String attrSuffix = plugin.getConfig().getString("Messages.WaystoneAttributes.Suffix");
                placeholders.put(prefix + "attributes", attrPrefix + String.join(plugin.getConfig().getString("Messages.WaystoneAttributes.Separator", ", "), attributes) + attrSuffix);
            }
        } else {
            placeholders.put(prefix + "attributes", "");
        }
    }
}
