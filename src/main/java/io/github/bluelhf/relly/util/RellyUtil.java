package io.github.bluelhf.relly.util;

import io.github.bluelhf.reflectors.Reflectors;
import io.github.bluelhf.reflectors.dynamics.InstanceFieldReference;
import io.github.bluelhf.reflectors.dynamics.InstanceReference;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommandYamlParser;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

public class RellyUtil {

    /**
     * Loads and enables a plugin from a {@link Path} to the jarfile.
     * @return An {@link OperationResult} depicting the result of the plugin load.
     * @param filePath The path to load the plugin from.
     * */
    public static OperationResult enablePlugin(Path filePath) {
        if (!filePath.toFile().exists()) {
            return OperationResult.fail(
                "The provided file could not be found."
            );
        }

        if (filePath.toFile().isDirectory()) {
            return OperationResult.fail(
                "The provided file is a directory."
            );
        }

        Plugin plugin;

        try {
            plugin = Bukkit.getPluginManager().loadPlugin(filePath.toFile());
            if (plugin == null) throw new InvalidPluginException("Invalid plugin");
            plugin.onLoad();
        } catch (InvalidPluginException exception) {
            return OperationResult.fail(
                "The provided file is not a valid plugin.",
                exception
            );
        } catch (InvalidDescriptionException exception) {
            return OperationResult.fail(
                "The provided file has an invalid plugin description.",
                exception
            );
        }

        Bukkit.getPluginManager().enablePlugin(plugin);
        return OperationResult.succeed(
            "The plugin was enabled successfully."
        );
    }

    @SuppressWarnings({"ConstantConditions"})
    private static OperationResult removeCommand(Command c) {
        InstanceReference ref = Reflectors.reflect(Bukkit.getPluginManager());

        Optional<InstanceFieldReference<Object>> maybeIfr = ref.field("commandMap");
        if (maybeIfr.isEmpty()) return OperationResult.fail("Could not find command map in plugin manager");
        InstanceFieldReference<Object> ifr = maybeIfr.get();

        if (ifr.get().hasResult()) {
            SimpleCommandMap commandMap = (SimpleCommandMap) ifr.get().getResult();
            commandMap.getKnownCommands().remove(c.getName());
        }
        Bukkit.getOnlinePlayers().forEach(Player::updateCommands);
        return OperationResult.succeed("Successfully removed command.");
    }

    @SuppressWarnings("ConstantConditions")
    public static OperationResult disablePlugin(Plugin plugin) {
        Bukkit.getPluginManager().disablePlugin(plugin);

        StringBuilder message = new StringBuilder();

        int operations = 0;
        int failed = 0;

        for (Command c : PluginCommandYamlParser.parse(plugin)) {
            operations++;
            OperationResult result = removeCommand(c);
            if (result.getState() == OperationResult.State.FAIL) {
                failed++;
                message.append("Failed to remove command ").append(c.getName()).append(": ").append(result.toString()).append("\n");
            }
        }

        Iterator<Recipe> iterator = Bukkit.recipeIterator();
        while (iterator.hasNext()) {
            Recipe r = iterator.next();
            if (r instanceof Keyed && ((Keyed) r).getKey().getNamespace().equalsIgnoreCase(plugin.getName())) {
                iterator.remove();
                message.append("Removed recipe ").append(((Keyed) r).getKey().toString()).append("\n");
            }

        }

        InstanceReference ref = Reflectors.reflect(Bukkit.getPluginManager());
        ref.field("plugins").ifPresent(ifr -> {
            if (ifr.get().hasResult()) {
                InstanceReference pluginsRef = Reflectors.reflect(ifr.get().getResult());
                pluginsRef.method("remove").ifPresent(imr -> imr.invoke(plugin));
                message.append("Removed from plugins in plugin manager");
            }
        });

        ref.field("fileAssociations").ifPresent(ifr -> {
            if (ifr.get().hasResult()) {
                InstanceReference fileAssociationsRef = Reflectors.reflect(ifr.get().getResult());
                //TODO: Do something with this
            }
        });

        return new OperationResult(
            operations == failed ? OperationResult.State.FAIL : (failed != 0 ? OperationResult.State.PARTIAL : OperationResult.State.SUCCEED),
            message.toString()
        );
    }
}
