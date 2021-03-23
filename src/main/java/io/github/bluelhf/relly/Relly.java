package io.github.bluelhf.relly;

import com.moderocky.mask.template.BukkitPlugin;
import dev.jorel.commandapi.CommandAPI;
import io.github.bluelhf.relly.command.RellyCommand;
import io.github.bluelhf.relly.util.FileMonitor;
import io.github.bluelhf.relly.util.RellyUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public final class Relly extends BukkitPlugin {

    public static final List<String> BLACKLIST = Arrays.asList("FastAsyncWorldEdit", "WorldEdit");

    private final HashMap<File, String> fileNameMap = new HashMap<>();

    FileMonitor pluginMonitor = new FileMonitor(getDataFolder().getParentFile(), Duration.ofSeconds(5), (file, change) -> {
        refreshMap();
        if (!file.getName().endsWith(".jar")) return;
        Bukkit.getScheduler().runTask(this, () -> {
            String name;
            Plugin plugin;
            if ((name = fileNameMap.get(file)) != null && (plugin = Bukkit.getPluginManager().getPlugin(name)) != null) {
                switch (change) {
                    case ADD:
                        if (plugin.isEnabled()) RellyUtil.disablePlugin(plugin, false);
                        RellyUtil.enablePlugin(file.toPath());
                        break;
                    case MODIFY:
                        RellyUtil.reloadPlugin(plugin, true);
                        break;
                    case REMOVE:
                        RellyUtil.disablePlugin(plugin, true);
                        break;
                }
            } else {
                switch (change) {
                    case ADD:
                    case MODIFY:
                        System.out.println(RellyUtil.enablePlugin(file.toPath()));
                        break;
                }
            }
        });
    });


    @Override
    public void onLoad() {
        CommandAPI.onLoad(false);
    }

    @Override
    public void startup() {
        pluginMonitor.start();
        CommandAPI.onEnable(this);
    }

    private void refreshMap() {
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            fileNameMap.put(RellyUtil.getPluginSource(plugin), plugin.getDescription().getName());
        }
    }

    @Override
    protected void registerCommands() {
        CommandAPI.registerCommand(RellyCommand.class);
    }

    @Override
    public void disable() {
        pluginMonitor.stop();
    }
}
