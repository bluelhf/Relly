package io.github.bluelhf.relly.util;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.nio.file.Path;

public class OperationOld {
    private final OperationResult result;
    private final Type type;
    private Plugin targetPlugin;
    private File targetFile;

    public OperationOld(Plugin target, Type type, OperationResult result) {
        this.targetPlugin = target;
        this.targetFile = RellyUtil.getPluginSource(target);
        this.type = type;
        this.result = result;
    }

    public OperationOld(File targetFile, Type type, OperationResult result) {
        this.targetPlugin = RellyUtil.getJavaPlugin(targetFile);
        this.targetFile = targetFile;
        this.type = type;
        this.result = result;
    }

    public static OperationOld enable(Path target, OperationResult result) {
        return new OperationOld(target.toFile(), Type.ENABLE, result);
    }

    public static OperationOld disable(Plugin target, OperationResult result) {
        return new OperationOld(target, Type.DISABLE, result);
    }

    public static OperationOld reload(Plugin target, OperationResult result) {
        return new OperationOld(target, Type.RELOAD, result);
    }

    public OperationResult getResult() {
        return result;
    }

    public Type getType() {
        return type;
    }

    public Plugin getPlugin() {
        return targetPlugin;
    }

    public File getFile() {
        return targetFile;
    }


    enum Type {
        ENABLE("enabled"),
        DISABLE("disabled"),
        RELOAD("reloaded");

        private final String verb;

        Type(String verb) {
            this.verb = verb;
        }

        public String getVerb() {
            return verb;
        }
    }

}
