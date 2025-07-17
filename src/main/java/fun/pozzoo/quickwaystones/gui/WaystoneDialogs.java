package fun.pozzoo.quickwaystones.gui;

import fun.pozzoo.quickwaystones.QuickWaystones;
import fun.pozzoo.quickwaystones.data.WaystoneData;
import io.github.projectunified.unidialog.core.dialog.Dialog;
import io.github.projectunified.unidialog.paper.dialog.PaperMultiActionDialog;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WaystoneDialogs {
    public static final String KEY_NAME_DIALOG_INPUT = "waystone_name_input";
    public static final String KEY_NAME_DIALOG_DONE = "waystone_name_input_done";
    public static final String KEY_TELEPORT = "teleport";
    public static final String KEY_UP = "up";
    public static final String KEY_DOWN = "down";
    public static final String KEY_CLOSE = "close";

    private final QuickWaystones plugin;

    public WaystoneDialogs(QuickWaystones plugin) {
        this.plugin = plugin;
    }

    public void registerEvents() {
        plugin.getDialogManager().registerCustomAction(KEY_CLOSE, (uuid, data) -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null)
                player.closeInventory();
        });
    }

    public void showNameInputDialog(Player player, String initialInput, boolean showNotice) {
        PaperMultiActionDialog dialog = QuickWaystones.getInstance().getDialogManager()
                .createMultiActionDialog()
                .title(QuickWaystones.message("NameInputDialog.Title"))
                .afterAction(Dialog.AfterAction.CLOSE)
                .canCloseWithEscape(true)
                .pause(false)
                .input(KEY_NAME_DIALOG_INPUT, builder -> builder
                        .textInput()
                        .initial(initialInput == null ? "" : initialInput)
                        .label(QuickWaystones.message("NameInputDialog.InputLabel")))
                .action(builder -> builder
                        .label(QuickWaystones.message("Done"))
                        .dynamicCustom(KEY_NAME_DIALOG_DONE));
        if (showNotice)
            dialog.body(builder -> builder
                    .text()
                    .width(300)
                    .text(QuickWaystones.message("NameInputDialog.Notice")));
        dialog.opener().open(player);
    }

    public void showListDialog(Player player, WaystoneData clickedWaystone) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("current_name", clickedWaystone.getName());
        placeholders.put("current_id", clickedWaystone.getID());
        placeholders.put("current_owner", clickedWaystone.getOwner());

        PaperMultiActionDialog dialog = QuickWaystones.getInstance().getDialogManager()
                .createMultiActionDialog()
                .title(QuickWaystones.message("WaystonesListDialog.Title", placeholders))
                .afterAction(Dialog.AfterAction.WAIT_FOR_RESPONSE)
                .canCloseWithEscape(true)
                .columns(3)
                .exitAction(builder -> builder.label(QuickWaystones.message("Close")).dynamicCustom(KEY_CLOSE))
                .pause(false);

        Map<Location, WaystoneData> waystones = plugin.getWaystonesMap();

        List<WaystoneData> sortedWaystones = waystones.values().stream()
                .sorted(Comparator.comparing(WaystoneData::getCreatedAt))
                .toList();

        for (WaystoneData waystone : sortedWaystones) {
            // Adding player ID just in case
            String baseId = player.getUniqueId() + "_" + waystone.getID() + "_";

            QuickWaystones.getInstance().getDialogManager().registerCustomAction(baseId + KEY_TELEPORT, (uuid, data) -> {
                Location loc = waystone.getLocation().toCenterLocation().add(0, 1, 0);
                if (loc.getBlock().getType().isOccluding())
                    loc.add(0, 1, 0);
                player.teleport(loc);
                player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 5);
                player.playSound(player, Sound.ENTITY_FOX_TELEPORT, 0.5f, 1f);
                player.setNoDamageTicks(10);
                player.closeInventory();
            });
            QuickWaystones.getInstance().getDialogManager().registerCustomAction(baseId + KEY_UP, (uuid, data) -> {
                showListDialog(player, clickedWaystone);
            });
            QuickWaystones.getInstance().getDialogManager().registerCustomAction(baseId + KEY_DOWN, (uuid, data) -> {
                showListDialog(player, clickedWaystone);
            });

            placeholders.put("name", waystone.getName());
            placeholders.put("id", waystone.getID());
            placeholders.put("owner", waystone.getOwner());
            placeholders.put("x", Integer.toString(waystone.getLocation().getBlockX()));
            placeholders.put("y", Integer.toString(waystone.getLocation().getBlockY()));
            placeholders.put("z", Integer.toString(waystone.getLocation().getBlockZ()));
            placeholders.put("world", QuickWaystones.config()
                    .getString("Messages.WorldReplacedNames." + waystone.getLocation().getWorld().getName(),
                            waystone.getLocation().getWorld().getName()
                    ));
            placeholders.put("visibility", QuickWaystones.config().getString("Messages.Visibilities."
                    + (waystone.isGloballyAccessible() ? "Public" : "Private")));

            dialog.action(builder -> builder.width(150)
                    .label(QuickWaystones.message("WaystonesListDialog.WaystoneButton", placeholders))
                    .tooltip(QuickWaystones.message("WaystonesListDialog.WaystoneTooltip", placeholders))
                    .dynamicCustom(baseId + KEY_TELEPORT));

            dialog.action(builder -> builder.width(20)
                    .label(QuickWaystones.message("ArrowUp"))
                    .dynamicCustom(baseId + KEY_UP));

            dialog.action(builder -> builder.width(20)
                    .label(QuickWaystones.message("ArrowDown"))
                    .dynamicCustom(baseId + KEY_DOWN));
        }

        dialog.opener().open(player);
    }
}
