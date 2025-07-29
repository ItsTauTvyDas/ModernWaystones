package fun.pozzoo.quickwaystones.managers;

import fun.pozzoo.quickwaystones.QuickWaystones;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class PlayerDataManager extends DataManagerBase {
    public PlayerDataManager(QuickWaystones plugin) throws IOException {
        super(plugin, "players");
    }

    public void loadData() {

    }

    public void saveData() {

    }
}
