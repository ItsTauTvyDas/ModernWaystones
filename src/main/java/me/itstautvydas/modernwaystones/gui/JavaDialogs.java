package me.itstautvydas.modernwaystones.gui;

import io.github.projectunified.unidialog.core.dialog.Dialog;
import io.github.projectunified.unidialog.paper.PaperDialogManager;
import io.github.projectunified.unidialog.paper.dialog.PaperMultiActionDialog;
import io.github.projectunified.unidialog.paper.dialog.PaperNoticeDialog;
import io.github.projectunified.unidialog.paper.input.PaperSingleOptionInput;
import me.itstautvydas.modernwaystones.ModernWaystones;
import me.itstautvydas.modernwaystones.Utils;
import me.itstautvydas.modernwaystones.data.PlayerData;
import me.itstautvydas.modernwaystones.data.WaystoneData;
import me.itstautvydas.modernwaystones.enums.PlayerSortType;
import me.itstautvydas.modernwaystones.enums.WaystoneSound;
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
    public static final String KEY_SORT = "sort";
    public static final String KEY_INVERT_SORT = "invert_sort";
    public static final String KEY_SHOW_NUMBERS = "show_numbers";
    public static final String KEY_SHOW_ATTRIBUTES = "show_attributes";
    public static final String KEY_HIDE_LOCATIONS = "hide_location";
    public static final String KEY_WAYSTONE_BUTTON_WIDTH = "waystone_button_width";
    public static final String KEY_WAYSTONE_SCREEN_COLUMNS = "waystone_screen_columns";
    public static final String KEY_SAVE_WD_AND_CLOSE = "save_wd_and_close";
    public static final String KEY_SAVE_PD_AND_CLOSE = "save_pd_and_close";
    public static final String KEY_SAVE_PSD_AND_CLOSE = "save_psd_and_close";
    public static final String KEY_CUSTOM = "custom";
    public static final String KEY_REMOVE_DEAD_WAYSTONE = "remove_dead_waystone";
    public static final String KEY_BACK_TO_THE_LIST = "back_to_the_list";
    public static final String KEY_OPEN_MANUAL_SORT = "open_manual_sort";
    public static final String KEY_RESET_WAYSTONE_SETTINGS = "reset";

    private final Map<UUID, Set<String>> storedDynamicCustomActionIdentities = new HashMap<>();
    private PaperDialogManager dialogManager;

    private final Map<UUID, Consumer<Player>> runAfterClose = new HashMap<>();

    public JavaDialogs(ModernWaystones plugin) {
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

    private void handleClose(String key, UUID uuid, Map<String, String> data) {
        cleanupPlayerCache(uuid);
        Player player = Bukkit.getPlayer(uuid);
        if (player == null)
            return;
        closeDialog(player);

        switch (key) {
            case KEY_SAVE_WD_AND_CLOSE:
                plugin.getWaystoneDataManager().saveData();
                break;
            case KEY_SAVE_PD_AND_CLOSE:
                plugin.getPlayerDataManager().saveData();
                break;
            case KEY_SAVE_PSD_AND_CLOSE:
                saveWaystoneSettings(player, data);
                break;
        }

        Consumer<Player> consume = runAfterClose.remove(uuid);
        if (consume != null)
            consume.accept(player);
    }

    @Override
    public void register() {
        dialogManager = new PaperDialogManager(plugin, "modernwaystones");
        dialogManager.register();

        dialogManager.registerCustomAction(KEY_CLOSE, (uuid, data) -> handleClose(KEY_CLOSE, uuid, data));
        dialogManager.registerCustomAction(KEY_SAVE_WD_AND_CLOSE, (uuid, data) -> handleClose(KEY_SAVE_WD_AND_CLOSE, uuid, data));
        dialogManager.registerCustomAction(KEY_SAVE_PD_AND_CLOSE, (uuid, data) -> handleClose(KEY_SAVE_PD_AND_CLOSE, uuid, data));
        dialogManager.registerCustomAction(KEY_SAVE_PSD_AND_CLOSE, (uuid, data) -> handleClose(KEY_SAVE_PSD_AND_CLOSE, uuid, data));
        dialogManager.registerCustomAction(KEY_RESET_WAYSTONE_SETTINGS, (uuid, data) -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null)
                return;
            plugin.getPlayerDataMap().remove(uuid);
            plugin.getPlayerDataManager().createData(player.getUniqueId(), true);
            showWaystonePlayerSettingsDialog(player);
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
                    .dynamicCustom(closeAction != null ? baseId + KEY_CUSTOM : KEY_CLOSE));
        }
        dialog.opener().open(viewer);
    }

    @Override
    public void showRenameDialog(Player viewer, WaystoneData clickedWaystone, String initialInput, boolean showNotice, boolean showError) {
        if (initialInput == null || initialInput.isBlank()) {
            throw new IllegalArgumentException("initialInput can't be null/blank!");
        }

        String baseId = viewer.getUniqueId() + "_" + clickedWaystone.getUniqueId() + "_";

        dialogManager.registerCustomAction(storeCustomActionIdentity(viewer, baseId + KEY_NAME_DIALOG_DONE), (uuid, data) -> {
            Player dialogViewer = Bukkit.getPlayer(uuid);
            if (dialogViewer == null)
                return;
            cleanupPlayerCache(uuid);
            String name = data.getOrDefault(KEY_NAME_DIALOG_INPUT, "");
            if (name.isBlank()) {
                showRenameDialog(dialogViewer, clickedWaystone, initialInput, showNotice, true);
                plugin.getWaystoneDataManager().saveData();
            } else {
                clickedWaystone.setName(name);
                plugin.getWaystoneDataManager().saveData();
            }
            closeDialog(dialogViewer);
        });

        dialogManager.registerCustomAction(storeCustomActionIdentity(viewer, baseId + KEY_NAME_DIALOG_CANCEL), (uuid, data) -> {
            Player dialogViewer = Bukkit.getPlayer(uuid);
            if (dialogViewer == null)
                return;
            cleanupPlayerCache(uuid);
            clickedWaystone.setName(initialInput);
            plugin.getWaystoneDataManager().saveData();
            closeDialog(dialogViewer);
        });

        PaperMultiActionDialog dialog = dialogManager
                .createMultiActionDialog()
                .title(ModernWaystones.message("NameInputDialog.Title"))
                .afterAction(Dialog.AfterAction.WAIT_FOR_RESPONSE)
                .canCloseWithEscape(true)
                .pause(false)
                .input(KEY_NAME_DIALOG_INPUT, builder -> builder
                        .textInput()
                        .initial(initialInput)
                        .label(ModernWaystones.message("NameInputDialog." + (showError ? "InputLabel" : "InputLabelRequired"))))
                .columns(2)
                .action(builder -> builder
                        .label(ModernWaystones.message("Done"))
                        .dynamicCustom(storeCustomActionIdentity(viewer, baseId + KEY_NAME_DIALOG_DONE)))
                .action(builder -> builder
                        .label(ModernWaystones.message("Cancel"))
                        .dynamicCustom(storeCustomActionIdentity(viewer, baseId + KEY_NAME_DIALOG_CANCEL)));
        if (showNotice)
            dialog.body(builder -> builder
                    .text()
                    .width(300)
                    .text(ModernWaystones.message("NameInputDialog.Notice")));
        dialog.opener().open(viewer);
    }

    public void showFriendsSettingsDialog(Player viewer, WaystoneData waystone, boolean canEdit, Map<String, String> placeholders, Set<OfflinePlayer> cachedPlayers) {
        PaperMultiActionDialog dialog = dialogManager
                .createMultiActionDialog()
                .title(ModernWaystones.message("FriendsSettingDialog.Title", placeholders))
                .body(builder -> builder.text()
                        .text(ModernWaystones.message("FriendsSettingDialog.Text", placeholders))
                        .width(300))
                .afterAction(Dialog.AfterAction.NONE)
                .canCloseWithEscape(true)
                .columns(canEdit ? 2 : 1)
                .exitAction(builder -> builder
                        .label(ModernWaystones.message("Close"))
                        .dynamicCustom(KEY_SAVE_WD_AND_CLOSE))
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
                    .label(ModernWaystones.message("FriendsSettingDialog.PlayerFormat", placeholders))
                    .tooltip(ModernWaystones.message("FriendsSettingDialog.PlayerTooltipFormat", placeholders))
                    .width(200)
                    .copyToClipboard(player.getName()));

            if (canEdit) {
                String dynamicId = viewer.getUniqueId() + "_" + waystone.getUniqueId() + "_" + player.getUniqueId();
                dialogManager.registerCustomAction(storeCustomActionIdentity(viewer, dynamicId), (uuid, data) -> {
                    Player dialogViewer = Bukkit.getPlayer(uuid);
                    if (dialogViewer == null)
                        return;
                    if (isAdded)
                        waystone.removePlayer(playerId);
                    else
                        waystone.addPlayer(playerId);
                    plugin.getPlayerDataManager().updatePlayersAccesses(waystone, false, true, true);
                    showFriendsSettingsDialog(dialogViewer, waystone, true, placeholders, cachedPlayers);
                });

                dialog.action(builder -> builder
                        .label(ModernWaystones.message("FriendsSettingDialog.ActionButtons." + (isAdded ? "Remove" : "Add") + ".Text", placeholders))
                        .width(plugin.getConfig().getInt("Messages.FriendsSettingDialog.ActionButtons.Width"))
                        .dynamicCustom(dynamicId)
                        .tooltip(ModernWaystones.message("FriendsSettingDialog.ActionButtons." + (isAdded ? "Remove" : "Add") + ".Tooltip", placeholders)));
            }
        }
        dialog.opener().open(viewer);
    }

    @Override
    public void showFriendsSettingsDialog(Player viewer, WaystoneData waystone, boolean canEdit) {
        Map<String, String> placeholders = new HashMap<>();
        fillPlaceholders(placeholders, null, null, waystone, null);

        BukkitTask task = Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Set<OfflinePlayer> players = Arrays.stream(Bukkit.getOfflinePlayers())
                    .sorted(Comparator.comparing((OfflinePlayer x) -> !x.isOnline())
                            .thenComparing(x -> x.getName() == null ? x.getUniqueId().toString() : x.getName()))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            Bukkit.getScheduler().runTask(plugin, () -> showFriendsSettingsDialog(viewer, waystone, canEdit, placeholders, players));
        });

        showSimpleNotice(viewer,
                ModernWaystones.message("FriendsSettingDialog.Title", placeholders),
                ModernWaystones.message("FriendsSettingDialog.LoadingPlayers", placeholders),
                ModernWaystones.message("Close"),
                player -> {
                    closeDialog(player);
                    task.cancel();
                },
                false);
    }

    @Override
    public void showWaystoneDestroyedNoticeDialog(Player viewer, WaystoneData previousClickedWaystone, WaystoneData clickedWaystone, boolean actuallyDestroyed) {
        Map<String, String> placeholders = new HashMap<>();
        fillPlaceholders(placeholders, viewer, null, clickedWaystone, null);
        String baseId = viewer.getUniqueId() + "_" + clickedWaystone.getUniqueId() + "_";

        dialogManager.registerCustomAction(storeCustomActionIdentity(viewer, baseId + KEY_BACK_TO_THE_LIST), (uuid, data) -> {
            Player dialogViewer = Bukkit.getPlayer(uuid);
            if (dialogViewer == null)
                return;
            cleanupPlayerCache(uuid);
            if (actuallyDestroyed && clickedWaystone.isOwner(viewer) &&
                    data.getOrDefault(KEY_REMOVE_DEAD_WAYSTONE, "0.0").equals("1.0")) {
                plugin.getWaystonesMap().remove(clickedWaystone.getLocation());
                plugin.getWaystoneDataManager().saveData();
                plugin.playWaystoneSound(dialogViewer, dialogViewer.getLocation(), WaystoneSound.DEACTIVATED);
            }
            showListDialog(dialogViewer, previousClickedWaystone);
        });

        String message = actuallyDestroyed ? "WaystoneDestroyedNoticeDialog" : "WaystoneInaccessibleNoticeDialog";

        if (actuallyDestroyed)
            placeholders.put("time_until_deletion", "0"); // TODO

        PaperMultiActionDialog dialog = dialogManager
                .createMultiActionDialog()
                .title(ModernWaystones.message(message + ".Title", placeholders))
                .afterAction(Dialog.AfterAction.WAIT_FOR_RESPONSE)
                .canCloseWithEscape(true)
                .columns(1)
                .body(builder -> builder
                        .text()
                        .width(300)
                        .text(ModernWaystones.message(message + ".Message", placeholders)))
                .body(builder -> builder
                        .item()
                        .item(plugin.getCraftManager().createWaystoneItem(clickedWaystone)))
                .action(builder -> builder
                        .dynamicCustom(baseId + KEY_BACK_TO_THE_LIST)
                        .label(ModernWaystones.message(message + ".BackToList", placeholders)))
                .pause(false);

        if (actuallyDestroyed && clickedWaystone.isOwner(viewer)) {
            dialog.input(KEY_REMOVE_DEAD_WAYSTONE, builder -> builder.booleanInput()
                    .label(ModernWaystones.message("WaystoneDestroyedNoticeDialog.Checkbox", placeholders))
                    .initial(false));
        }

        dialog.opener().open(viewer);
    }

    @Override
    public void showSortSettingsDialog(Player viewer) {
        showListDialog(viewer, null, true);
    }

    private void saveWaystoneSettings(Player player, Map<String, String> data) {
        PlayerData playerData = plugin.getPlayerData(player);
        boolean showNumbers = data.get(KEY_SHOW_NUMBERS).equals("1.0");
        boolean hideLocations = data.get(KEY_HIDE_LOCATIONS).equals("1.0");
        boolean showAttributes = data.get(KEY_SHOW_ATTRIBUTES).equals("1.0");
        int width = (int) Double.parseDouble(data.get(KEY_WAYSTONE_BUTTON_WIDTH));
        int columns = (int) Double.parseDouble(data.get(KEY_WAYSTONE_SCREEN_COLUMNS));
        int i = 0;
        if (playerData.setSortType(PlayerSortType.valueOf(data.get(KEY_SORT)), data.get(KEY_INVERT_SORT).equals("1.0"), true))
            i++;
        if (playerData.getHideLocation() != hideLocations) {
            playerData.setHideLocation(hideLocations);
            i++;
        }
        if (playerData.getWaystoneButtonWidth() != width) {
            playerData.setWaystoneButtonWidth(width);
            i++;
        }
        if (playerData.getWaystoneScreenColumns() != columns) {
            playerData.setWaystoneScreenColumns(columns);
            i++;
        }
        if (playerData.getShowNumbers() != showNumbers) {
            playerData.setShowNumbers(showNumbers);
            i++;
        }
        if (playerData.getShowAttributes() != showAttributes) {
            playerData.setShowAttributes(showAttributes);
            i++;
        }
        if (i > 0)
            plugin.getPlayerDataManager().saveData();
    }

    @Override
    public void showWaystonePlayerSettingsDialog(Player viewer) {
        String baseId = viewer.getUniqueId() + "_ws_";

        dialogManager.registerCustomAction(storeCustomActionIdentity(viewer, baseId + KEY_OPEN_MANUAL_SORT), (uuid, data) -> {
            cleanupPlayerCache(uuid);
            Player player = Bukkit.getPlayer(uuid);
            if (player == null)
                return;
            saveWaystoneSettings(player, data);
            showSortSettingsDialog(player);
        });

        PlayerData data = plugin.getPlayerData(viewer);

        dialogManager.createMultiActionDialog()
                .title(ModernWaystones.message("WaystoneSettingsDialog.Title"))
                .afterAction(Dialog.AfterAction.NONE)
                .canCloseWithEscape(true)
                .columns(1)
                .input(KEY_SHOW_NUMBERS, builder -> builder.booleanInput()
                        .label(ModernWaystones.message("WaystoneSettingsDialog.Labels.ShowNumbers"))
                        .initial(data.getShowNumbers()))
                .input(KEY_HIDE_LOCATIONS, builder -> builder.booleanInput()
                        .label(ModernWaystones.message("WaystoneSettingsDialog.Labels.HideLocation"))
                        .initial(data.getHideLocation()))
                .input(KEY_SHOW_ATTRIBUTES, builder -> builder.booleanInput()
                        .label(ModernWaystones.message("WaystoneSettingsDialog.Labels.ShowAttributes"))
                        .initial(data.getShowAttributes()))
                .input(KEY_WAYSTONE_BUTTON_WIDTH, builder -> builder.numberRangeInput()
                        .label(ModernWaystones.message("WaystoneSettingsDialog.Labels.WaystoneButtonWidth"))
                        .labelFormat(ModernWaystones.multiMessage("WaystoneSettingsDialog.Labels.WaystoneButtonWidthFormat"))
                        .start((float) plugin.getConfig().getInt("DefaultPlayerData.WaystoneButtonWidth.Min"))
                        .end((float) plugin.getConfig().getInt("DefaultPlayerData.WaystoneButtonWidth.Max"))
                        .step((float) plugin.getConfig().getInt("DefaultPlayerData.WaystoneButtonWidth.SliderStep"))
                        .initial((float) data.getWaystoneButtonWidth()))
                .input(KEY_WAYSTONE_SCREEN_COLUMNS, builder -> builder.numberRangeInput()
                        .label(ModernWaystones.message("WaystoneSettingsDialog.Labels.WaystoneColumns"))
                        .labelFormat(ModernWaystones.multiMessage("WaystoneSettingsDialog.Labels.WaystoneColumnsFormat"))
                        .start((float) plugin.getConfig().getInt("DefaultPlayerData.WaystoneScreenColumns.Min"))
                        .end((float) plugin.getConfig().getInt("DefaultPlayerData.WaystoneScreenColumns.Max"))
                        .step(1f)
                        .initial((float) data.getWaystoneScreenColumns()))
                .input(KEY_SORT, builder -> {
                    PaperSingleOptionInput input = builder.singleOptionInput()
                            .label(ModernWaystones.message("WaystoneSettingsDialog.Labels.SortBy.Label"));
                    PlayerSortType selectedType = data.getSortType();
                    for (PlayerSortType type : PlayerSortType.values()) {
                        input.option(
                                type.toString(),
                                ModernWaystones.message("WaystoneSettingsDialog.Labels.SortBy.Values." + type),
                                type == selectedType
                        );
                    }
                })
                .input(KEY_INVERT_SORT, builder -> builder.booleanInput()
                        .label(ModernWaystones.message("WaystoneSettingsDialog.Labels.InvertSorting"))
                        .initial(data.isSortingInverted()))
                .action(builder -> builder.dynamicCustom(baseId + KEY_OPEN_MANUAL_SORT)
                        .label(ModernWaystones.message("WaystoneSettingsDialog.Labels.ManualSorting"))
                        .width(200))
                .action(builder -> builder.dynamicCustom(KEY_RESET_WAYSTONE_SETTINGS)
                        .label(ModernWaystones.message("WaystoneSettingsDialog.ResetButton"))
                        .width(200))
                .exitAction(builder -> builder.label(ModernWaystones.message("Close")).dynamicCustom(KEY_SAVE_PSD_AND_CLOSE))
                .pause(false)
                .opener()
                .open(viewer);
    }

    @Override
    public void showListDialog(Player viewer, WaystoneData clickedWaystone) {
        showListDialog(viewer, clickedWaystone, false);
    }

    private void showListDialog(Player viewer, WaystoneData clickedWaystone, boolean isSorting) {
        PlayerData playerData = plugin.getPlayerData(viewer);
        Map<String, String> placeholders = new HashMap<>();
        fillPlaceholders(placeholders, viewer, playerData, null, clickedWaystone);
        Collection<WaystoneData> sortedWaystones = playerData.getSortedWaystones();

        if (sortedWaystones.isEmpty()) {
            showNoWaystonesNotice(viewer, placeholders);
            return;
        }

        PaperMultiActionDialog dialog = dialogManager.createMultiActionDialog();

        int index = 0;
        for (WaystoneData waystone : sortedWaystones) {
            fillPlaceholders(placeholders, viewer, playerData, waystone, null);
            String baseId = viewer.getUniqueId() + "_" + waystone.getUniqueId() + "_";

            if (!isSorting) {
                dialogManager.registerCustomAction(storeCustomActionIdentity(viewer, baseId + KEY_TELEPORT), (uuid, data) -> {
                    cleanupPlayerCache(uuid);
                    Player dialogViewer = Bukkit.getPlayer(uuid);
                    if (dialogViewer == null)
                        return;
                    onWaystoneClick(dialogViewer, playerData, waystone, clickedWaystone);
                });
            }

            fillPlaceholders(placeholders, viewer, playerData, waystone, null);

            if (playerData.getShowNumbers()) {
                int finalIndex = index + 1;
                dialog.action(builder -> builder.width(20).label(Component.text(finalIndex)));
            }

            dialog.action(builder -> builder.width(isSorting ? 200 : playerData.getWaystoneButtonWidth())
                    .label(getWaystoneLabel(waystone, clickedWaystone, placeholders))
                    .tooltip(ModernWaystones.message("WaystoneTooltip", placeholders))
                    .dynamicCustom(baseId + KEY_TELEPORT));

            if (isSorting) {
                boolean canMoveUp = index - 1 >= 0;
                boolean canMoveDown = index + 1 < sortedWaystones.size();

                if (canMoveUp) {
                    dialogManager.registerCustomAction(storeCustomActionIdentity(viewer, baseId + KEY_UP), (uuid, data) -> {
                        Player dialogViewer = Bukkit.getPlayer(uuid);
                        if (dialogViewer == null)
                            return;
                        playerData.moveUp(waystone);
                        showSortSettingsDialog(dialogViewer);
                    });
                }

                if (canMoveDown) {
                    dialogManager.registerCustomAction(storeCustomActionIdentity(viewer, baseId + KEY_DOWN), (uuid, data) -> {
                        Player dialogViewer = Bukkit.getPlayer(uuid);
                        if (dialogViewer == null)
                            return;
                        playerData.moveDown(waystone);
                        showSortSettingsDialog(dialogViewer);
                    });
                }

                dialog.action(builder -> builder.width(20)
                        .label(ModernWaystones.message("ArrowUp")
                                .color(canMoveUp ? NamedTextColor.WHITE : NamedTextColor.GRAY))
                        .tooltip(canMoveUp ? null : ModernWaystones.message("WaystonesListDialog.Sorting.DisallowedUp", placeholders))
                        .dynamicCustom(baseId + KEY_UP));

                dialog.action(builder -> builder.width(20)
                        .label(ModernWaystones.message("ArrowDown")
                                .color(canMoveDown ? NamedTextColor.WHITE : NamedTextColor.GRAY))
                        .tooltip(canMoveDown ? null : ModernWaystones.message("WaystonesListDialog.Sorting.DisallowedDown", placeholders))
                        .dynamicCustom(baseId + KEY_DOWN));
            }

            if (isSorting) {
                runAfterClose.put(viewer.getUniqueId(), x -> x.removeMetadata(KEY_MOVE_WAYSTONE, plugin));
                dialogManager.registerCustomAction(storeCustomActionIdentity(viewer, baseId + KEY_MOVE_WAYSTONE), (uuid, data) -> {
                    Player dialogViewer = Bukkit.getPlayer(uuid);
                    if (dialogViewer == null)
                        return;
                    List<MetadataValue> list = dialogViewer.getMetadata(KEY_MOVE_WAYSTONE);
                    if (!list.isEmpty()) {
                        WaystoneData waystone1 = (WaystoneData) list.getFirst().value();
                        if (waystone != waystone1 && waystone1 != null)
                            playerData.exchange(waystone1, waystone);
                        dialogViewer.removeMetadata(KEY_MOVE_WAYSTONE, plugin);
                    } else {
                        dialogViewer.setMetadata(KEY_MOVE_WAYSTONE, new FixedMetadataValue(plugin, waystone));
                    }
                    showSortSettingsDialog(dialogViewer);
                });
                List<MetadataValue> list = viewer.getMetadata(KEY_MOVE_WAYSTONE);
                if (list.isEmpty()) {
                    dialog.action(builder -> builder
                            .label(ModernWaystones.message("WaystonesListDialog.Sorting.MoveButtons.Move.Text", placeholders))
                            .tooltip(ModernWaystones.message("WaystonesListDialog.Sorting.MoveButtons.Move.Tooltip", placeholders))
                            .width(plugin.getConfig().getInt("Messages.WaystonesListDialog.Sorting.MoveButtons.Width"))
                            .dynamicCustom(baseId + KEY_MOVE_WAYSTONE));
                } else {
                    WaystoneData waystone1 = (WaystoneData) list.getFirst().value();
                    if (waystone1 != null)
                        dialog.action(builder -> builder
                                .label(ModernWaystones.message("WaystonesListDialog.Sorting.MoveButtons.Here." +
                                        (waystone == waystone1 ? "TextWhenSelected" : "Text"), placeholders))
                                .tooltip(ModernWaystones.message("WaystonesListDialog.Sorting.MoveButtons.Here.Tooltip", placeholders)
                                        .replaceText(x -> x.matchLiteral("{selected_waystone}").replacement(waystone1.getName())))
                                .width(plugin.getConfig().getInt("Messages.WaystonesListDialog.Sorting.MoveButtons.Width"))
                                .dynamicCustom(baseId + KEY_MOVE_WAYSTONE));
                }
            }
            index++;
        }

        if (clickedWaystone != null && !clickedWaystone.isGloballyAccessible() && !clickedWaystone.isOwner(viewer)
                && !clickedWaystone.getAddedPlayers().contains(viewer.getUniqueId()))
            dialog.body(builder -> builder.text()
                    .text(ModernWaystones.message("WaystonesListDialog.PrivateWaystoneNotice"))
                    .width(300));

        int columns = isSorting ? 1 : playerData.getWaystoneScreenColumns();
        if (playerData.getShowNumbers())
            columns *= 2;

        if (isSorting) {
            columns += 3; // Add arrow buttons and move button
        } else {
            dialog.body(builder -> builder.item().item(createItem(clickedWaystone, placeholders)));
        }

        dialog.title(ModernWaystones.message("WaystonesListDialog." + (isSorting ? "SortingTitle" : "Title"), placeholders))
                .afterAction(Dialog.AfterAction.NONE)
                .canCloseWithEscape(true)
                .columns(columns)
                .exitAction(builder -> builder.label(ModernWaystones.message("Close"))
                        .dynamicCustom(isSorting ? KEY_SAVE_PD_AND_CLOSE : KEY_CLOSE))
                .pause(false)
                .opener()
                .open(viewer);
    }

    private ItemStack createItem(WaystoneData clickedWaystone, Map<String, String> placeholders) {
        ItemStack item = plugin.getCraftManager().createWaystoneItem(clickedWaystone);
        item.lore(plugin.getConfig()
                .getStringList("Messages.CurrentWaystoneTooltip")
                .stream()
                .map(x -> ModernWaystones.rawMessage("<reset>" + x, placeholders)
                        .decoration(TextDecoration.ITALIC, false)
                        .color(NamedTextColor.WHITE))
                .toList());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Utils.formatItemName(clickedWaystone.getName()));
        item.setItemMeta(meta);
        return item;
    }
}
