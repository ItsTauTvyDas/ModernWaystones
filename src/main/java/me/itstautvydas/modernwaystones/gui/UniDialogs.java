package me.itstautvydas.modernwaystones.gui;

import me.itstautvydas.modernwaystones.ModernWaystones;
import me.itstautvydas.modernwaystones.data.WaystoneData;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;
import java.util.function.Consumer;

public class UniDialogs extends DialogGUI {
    private final JavaDialogs java;
    private final BedrockDialogs bedrock;
    private final FloodgateApi bedrockAPI;

    private UniDialogs(ModernWaystones plugin) {
        super(plugin);
        java = new JavaDialogs(plugin);
        bedrockAPI = FloodgateApi.getInstance();
        bedrock = new BedrockDialogs(plugin, bedrockAPI);
    }

    public static DialogGUI tryCreate(ModernWaystones plugin) {
        if (ModernWaystones.isFloodgateRunning()) {
            plugin.getLogger().info("Floodgate is detected");
            return new UniDialogs(plugin);
        }
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
        if (!bedrockAPI.isFloodgatePlayer(uuid))
            java.cleanupPlayerCache(uuid);
    }

    @Override
    public void closeDialogIfOpened(Player player) {
        if (bedrockAPI.isFloodgatePlayer(player.getUniqueId())) {
            bedrock.closeDialogIfOpened(player);
        } else {
            java.closeDialogIfOpened(player);
        }
    }

    @Override
    public void showWaystoneInaccessibleNoticeDialog(Player viewer, WaystoneData previousClickedWaystone, WaystoneData clickedWaystone, boolean actuallyDestroyed) {
        if (bedrockAPI.isFloodgatePlayer(viewer.getUniqueId())) {
            bedrock.showWaystoneInaccessibleNoticeDialog(viewer, previousClickedWaystone, clickedWaystone, actuallyDestroyed);
        } else {
            java.showWaystoneInaccessibleNoticeDialog(viewer, previousClickedWaystone, clickedWaystone, actuallyDestroyed);
        }
    }

    @Override
    public void showListDialog(Player viewer, WaystoneData clickedWaystone) {
        if (bedrockAPI.isFloodgatePlayer(viewer.getUniqueId())) {
            bedrock.showListDialog(viewer, clickedWaystone);
        } else {
            java.showListDialog(viewer, clickedWaystone);
        }
    }

    @Override
    public void showRenameDialog(Player viewer, WaystoneData clickedWaystone, String initialInput, boolean showNotice, boolean showError) {
        if (bedrockAPI.isFloodgatePlayer(viewer.getUniqueId())) {
            bedrock.showRenameDialog(viewer, clickedWaystone, initialInput, showNotice, showError);
        } else {
            java.showRenameDialog(viewer, clickedWaystone, initialInput, showNotice, showError);
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
    public void showSortSettingsDialog(Player viewer) {
        if (bedrockAPI.isFloodgatePlayer(viewer.getUniqueId())) {
            bedrock.showSortSettingsDialog(viewer);
        } else {
            java.showSortSettingsDialog(viewer);
        }
    }

    @Override
    public void showWaystonePlayerSettingsDialog(Player viewer) {
        if (bedrockAPI.isFloodgatePlayer(viewer.getUniqueId())) {
            bedrock.showWaystonePlayerSettingsDialog(viewer);
        } else {
            java.showWaystonePlayerSettingsDialog(viewer);
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
