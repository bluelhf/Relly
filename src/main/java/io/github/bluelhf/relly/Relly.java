package io.github.bluelhf.relly;

import com.moderocky.mask.template.BukkitPlugin;
import dev.jorel.commandapi.CommandAPI;
import io.github.bluelhf.relly.command.RellyCommand;
import io.github.bluelhf.relly.util.FileMonitor;
import io.github.bluelhf.relly.util.RellyUtil;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;

public final class Relly extends BukkitPlugin {

    FileMonitor pluginMonitor = new FileMonitor(getDataFolder().getParentFile(), Duration.ofSeconds(5), (file, change) -> {

        if (!file.getName().endsWith(".jar")) return;
        if (RellyUtil.isLoaded(file)) {
            JavaPlugin plugin;
            if ((plugin = RellyUtil.getJavaPlugin(file)) == null) return;
            switch (change) {
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
                    RellyUtil.enablePlugin(file.toPath());
                    break;
            }
        }
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

    @Override
    protected void registerCommands() {
        CommandAPI.registerCommand(RellyCommand.class);
    }

    @Override
    public void disable() {
        pluginMonitor.stop();
    }
}
