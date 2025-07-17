package fun.pozzoo.quickwaystones.events;

import fun.pozzoo.quickwaystones.QuickWaystones;
import fun.pozzoo.quickwaystones.Utils;
import fun.pozzoo.quickwaystones.data.WaystoneData;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class WaystoneEventsHandler implements Listener {
    private final QuickWaystones plugin;

    public WaystoneEventsHandler(QuickWaystones plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.isWaystoneBlock(event.getBlock())) return;

        Player player = event.getPlayer();
        WaystoneData waystone = plugin.getWaystonesMap().get(event.getBlock().getLocation());

        if ((player.isOp() && player.getGameMode() == GameMode.CREATIVE) || player.getUniqueId().equals(waystone.getOwnerUniqueId())) {
            plugin.getWaystonesMap().remove(event.getBlock().getLocation());
            QuickWaystones.getInstance().getDataManager().saveWaystoneData(plugin.getWaystonesMap().values());
            event.setDropItems(false);

            return;
        }

        event.setCancelled(true);
        player.sendMessage(Utils.formatString(this.plugin.getConfig().getString("Messages.WaystoneBrokenByOther")));
    }

    @EventHandler
    public void onBlockPlaceEvent(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (!plugin.isWaystoneItem(item))
            return;

        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();

        player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
        player.sendMessage(QuickWaystones.message("WaystoneActivated"));
        plugin.getWaystonesMap().put(block.getLocation(), new WaystoneData(block.getLocation(), player.getName(), player.getUniqueId()));
        plugin.getDataManager().saveWaystoneData(plugin.getWaystonesMap().values());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (plugin.isWaystoneBlock(event.getClickedBlock())) return;

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
}
