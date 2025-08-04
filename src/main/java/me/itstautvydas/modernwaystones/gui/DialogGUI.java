package me.itstautvydas.modernwaystones.gui;

import me.itstautvydas.modernwaystones.ModernWaystones;
import me.itstautvydas.modernwaystones.Utils;
import me.itstautvydas.modernwaystones.data.PlayerData;
import me.itstautvydas.modernwaystones.data.WaystoneData;
import me.itstautvydas.modernwaystones.enums.WaystoneSound;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class DialogGUI {
    public static final String KEY_LAST_WAYSTONE = "last_waystone";

    protected final ModernWaystones plugin;
//    private long lastSaved;

    private List<Consumer<Player>> runAfterClose;

    public DialogGUI(ModernWaystones plugin) {
        this.plugin = plugin;
    }

    public abstract void register();
    public abstract void unregister();

    public void cleanupPlayerCache(UUID uuid) {
        // Nothing by default
    }

    public abstract void showWaystoneDestroyedNoticeDialog(Player viewer, WaystoneData previousClickedWaystone, WaystoneData clickedWaystone, boolean actuallyDestroyed);
    public abstract void showListDialog(Player viewer, WaystoneData clickedWaystone);
    public abstract void showRenameDialog(Player viewer, WaystoneData clickedWaystone, String initialInput, boolean showNotice, boolean showError);
    public abstract void showSimpleNotice(Player viewer, Component title, Component text, Component button, Consumer<Player> action, boolean closeOnEscape);
    public abstract void showFriendsSettingsDialog(Player viewer, WaystoneData waystone, boolean canEdit);
    public abstract void showSortSettingsDialog(Player viewer);
    public abstract void showWaystonePlayerSettingsDialog(Player viewer);

    public void showWaitingDialog(Player viewer, Component title, Function<Long, Component> text, Component cancelButton, long waitTicks, Consumer<Player> onClose, Consumer<Player> onFinish, boolean closeOnEscape) {
        if (waitTicks == 0) {
            onFinish.accept(viewer);
        } else {
            AtomicLong left = new AtomicLong(waitTicks);
            Bukkit.getScheduler().runTaskTimer(
                    plugin,
                    (timer) -> {
                        if (left.addAndGet(-20) == 0) {
                            closeDialog(viewer);
                            onFinish.accept(viewer);
                            timer.cancel();
                            return;
                        }
                        showSimpleNotice(viewer, title, text.apply(left.longValue()), cancelButton, player -> {
                            closeDialog(player);
                            timer.cancel();
                            if (onClose != null)
                                onClose.accept(viewer);
                        }, closeOnEscape);
                    }, 0, 20
            );
        }
    }

    public void closeDialog(Player player) {
        player.closeInventory();
    }

    protected void onWaystoneClick(Player viewer, PlayerData playerData, WaystoneData waystone, WaystoneData clickedWaystone) {
        Map<String, String> teleportPlaceholders = new HashMap<>();
        fillPlaceholders(teleportPlaceholders, viewer, playerData, waystone, (WaystoneData)null);

        Utils.loadChunkIfNeeded(waystone.getLocation());
        if (plugin.isWaystoneDestroyed(waystone.getBlock())) {
            showWaystoneDestroyedNoticeDialog(viewer, clickedWaystone, waystone, true);
            return;
        }
        applyNoDamageTicks(viewer);

        long delayBefore = Math.max(0, plugin.getConfig().getLong("Teleportation.DelayBefore"));
        showWaitingDialog(viewer,
                ModernWaystones.message("WaystonesListDialog.Teleporting.Title", teleportPlaceholders),
                ticksLeft -> {
                    teleportPlaceholders.put("seconds", Long.toString(ticksLeft / 20));
                    return ModernWaystones.message("WaystonesListDialog.Teleporting.Text", teleportPlaceholders);
                },
                ModernWaystones.message("Cancel"),
                delayBefore * 20,
                null, p -> {
                    if (!waystone.isOwner(viewer) && !waystone.isGloballyAccessible()) {
                        if (!waystone.getAddedPlayers().contains(viewer.getUniqueId())) {
                            showWaystoneDestroyedNoticeDialog(p, clickedWaystone, waystone, false);
                            return;
                        }
                    }
                    doTeleport(p, false, waystone);
                }, true);
    }

    protected void applyNoDamageTicks(Player player) {
        int noDamageTicks = plugin.getConfig().getInt("Teleportation.NoDamageTicksBefore");
        if (noDamageTicks > 0)
            player.setNoDamageTicks(noDamageTicks);
    }

    protected Component getWaystoneLabel(WaystoneData waystone, WaystoneData clickedWaystone, Map<String, String> placeholders) {
        if (waystone.isInternal())
            return ModernWaystones.message("WaystonesListDialog.ServerWaystoneButton", placeholders);
        else if (waystone == clickedWaystone)
            return ModernWaystones.message("WaystonesListDialog.CurrentWaystoneButton", placeholders);
        return ModernWaystones.message("WaystonesListDialog.WaystoneButton", placeholders);
    }

    protected void showNoWaystonesNotice(Player viewer, Map<String, String> placeholders) {
        showSimpleNotice(viewer,
                ModernWaystones.message("WaystonesListDialog.Title", placeholders),
                ModernWaystones.message("WaystonesListDialog.NoWaystonesNotice", placeholders),
                ModernWaystones.message("Close", placeholders),
                Player::closeInventory,
                true);
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
                if (plugin.getConfig().getBoolean("WaystoneScreen.ShowRecentUsedWaystoneAttribute") && player.hasMetadata(KEY_LAST_WAYSTONE)) {
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
