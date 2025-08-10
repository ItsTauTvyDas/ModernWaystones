package me.itstautvydas.modernwaystones.events;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import io.papermc.paper.event.entity.EntityKnockbackEvent;
import me.itstautvydas.modernwaystones.ModernWaystones;
import me.itstautvydas.modernwaystones.data.PlayerData;
import me.itstautvydas.modernwaystones.data.WaystoneData;
import me.itstautvydas.modernwaystones.enums.WaystoneSound;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class WaystoneEventsHandler implements Listener {
    private final ModernWaystones plugin;

    public WaystoneEventsHandler(ModernWaystones plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private boolean canPlaceBlock(Block block) {
        if (!block.getType().isOccluding())
            return true;
        Block block1 = block.getRelative(BlockFace.DOWN, 1);
        Block block2 = block.getRelative(BlockFace.DOWN, 2);
        Block blockUp2 = block.getRelative(BlockFace.UP, 2);
        Block block3 = block.getRelative(BlockFace.DOWN, 3);
        if (plugin.isWaystoneBlock(block2))
            return false;
        if (plugin.isWaystoneBlock(block1) && blockUp2.getType().isOccluding())
            return false;
        return !(block2.getType().isOccluding() && plugin.isWaystoneBlock(block3));
    }

    private void destroyWaystone(Location location) {
        plugin.getWaystonesMap().remove(location);
        plugin.getWaystoneDataManager().saveData();
    }

    @EventHandler
    public void onBlockDestroy(BlockDestroyEvent event) {
        WaystoneData waystone = plugin.getWaystonesMap().get(event.getBlock().getLocation());
        if (waystone == null) return;
        if (waystone.isInternal()) return;
        Location location = event.getBlock().getLocation();
        if (event.willDrop()) {
            event.setWillDrop(false);
            location.getWorld().dropItem(
                    location.toCenterLocation(),
                    plugin.getCraftManager().createWaystoneItem(waystone)
            );
            plugin.playWaystoneSound(null, location, WaystoneSound.DEACTIVATED);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        WaystoneData waystone = plugin.getWaystonesMap().get(event.getBlock().getLocation());
        if (waystone == null) return;
        if (waystone.isInternal()) return;
        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        if ((player.isOp() && player.getGameMode() == GameMode.CREATIVE) || player.getUniqueId().equals(waystone.getOwnerUniqueId()))
            destroyWaystone(location);
        plugin.playWaystoneSound(null, location, WaystoneSound.DEACTIVATED);
        event.setDropItems(false);
        if (event.getPlayer().getInventory().getItemInMainHand().getType().toString().endsWith("_PICKAXE"))
            location.getWorld().dropItem(
                    location.toCenterLocation(),
                    plugin.getCraftManager().createWaystoneItem(waystone)
            );
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        Block block = event.getBlockPlaced();
        if (!canPlaceBlock(event.getBlockPlaced())) {
            plugin.playWaystoneSound(event.getPlayer(), block.getLocation(), WaystoneSound.DISALLOWED);
            event.setCancelled(true);
            return;
        }
        if (!plugin.isWaystoneItem(item))
            return;
        Player player = event.getPlayer();
        int limit = plugin.getConfig().getInt("PlayerLimitations.MaxWaystonesPerPlayer", -1);
        if (limit != -1 && plugin.getPlayerOwnedWaystones(player.getUniqueId()).size() + 1 >= limit) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("max", Integer.toString(limit));
            plugin.sendMessage(player, ModernWaystones.message("Limitations.WaystoneCountLimitReached", placeholders), "PlayerLimitations.UseActionBar");
            event.setCancelled(true);
            return;
        }
        WaystoneData data = new WaystoneData(block.getLocation(), player.getName(), player.getUniqueId());
        plugin.getWaystonesMap().put(block.getLocation(), data);
        String name = item.getItemMeta().getPersistentDataContainer().get(plugin.getCraftManager().getPersistentWaystoneNameKey(), PersistentDataType.STRING);
        Boolean visible = item.getItemMeta().getPersistentDataContainer().get(plugin.getCraftManager().getPersistentWaystoneVisibilityKey(), PersistentDataType.BOOLEAN);
        if (name != null && !name.isEmpty())
            data.setName(name);
        if (visible != null)
            data.setGloballyAccessible(visible);
        plugin.getWaystoneDataManager().saveData();
        plugin.playWaystoneSound(null, block.getLocation(), WaystoneSound.ACTIVATED);
    }

    @EventHandler
    public void onPlayerInteractWaystone(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        boolean clickedFriendsBlock = false;
        if (plugin.isWaystoneFriendsBlock(block)) {
            block = block.getRelative(BlockFace.UP);
            clickedFriendsBlock = true;
            if (!plugin.getConfig().getBoolean("Features.AddFriends.Enabled"))
                return;
        }
        WaystoneData waystone = plugin.getWaystonesMap().get(block.getLocation());
        if (waystone == null)
            return;
        if (!waystone.isInternal() && plugin.isWaystoneDestroyed(block)) return;
        Player player = event.getPlayer();

        if (event.getItem() == null) {
            if (clickedFriendsBlock) {
                if (!waystone.isInternal() && (waystone.isOwner(player) || plugin.getConfig().getBoolean("Features.AddFriends.AllowEveryoneViewAddedPlayersList")))
                    canUseWaystone(player, ModernWaystones.LAST_USED_FRIENDS_BLOCK_AT, true, () ->
                            plugin.getWaystoneDialogs().showFriendsSettingsDialog(player, waystone, waystone.isOwner(player))
                    );
                return;
            }
            canUseWaystone(player, ModernWaystones.LAST_USED_WAYSTONE_AT, false, () -> plugin.getWaystoneDialogs().showListDialog(player, waystone));
            return;
        }

        if (!waystone.isOwner(player) || !(player.isOp() && player.getGameMode() == GameMode.CREATIVE))
            return;

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("name", waystone.getName());
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("Features.RenameByNameTag");
        if (section != null && section.getBoolean("Enabled")
                && event.getItem().getType() == Material.getMaterial(section.getString("Material", "NAME_TAG"))) {
            TextComponent textComponent = (TextComponent) event.getItem().getItemMeta().displayName();
            if (textComponent == null) return;
            waystone.setName(textComponent.decoration(TextDecoration.ITALIC, false).content());
            placeholders.put("new_name", waystone.getName());
            plugin.getWaystoneDataManager().saveData();
            if (section.getBoolean("SubtractItemCount"))
                player.getInventory().getItemInMainHand().subtract();
            plugin.playWaystoneSound(player, block.getLocation(), WaystoneSound.RENAMED);
            plugin.sendMessage(player, ModernWaystones.message("Renamed", placeholders), "Features.RenameByNameTag.UseActionBar");
            return;
        }
        section = plugin.getConfig().getConfigurationSection("Features.ChangeVisibility");
        if (section != null && section.getBoolean("Enabled")
                && event.getItem().getType() == Material.getMaterial(section.getString("Material", "ECHO_SHARD"))) {
            waystone.setGloballyAccessible(!waystone.isGloballyAccessible());
            plugin.getPlayerDataManager().updatePlayersAccesses(waystone, true, false, true);
            plugin.getWaystoneDataManager().saveData();
            if (section.getBoolean("SubtractItemCount"))
                player.getInventory().getItemInMainHand().subtract();
            String type = waystone.isGloballyAccessible() ? "Public" : "Private";
            plugin.playWaystoneSound(player,
                    block.getLocation(),
                    waystone.isGloballyAccessible() ? WaystoneSound.VISIBILITY_CHANGE_TO_PUBLIC
                            : WaystoneSound.VISIBILITY_CHANGE_TO_PRIVATE);
            placeholders.put("type", plugin.getConfig().getString("Messages.WaystoneAttributes." + type));
            plugin.sendMessage(player, ModernWaystones.message("VisibilityChanged", placeholders), "Features.RenameByNameTag.UseActionBar");
        }
        section = plugin.getConfig().getConfigurationSection("Features.ServerWaystones");
        if (section != null && section.getBoolean("Enabled") && player.isOp() && player.getGameMode() == GameMode.CREATIVE
                && event.getItem().getType() == Material.getMaterial(section.getString("Material", "DEBUG_STICK"))) {
            waystone.setInternal(!waystone.isInternal());
            waystone.setGloballyAccessible(true);
            plugin.getPlayerDataManager().updatePlayersAccesses(waystone, true, false, true);
            plugin.getWaystoneDataManager().saveData();
            event.setCancelled(true);
            plugin.sendMessage(
                    player,
                    ModernWaystones.message("ServerWaystoneMarking." + (waystone.isInternal() ? "Set" : "Unset"), placeholders),
                    "Features.ServerWaystones.UseActionBar"
            );
        }
    }

    @EventHandler
    public void onEntityInteract(EntityInteractEvent event) {
        pressurePlate(event.getBlock(), event.getEntity(), event);
    }

    @EventHandler
    public void onPlayerInteractRedstone(PlayerInteractEvent event) {
        if (!plugin.getConfig().getBoolean("RedstoneActivation.PressurePlate.Enabled"))
            return;
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (event.getAction() != Action.PHYSICAL) return;
        if (event.getClickedBlock() == null) return;
        if (plugin.isWaystoneBlock(event.getClickedBlock())) return;
        if (event.getPlayer().isInsideVehicle()) return;

        List<MetadataValue> list = event.getPlayer().getMetadata(ModernWaystones.LAST_USED_WAYSTONE_AT);
        if (!list.isEmpty()) {
            long current = System.currentTimeMillis();
            long last = list.getFirst().asLong();
            if (last + 500 >= current)
                return;
        }

        pressurePlate(event.getClickedBlock(), event.getPlayer(), event);
    }

    private void pressurePlate(Block block, Entity entity, Cancellable cancellable) {
        Material type = block.getType();
        switch (type) {
            case Material.LIGHT_WEIGHTED_PRESSURE_PLATE:
            case Material.HEAVY_WEIGHTED_PRESSURE_PLATE:
                break;
            default:
                for (int i = 1; i <= 2; i++) {
                    Location loc = block.getLocation().add(0, -i, 0);
                    WaystoneData waystone = plugin.getWaystonesMap().get(loc);
                    if (waystone != null && !plugin.isWaystoneDestroyed(loc.getBlock())) {
                        if (!(entity instanceof Player player)) {
                            cancellable.setCancelled(true);
                            return;
                        }
                        List<MetadataValue> list = player.getMetadata(ModernWaystones.WAS_PLAYER_FORCED);
                        if (list.isEmpty())
                            canUseWaystone(player, ModernWaystones.LAST_USED_WAYSTONE_AT, false, () -> plugin.getWaystoneDialogs().showListDialog(player, waystone));
                        return;
                    }
                }
                break;
        }
    }

    private boolean isCheckForForcefulPressurePlateActivationEnabled() {
        return plugin.getConfig().getBoolean("RedstoneActivation.PressurePlate.Enabled") && plugin.getConfig().getBoolean("RedstoneActivation.PressurePlate.CheckForForcefulActivation");
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (!isCheckForForcefulPressurePlateActivationEnabled())
            return;
        List<Block> blocks = new ArrayList<>(event.getBlocks());
        blocks.add(event.getBlock());
        blocks.add(event.getBlock().getRelative(event.getDirection()));
        for (Block block : blocks) {
            Location location = block.getRelative(event.getDirection()).getLocation();
            for (Entity entity : location.getWorld().getNearbyEntities(location, 1, 1, 1, x -> x instanceof Player)) {
                Player player = (Player) entity;
                player.setMetadata(ModernWaystones.WAS_PLAYER_FORCED, new FixedMetadataValue(plugin, System.currentTimeMillis()));
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!isCheckForForcefulPressurePlateActivationEnabled())
            return;
        // Maybe it's a little too expensive to run this all in player move event?
        if (event.getFrom().getBlock().getType() == Material.WATER) {
            event.getPlayer().setMetadata(ModernWaystones.WAS_PLAYER_FORCED, new FixedMetadataValue(plugin, System.currentTimeMillis()));
            return;
        }
        if (event.getFrom().getYaw() == event.getTo().getYaw() && event.getFrom().getPitch() == event.getTo().getPitch()) {
            for (Entity entity : event.getPlayer().getWorld().getNearbyEntities(event.getFrom(), 1, 2, 1)) {
                if (entity == event.getPlayer())
                    continue;
                Location entityLocation = entity.getLocation();
                if (entityLocation.distance(event.getFrom()) <= 0.7) {
                    event.getPlayer().setMetadata(ModernWaystones.WAS_PLAYER_FORCED, new FixedMetadataValue(plugin, System.currentTimeMillis()));
                    return;
                }
            }
        }
        List<MetadataValue> list = event.getPlayer().getMetadata(ModernWaystones.WAS_PLAYER_FORCED);
        if (list.isEmpty()) return;
        long timestamp = list.getFirst().asLong();
        if (timestamp + 1000L >= System.currentTimeMillis())
            return;
        event.getPlayer().removeMetadata(ModernWaystones.WAS_PLAYER_FORCED, plugin);
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!isCheckForForcefulPressurePlateActivationEnabled())
            return;
        if (event.getEntity() instanceof Player player)
            player.setMetadata(ModernWaystones.WAS_PLAYER_FORCED, new FixedMetadataValue(plugin, null));
    }

    @EventHandler
    public void onPlayerKnockback(EntityKnockbackEvent event) {
        if (!isCheckForForcefulPressurePlateActivationEnabled())
            return;
        if (event.getEntity() instanceof Player player)
            player.setMetadata(ModernWaystones.WAS_PLAYER_FORCED, new FixedMetadataValue(plugin, null));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getWaystoneDialogs().cleanupPlayerCache(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getWaystoneDataManager().updatePlayerName(event.getPlayer().getUniqueId());
        Collection<WaystoneData> waystones = plugin.getWaystoneDataManager().filterPlayerWaystones(event.getPlayer().getUniqueId());
        if (!waystones.isEmpty()) {
            PlayerData data = plugin.getPlayerData(event.getPlayer());
            data.setWaystones(waystones);
            plugin.getPlayerDataManager().saveData();
        }
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player)
            tryToDiscoverRecipe(player, event.getItem().getItemStack());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        tryToDiscoverRecipe(event.getWhoClicked(), event.getCurrentItem());
    }

    private void tryToDiscoverRecipe(HumanEntity player, ItemStack item) {
        if (!plugin.getConfig().getBoolean("Item.UnlockRecipe.Enabled") || player == null || item == null)
            return;
        if (player.hasDiscoveredRecipe(plugin.getCraftManager().getRecipeKey()))
            return;
        Material material = Material.getMaterial(plugin.getConfig().getString("Item.UnlockRecipe.OnMaterial", "_null_"));
        if (material == null)
            return;
        if (item.getType() == material)
            player.discoverRecipe(plugin.getCraftManager().getRecipeKey());
    }

    private void canUseWaystone(Player player, String key, boolean markLastUsed, Runnable success) {
        if (player.isSneaking()) {
            plugin.getWaystoneDialogs().showWaystonePlayerSettingsDialog(player);
            return;
        }
        List<MetadataValue> list = player.getMetadata(key);
        if (!list.isEmpty()) {
            long current = System.currentTimeMillis();
            long last = list.getFirst().asLong();
            long delayAfter = plugin.getConfig().getLong("Teleportation.DelayBetweenUses.Value") * 1000;
            if (delayAfter != 0 && last + delayAfter >= current) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("time", String.format("%.1f", (last + delayAfter - current) / 1000.0));
                plugin.sendMessage(player, ModernWaystones.message("WaitBeforeUse", placeholders), "Teleportation.DelayBetweenUses.UseActionBar");
                return;
            }
            player.removeMetadata(key, plugin);
        }
        success.run();
        if (markLastUsed)
            player.setMetadata(key, new FixedMetadataValue(plugin, System.currentTimeMillis()));
    }
}