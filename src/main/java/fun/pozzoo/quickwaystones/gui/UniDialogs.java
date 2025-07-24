package fun.pozzoo.quickwaystones.gui;

import fun.pozzoo.quickwaystones.QuickWaystones;
import fun.pozzoo.quickwaystones.data.WaystoneData;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;

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
}
