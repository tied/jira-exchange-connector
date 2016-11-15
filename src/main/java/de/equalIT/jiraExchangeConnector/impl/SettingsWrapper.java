package de.equalIT.jiraExchangeConnector.impl;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

public class SettingsWrapper {
	private static final String PLUGIN_STORAGE_KEY = "de.equalIT.jiraExchangeConnector.";

	protected PluginSettings pluginSettings;

	public SettingsWrapper(PluginSettingsFactory pluginSettingsFactory) {
		super();
		this.pluginSettings = pluginSettingsFactory.createSettingsForKey(PLUGIN_STORAGE_KEY);
	}

	protected String getString(String key, String defaultValue) {
		Object result = pluginSettings.get(key);
		if (result instanceof String) {
			return (String) result;
		}
		return defaultValue;
	}

	protected boolean getBoolean(String key, boolean defaultValue) {
		Object result = pluginSettings.get(key);
		if (result instanceof String) {
			return Boolean.parseBoolean((String) result);
		}
		return defaultValue;
	}

	protected void set(String key, Object value) {
		if (value != null) {
			pluginSettings.put(key, value.toString());
		} else {
			pluginSettings.remove(key);
		}
	}

	//	public boolean isActive() {
	//		return getBoolean("active", false);
	//	}
	//
	//	public void setActive(boolean active) {
	//		set("active", active);
	//	}

	public boolean isImapDeleteMessage() {
		return getBoolean("imapDeleteMessage", false);
	}

	public void setImapDeleteMessage(boolean active) {
		set("imapDeleteMessage", active);
	}

	public String getImapServer() {
		return getString("imapServer", "");
	}

	public void setImapServer(String value) {
		set("imapServer", value);
	}

	public String getImapUserName() {
		return getString("imapUserName", "");
	}

	public void setImapUserName(String value) {
		set("imapUserName", value);
	}
	public String getImapPassword() {
		return getString("imapPassword", "");
	}

	public void setImapPassword(String value) {
		set("imapPassword", value);
	}

	public String getImapInboxName() {
		return getString("imapInbox", "INBOX");
	}

	public void setImapInboxName(String value) {
		set("imapInbox", value);
	}

	public String getProjectName() {
		return getString("projectName", "Test");
	}

	public void setProjectName(String value) {
		set("projectName", value);
	}

	public String getIssueOwner() {
		return getString("issueOwner", "admin");
	}

	public void setIssueOwner(String value) {
		set("issueOwner", value);
	}

	public String getIssueType() {
		return getString("issueType", "Aufgabe");
	}

	public void setIssueType(String value) {
		set("issueType", value);
	}

	public String getIssueStatus() {
		return getString("issueStatus", "Open");
	}

	public void setIssueStatus(String value) {
		set("issueStatus", value);
	}
}
