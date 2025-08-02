package me.itstautvydas.modernwaystones.enums;

public enum WaystoneSound {
    ACTIVATED("Activation"),
    DEACTIVATED("Deactivation"),
    TELEPORTED("Teleported"),
    VISIBILITY_CHANGE_TO_PUBLIC("VisibilityChangeToPublic"),
    VISIBILITY_CHANGE_TO_PRIVATE("VisibilityChangeToPrivate"),
    RENAMED("Renamed"),
    DISALLOWED("Disallowed");

    public final String configKey;

    WaystoneSound(String configKey) {
        this.configKey = configKey;
    }
}
