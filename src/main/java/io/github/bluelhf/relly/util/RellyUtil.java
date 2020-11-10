package io.github.bluelhf.relly.util;

import com.google.common.graph.EndpointPair;
import com.google.common.graph.MutableGraph;
import io.github.bluelhf.reflectors.Reflectors;
import io.github.bluelhf.reflectors.dynamics.InstanceFieldReference;
import io.github.bluelhf.reflectors.dynamics.InstanceReference;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommandYamlParser;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.*;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

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

    /**
     * @return A list of Plugin names that depend on the input plugin.
     * @param name The name of the plugin to find the dependants of  */
    @SuppressWarnings({"UnstableApiUsage", "unchecked"})
    public static HashSet<String> findDependants(String name) {
        PluginManager pm = Bukkit.getPluginManager();
        if (!(pm instanceof SimplePluginManager)) return new HashSet<>();
        InstanceReference ref = Reflectors.reflect(pm);

        Optional<InstanceFieldReference<Object>> maybeField = ref.field("dependencyGraph");
        if (maybeField.isEmpty()) return new HashSet<>();
        MutableGraph<String> graph = (MutableGraph<String>) maybeField.get().get().getResult();
        if (graph == null) return new HashSet<>();

        HashSet<String> dependants = new HashSet<>();
        HashSet<String> targets = new HashSet<>();
        targets.add(name);

        while (targets.size() != 0) {
            Iterator<String> targetIterator = targets.iterator();
            while (targetIterator.hasNext()) {
                String target = targetIterator.next();
                HashSet<String> tempDependants = graph.edges().stream()
                    .filter(pair -> pair.target().equalsIgnoreCase(target))
                    .map(EndpointPair::source)
                    .collect(Collectors.toCollection(HashSet::new));

                targets.addAll(tempDependants);
                dependants.addAll(tempDependants);
                targetIterator.remove();
            }
        }

        return dependants;
    }

    @SuppressWarnings("ConstantConditions")
    private static OperationResult disablePluginUnsafe(Plugin plugin) {
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

        return new OperationResult(
            operations == failed ? OperationResult.State.FAIL : (failed != 0 ? OperationResult.State.PARTIAL : OperationResult.State.SUCCEED),
            message.toString()
        );
    }

    public static OperationResult disablePlugin(Plugin plugin, boolean deep) {
        if (!deep) {
            return disablePluginUnsafe(plugin);
        }
        HashSet<String> dependants = findDependants(plugin.getName());
        dependants.forEach(s -> {
            Plugin p;
            if ((p = Bukkit.getPluginManager().getPlugin(s)) != null) disablePluginUnsafe(p);
        });

        disablePluginUnsafe(plugin);

        // TODO: Do OperationResult
        return OperationResult.succeed();
    }

    public static OperationResult reloadPlugin(Plugin plugin, boolean deep) {
        if (!deep) {
            return disablePluginUnsafe(plugin);
        }
        HashSet<String> dependants = findDependants(plugin.getName());
        HashSet<Path> disabled = new HashSet<>();
        dependants.forEach(s -> {
            Plugin p;
            if ((p = Bukkit.getPluginManager().getPlugin(s)) != null) {
                try {
                    disabled.add(Paths.get(p.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()));
                    disablePluginUnsafe(p);
                } catch (URISyntaxException e) {
                    //TODO: Log to OperationResult
                }
            }

        });

        try {
            Path jarPath = Paths.get(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            disablePluginUnsafe(plugin);
            enablePlugin(jarPath);
        } catch (URISyntaxException e) {
            //TODO: Log to OperationResult
        }

        disabled.forEach(RellyUtil::enablePlugin);
        return OperationResult.succeed();
    }
}
