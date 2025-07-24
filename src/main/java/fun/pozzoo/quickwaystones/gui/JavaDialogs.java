package fun.pozzoo.quickwaystones.gui;

import fun.pozzoo.quickwaystones.QuickWaystones;
import fun.pozzoo.quickwaystones.WaystoneSound;
import fun.pozzoo.quickwaystones.data.WaystoneData;
import io.github.projectunified.unidialog.core.dialog.Dialog;
import io.github.projectunified.unidialog.paper.PaperDialogManager;
import io.github.projectunified.unidialog.paper.dialog.PaperMultiActionDialog;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class JavaDialogs extends DialogGUI {
//    public static final String KEY_NAME_DIALOG_INPUT = "waystone_name_input";
//    public static final String KEY_NAME_DIALOG_DONE = "waystone_name_input_done";
    public static final String KEY_TELEPORT = "teleport";
    public static final String KEY_UP = "up";
    public static final String KEY_DOWN = "down";
    public static final String KEY_CLOSE = "close";
    public static final String KEY_REMOVE_DEAD_WAYSTONE = "remove_dead_waystone";
    public static final String KEY_BACK_TO_THE_LIST = "back_to_the_list";

    private final Map<UUID, Set<String>> storedDynamicCustomActionIdentities = new HashMap<>();
    private PaperDialogManager dialogManager;

    public JavaDialogs(QuickWaystones plugin) {
        super(plugin);
    }

    private String storeCustomActionIdentity(Player player, String id) {
        UUID uuid = player.getUniqueId();
        Set<String> list = storedDynamicCustomActionIdentities.computeIfAbsent(uuid, k -> new HashSet<>());
        list.add(id);
        return id;
    }

    private void removeCustomActionIdentity(Player player, String id) {
        UUID uuid = player.getUniqueId();
        Set<String> list = storedDynamicCustomActionIdentities.computeIfAbsent(uuid, k -> new HashSet<>());
        list.remove(id);
        dialogManager.unregisterCustomAction(id);
    }

    @Override
    public void register() {
        dialogManager = new PaperDialogManager(plugin, "quickwaystones");
        dialogManager.register();

        dialogManager.registerCustomAction(KEY_CLOSE, (uuid, data) -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null)
                player.closeInventory();
        });
    }

    @Override
    public void unregister() {
        dialogManager.unregister();
    }

    @Override
    public void cleanupPlayerCache(UUID uuid) {
        Set<String> list = storedDynamicCustomActionIdentities.remove(uuid);
        if (list != null)
            list.forEach(dialogManager::unregisterCustomAction);
    }

    // Unused
//    public void showNameInputDialog(Player player, String initialInput, boolean showNotice) {
//        PaperMultiActionDialog dialog = dialogManager
//                .createMultiActionDialog()
//                .title(QuickWaystones.message("NameInputDialog.Title"))
//                .afterAction(Dialog.AfterAction.CLOSE)
//                .canCloseWithEscape(true)
//                .pause(false)
//                .input(KEY_NAME_DIALOG_INPUT, builder -> builder
//                        .textInput()
//                        .initial(initialInput == null ? "" : initialInput)
//                        .label(QuickWaystones.message("NameInputDialog.InputLabel")))
//                .action(builder -> builder
//                        .label(QuickWaystones.message("Done"))
//                        .dynamicCustom(KEY_NAME_DIALOG_DONE));
//        if (showNotice)
//            dialog.body(builder -> builder
//                    .text()
//                    .width(300)
//                    .text(QuickWaystones.message("NameInputDialog.Notice")));
//        dialog.opener().open(player);
//    }

    @Override
    public void showWaystoneDestroyedNoticeDialog(Player player, Player viewer, WaystoneData previousClickedWaystone, WaystoneData clickedWaystone) {
        Map<String, String> placeholders = new HashMap<>();
        fillPlaceholders(placeholders, player, clickedWaystone, null);
        String baseId = player.getUniqueId() + "_" + clickedWaystone.getID() + "_";

        dialogManager.registerCustomAction(storeCustomActionIdentity(player, baseId + KEY_BACK_TO_THE_LIST), (uuid, data) -> {
            Player dialogViewer = Bukkit.getPlayer(uuid);
            if (dialogViewer == null)
                return;
            if (data.getOrDefault(KEY_REMOVE_DEAD_WAYSTONE, "0.0").equals("1.0")) {
                plugin.getWaystonesMap().remove(clickedWaystone.getLocation());
                plugin.getDataManager().saveWaystoneData();
                plugin.playWaystoneSound(dialogViewer, dialogViewer.getLocation(), WaystoneSound.DEACTIVATED);
            }
            showListDialog(player, dialogViewer, previousClickedWaystone);
            removeCustomActionIdentity(dialogViewer, baseId + KEY_BACK_TO_THE_LIST);
        });

        PaperMultiActionDialog dialog = dialogManager
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

    @Override
    public void showListDialog(Player player, Player viewer, WaystoneData clickedWaystone) {
        Map<String, String> placeholders = new HashMap<>();
        fillPlaceholders(placeholders, player, null, clickedWaystone);

        PaperMultiActionDialog dialog = dialogManager
                .createMultiActionDialog()
                .title(QuickWaystones.message("WaystonesListDialog.Title", placeholders))
                .afterAction(Dialog.AfterAction.WAIT_FOR_RESPONSE)
                .canCloseWithEscape(true)
                .columns(3)
                .exitAction(builder -> builder.label(QuickWaystones.message("Close")).dynamicCustom(KEY_CLOSE))
                .pause(false);

        List<WaystoneData> sortedWaystones = getSortedWaystones(player, clickedWaystone);
        for (WaystoneData waystone : sortedWaystones) {
            fillPlaceholders(placeholders, player, waystone, null);
            String baseId = player.getUniqueId() + "_" + waystone.getID() + "_";

            dialogManager.registerCustomAction(storeCustomActionIdentity(player, baseId + KEY_TELEPORT), (uuid, data) -> {
                Player dialogViewer = Bukkit.getPlayer(uuid);
                if (dialogViewer == null)
                    return;
                long delayBefore = plugin.getConfig().getLong("Teleportation.DelayBefore");
                Bukkit.getScheduler().runTaskLater(
                        plugin,
                        () -> doTeleport(uuid, dialogViewer != player, waystone, clickedWaystone),
                        Math.max(0, delayBefore) * 20
                );
                removeCustomActionIdentity(dialogViewer, baseId + KEY_TELEPORT);
            });
            // Sorting
            dialogManager.registerCustomAction(storeCustomActionIdentity(player, baseId + KEY_UP), (uuid, data) -> {
                Player dialogViewer = Bukkit.getPlayer(uuid);
                if (dialogViewer == null)
                    return;
                showListDialog(player, dialogViewer, clickedWaystone);
            });
            dialogManager.registerCustomAction(storeCustomActionIdentity(player, baseId + KEY_DOWN), (uuid, data) -> {
                Player dialogViewer = Bukkit.getPlayer(uuid);
                if (dialogViewer == null)
                    return;
                showListDialog(player, dialogViewer, clickedWaystone);
            });

            fillPlaceholders(placeholders, player, waystone, clickedWaystone);

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
}
