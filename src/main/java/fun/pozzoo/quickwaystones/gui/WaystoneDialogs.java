package fun.pozzoo.quickwaystones.gui;

import fun.pozzoo.quickwaystones.QuickWaystones;
import fun.pozzoo.quickwaystones.WaystoneSound;
import fun.pozzoo.quickwaystones.data.WaystoneData;
import io.github.projectunified.unidialog.core.dialog.Dialog;
import io.github.projectunified.unidialog.paper.dialog.PaperMultiActionDialog;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import java.util.*;

public class WaystoneDialogs {
    public static final String KEY_NAME_DIALOG_INPUT = "waystone_name_input";
    public static final String KEY_NAME_DIALOG_DONE = "waystone_name_input_done";
    public static final String KEY_TELEPORT = "teleport";
    public static final String KEY_UP = "up";
    public static final String KEY_DOWN = "down";
    public static final String KEY_CLOSE = "close";
    public static final String KEY_LAST_WAYSTONE = "last_waystone";
    public static final String KEY_REMOVE_DEAD_WAYSTONE = "remove_dead_waystone";
    public static final String KEY_BACK_TO_THE_LIST = "back_to_the_list";

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
        PaperMultiActionDialog dialog = plugin.getDialogManager()
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

    public void showWaystoneDestroyedNoticeDialog(Player player, WaystoneData previousClickedWaystone, WaystoneData clickedWaystone) {
        showWaystoneDestroyedNoticeDialog(player, player, previousClickedWaystone, clickedWaystone);
    }

    public void showWaystoneDestroyedNoticeDialog(Player player, Player viewer, WaystoneData previousClickedWaystone, WaystoneData clickedWaystone) {
        Map<String, String> placeholders = new HashMap<>();
        fillPlaceholders(placeholders, clickedWaystone);
        String baseId = clickedWaystone.getID() + "_";

        plugin.getDialogManager().registerCustomAction(baseId + KEY_BACK_TO_THE_LIST, (uuid, data) -> {
            Player dialogPlayer = Bukkit.getPlayer(uuid);
            if (dialogPlayer == null)
                return;
            if (data.getOrDefault(KEY_REMOVE_DEAD_WAYSTONE, "0.0").equals("1.0")) {
                plugin.getWaystonesMap().remove(clickedWaystone.getLocation());
                plugin.getDataManager().saveWaystoneData();
                plugin.playWaystoneSound(dialogPlayer, dialogPlayer.getLocation(), WaystoneSound.DEACTIVATED);
            }
            showListDialog(dialogPlayer, viewer, previousClickedWaystone);
        });

        PaperMultiActionDialog dialog = plugin.getDialogManager()
                .createMultiActionDialog()
                .title(QuickWaystones.message("WaystoneDestroyedNoticeDialog.Title", placeholders))
                .afterAction(Dialog.AfterAction.WAIT_FOR_RESPONSE)
                .canCloseWithEscape(true)
                .columns(1)
                .body(builder -> builder
                        .text()
                        .width(300)
                        .text(QuickWaystones.message("WaystoneDestroyedNoticeDialog.Message", placeholders)))
                .body(builder -> builder
                        .item()
                        .item(plugin.getCraftManager().createWaystoneItem(clickedWaystone)))
                .action(builder -> builder
                        .dynamicCustom(baseId + KEY_BACK_TO_THE_LIST)
                        .label(QuickWaystones.message("WaystoneDestroyedNoticeDialog.BackToList", placeholders)))
                .pause(false);

        if (clickedWaystone.getOwnerUniqueId().equals(viewer.getUniqueId())) {
            dialog.input(KEY_REMOVE_DEAD_WAYSTONE, builder -> builder.booleanInput()
                    .label(QuickWaystones.message("WaystoneDestroyedNoticeDialog.Checkbox", placeholders))
                    .initial(false));
        }

        dialog.opener().open(viewer);
    }

    public void showListDialog(Player player, WaystoneData clickedWaystone) {
        showListDialog(player, player, clickedWaystone);
    }

