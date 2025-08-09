package me.itstautvydas.modernwaystones;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.BlockPositionResolver;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.itstautvydas.modernwaystones.data.PlayerData;
import me.itstautvydas.modernwaystones.data.WaystoneData;
import me.itstautvydas.modernwaystones.enums.WaystoneSound;
import me.itstautvydas.modernwaystones.events.WaystoneEventsHandler;
import me.itstautvydas.modernwaystones.gui.DialogGUI;
import me.itstautvydas.modernwaystones.gui.UniDialogs;
import me.itstautvydas.modernwaystones.managers.CraftManager;
import me.itstautvydas.modernwaystones.managers.PlayerDataManager;
import me.itstautvydas.modernwaystones.managers.WaystoneDataManager;
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
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

public final class ModernWaystones extends JavaPlugin {
    private static ModernWaystones plugin;
    private static int lastWaystoneID = 0;
    private static Metrics metrics;

    public static final String LAST_USED_WAYSTONE_AT = "last_used_waystone_at";
    public static final String LAST_USED_FRIENDS_BLOCK_AT = "last_used_friends_block_at";
    public static final String LAST_WAYSTONE = "last_waystone";

    private static boolean bedrockSupported;

    private WaystoneDataManager waystoneDataManager;
    private PlayerDataManager playerDataManager;
    private CraftManager craftManager;
    private DialogGUI waystoneDialogs;
    private Material waystoneBlockType;
    private Material friendsBlockType;

    private Random random;

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void onEnable() {
        plugin = this;

        random = new Random();

        Plugin plugin = getServer().getPluginManager().getPlugin("floodgate");
        bedrockSupported = plugin != null && plugin.isEnabled();

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

//        metrics = new Metrics(this, 22064);

        waystoneBlockType = Material.valueOf(Objects.requireNonNull(getConfig().getString("Item.Material")).toUpperCase(Locale.ROOT));
        friendsBlockType = Material.valueOf(Objects.requireNonNull(getConfig().getString("Features.AddFriends.SpecialBlockMaterial")).toUpperCase(Locale.ROOT));

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("modernwaystones")
                    .requires(sender -> sender.getSender().isOp())
                    .then(Commands.literal("listdialog")
                            .then(Commands.argument("target", ArgumentTypes.player())
                                    .executes(ctx -> {
                                        final PlayerSelectorArgumentResolver resolver = ctx.getArgument("target", PlayerSelectorArgumentResolver.class);
                                        final Player target = resolver.resolve(ctx.getSource()).getFirst();
//                                        waystoneDialogs.showListDialog(target, (Player) ctx.getSource().getExecutor(), null);
                                        // TODO make a simple dialog
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
    }

    @Override
    public void onDisable() {
        if (waystoneDialogs != null) {
            for (Player player : getServer().getOnlinePlayers())
                waystoneDialogs.closeDialogIfOpened(player);
            waystoneDialogs.unregister();
        }
        if (waystoneDataManager != null)
            waystoneDataManager.saveData();
        if (metrics != null)
            metrics.shutdown();
    }

    public Random getRandom() {
        return random;
    }

    public WaystoneData getRecentWaystone(Player player) {
        List<MetadataValue> metadata = player.getMetadata(ModernWaystones.LAST_WAYSTONE);
        if (!metadata.isEmpty()) {
            String uuid = player.getMetadata(ModernWaystones.LAST_WAYSTONE).getFirst().asString();
            return getWaystoneDataManager().findById(uuid);
        }
        return null;
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
            return getPlayerDataManager().createData(player.getUniqueId(), true);
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

    public Map<String, WaystoneData> getPlayerOwnedWaystones(UUID playerUUID) {
        return getWaystonesMap().values()
                .stream()
                .filter(x -> x.isOwner(playerUUID) && !x.isInternal())
                .collect(Collectors.toMap(WaystoneData::getUniqueId, x -> x));
    }

    public DialogGUI getWaystoneDialogs() {
        return waystoneDialogs;
    }

    public void sendMessage(Player player, Component component, String configPathToCheckForActionBar) {
        if (getConfig().getBoolean(configPathToCheckForActionBar, true)) {
            player.sendActionBar(component);
            return;
        }
        player.sendMessage(component);
    }

    public boolean shouldDefaultDataBeSaved() {
        return getConfig().getBoolean("DataConfigurations.SaveDefaultData");
    }

    public static ModernWaystones getInstance() {
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
