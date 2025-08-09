package me.itstautvydas.modernwaystones.gui;

import me.itstautvydas.modernwaystones.ModernWaystones;
import me.itstautvydas.modernwaystones.data.PlayerData;
import me.itstautvydas.modernwaystones.data.WaystoneData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.Form;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.*;
import java.util.function.Consumer;

public class BedrockDialogs extends DialogGUI {
    private final FloodgateApi api;

    public BedrockDialogs(ModernWaystones plugin, FloodgateApi bedrockAPI) {
        super(plugin);
        this.api = bedrockAPI;
    }

    @Override
    public void register() {
        // Nothing to register
    }

    @Override
    public void unregister() {
        // Nothing to unregister
    }

    @Override
    public void showWaystoneInaccesibleNoticeDialog(Player viewer, WaystoneData previousClickedWaystone, WaystoneData clickedWaystone, boolean actuallyDestroyed) {

    }

    @Override
    public void showListDialog(Player viewer, WaystoneData clickedWaystone) {
        Map<String, String> placeholders = new HashMap<>();
        PlayerData playerData = plugin.getPlayerData(viewer);
        fillPlaceholders(placeholders, viewer, playerData, null, clickedWaystone);
        List<WaystoneData> sortedWaystones = new ArrayList<>(playerData.getSortedWaystones());
        if (sortedWaystones.isEmpty()) {
            showNoWaystonesNotice(viewer, placeholders);
            return;
        }
        int recentIndex = sortedWaystones.indexOf(plugin.getRecentWaystone(viewer));
        if (recentIndex == -1)
            recentIndex = 0;
        CustomForm form = CustomForm.builder()
                .label(serialize(ModernWaystones.message("BedrockExtraMessages.CurrentWaystoneTooltip", placeholders)))
                .optionalLabel(
                        serialize(ModernWaystones.message("WaystonesListDialog.PrivateWaystoneNotice", placeholders)),
                        clickedWaystone != null && !clickedWaystone.isGloballyAccessible() && !clickedWaystone.isOwner(viewer)
                                && !clickedWaystone.getAddedPlayers().contains(viewer.getUniqueId())
                ).title(serialize(ModernWaystones.message("WaystonesListDialog.Title", placeholders)))
                .dropdown(
                        serialize(ModernWaystones.message("BedrockExtraMessages.SelectWaystone", placeholders)),
                        sortedWaystones
                                .stream()
                                .map(waystone -> {
                                    fillPlaceholders(placeholders, viewer, playerData, waystone, null);
                                    return serialize(getWaystoneLabel(waystone, clickedWaystone, placeholders));
                                })
                                .toList(),
                        recentIndex)
                .validResultHandler(resp -> {
                    int button = resp.asDropdown();
                    WaystoneData waystone = sortedWaystones.stream().toList().get(button);
                    onWaystoneClick(viewer, playerData, waystone, clickedWaystone);
                }).build();
        sendForm(viewer, form);
    }

    @Override
    public void showRenameDialog(Player viewer, WaystoneData clickedWaystone, String initialInput, boolean showNotice, boolean showError) {

    }

    @Override
    public void showSimpleNotice(Player viewer, Component title, Component text, Component button, Consumer<Player> action, boolean closeOnEscape) {
        SimpleForm.Builder form = SimpleForm.builder()
                .title(serialize(title))
                .content(serialize(text));
        if (!closeOnEscape)
            form.closedResultHandler(modal -> sendForm(viewer, modal));
        if (button != null) {
            form.button(serialize(button));
            if (action != null)
                form.validResultHandler(result -> action.accept(viewer));
        }
        sendForm(viewer, form.build());
    }

    public void showFriendsSettingsDialog(Player viewer, WaystoneData waystone, boolean canEdit, List<OfflinePlayer> cachedPlayers) {
        Map<String, String> placeholders = new HashMap<>();
        fillPlaceholders(placeholders, null, null, waystone, null);

        CustomForm.Builder form = CustomForm.builder()
                .title(serialize(ModernWaystones.message("FriendsSettingDialog.Title", placeholders)))
                .optionalLabel(serialize(ModernWaystones.message("FriendsSettingDialog.Text", placeholders)), canEdit);

        for (OfflinePlayer player : cachedPlayers) {
            if (player.getUniqueId().equals(waystone.getOwnerUniqueId()))
                continue;
            boolean isAdded = waystone.getAddedPlayers().contains(player.getUniqueId());
            fillPlaceholders(placeholders, player);

            if (canEdit) {
                form.toggle(serialize(ModernWaystones.message("FriendsSettingDialog.PlayerFormat", placeholders)), isAdded);
                form.validResultHandler(response -> {
                    // start from 1 because 0 is a label
                    for (int i = 1; i <= cachedPlayers.size(); i++) {
                        if (response.asToggle(i))
                            waystone.addPlayer(cachedPlayers.get(i).getUniqueId());
                        else
                            waystone.removePlayer(cachedPlayers.get(i).getUniqueId());
                    }
                    plugin.getPlayerDataManager().updatePlayersAccesses(waystone, false, true, true);
                });
            } else {
                form.optionalLabel(serialize(ModernWaystones.message("FriendsSettingDialog.PlayerFormat", placeholders)), isAdded);
            }
        }

        sendForm(viewer, form.build());
    }

    @Override
    public void showFriendsSettingsDialog(Player viewer, WaystoneData waystone, boolean canEdit) {
        showLoadingOfflinePlayersDialog(viewer, players -> showFriendsSettingsDialog(viewer, waystone, canEdit, players));
    }

    @Override
    public void showSortSettingsDialog(Player viewer) {
        // No idea how to do manual sorting in bedrock forms, they are way more restricted compared to dialogs in Java.
        throw new UnsupportedOperationException();
    }

    @Override
    public void showWaystonePlayerSettingsDialog(Player viewer) {

    }

    private void sendForm(Player player, Form form) {
        api.closeForm(player.getUniqueId());
        if (form != null)
            api.sendForm(player.getUniqueId(), form);
    }

    @Override
    public void closeDialog(Player player) {
        sendForm(player, null);
    }

    @Override
    public void closeDialogIfOpened(Player player) {
        sendForm(player, null);
    }

    @SuppressWarnings("deprecation")
    private String serialize(Component component) {
        return ChatColor.translateAlternateColorCodes('&', LegacyComponentSerializer.legacyAmpersand().serialize(component));
    }
}
