package fun.pozzoo.quickwaystones.gui;

import fun.pozzoo.quickwaystones.QuickWaystones;
import fun.pozzoo.quickwaystones.Utils;
import fun.pozzoo.quickwaystones.enums.WaystoneSound;
import fun.pozzoo.quickwaystones.data.WaystoneData;
import io.github.projectunified.unidialog.core.dialog.Dialog;
import io.github.projectunified.unidialog.paper.PaperDialogManager;
import io.github.projectunified.unidialog.paper.dialog.PaperMultiActionDialog;
import io.github.projectunified.unidialog.paper.dialog.PaperNoticeDialog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JavaDialogs extends DialogGUI {
    public static final String KEY_NAME_DIALOG_INPUT = "waystone_name_input";
    public static final String KEY_NAME_DIALOG_DONE = "waystone_name_input_done";
    public static final String KEY_NAME_DIALOG_CANCEL = "waystone_name_input_cancel";
    public static final String KEY_TELEPORT = "teleport";
    public static final String KEY_MOVE_WAYSTONE = "move_waystone";
    public static final String KEY_UP = "up";
    public static final String KEY_DOWN = "down";
    public static final String KEY_CLOSE = "close";
    public static final String KEY_SAVE_AND_CLOSE = "save_and_close";
    public static final String KEY_CUSTOM = "custom";
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
            cleanupPlayerCache(uuid);
        });

        dialogManager.registerCustomAction(KEY_SAVE_AND_CLOSE, (uuid, data) -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null)
                player.closeInventory();
            plugin.getWaystoneDataManager().saveData();
            cleanupPlayerCache(uuid);
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

    @Override
    public void showSimpleNotice(Player viewer, Component title, Component text, Component button, Consumer<Player> closeAction, boolean closeOnEscape) {
        String baseId = viewer.getUniqueId() + "_";

        PaperNoticeDialog dialog = dialogManager.createNoticeDialog()
                .body(builder -> builder.text()
                        .text(text)
                        .width(250))
                .title(title)
                .canCloseWithEscape(closeOnEscape)
                .afterAction(Dialog.AfterAction.NONE);
        if (button != null) {
            if (closeAction != null)
                dialogManager.registerCustomAction(storeCustomActionIdentity(viewer, baseId + KEY_CUSTOM), (uuid, data) -> {
                    Player dialogViewer = Bukkit.getPlayer(uuid);
                    if (dialogViewer == null)
                        return;
                    closeAction.accept(dialogViewer);
                    removeCustomActionIdentity(dialogViewer, baseId + KEY_CUSTOM);
                });
            dialog.action(builder -> builder
                    .label(button)
                    .dynamicCustom(baseId + KEY_CUSTOM));
        }
        dialog.opener().open(viewer);
    }

    @Override
    public void showRenameDialog(Player player, Player viewer, WaystoneData clickedWaystone, String initialInput, boolean showNotice, boolean showError) {
        if (initialInput == null || initialInput.isBlank()) {
            throw new IllegalArgumentException("initialInput can't be null/blank!");
        }

        String baseId = player.getUniqueId() + "_" + clickedWaystone.getUniqueId() + "_";

        dialogManager.registerCustomAction(storeCustomActionIdentity(player, baseId + KEY_NAME_DIALOG_DONE), (uuid, data) -> {
            Player dialogViewer = Bukkit.getPlayer(uuid);
            if (dialogViewer == null)
                return;
            String name = data.getOrDefault(KEY_NAME_DIALOG_INPUT, "");
            if (name.isBlank()) {
                showRenameDialog(player, dialogViewer, clickedWaystone, initialInput, showNotice, true);
                plugin.getWaystoneDataManager().saveData();
            } else {
                clickedWaystone.setName(name);
                plugin.getWaystoneDataManager().saveData();
            }
            removeCustomActionIdentity(dialogViewer, baseId + KEY_NAME_DIALOG_DONE);
            removeCustomActionIdentity(dialogViewer, baseId + KEY_NAME_DIALOG_CANCEL);
            dialogViewer.closeInventory();
        });

        dialogManager.registerCustomAction(storeCustomActionIdentity(player, baseId + KEY_NAME_DIALOG_CANCEL), (uuid, data) -> {
            Player dialogViewer = Bukkit.getPlayer(uuid);
            if (dialogViewer == null)
                return;
            clickedWaystone.setName(initialInput);
            plugin.getWaystoneDataManager().saveData();
            removeCustomActionIdentity(dialogViewer, baseId + KEY_NAME_DIALOG_DONE);
            removeCustomActionIdentity(dialogViewer, baseId + KEY_NAME_DIALOG_CANCEL);
            dialogViewer.closeInventory();
        });

        PaperMultiActionDialog dialog = dialogManager
                .createMultiActionDialog()
                .title(QuickWaystones.message("NameInputDialog.Title"))
                .afterAction(Dialog.AfterAction.WAIT_FOR_RESPONSE)
                .canCloseWithEscape(true)
                .pause(false)
                .input(KEY_NAME_DIALOG_INPUT, builder -> builder
                        .textInput()
                        .initial(initialInput)
                        .label(QuickWaystones.message("NameInputDialog." + (showError ? "InputLabel" : "InputLabelRequired"))))
                .columns(2)
                .action(builder -> builder
                        .label(QuickWaystones.message("Done"))
                        .dynamicCustom(storeCustomActionIdentity(player, baseId + KEY_NAME_DIALOG_DONE)))
                .action(builder -> builder
                        .label(QuickWaystones.message("Cancel"))
                        .dynamicCustom(storeCustomActionIdentity(player, baseId + KEY_NAME_DIALOG_CANCEL)));
        if (showNotice)
            dialog.body(builder -> builder
                    .text()
                    .width(300)
                    .text(QuickWaystones.message("NameInputDialog.Notice")));
        dialog.opener().open(player);
    }

    public void showFriendsSettingsDialog(Player viewer, WaystoneData waystone, boolean canEdit, Map<String, String> placeholders, Set<OfflinePlayer> cachedPlayers) {
        PaperMultiActionDialog dialog = dialogManager
                .createMultiActionDialog()
                .title(QuickWaystones.message("FriendsSettingDialog.Title", placeholders))
                .body(builder -> builder.text()
                        .text(QuickWaystones.message("FriendsSettingDialog.Text", placeholders))
                        .width(300))
                .afterAction(Dialog.AfterAction.NONE)
                .canCloseWithEscape(true)
                .columns(canEdit ? 2 : 1)
                .exitAction(builder -> builder
                        .label(QuickWaystones.message("Close"))
                        .dynamicCustom(KEY_SAVE_AND_CLOSE))
                .pause(false);
        for (OfflinePlayer player : cachedPlayers) {
            if (player.getUniqueId().equals(waystone.getOwnerUniqueId()))
                continue;
            boolean isAdded = waystone.getAddedPlayers().contains(player.getUniqueId());

            UUID playerId = player.getUniqueId();
            String name = player.hasPlayedBefore() ? player.getName() : playerId.toString();

            placeholders.put("username", name);
            placeholders.put("player_id", playerId.toString());
            placeholders.put("online_status", plugin.getConfig().getString("Messages.PlayerStatuses." + (player.isOnline() ? "Online" : "Offline")));

            dialog.action(builder -> builder
                    .label(QuickWaystones.message("FriendsSettingDialog.PlayerFormat", placeholders))
                    .tooltip(QuickWaystones.message("FriendsSettingDialog.PlayerTooltipFormat", placeholders))
                    .width(200)
                    .copyToClipboard(player.getName()));

            if (canEdit) {
                String dynamicId = viewer.getUniqueId() + "_" + waystone.getUniqueId() + "_" + player.getUniqueId();
                dialogManager.registerCustomAction(storeCustomActionIdentity(viewer, dynamicId), (uuid, data) -> {
                    Player dialogViewer = Bukkit.getPlayer(uuid);
                    System.out.println(data);
                    if (dialogViewer == null)
                        return;
                    if (isAdded)
                        waystone.removePlayer(playerId);
                    else
                        waystone.addPlayer(playerId);
                    showFriendsSettingsDialog(dialogViewer, waystone, true, placeholders, cachedPlayers);
                });

                dialog.action(builder -> builder
                        .label(QuickWaystones.message("FriendsSettingDialog.ActionButtons." + (isAdded ? "Remove" : "Add") + ".Text", placeholders))
                        .width(plugin.getConfig().getInt("Messages.FriendsSettingDialog.ActionButtons.Width"))
                        .dynamicCustom(dynamicId)
                        .tooltip(QuickWaystones.message("FriendsSettingDialog.ActionButtons." + (isAdded ? "Remove" : "Add") + ".Tooltip", placeholders)));
            }
        }
        dialog.opener().open(viewer);
    }

    @Override
    public void showFriendsSettingsDialog(Player viewer, WaystoneData waystone, boolean canEdit) {
        Map<String, String> placeholders = new HashMap<>();
        fillPlaceholders(placeholders, null, waystone, null);

        BukkitTask task = Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Set<OfflinePlayer> players = Arrays.stream(Bukkit.getOfflinePlayers())
                    .sorted(Comparator.comparing((OfflinePlayer x) -> !x.isOnline())
                            .thenComparing(x -> x.getName() == null ? x.getUniqueId().toString() : x.getName()))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            Bukkit.getScheduler().runTask(plugin, () -> showFriendsSettingsDialog(viewer, waystone, canEdit, placeholders, players));
        });

        showSimpleNotice(viewer,
                QuickWaystones.message("FriendsSettingDialog.Title", placeholders),
                QuickWaystones.message("FriendsSettingDialog.LoadingPlayers", placeholders),
                QuickWaystones.message("Close"),
                player -> {
                    player.closeInventory();
                    task.cancel();
                },
                false);
    }

    @Override
    public void showWaystoneDestroyedNoticeDialog(Player player, Player viewer, WaystoneData previousClickedWaystone, WaystoneData clickedWaystone) {
        Map<String, String> placeholders = new HashMap<>();
        fillPlaceholders(placeholders, player, clickedWaystone, null);
        String baseId = player.getUniqueId() + "_" + clickedWaystone.getUniqueId() + "_";

        dialogManager.registerCustomAction(storeCustomActionIdentity(player, baseId + KEY_BACK_TO_THE_LIST), (uuid, data) -> {
            Player dialogViewer = Bukkit.getPlayer(uuid);
            if (dialogViewer == null)
                return;
            if (data.getOrDefault(KEY_REMOVE_DEAD_WAYSTONE, "0.0").equals("1.0")) {
                plugin.getWaystonesMap().remove(clickedWaystone.getLocation());
                plugin.getWaystoneDataManager().saveData();
                plugin.playWaystoneSound(dialogViewer, dialogViewer.getLocation(), WaystoneSound.DEACTIVATED);
            }
            cleanupPlayerCache(uuid);
            showListDialog(player, dialogViewer, previousClickedWaystone);
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

        if (clickedWaystone.isOwner(viewer)) {
            dialog.input(KEY_REMOVE_DEAD_WAYSTONE, builder -> builder.booleanInput()
                    .label(QuickWaystones.message("WaystoneDestroyedNoticeDialog.Checkbox", placeholders))
                    .initial(false));
        }

        dialog.opener().open(viewer);
    }

    @Override
    public void showWaitingDialog(Player viewer, Component title, Function<Long, Component> text, Component cancelButton, long waitTicks, Runnable onClose, Runnable onFinish) {
        if (waitTicks == 0) {
            onFinish.run();
        } else {
            AtomicLong left = new AtomicLong(waitTicks);
            Bukkit.getScheduler().runTaskTimer(
                    plugin,
                    (timer) -> {
                        if (left.addAndGet(-20) == 0) {
                            onFinish.run();
                            timer.cancel();
                            return;
                        }
                        showSimpleNotice(viewer, title, text.apply(left.longValue()), cancelButton, player -> {
                            player.closeInventory();
                            timer.cancel();
                            if (onClose != null)
                                onClose.run();
                        }, false);
                    }, 0, 20
            );
        }
    }

    @Override
    public void showSortSettingsDialog(Player viewer, WaystoneData clickedWaystone) {
        showListDialog(viewer, viewer, clickedWaystone, true);
    }

    @Override
    public void showListDialog(Player player, Player viewer, WaystoneData clickedWaystone) {
        showListDialog(player, viewer, clickedWaystone, false);
    }

    private void showListDialog(Player player, Player viewer, WaystoneData clickedWaystone, boolean isSorting) {
        boolean allowManualSorting = plugin.getConfig().getBoolean("WaystoneScreen.AllowManualSort");
        boolean moveFromToButtons = plugin.getConfig().getBoolean("WaystoneScreen.MoveFromToButtons");
        if (isSorting && !allowManualSorting && !moveFromToButtons)
            return;

        List<WaystoneData> sortedWaystones = getSortedWaystones(player, clickedWaystone);
        Map<String, String> placeholders = new HashMap<>();
        fillPlaceholders(placeholders, player, null, clickedWaystone);

        PaperMultiActionDialog dialog = dialogManager.createMultiActionDialog();

        boolean empty = true;
        int index = 0;
        int failedWaystones = 0; // clickedWaystone is always going to be there anyway
        for (WaystoneData waystone : sortedWaystones) {
            Utils.loadChunkIfNeeded(waystone.getLocation());
            if (waystone.getAddedPlayers().contains(player.getUniqueId()) && !plugin.isWaystoneFriendsBlock(waystone.getLocation().clone().add(0, -1, 0).getBlock())) {
                failedWaystones++;
                continue;
            }
            if (plugin.isWaystoneDestroyed(waystone.getBlock()) && !waystone.isOwner(player)) {
                failedWaystones++;
                continue;
            }
            empty = false;
            fillPlaceholders(placeholders, player, waystone, null);
            String baseId = player.getUniqueId() + "_" + waystone.getUniqueId() + "_";

            if (!isSorting) {
                dialogManager.registerCustomAction(storeCustomActionIdentity(player, baseId + KEY_TELEPORT), (uuid, data) -> {
                    Player dialogViewer = Bukkit.getPlayer(uuid);
                    if (dialogViewer == null)
                        return;
                    long delayBefore = Math.max(0, plugin.getConfig().getLong("Teleportation.DelayBefore"));
                    cleanupPlayerCache(uuid);
                    fillPlaceholders(placeholders, player, waystone, null);

                    Utils.loadChunkIfNeeded(waystone.getLocation());
                    if (plugin.isWaystoneDestroyed(waystone.getBlock())) {
                        showWaystoneDestroyedNoticeDialog(dialogViewer, clickedWaystone, waystone);
                        return;
                    }

                    showWaitingDialog(dialogViewer,
                            QuickWaystones.message("WaystonesListDialog.Teleporting.Title", placeholders),
                            ticksLeft -> {
                                placeholders.put("seconds", Long.toString(ticksLeft / 20));
                                return QuickWaystones.message("WaystonesListDialog.Teleporting.Text", placeholders);
                            },
                            QuickWaystones.message("Cancel"),
                            delayBefore * 20,
                            null, () -> doTeleport(uuid, dialogViewer != player, waystone, clickedWaystone));
                });
            }

            boolean canMoveUp = index - 1 >= 0;
            boolean canMoveDown = index + 1 < sortedWaystones.size() - failedWaystones;

            fillPlaceholders(placeholders, player, waystone, clickedWaystone);

            if (plugin.getConfig().getBoolean("WaystoneScreen.ShowNumbers")) {
                int finalIndex = index + 1;
                dialog.action(builder -> builder.width(20).label(Component.text(finalIndex)));
            }

            dialog.action(builder -> builder.width(200)
                    .label(QuickWaystones.message(
                            "WaystonesListDialog." + (waystone.isInternal() ?
                                    "ServerWaystoneButton" :
                                    (waystone == clickedWaystone ?
                                            "CurrentWaystoneButton" :
                                            "WaystoneButton")), placeholders))
                    .tooltip(QuickWaystones.message("WaystonesListDialog.WaystoneTooltip", placeholders))
                    .dynamicCustom(baseId + KEY_TELEPORT));

            if (isSorting && allowManualSorting) {
                if (canMoveUp)
                    dialogManager.registerCustomAction(storeCustomActionIdentity(player, baseId + KEY_UP), (uuid, data) -> {
                        Player dialogViewer = Bukkit.getPlayer(uuid);
                        if (dialogViewer == null)
                            return;
                        showSortSettingsDialog(dialogViewer, clickedWaystone);
                    });
                if (canMoveDown)
                    dialogManager.registerCustomAction(storeCustomActionIdentity(player, baseId + KEY_DOWN), (uuid, data) -> {
                        Player dialogViewer = Bukkit.getPlayer(uuid);
                        if (dialogViewer == null)
                            return;
                        showSortSettingsDialog(dialogViewer, clickedWaystone);
                    });

                dialog.action(builder -> builder.width(20)
                        .label(QuickWaystones.message("ArrowUp")
                                .color(canMoveUp ? NamedTextColor.WHITE : NamedTextColor.DARK_GRAY))
                        .tooltip(canMoveUp ? null : QuickWaystones.message("WaystonesListDialog.Sorting.DisallowedUp"))
                        .dynamicCustom(baseId + KEY_UP));

                dialog.action(builder -> builder.width(20)
                        .label(QuickWaystones.message("ArrowDown")
                                .color(canMoveDown ? NamedTextColor.WHITE : NamedTextColor.DARK_GRAY))
                        .tooltip(canMoveDown ? null : QuickWaystones.message("WaystonesListDialog.Sorting.DisallowedDown"))
                        .dynamicCustom(baseId + KEY_DOWN));
            }

            if (isSorting && moveFromToButtons) {
                dialogManager.registerCustomAction(storeCustomActionIdentity(player, baseId + KEY_MOVE_WAYSTONE), (uuid, data) -> {
                    Player dialogViewer = Bukkit.getPlayer(uuid);
                    if (dialogViewer == null)
                        return;
                    List<MetadataValue> list = viewer.getMetadata(KEY_MOVE_WAYSTONE);
                    if (!list.isEmpty()) {
                        viewer.removeMetadata(KEY_MOVE_WAYSTONE, plugin);
                    } else {
                        viewer.setMetadata(KEY_MOVE_WAYSTONE, new FixedMetadataValue(plugin, null));
                    }
                    showSortSettingsDialog(dialogViewer, clickedWaystone);
                });
                List<MetadataValue> list = viewer.getMetadata(KEY_MOVE_WAYSTONE);
                if (!list.isEmpty()) {
                    dialog.action(builder -> builder
                            .label(QuickWaystones.message("WaystonesListDialog.Sorting.MoveButtons.Move.Text"))
                            .tooltip(QuickWaystones.message("WaystonesListDialog.Sorting.MoveButtons.Move.Tooltip"))
                            .width(plugin.getConfig().getInt("Messages.WaystonesListDialog.Sorting.MoveButtons.Width"))
                            .dynamicCustom(baseId + KEY_MOVE_WAYSTONE));
                } else {
                    dialog.action(builder -> builder
                            .label(QuickWaystones.message("WaystonesListDialog.Sorting.MoveButtons.Here.Text"))
                            .tooltip(QuickWaystones.message("WaystonesListDialog.Sorting.MoveButtons.Here.Tooltip"))
                            .width(plugin.getConfig().getInt("Messages.WaystonesListDialog.Sorting.MoveButtons.Width"))
                            .dynamicCustom(baseId + KEY_MOVE_WAYSTONE));
                }
            }
            index++;
        }

        if (empty) {
            showSimpleNotice(viewer,
                    QuickWaystones.message("WaystonesListDialog.Title", placeholders),
                    QuickWaystones.message("WaystonesListDialog.NoWaystonesNotice", placeholders),
                    QuickWaystones.message("Close", placeholders),
                    Player::closeInventory,
                    true);
            return;
        }

        if (!clickedWaystone.isGloballyAccessible() && !clickedWaystone.isOwner(player) && !clickedWaystone.getAddedPlayers().contains(player.getUniqueId()))
            dialog.body(builder -> builder.text()
                    .text(QuickWaystones.message("WaystonesListDialog.PrivateWaystoneNotice"))
                    .width(300));

        int columns = 1;
        if (plugin.getConfig().getBoolean("WaystoneScreen.ShowNumbers"))
            columns++;

        if (isSorting) {
            if (moveFromToButtons)
                columns++;
            if (allowManualSorting)
                columns += 2;
        } else {
            ItemStack item = plugin.getCraftManager().createWaystoneItem(clickedWaystone);
            item.lore(plugin.getConfig()
                    .getStringList("Messages.WaystonesListDialog.CurrentWaystoneTooltip")
                    .stream()
                    .map(x -> QuickWaystones.rawMessage("<reset>" + x, placeholders)
                            .decoration(TextDecoration.ITALIC, false)
                            .color(NamedTextColor.WHITE))
                    .toList());
            ItemMeta meta = item.getItemMeta();
            meta.displayName(QuickWaystones.rawMessage("<reset>" + clickedWaystone.getName(), placeholders)
                    .decoration(TextDecoration.ITALIC, false)
                    .color(NamedTextColor.WHITE));
            item.setItemMeta(meta);
            dialog.body(builder -> builder.item().item(item));
        }

        dialog.title(QuickWaystones.message("WaystonesListDialog.Title", placeholders))
                .afterAction(Dialog.AfterAction.NONE)
                .canCloseWithEscape(true)
                .columns(columns)
                .exitAction(builder -> builder.label(QuickWaystones.message("Close")).dynamicCustom(KEY_CLOSE))
                .pause(false)
                .opener()
                .open(viewer);
    }
}
