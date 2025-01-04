package fun.pozzoo.quickwaystones.gui;

import fun.pozzoo.quickwaystones.QuickWaystones;
import fun.pozzoo.quickwaystones.data.WaystoneData;
import fun.pozzoo.quickwaystones.utils.StringUtils;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.Map;
import java.util.stream.Collectors;

public class WaystoneGUI {
    public static void runGUI(Player player) {
        PaginatedGui gui = Gui.paginated()
                .title(StringUtils.formatString("Waystones"))
                .rows(6)
                .pageSize(45)
                .create();

        Map<Location, WaystoneData> waystones = QuickWaystones.getWaystonesMap();

        List<WaystoneData> sortedWaystones = waystones.values().stream()
            .sorted(Comparator.comparing(WaystoneData::getName))
            .collect(Collectors.toList());

        for (WaystoneData waystone : sortedWaystones) {
            GuiItem item = ItemBuilder.from(Material.ENDER_PEARL)
                .name(StringUtils.formatItemName(waystone.getName()))
                .lore("§8POS §7" + waystone.getLocation().getBlockX() + ", " + waystone.getLocation().getBlockY() + ", " + waystone.getLocation().getBlockZ())
                .lore("§8WORLD §7" + waystone.getLocation().getWorld().getName())
                .asGuiItem(inventoryClickEvent -> {
                    inventoryClickEvent.setCancelled(true);
                    player.teleport(waystone.getLocation().clone().add(0.5, 1, 0.5));
                    player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 5);
                    player.playSound(player, Sound.ENTITY_FOX_TELEPORT, 0.5f, 1f);
                    player.closeInventory();
                });

            gui.addItem(item);
        }

        if (gui.getPagesNum() > 1) {
            gui.setItem(6, 3, ItemBuilder.from(Material.PAPER).name(StringUtils.formatString("Previous")).asGuiItem(event -> {
                event.setCancelled(true);
                gui.previous();
            }));

            gui.setItem(6, 7, ItemBuilder.from(Material.PAPER).name(StringUtils.formatString("Next")).asGuiItem(event -> {
                event.setCancelled(true);
                gui.next();
            }));
        }

        gui.open(player);
    }
}
