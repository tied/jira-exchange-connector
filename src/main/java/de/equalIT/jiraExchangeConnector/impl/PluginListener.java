package de.equalIT.jiraExchangeConnector.impl;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.event.PluginEventListener;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.event.events.PluginDisabledEvent;
import com.atlassian.plugin.event.events.PluginEnabledEvent;
import com.atlassian.plugin.event.events.PluginFrameworkShutdownEvent;
import com.atlassian.plugin.event.events.PluginFrameworkWarmRestartingEvent;
import com.atlassian.plugin.event.events.PluginModuleDisabledEvent;
import com.atlassian.plugin.event.events.PluginRefreshedEvent;
import com.atlassian.plugin.event.events.PluginUninstalledEvent;

/**
 * Taken from https://answers.atlassian.com/questions/10694/how-to-handle-the-uninstall-event-from-within-a-plugin and
 * modified.
 * 
 * @author Volker Gronau
 * @version 1.0
 *
 */
public final class PluginListener {
	private final String myModuleKey;
	private final Runnable myStart;
	private final Runnable myStop;
	private final PluginEventManager pluginEventManager;

	private PluginListener(PluginEventManager pluginEventManager, String moduleKey, Runnable start, Runnable stop) {
		this.pluginEventManager = pluginEventManager;
		myModuleKey = moduleKey;
		myStart = start;
		myStop = stop;
	}

	public static void install(PluginEventManager pluginEventManager, String moduleKey, Runnable start, Runnable stop) {
		pluginEventManager.register(new PluginListener(pluginEventManager, moduleKey, start, stop));
	}

	public void start() {
		if (myStart != null) {
			myStart.run();
		}
	}

	public void stop() {
		try {
			pluginEventManager.unregister(this);
		} catch (Exception e) {
		}
		if (myStop != null) {
			myStop.run();
		}
	}

	@PluginEventListener
	public void onShutdown(PluginFrameworkShutdownEvent event) {
		stop();
	}

	@PluginEventListener
	public void onPluginDisabled(PluginDisabledEvent event) {
		if (event != null) {
			stopIfMe(event.getPlugin());
		}
	}

	@PluginEventListener
	public void onPluginEnabled(PluginEnabledEvent event) {
		if (event != null) {
			startIfMe(event.getPlugin());
		}
	}

	@PluginEventListener
	public void onFrameworkRestarting(PluginFrameworkWarmRestartingEvent event) {
		stop();
	}

	@PluginEventListener
	public void onModuleDisabled(PluginModuleDisabledEvent event) {
		if (event == null) {
			return;
		}

		ModuleDescriptor<?> module = event.getModule();
		if (module != null) {
			stopIfMe(module.getPlugin());
		}
	}

	@PluginEventListener
	public void onPluginUninstalledEvent(PluginUninstalledEvent event) {
		if (event != null) {
			stopIfMe(event.getPlugin());
		}
	}

	@PluginEventListener
	public void onPluginRefreshedEvent(PluginRefreshedEvent event) {
		if (event != null) {
			stopIfMe(event.getPlugin());
			startIfMe(event.getPlugin());
		}
	}

	protected void startIfMe(Plugin plugin) {
		if (plugin != null) {
			if (myModuleKey.equals(plugin.getKey())) {
				start();
			}
		}
	}

	protected void stopIfMe(Plugin plugin) {
		if (plugin != null) {
			if (myModuleKey.equals(plugin.getKey())) {
				stop();
			}
		}
	}

}
