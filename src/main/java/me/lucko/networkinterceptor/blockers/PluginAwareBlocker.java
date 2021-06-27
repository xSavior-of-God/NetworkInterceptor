package me.lucko.networkinterceptor.blockers;

import java.util.Collection;

import org.bukkit.plugin.java.JavaPlugin;

import me.lucko.networkinterceptor.InterceptEvent;
import me.lucko.networkinterceptor.plugin.PluginOptions;

public abstract class PluginAwareBlocker implements Blocker {
    private final PluginOptions options;

    public PluginAwareBlocker(PluginOptions options) {
        this.options = options;
    }

    public boolean shouldBlockProcesses(InterceptEvent event) {
        return !options.isFromTrustedProcess(event.getStackTrace(), event.getHost());
    }

    public boolean shouldBlockPlugins(InterceptEvent event) {
        Collection<JavaPlugin> plugins = event.getOrderedTracedPlugins();
        if (plugins.isEmpty()) {
            return !options.shouldAllowNonPlugin();
        }
        for (JavaPlugin plugin : plugins) { // TODO - make more versatile. Right now the FIRST untrusted plugin will
                                            // suffice
            if (options.isTrusted(plugin)) {
                return false; // trusted!
            }
        }
        return true; // not trusted -> block
    }

}
