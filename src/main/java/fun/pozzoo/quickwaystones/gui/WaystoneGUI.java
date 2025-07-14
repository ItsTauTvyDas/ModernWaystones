package fun.pozzoo.quickwaystones.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import fun.pozzoo.quickwaystones.QuickWaystones;
import fun.pozzoo.quickwaystones.data.WaystoneData;
import fun.pozzoo.quickwaystones.utils.StringUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

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
            .toList();

        for (WaystoneData waystone : sortedWaystones) {
            Material material = switch (waystone.getLocation().getWorld().getEnvironment()) {
                case NETHER -> Material.NETHERRACK;
                case THE_END -> Material.END_STONE;
                default -> Material.GRASS_BLOCK;
            };

            GuiItem item = ItemBuilder.from(material)
                .name(StringUtils.formatItemName(waystone.getName()))
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
