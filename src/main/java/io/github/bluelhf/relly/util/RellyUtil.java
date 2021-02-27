package io.github.bluelhf.relly.util;

import com.google.common.graph.EndpointPair;
import com.google.common.graph.MutableGraph;
import com.moderocky.mask.mirror.FieldMirror;
import com.moderocky.mask.mirror.Mirror;
import io.github.bluelhf.relly.Relly;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommandYamlParser;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RellyUtil {


    /**
     * @param file The file to look for a plugin in.
     * @return Whether the plugin associated with the {@link File} is loaded or not.
     * */
    public static boolean isLoaded(File file) {
        PluginLoader loader = getAssociatedLoader(file);
        if (loader == null) return false;

        try {
            PluginDescriptionFile descriptionFile = loader.getPluginDescription(file);
            if (Bukkit.getPluginManager().isPluginEnabled(descriptionFile.getName())) {
                return true;
            }
        } catch (InvalidDescriptionException e) {
            return false;
        }

        return false;
    }

    /**
     * @param file The file to find a JavaPlugin for.
     * @return The JavaPlugin associated with the {@link File}, or null if there is none.
     * */
    @Nullable
    public static JavaPlugin getJavaPlugin(File file) {
        PluginLoader loader = getAssociatedLoader(file);
        if (loader == null) return null;

        try {
            PluginDescriptionFile descriptionFile = loader.getPluginDescription(file);
            return JavaPlugin.getProvidingPlugin(Class.forName(descriptionFile.getMain()));
        } catch (InvalidDescriptionException | ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * @param file The file to find the PluginLoader for.
     * @return The PluginLoader associated with the given {@link File}, or null if none is found.
     * */
    @Nullable
    public static PluginLoader getAssociatedLoader(File file) {
        Mirror<PluginManager> pm = new Mirror<>(Bukkit.getPluginManager());
        Map<Pattern, PluginLoader> fileAssociations = (Map<Pattern, PluginLoader>) pm.field("fileAssociations", SimplePluginManager.class).get();

        PluginLoader loader = null;
        for (Map.Entry<Pattern, PluginLoader> association : fileAssociations.entrySet()) {
            Matcher matcher = association.getKey().matcher(file.getName());
            if (matcher.find()) {
                loader = association.getValue();
                break;
            }
        }
        return loader;
    }

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
            Plugin pl = getJavaPlugin(filePath.toFile());
            if (pl != null && Relly.BLACKLIST.contains(pl.getDescription().getName())) {
                return OperationResult.fail("The provided plugin is blacklisted.");
            }
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
        } catch (Throwable t) {
            return OperationResult.fail(
                    "An exception occurred while loading the plugin.",
                    t
            );
        }

        Bukkit.getPluginManager().enablePlugin(plugin);
        return OperationResult.succeed(
            "The plugin was enabled successfully."
        );
    }

    @SuppressWarnings({"ConstantConditions"})
    private static OperationResult removeCommand(Command c) {
        Mirror<PluginManager> ref = new Mirror<>(Bukkit.getPluginManager());

        FieldMirror<Object> commandMapField = ref.field("commandMap", SimplePluginManager.class);
        SimpleCommandMap commandMap = (SimpleCommandMap) commandMapField.get();
        if (commandMap == null) return OperationResult.fail("Could not find command map in plugin manager");

        if (commandMap != null) {
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
        Mirror<PluginManager> ref = new Mirror<>(pm);

        FieldMirror<Object> graphField = ref.field("dependencyGraph", SimplePluginManager.class);
        MutableGraph<String> graph = (MutableGraph<String>) graphField.get();
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

    private static OperationResult disablePluginUnsafe(Plugin plugin) {
        if (Relly.BLACKLIST.contains(plugin.getDescription().getName())) {
            return OperationResult.fail("The provided plugin is blacklisted.");
        }
        ArrayList<OperationResult> results = new ArrayList<>();
        try {
            Bukkit.getPluginManager().disablePlugin(plugin);
        } catch (Throwable t) {
            results.add(new OperationResult(OperationResult.State.FAIL, "Failed to disable plugin '" + plugin.getName() + "'", t));
        }



        for (Command c : PluginCommandYamlParser.parse(plugin)) {
            results.add(removeCommand(c));
        }

        Iterator<Recipe> iterator = Bukkit.recipeIterator();
        while (iterator.hasNext()) {
            Recipe r = iterator.next();
            if (r instanceof Keyed && ((Keyed) r).getKey().getNamespace().equalsIgnoreCase(plugin.getName())) {
                iterator.remove();
                results.add(new OperationResult(OperationResult.State.SUCCEED, "Removed recipe " + ((Keyed) r).getKey().toString()));
            }

        }

        Mirror<PluginManager> ref = new Mirror<>(Bukkit.getPluginManager());
        FieldMirror<Object> pluginsField = ref.field("plugins", SimplePluginManager.class);
        List<Plugin> plugins = (List<Plugin>) pluginsField.get();
        if (plugins != null) {
            plugins.remove(plugin);
            results.add(new OperationResult(OperationResult.State.SUCCEED, "Removed from plugins in plugin manager"));
        }

        return OperationResult.combine(results);
    }

    public static OperationResult disablePlugin(Plugin plugin, boolean deep) {
        if (Relly.BLACKLIST.contains(plugin.getDescription().getName())) {
            return OperationResult.fail("The provided plugin is blacklisted.");
        }

        if (!deep) {
            return disablePluginUnsafe(plugin);
        }

        HashSet<String> dependants = findDependants(plugin.getName());
        ArrayList<OperationResult> results = new ArrayList<>();

        dependants.forEach(s -> {
            Plugin p;
            if ((p = Bukkit.getPluginManager().getPlugin(s)) != null) {
                results.add(disablePluginUnsafe(p));
            }
        });

        results.add(disablePluginUnsafe(plugin));


        return OperationResult.combine(results);
    }

    public static OperationResult reloadPlugin(Plugin plugin, boolean deep) {
        if (Relly.BLACKLIST.contains(plugin.getDescription().getName())) {
            return OperationResult.fail("The provided plugin is blacklisted.");
        }

        ArrayList<OperationResult> results = new ArrayList<>();
        HashSet<Path> disabled = new HashSet<>();

        if (deep) {
            HashSet<String> dependants = findDependants(plugin.getName());
            dependants.forEach(s -> {
                Plugin p;
                if ((p = Bukkit.getPluginManager().getPlugin(s)) != null) {
                    try {
                        disabled.add(Paths.get(p.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()));
                        results.add(disablePluginUnsafe(p));
                    } catch (URISyntaxException e) {
                        results.add(new OperationResult(OperationResult.State.FAIL, "Failed to disable plugin " + p.getName() + ".", e));
                    }
                }
            });
        }

        try {
            Path jarPath = Paths.get(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            results.add(disablePluginUnsafe(plugin));
            results.add(enablePlugin(jarPath));
        } catch (URISyntaxException e) {
            results.add(new OperationResult(OperationResult.State.FAIL, "Failed to reload plugin " + plugin.getName() + ".", e));
        }

        if (deep) {
            for (Path p : disabled) {
                results.add(RellyUtil.enablePlugin(p));
            }
        }
        return OperationResult.combine(results);
    }
}
