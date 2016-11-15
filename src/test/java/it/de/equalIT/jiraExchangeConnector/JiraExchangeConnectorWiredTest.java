package it.de.equalIT.jiraExchangeConnector;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.atlassian.plugins.osgi.test.AtlassianPluginsTestRunner;
import com.atlassian.sal.api.ApplicationProperties;

import de.equalIT.jiraExchangeConnector.api.JiraExchangeConnectorPlugin;

@RunWith(AtlassianPluginsTestRunner.class)
public class JiraExchangeConnectorWiredTest {
	private final ApplicationProperties applicationProperties;
	private final JiraExchangeConnectorPlugin myPluginComponent;

	public JiraExchangeConnectorWiredTest(ApplicationProperties applicationProperties, JiraExchangeConnectorPlugin myPluginComponent) {
		this.applicationProperties = applicationProperties;
		this.myPluginComponent = myPluginComponent;
	}

	@Test
	public void testMyName() {
		assertEquals("names do not match!", "Jira Exchange Connector Plugin", myPluginComponent.getName());
	}
}