package fun.pozzoo.quickwaystones.data;

import fun.pozzoo.quickwaystones.QuickWaystones;
import org.bukkit.Location;

import java.util.UUID;

public class WaystoneData {
    private String name;

    private final String id;
    private final String owner;
    private final Location location;

    public WaystoneData(Location location, String owner) {
        id = UUID.randomUUID().toString();
        name = "Waystone " + QuickWaystones.getAndIncrementLastWaystoneID();
        this.location = location;
        this.owner = owner;
    }

    public WaystoneData(String id, String name, Location location, String owner) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.owner = owner;
    }

    public String getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public Location getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return "WaystoneData{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", location=" + location +
                ", owner='" + owner + '\'' +
                '}';
    }
}
