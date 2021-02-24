package io.github.bluelhf.relly.command;

import dev.jorel.commandapi.annotations.Command;
import dev.jorel.commandapi.annotations.Default;
import dev.jorel.commandapi.annotations.Permission;
import dev.jorel.commandapi.annotations.Subcommand;
import dev.jorel.commandapi.annotations.arguments.ABooleanArgument;
import dev.jorel.commandapi.annotations.arguments.AStringArgument;
import io.github.bluelhf.relly.style.Colours;
import io.github.bluelhf.relly.style.Messages;
import io.github.bluelhf.relly.util.OperationResult;
import io.github.bluelhf.relly.util.RellyUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

@Command(RellyCommand.COMMAND)
@Permission(RellyCommand.PERMISSION)
public class RellyCommand {
    public static final String COMMAND = "relly";
    public static final String PERMISSION = "relly";

    @Default
    public static void relly(CommandSender sender) {
        TextComponent base = Component.text("/" + COMMAND).color(Colours.MAIN.getColour());
        TextComponent component = Component.text("Help for ").color(Colours.ACCENT_ONE.getColour())
                .append(base).append(Component.text("\n"))
                .append(base).append(Component.text(" reload").color(Colours.MAIN.getColour()));
        sender.sendMessage(component);
    }


    @Subcommand("reload")
    @Permission("relly.reload")
    public static void reload(CommandSender sender, @AStringArgument String plugin) {
        reload(sender, plugin, false);
    }

    @Subcommand("reload")
    @Permission("relly.reload")
    public static void reload(CommandSender sender, @AStringArgument String plugin, @ABooleanArgument boolean deep) {
        Plugin pluginInst = Bukkit.getPluginManager().getPlugin(plugin);
        if (pluginInst == null) {
            sender.sendMessage(Messages.NO_SUCH_PLUGIN.apply(plugin));
            return;
        }

        if (!pluginInst.isEnabled()) {
            sender.sendMessage(Messages.PLUGIN_IS_DISABLED.apply(pluginInst));
            return;
        }

        OperationResult result = RellyUtil.reloadPlugin(pluginInst, deep);
        sender.sendMessage(Messages.PLUGIN_RELOADED.apply(pluginInst, result));
    }
}
