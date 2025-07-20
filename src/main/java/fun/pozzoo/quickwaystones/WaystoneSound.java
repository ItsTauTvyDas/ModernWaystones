package fun.pozzoo.quickwaystones;

public enum WaystoneSound {
    ACTIVATED("Activation"),
    DEACTIVATED("Deactivation"),
    TELEPORTED("Teleported"),
    DISALLOWED("Disallowed");

    public final String configKey;

    WaystoneSound(String configKey) {
        this.configKey = configKey;
    }
}
