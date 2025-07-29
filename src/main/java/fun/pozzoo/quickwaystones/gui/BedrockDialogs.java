package fun.pozzoo.quickwaystones.gui;

import fun.pozzoo.quickwaystones.QuickWaystones;
import fun.pozzoo.quickwaystones.data.WaystoneData;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.function.Consumer;
import java.util.function.Function;

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

    @Override
    public void showRenameDialog(Player player, Player viewer, WaystoneData clickedWaystone, String initialInput, boolean showNotice, boolean showError) {

    }

    @Override
    public void showSimpleNotice(Player viewer, Component title, Component text, Component button, Consumer<Player> action, boolean closeOnEscape) {

    }

    @Override
    public void showFriendsSettingsDialog(Player viewer, WaystoneData waystone, boolean canEdit) {

    }

    @Override
    public void showWaitingDialog(Player viewer, Component title, Function<Long, Component> text, Component cancelButton, long waitTicks, Runnable onClose, Runnable onFinish) {

    }

    @Override
    public void showSortSettingsDialog(Player viewer, WaystoneData clickedWaystone) {

    }
}
