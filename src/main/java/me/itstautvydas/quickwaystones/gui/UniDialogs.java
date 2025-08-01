package me.itstautvydas.quickwaystones.gui;

import me.itstautvydas.quickwaystones.QuickWaystones;
import me.itstautvydas.quickwaystones.data.WaystoneData;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

public class UniDialogs extends DialogGUI {
    private final JavaDialogs java;
    private final BedrockDialogs bedrock;
    private final FloodgateApi bedrockAPI;

    private UniDialogs(QuickWaystones plugin) {
        super(plugin);
        java = new JavaDialogs(plugin);
        bedrock = new BedrockDialogs(plugin);
        bedrockAPI = FloodgateApi.getInstance();
    }

    public static DialogGUI tryCreate(QuickWaystones plugin) {
        if (QuickWaystones.isFloodgateRunning())
            return new UniDialogs(plugin);
        return new JavaDialogs(plugin);
    }

    @Override
    public void register() {
        java.register();
    }

    @Override
    public void unregister() {
        java.unregister();
    }

    @Override
    public void cleanupPlayerCache(UUID uuid) {
        java.cleanupPlayerCache(uuid);
    }

    @Override
    public void showWaystoneDestroyedNoticeDialog(Player player, Player viewer, WaystoneData previousClickedWaystone, WaystoneData clickedWaystone) {
        if (bedrockAPI.isFloodgatePlayer(player.getUniqueId())) {
            bedrock.showWaystoneDestroyedNoticeDialog(player, viewer, previousClickedWaystone, clickedWaystone);
        } else {
            java.showWaystoneDestroyedNoticeDialog(player, viewer, previousClickedWaystone, clickedWaystone);
        }
    }

    @Override
    public void showListDialog(Player player, Player viewer, WaystoneData clickedWaystone) {
        if (bedrockAPI.isFloodgatePlayer(player.getUniqueId())) {
            bedrock.showListDialog(player, viewer, clickedWaystone);
        } else {
            java.showListDialog(player, viewer, clickedWaystone);
        }
    }

    @Override
    public void showRenameDialog(Player player, Player viewer, WaystoneData clickedWaystone, String initialInput, boolean showNotice, boolean showError) {
        if (bedrockAPI.isFloodgatePlayer(player.getUniqueId())) {
            bedrock.showRenameDialog(player, viewer, clickedWaystone, initialInput, showNotice, showError);
        } else {
            java.showRenameDialog(player, viewer, clickedWaystone, initialInput, showNotice, showError);
        }
    }

    @Override
    public void showFriendsSettingsDialog(Player viewer, WaystoneData waystone, boolean canEdit) {
        if (bedrockAPI.isFloodgatePlayer(viewer.getUniqueId())) {
            bedrock.showFriendsSettingsDialog(viewer, waystone, canEdit);
        } else {
            java.showFriendsSettingsDialog(viewer, waystone, canEdit);
        }
    }

    @Override
    public void showWaitingDialog(Player viewer, Component title, Function<Long, Component> text, Component cancelButton, long waitTicks, Runnable onClose, Runnable onFinish) {
        if (bedrockAPI.isFloodgatePlayer(viewer.getUniqueId())) {
            bedrock.showWaitingDialog(viewer, title, text, cancelButton, waitTicks, onClose, onFinish);
        } else {
            java.showWaitingDialog(viewer, title, text, cancelButton, waitTicks, onClose, onFinish);
        }
    }

    @Override
    public void showSortSettingsDialog(Player viewer, WaystoneData clickedWaystone) {
        if (bedrockAPI.isFloodgatePlayer(viewer.getUniqueId())) {
            bedrock.showSortSettingsDialog(viewer, clickedWaystone);
        } else {
            java.showSortSettingsDialog(viewer, clickedWaystone);
        }
    }

    @Override
    public void showWaystoneSettingsDialog(Player viewer, WaystoneData clickedWaystone) {
        if (bedrockAPI.isFloodgatePlayer(viewer.getUniqueId())) {
            bedrock.showWaystoneSettingsDialog(viewer, clickedWaystone);
        } else {
            java.showWaystoneSettingsDialog(viewer, clickedWaystone);
        }
    }

    @Override
    public void showSimpleNotice(Player viewer, Component title, Component text, Component button, Consumer<Player> action, boolean closeOnEscape) {
        if (bedrockAPI.isFloodgatePlayer(viewer.getUniqueId())) {
            bedrock.showSimpleNotice(viewer, title, text, button, action, closeOnEscape);
        } else {
            java.showSimpleNotice(viewer, title, text, button, action, closeOnEscape);
        }
    }
}
