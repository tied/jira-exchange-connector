package it.de.equalIT.jiraExchangeConnector;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.atlassian.plugins.osgi.test.AtlassianPluginsTestRunner;
import com.atlassian.sal.api.ApplicationProperties;

import de.equalIT.jiraExchangeConnector.api.MyPluginComponent;

@RunWith(AtlassianPluginsTestRunner.class)
public class MyComponentWiredTest {
	private final ApplicationProperties applicationProperties;
	private final MyPluginComponent myPluginComponent;

	public MyComponentWiredTest(ApplicationProperties applicationProperties, MyPluginComponent myPluginComponent) {
		this.applicationProperties = applicationProperties;
		this.myPluginComponent = myPluginComponent;
	}

	@Test
	public void testMyName() {
		assertEquals("names do not match!", "Jira Exchange Connector Plugin", myPluginComponent.getName());
	}
}