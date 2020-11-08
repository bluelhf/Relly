package io.github.bluelhf.relly;

import io.github.bluelhf.relly.util.OperationResult;
import io.github.bluelhf.relly.util.RellyUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class Relly extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        OperationResult result = RellyUtil.enablePlugin(Bukkit.getWorldContainer().toPath().resolve("Test.jar"));
        getLogger().info("Loading Test.jar resulted in operation result: " + result);
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
            OperationResult unloadResult = RellyUtil.disablePlugin(Bukkit.getPluginManager().getPlugin("Vault"));
            getLogger().info("Unloading Vault resulted in operation result: " + unloadResult);
        }, 80);

        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
            OperationResult loadResult = RellyUtil.enablePlugin(Bukkit.getWorldContainer().toPath().resolve("Test.jar"));
            getLogger().info("Loading Vault resulted in operation result: " + loadResult);
        }, 280);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
