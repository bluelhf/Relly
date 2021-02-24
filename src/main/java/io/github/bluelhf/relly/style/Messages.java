package io.github.bluelhf.relly.style;

import io.github.bluelhf.relly.util.OperationResult;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.plugin.Plugin;

import java.util.function.BiFunction;
import java.util.function.Function;

public class Messages {
    public static final Function<String, TextComponent> NO_SUCH_PLUGIN = (plugin) ->
            Component.text("A plugin by the name of '").color(Colours.ACCENT_TWO.getColour())
                    .append(Component.text(plugin).color(Colours.MAIN.getColour()))
                    .append(Component.text("' doesn't exist!").color(Colours.ACCENT_TWO.getColour()));

    public static final Function<Plugin, TextComponent> PLUGIN_IS_DISABLED = (plugin) ->
            Component.text("The plugin '").color(Colours.ACCENT_TWO.getColour())
                    .append(Component.text(plugin.getName()).color(Colours.MAIN.getColour()))
                    .append(Component.text("' is disabled!").color(Colours.ACCENT_TWO.getColour()))
                    .append(Component.text("\n(Did you mean to use ").color(Colours.ACCENT_TWO.comment().getColour()))
                    .append(Component.text("/relly load " + plugin.getName().toLowerCase()).color(Colours.MAIN.comment().getColour()))
                    .append(Component.text("?)").color(Colours.ACCENT_TWO.comment().getColour()));

    public static final BiFunction<Plugin, OperationResult, TextComponent> PLUGIN_RELOADED = (plugin, result) -> {
        if (result.getState() == OperationResult.State.SUCCEED) {
            return Component.text(plugin.getName()).color(Colours.ACCENT_ONE.getColour())
                    .append(Component.text(" was reloaded successfully!").color(Colours.MAIN.getColour()));
        }

        return Component.text("Reloading the plugin failed ").color(Colours.ACCENT_TWO.getColour())
                .append(Component.text(result.getState() == OperationResult.State.PARTIAL ? "partially" : "completely").color(Colours.MAIN.getColour()))
                .append(Component.text("\n"))
                .append(Component.text("(Hover over to see details)").color(Colours.ACCENT_ONE.comment().getColour()))
                .hoverEvent(Component.text(result.getMessage() != null ? result.getMessage() : "No message provided.").color(Colours.ACCENT_TWO.getColour()).asHoverEvent());
    };

}
