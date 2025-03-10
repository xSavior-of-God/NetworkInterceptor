package me.lucko.networkinterceptor.loggers;

import me.lucko.networkinterceptor.InterceptEvent;
import me.lucko.networkinterceptor.common.NetworkInterceptorPlugin;

import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public abstract class AbstractEventLogger<PLUGIN> implements EventLogger<PLUGIN> {
    private final boolean includeTraces;
    // protected final Platform platform;
    private final NetworkInterceptorPlugin<PLUGIN> ni;

    protected AbstractEventLogger(boolean includeTraces, NetworkInterceptorPlugin<PLUGIN> ni) {
        this.includeTraces = includeTraces;
        // this.platform = platform;
        this.ni = ni;
    }

    protected abstract Logger getLogger();

    @Override
    public void logAttempt(InterceptEvent<PLUGIN> event) {
        String host = event.getHost();
        // prevent log if the plugin is blocked
        PLUGIN trustedPlugin = event.getTrustedPlugin();
        if(trustedPlugin != null) {
            // We don't want to log the attempt if the plugin is trusted
            return;
        }

        StringBuilder sb = new StringBuilder("Intercepted connection to ").append(host);
        String origHost = event.getOriginalHost();
        if (origHost != null) {
            sb.append(" (").append(origHost).append(")");
        }
        appendPluginIfPossible(sb, event);
        sb.append("\n");

        // print stacktrace
        if (this.includeTraces && !event.isRepeatCall()) {
            Map<StackTraceElement, PLUGIN> map = event.getNonInternalStackTraceWithPlugins();
            for (StackTraceElement element : map.keySet()) {
                sb.append("\tat ").append(element);
                PLUGIN providingPlugin = map.get(element);
                if (providingPlugin != null) {
                    String name = ni.getNIPluginManager().getName(providingPlugin);
                    sb.append(" [").append(name).append(']');
                }
                sb.append("\n");
            }
        } else if (this.includeTraces) {
            sb.append("\tat (identical stack trace omitted)\n");
        }

        sb.append("\n ").append(event.getContext().toString());
        sb.setLength(sb.length() - 1);
        getLogger().info(sb.toString());
    }

    private void appendPluginIfPossible(StringBuilder sb, InterceptEvent<PLUGIN> event) {
        PLUGIN trustedPlugin = event.getTrustedPlugin();
        PLUGIN blockedPlugin = event.getBlockedPlugin();
        PLUGIN target = null;
        if (trustedPlugin != null) {
            target = trustedPlugin;
            sb.append(" by trusted-plugin ");
        } else if (blockedPlugin != null) {
            target = blockedPlugin;
            sb.append(" by blocked-plugin ");
        } else {
            Set<PLUGIN> traced = event.getOrderedTracedPlugins();
            if (!traced.isEmpty()) {
                sb.append(" by plugin ");
                target = traced.iterator().next();
            }
        }
        if (target != null) {
            String name = ni.getNIPluginManager().getName(target);
            sb.append(name);
        }
    }

    @Override
    public void logBlock(InterceptEvent<PLUGIN> event) {
        // prevent log if the plugin is blocked
        PLUGIN blockedPlugin = event.getBlockedPlugin();
        if(blockedPlugin != null) {
            // We don't want to log the block if the plugin is blocked
            return;
        }

        StringBuilder sb = new StringBuilder("Blocked connection to ");
        sb.append(event.getHost());
        String origHost = event.getOriginalHost();
        if (origHost != null) {
            sb.append(" (").append(origHost).append(")");
        }
        appendPluginIfPossible(sb, event);
        sb.append("\n ").append(event.getContext().toString());
        getLogger().info(sb.toString());
    }
}
