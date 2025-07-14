package fun.pozzoo.quickwaystones;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.List;

public class Utils {
    public static Component formatString(String string) {
        return MiniMessage.miniMessage().deserialize(string);
    }

    public static List<Component> formatStringList(List<String> strings, boolean isLore) {
        List<Component> components = new ArrayList<>();
        for (String string : strings) {
            Component component = formatString(string);
            if (isLore)
                component = component.decoration(TextDecoration.ITALIC, false).color(NamedTextColor.GRAY);
            components.add(component);
        }
        return components;
    }

    public static Component formatItemName(String itemName) {
        return MiniMessage.miniMessage().deserialize(itemName).decoration(TextDecoration.ITALIC, false);
    }
}