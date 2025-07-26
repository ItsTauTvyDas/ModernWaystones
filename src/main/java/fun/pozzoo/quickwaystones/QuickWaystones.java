package fun.pozzoo.quickwaystones;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fun.pozzoo.quickwaystones.data.WaystoneData;
import fun.pozzoo.quickwaystones.events.WaystoneEventsHandler;
import fun.pozzoo.quickwaystones.gui.DialogGUI;
import fun.pozzoo.quickwaystones.gui.UniDialogs;
import fun.pozzoo.quickwaystones.managers.CraftManager;
import fun.pozzoo.quickwaystones.managers.DataManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.BlockPositionResolver;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class QuickWaystones extends JavaPlugin {
    private static QuickWaystones plugin;
    private static int lastWaystoneID = 0;
    private static Metrics metrics;

    private static boolean bedrockSupported;

    private DataManager dataManager;
    private CraftManager craftManager;
    private DialogGUI waystoneDialogs;
    private Material waystoneBlockType;
    private Material friendsBlockType;
    private Map<Location, WaystoneData> waystonesMap;

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void onEnable() {
        plugin = this;

        saveDefaultConfig();

        craftManager = new CraftManager(this);
        craftManager.registerRecipes();

        new WaystoneEventsHandler(this);

        waystonesMap = new HashMap<>();

        dataManager = new DataManager(this);
        dataManager.loadWaystonesData();

        waystoneDialogs = UniDialogs.tryCreate(this);
        waystoneDialogs.register();

        lastWaystoneID = waystonesMap.size();

        metrics = new Metrics(this, 22064);

        waystoneBlockType = Material.valueOf(Objects.requireNonNull(getConfig().getString("Item.Material")).toUpperCase(Locale.ROOT));
        friendsBlockType = Material.valueOf(Objects.requireNonNull(getConfig().getString("Features.AddFriends.SpecialBlockMaterial")).toUpperCase(Locale.ROOT));

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("quickwaystones")
                    .requires(sender -> sender.getSender().isOp())
                    .then(Commands.literal("listdialog")
                            .then(Commands.argument("target", ArgumentTypes.player())
                                    .executes(ctx -> {
                                        final PlayerSelectorArgumentResolver resolver = ctx.getArgument("target", PlayerSelectorArgumentResolver.class);
                                        final Player target = resolver.resolve(ctx.getSource()).getFirst();
                                        waystoneDialogs.showListDialog(target, (Player) ctx.getSource().getExecutor(), null);
                                        return Command.SINGLE_SUCCESS;
                                    })))
                    .then(Commands.literal("check")
                            .then(Commands.argument("location", ArgumentTypes.blockPosition())
                                    .executes(ctx -> {
                                        final BlockPositionResolver resolver = ctx.getArgument("location", BlockPositionResolver.class);
                                        final Location location = resolver.resolve(ctx.getSource()).toLocation(ctx.getSource().getLocation().getWorld());
                                        WaystoneData data = waystonesMap.get(location);
                                        if (data == null)
                                            ctx.getSource().getSender().sendMessage(Component.text("No data in this location").color(NamedTextColor.RED));
                                        else
                                            ctx.getSource().getSender().sendMessage(data.toString());
                                        return Command.SINGLE_SUCCESS;
                                    })));
            commands.registrar().register(command.build());
        });

        bedrockSupported = getServer().getPluginManager().getPlugin("floodgate") != null;
    }

    @Override
    public void onDisable() {
        if (dataManager != null)
            dataManager.saveWaystoneData();
        if (waystoneDialogs != null)
            waystoneDialogs.unregister();
        if (metrics != null)
            metrics.shutdown();
    }

    @SuppressWarnings("PatternValidation")
    public void playWaystoneSound(Player player, Location location, WaystoneSound sound) {
        String type = sound.configKey;
        boolean status = getConfig().getBoolean("Sounds." + type + ".Enabled", false);
        if (!status)
            return;
        String id = getConfig().getString("Sounds." + type + ".Key");
        if (id == null)
            return;
        double volume = Math.clamp(getConfig().getDouble("Sounds." + type + ".Volume", 1), 0, 1);
        double pitch = Math.clamp(getConfig().getDouble("Sounds." + type + ".Pitch", 1), 0, 2);

        (player == null ? getServer() : player).playSound(
                Sound.sound(Key.key(id), Sound.Source.BLOCK, (float) pitch, (float) volume),
                location.x(), location.y(), location.z()
        );
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public CraftManager getCraftManager() {
        return craftManager;
    }

    public boolean isWaystoneDestroyed(Block block) {
        return isWaystoneBlock(block) && block.getType() != waystoneBlockType;
    }

    public boolean isWaystoneBlock(Block block) {
        return waystonesMap.containsKey(block.getLocation());
    }

    public boolean isWaystoneFriendsBlock(Block block) {
        return block.getType() == friendsBlockType && isWaystoneBlock(block.getRelative(BlockFace.UP));
    }

    public boolean isWaystoneItem(ItemStack item) {
        return item.getItemMeta().getPersistentDataContainer().has(craftManager.getPersistentWaystoneNameKey(), PersistentDataType.STRING);
    }

    public Map<Location, WaystoneData> getWaystonesMap() {
        return waystonesMap;
    }

    public List<WaystoneData> getWaystones(UUID ownerUniqueId) {
        return getWaystonesMap().values().stream().filter(x -> x.isOwner(ownerUniqueId)).toList();
    }

    public DialogGUI getWaystoneDialogs() {
        return waystoneDialogs;
    }

    public static QuickWaystones getInstance() {
        return plugin;
    }

    public static int getAndIncrementLastWaystoneID() {
        lastWaystoneID++;
        return lastWaystoneID;
    }

    public static String multiMessage(String path) {
        path = "Messages." + path;
        if (config().isList(path))
            return String.join("\n<reset>", config().getStringList(path));
        return config().getString(path, "<message_not_found>");
    }

    public static Component message(String path) {
        return Utils.formatString(multiMessage(path));
    }

    public static Component message(String path, Map<String, String> placeholders) {
        return rawMessage(multiMessage(path), placeholders);
    }

    public static Component rawMessage(String message, Map<String, String> placeholders) {
        for (Map.Entry<String, String> entry : placeholders.entrySet())
            message = message.replace("{" + entry.getKey() + "}", entry.getValue() == null ? "<null_error>" : entry.getValue());
        return Utils.formatString(message);
    }

    public static FileConfiguration config() {
        return getInstance().getConfig();
    }

    public static boolean isFloodgateRunning() {
        return bedrockSupported;
    }
}
