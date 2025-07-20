package fun.pozzoo.quickwaystones.events;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import fun.pozzoo.quickwaystones.QuickWaystones;
import fun.pozzoo.quickwaystones.WaystoneSound;
import fun.pozzoo.quickwaystones.data.WaystoneData;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class WaystoneEventsHandler implements Listener {
    private final QuickWaystones plugin;

    public WaystoneEventsHandler(QuickWaystones plugin) {
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
        plugin.getDataManager().saveWaystoneData();
    }

    @EventHandler
    public void onBlockDestroy(BlockDestroyEvent event) {
        if (!plugin.isWaystoneBlock(event.getBlock())) return;

        WaystoneData waystone = plugin.getWaystonesMap().get(event.getBlock().getLocation());
        Location location = event.getBlock().getLocation();
        if (event.willDrop()) {
            event.setWillDrop(false);
            location.getWorld().dropItem(location.toCenterLocation(), plugin.getCraftManager().createWaystoneItem(waystone.getName()));
            plugin.playWaystoneSound(null, location, WaystoneSound.DEACTIVATED);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.isWaystoneBlock(event.getBlock())) return;

        Player player = event.getPlayer();
        WaystoneData waystone = plugin.getWaystonesMap().get(event.getBlock().getLocation());

        Location location = event.getBlock().getLocation();
        if ((player.isOp() && player.getGameMode() == GameMode.CREATIVE) || player.getUniqueId().equals(waystone.getOwnerUniqueId()))
            destroyWaystone(location);
        plugin.playWaystoneSound(null, location, WaystoneSound.DEACTIVATED);
        event.setDropItems(false);
        if (event.getPlayer().getInventory().getItemInMainHand().getType().toString().endsWith("_PICKAXE"))
            location.getWorld().dropItem(location.toCenterLocation(), plugin.getCraftManager().createWaystoneItem(waystone.getName()));
    }

    @EventHandler
    public void onBlockPlaceEvent(BlockPlaceEvent event) {
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
        WaystoneData data = new WaystoneData(block.getLocation(), player.getName(), player.getUniqueId());
        plugin.getWaystonesMap().put(block.getLocation(), data);
        String name = item.getItemMeta().getPersistentDataContainer().get(plugin.getCraftManager().getPersistentItemDataKey(), PersistentDataType.STRING);
        if (name != null && !name.isEmpty())
            data.setName(name);
        plugin.getDataManager().saveWaystoneData();
        plugin.playWaystoneSound(null, block.getLocation(), WaystoneSound.ACTIVATED);
    }

    @EventHandler
    public void onPlayerInteractWaystone(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (!plugin.isWaystoneBlock(event.getClickedBlock())) return;
        if (plugin.isWaystoneDestroyed(event.getClickedBlock())) return;

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (event.getItem() == null) {
            WaystoneData data = plugin.getWaystonesMap().get(block.getLocation());
            if (data == null)
                return;
            plugin.getWaystoneDialogs().showListDialog(player, data);
            return;
        }

        if (event.getItem().getType() == Material.NAME_TAG) {
            TextComponent textComponent = (TextComponent) event.getItem().getItemMeta().displayName();

            if (textComponent == null) return;

            plugin.getWaystonesMap().get(block.getLocation()).setName(textComponent.decoration(TextDecoration.ITALIC, false).content());
            player.getInventory().getItemInMainHand().subtract();
            player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
        }
    }

    @EventHandler
    public void onPlayerInteractRedstone(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if ( event.getAction() != Action.PHYSICAL) return;
        if (event.getClickedBlock() == null) return;
        if (plugin.isWaystoneBlock(event.getClickedBlock())) return;
        if (event.getPlayer().getNoDamageTicks() > 0) return;

        Material type = event.getClickedBlock().getType();
        if (type.toString().endsWith("_PRESSURE_PLATE") && !List.of(Material.LIGHT_WEIGHTED_PRESSURE_PLATE, Material.HEAVY_WEIGHTED_PRESSURE_PLATE).contains(type)) {
            for (int i = 1; i <= 2; i++) {
                Location loc = event.getClickedBlock().getLocation().add(0, -i, 0);
                WaystoneData data = plugin.getWaystonesMap().get(loc);
                if (data != null && !plugin.isWaystoneDestroyed(loc.getBlock())) {
                    plugin.getWaystoneDialogs().showListDialog(event.getPlayer(), data);
                    return;
                }
            }
        }
    }
}
