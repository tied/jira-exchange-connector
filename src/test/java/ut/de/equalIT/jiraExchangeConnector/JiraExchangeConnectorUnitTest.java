package ut.de.equalIT.jiraExchangeConnector;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.equalIT.jiraExchangeConnector.api.JiraExchangeConnectorPlugin;
import de.equalIT.jiraExchangeConnector.impl.JiraExchangeConnectorPluginImpl;

public class JiraExchangeConnectorUnitTest {
	@Test
	public void testMyName() {
		JiraExchangeConnectorPlugin component = new JiraExchangeConnectorPluginImpl(null, null, null);
		assertEquals("names do not match!", "Jira Exchange Connector Plugin", component.getName());
	}
}