package ut.de.equalIT.jiraExchangeConnector;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.equalIT.jiraExchangeConnector.api.MyPluginComponent;
import de.equalIT.jiraExchangeConnector.impl.MyPluginComponentImpl;

public class MyComponentUnitTest {
	@Test
	public void testMyName() {
		MyPluginComponent component = new MyPluginComponentImpl(null, null, null);
		assertEquals("names do not match!", "Jira Exchange Connector Plugin", component.getName());
	}
}