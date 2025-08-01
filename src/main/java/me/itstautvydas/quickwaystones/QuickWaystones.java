package me.itstautvydas.quickwaystones;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.itstautvydas.quickwaystones.data.PlayerData;
import me.itstautvydas.quickwaystones.data.WaystoneData;
import me.itstautvydas.quickwaystones.enums.WaystoneSound;
import me.itstautvydas.quickwaystones.events.WaystoneEventsHandler;
import me.itstautvydas.quickwaystones.gui.DialogGUI;
import me.itstautvydas.quickwaystones.gui.UniDialogs;
import me.itstautvydas.quickwaystones.managers.CraftManager;
import me.itstautvydas.quickwaystones.managers.PlayerDataManager;
import me.itstautvydas.quickwaystones.managers.WaystoneDataManager;
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

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public final class QuickWaystones extends JavaPlugin {
    private static QuickWaystones plugin;
    private static int lastWaystoneID = 0;
    private static Metrics metrics;

    private static boolean bedrockSupported;

    private WaystoneDataManager waystoneDataManager;
    private PlayerDataManager playerDataManager;
    private CraftManager craftManager;
    private DialogGUI waystoneDialogs;
    private Material waystoneBlockType;
    private Material friendsBlockType;

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void onEnable() {
        plugin = this;

        saveDefaultConfig();

        craftManager = new CraftManager(this);
        craftManager.registerRecipes();

        new WaystoneEventsHandler(this);

        try {
            waystoneDataManager = new WaystoneDataManager(this);
            waystoneDataManager.loadData();

            playerDataManager = new PlayerDataManager(this);
            playerDataManager.loadData();
        } catch (Exception e) {
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        waystoneDialogs = UniDialogs.tryCreate(this);
        waystoneDialogs.register();

        lastWaystoneID = getWaystonesMap().size();

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
                                        WaystoneData data = getWaystonesMap().get(location);
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
        if (waystoneDataManager != null)
            waystoneDataManager.saveData();
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

    public WaystoneDataManager getWaystoneDataManager() {
        return waystoneDataManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public PlayerData getPlayerData(Player player) {
        PlayerData data = getPlayerDataMap().get(player.getUniqueId());
        if (data == null)
            return getPlayerDataManager().createData(player, true);
        return data;
    }

    public Map<UUID, PlayerData> getPlayerDataMap() {
        return getPlayerDataManager().getData();
    }

    public CraftManager getCraftManager() {
        return craftManager;
    }

    public boolean isWaystoneDestroyed(Block block) {
        return isWaystoneBlock(block) && block.getType() != waystoneBlockType;
    }

    public boolean isWaystoneBlock(Block block) {
        return getWaystonesMap().containsKey(block.getLocation());
    }

    public boolean isWaystoneFriendsBlock(Block block) {
        return block.getType() == friendsBlockType && isWaystoneBlock(block.getRelative(BlockFace.UP));
    }

    public boolean isWaystoneItem(ItemStack item) {
        return item.getItemMeta().getPersistentDataContainer().has(craftManager.getPersistentWaystoneNameKey(), PersistentDataType.STRING);
    }

    public Map<Location, WaystoneData> getWaystonesMap() {
        return getWaystoneDataManager().getData();
    }

    public Map<String, WaystoneData> getSimpleWaystonesMap() {
        return getWaystonesMap().values()
                .stream()
                .collect(Collectors.toMap(WaystoneData::getUniqueId, x -> x));
    }

    public Map<String, WaystoneData> getWaystones(UUID ownerUniqueId) {
        return getWaystonesMap().values()
                .stream()
                .filter(x -> x.isOwner(ownerUniqueId) && !x.isInternal())
                .collect(Collectors.toMap(WaystoneData::getUniqueId, x -> x));
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