    public void showListDialog(Player player, Player viewer, WaystoneData clickedWaystone) {
        Map<String, String> placeholders = new HashMap<>();
        if (clickedWaystone != null) {
            placeholders.put("current_name", clickedWaystone.getName());
            placeholders.put("current_id", clickedWaystone.getID());
            placeholders.put("current_owner", clickedWaystone.getOwner());
        } else {
            placeholders.put("current_name", "");
            placeholders.put("current_id", "");
            placeholders.put("current_owner", "");
        }

        PaperMultiActionDialog dialog = plugin.getDialogManager()
                .createMultiActionDialog()
                .title(QuickWaystones.message("WaystonesListDialog.Title", placeholders))
                .afterAction(Dialog.AfterAction.WAIT_FOR_RESPONSE)
                .canCloseWithEscape(true)
                .columns(3)
                .exitAction(builder -> builder.label(QuickWaystones.message("Close")).dynamicCustom(KEY_CLOSE))
                .pause(false);

        Map<Location, WaystoneData> waystones = plugin.getWaystonesMap();
        List<WaystoneData> sortedWaystones = waystones.values().stream()
                .filter(x ->
                        (clickedWaystone == null && x.getOwnerUniqueId().equals(player.getUniqueId())) ||
                        (clickedWaystone != null && (x.isGloballyAccessible()
                                || x.getAddedPlayers().contains(player.getUniqueId())
                                || x.getOwnerUniqueId().equals(player.getUniqueId())
                        )))
                .sorted(Comparator.comparing(WaystoneData::getCreatedAt))
                .toList();

        for (WaystoneData waystone : sortedWaystones) {
            String baseId = waystone.getID() + "_";

            plugin.getDialogManager().registerCustomAction(baseId + KEY_TELEPORT, (uuid, data) -> {
                Player dialogPlayer = Bukkit.getPlayer(uuid);
                if (dialogPlayer == null)
                    return;
                if (plugin.isWaystoneDestroyed(waystone.getBlock())) {
                    showWaystoneDestroyedNoticeDialog(dialogPlayer, viewer, clickedWaystone, waystone);
                    return;
                }
                plugin.playWaystoneSound(null, player.getLocation(), WaystoneSound.TELEPORTED);
                Location location = waystone.getTeleportLocation();
                dialogPlayer.teleport(location);
                dialogPlayer.getWorld().spawnParticle(Particle.PORTAL, dialogPlayer.getLocation(), 5);
//                dialogPlayer.playSound(dialogPlayer, Sound.ENTITY_FOX_TELEPORT, 0.5f, 1f);
                plugin.playWaystoneSound(null, location, WaystoneSound.TELEPORTED);
                dialogPlayer.setNoDamageTicks(10);
                dialogPlayer.setMetadata(KEY_LAST_WAYSTONE, new FixedMetadataValue(plugin, waystone.getID()));
                waystone.markLastUsed();
                plugin.getDataManager().saveWaystoneData();
                dialogPlayer.closeInventory();
            });
            plugin.getDialogManager().registerCustomAction(baseId + KEY_UP, (uuid, data) -> {
                Player dialogPlayer = Bukkit.getPlayer(uuid);
                if (dialogPlayer == null)
                    return;
                showListDialog(dialogPlayer, clickedWaystone);
            });
            plugin.getDialogManager().registerCustomAction(baseId + KEY_DOWN, (uuid, data) -> {
                Player dialogPlayer = Bukkit.getPlayer(uuid);
                if (dialogPlayer == null)
                    return;
                showListDialog(dialogPlayer, clickedWaystone);
            });

            fillPlaceholders(placeholders, waystone);

            List<String> attributes = new ArrayList<>();
            attributes.add(plugin.getConfig().getString("Messages.WaystoneAttributes." + (waystone.isGloballyAccessible() ? "Public" : "Private")));
            if (plugin.isWaystoneDestroyed(waystone.getLocation().getBlock()))
                attributes.add(plugin.getConfig().getString("Messages.WaystoneAttributes.Destroyed"));
            if (player.hasMetadata(KEY_LAST_WAYSTONE)) {
                List<MetadataValue> metadata = player.getMetadata(KEY_LAST_WAYSTONE);
                if (!metadata.isEmpty() && waystone.getID().equals(player.getMetadata(KEY_LAST_WAYSTONE).getFirst().asString()))
                    attributes.add(plugin.getConfig().getString("Messages.WaystoneAttributes.LastlyUsed"));
            }
            placeholders.put("attributes", String.join(plugin.getConfig().getString("Messages.WaystoneAttributes.Separator", ", "), attributes));

            dialog.action(builder -> builder.width(200)
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

        dialog.opener().open(viewer);
    }

    private void fillPlaceholders(Map<String, String> placeholders, WaystoneData waystone) {
        placeholders.put("name", waystone.getName());
        placeholders.put("id", waystone.getID());
        placeholders.put("owner", waystone.getOwner());
        placeholders.put("x", Integer.toString(waystone.getLocation().getBlockX()));
        placeholders.put("y", Integer.toString(waystone.getLocation().getBlockY()));
        placeholders.put("z", Integer.toString(waystone.getLocation().getBlockZ()));
        placeholders.put("world", plugin.getConfig()
                .getString("Messages.WorldReplacedNames." + waystone.getLocation().getWorld().getName(),
                        waystone.getLocation().getWorld().getName()
                ));
    }
}
