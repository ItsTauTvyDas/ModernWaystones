package me.itstautvydas.modernwaystones.gui;

import me.itstautvydas.modernwaystones.ModernWaystones;
import me.itstautvydas.modernwaystones.Utils;
import me.itstautvydas.modernwaystones.data.PlayerData;
import me.itstautvydas.modernwaystones.data.WaystoneData;
import me.itstautvydas.modernwaystones.enums.WaystoneSound;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class DialogGUI {
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

    public void showLoadingOfflinePlayersDialog(Player viewer, Consumer<List<OfflinePlayer>> afterLoading) {
        BukkitTask task = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            List<OfflinePlayer> players = Arrays.stream(Bukkit.getOfflinePlayers())
                    .sorted(Comparator.comparing((OfflinePlayer x) -> !x.isOnline())
                            .thenComparing(x -> x.getName() == null ? x.getUniqueId().toString() : x.getName()))
                    .toList();
            Bukkit.getScheduler().runTask(plugin, () -> afterLoading.accept(players));
        }, 5); // Sometimes afterLoading can finish faster than showing loading players dialog (bedrock related issue, waiting won't hurt anyone lol)

        showSimpleNotice(viewer,
                ModernWaystones.message("FriendsSettingDialog.Title"),
                ModernWaystones.message("FriendsSettingDialog.LoadingPlayers"),
                ModernWaystones.message("Close"),
                player -> {
                    closeDialog(player);
                    task.cancel();
                },
                true);
    }

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

        int noDamageTicksBefore = plugin.getConfig().getInt("Teleportation.NoDamageTicksBefore");
        if (noDamageTicksBefore > 0)
            viewer.setNoDamageTicks(noDamageTicksBefore);

        ConfigurationSection potionSection = plugin.getConfig().getConfigurationSection("Teleportation.PotionEffect");
        long delayBefore = Math.max(0, plugin.getConfig().getLong("Teleportation.DelayBefore"));
        showWaitingDialog(viewer,
                ModernWaystones.message("WaystonesListDialog.Teleporting.Title", teleportPlaceholders),
                ticksLeft -> {
                    tryApplyingPotionEffect(viewer, potionSection, ticksLeft);
                    teleportPlaceholders.put("seconds", Long.toString(ticksLeft / 20));
                    return ModernWaystones.message("WaystonesListDialog.Teleporting.Text", teleportPlaceholders);
                },
                ModernWaystones.message("Cancel"),
                delayBefore * 20,
                null, p -> {
                    if (!waystone.isOwner(p) && !(waystone.isGloballyAccessible() || waystone.getAddedPlayers().contains(p.getUniqueId()))) {
                        showWaystoneDestroyedNoticeDialog(p, clickedWaystone, waystone, false);
                        return;
                    }
                    if (delayBefore == 0)
                        tryApplyingPotionEffect(p, potionSection, null);
                    doTeleport(p, false, waystone, () -> {
                        int noDamageTicks = plugin.getConfig().getInt("Teleportation.NoDamageTicksAfter");
                        if (noDamageTicks > 0)
                            p.setNoDamageTicks(noDamageTicks);
                        tryApplyingSicknessEffects(p);
                    });
                }, true);
    }

    protected void tryApplyingSicknessEffects(Player player) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("Teleportation.Sickness");
        if (section == null || !section.getBoolean("Enabled")) return;
        ConfigurationSection effectsSection = section.getConfigurationSection("Effects");
        if (effectsSection == null) return;
        List<PotionEffect> effects = new ArrayList<>();
        for (String effectName : effectsSection.getKeys(false)) {
            ConfigurationSection potionSection = effectsSection.getConfigurationSection(effectName);
            if (potionSection == null) continue;
            PotionEffectType effect = Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft(effectName.toLowerCase()));
            if (effect == null) {
                plugin.getLogger().warning("[Teleportation Sickness] potion effect '" + effectName + "' doesn't exist!");
                continue;
            }
            int chance = Math.clamp(potionSection.getInt("Chance", 0), 0, 100);
            int random = plugin.getRandom().nextInt(0, 100);
            if (random < chance)
                effects.add(new PotionEffect(
                        effect,
                        (int) (potionSection.getDouble("Duration", 1) * 20),
                        potionSection.getInt("Amplifier", 1))
                );
        }
        if (!effectsSection.getBoolean("MultipleEffectsAtOnce")) {
            if (!effects.isEmpty())
                player.addPotionEffect(effects.getFirst());
            return;
        }
        player.addPotionEffects(effects);
    }

    protected void tryApplyingPotionEffect(Player player, ConfigurationSection section, Long ticksLeft) {
        if (section == null || !section.getBoolean("Enabled")) return;
        if (ticksLeft != null && section.getInt("ApplyWhenSecondsLeft") != ticksLeft / 20) return;
        PotionEffectType effect = Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft(section.getString("Effect", "blindness")));
        if (effect == null) return;
        player.addPotionEffect(new PotionEffect(
                effect,
                (int) (section.getDouble("Duration", 2) * 20),
                section.getInt("Amplifier", 1))
        );
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

    protected void doTeleport(Player dialogViewer, boolean isViewer, WaystoneData waystone, Runnable afterTeleport) {
        Utils.loadChunkIfNeeded(waystone.getLocation());
        dialogViewer.setMetadata(ModernWaystones.LAST_USED_WAYSTONE_AT, new FixedMetadataValue(plugin, System.currentTimeMillis()));
        plugin.playWaystoneSound(null, dialogViewer.getLocation(), WaystoneSound.TELEPORTED);
        Location location = waystone.getTeleportLocation();
        dialogViewer.getWorld().spawnParticle(Particle.PORTAL, dialogViewer.getLocation(), 5);
        dialogViewer.teleport(location);
        dialogViewer.getWorld().spawnParticle(Particle.PORTAL, location, 5);
        plugin.playWaystoneSound(null, location, WaystoneSound.TELEPORTED);
        if (afterTeleport != null)
            afterTeleport.run();
        if (!isViewer) {
            dialogViewer.setMetadata(ModernWaystones.LAST_WAYSTONE, new FixedMetadataValue(plugin, waystone.getUniqueId()));
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

    protected void fillPlaceholders(Map<String, String> placeholders, OfflinePlayer player) {
        String name = player.hasPlayedBefore() ? player.getName() : player.getUniqueId().toString();
        placeholders.put("username", name);
        placeholders.put("player_id", player.getUniqueId().toString());
        placeholders.put("online_status", plugin.getConfig().getString("Messages.PlayerStatuses." + (player.isOnline() ? "Online" : "Offline")));
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
                if (plugin.getConfig().getBoolean("WaystoneScreen.ShowRecentUsedWaystoneAttribute") && player.hasMetadata(ModernWaystones.LAST_WAYSTONE)) {
                    List<MetadataValue> metadata = player.getMetadata(ModernWaystones.LAST_WAYSTONE);
                    if (!metadata.isEmpty() && waystone.getUniqueId().equals(player.getMetadata(ModernWaystones.LAST_WAYSTONE).getFirst().asString()))
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
