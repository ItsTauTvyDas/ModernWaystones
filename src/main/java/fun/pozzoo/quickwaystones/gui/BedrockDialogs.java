package fun.pozzoo.quickwaystones.gui;

import fun.pozzoo.quickwaystones.QuickWaystones;
import fun.pozzoo.quickwaystones.data.WaystoneData;
import org.bukkit.entity.Player;

public class BedrockDialogs extends DialogGUI {
    public BedrockDialogs(QuickWaystones plugin) {
        super(plugin);
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
    public void showWaystoneDestroyedNoticeDialog(Player player, Player viewer, WaystoneData previousClickedWaystone, WaystoneData clickedWaystone) {

    }

    @Override
    public void showListDialog(Player player, Player viewer, WaystoneData clickedWaystone) {

    }
}
