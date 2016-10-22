package ut.de.equalIT.jiraExchangeConnector;

import org.junit.Test;
import de.equalIT.jiraExchangeConnector.api.MyPluginComponent;
import de.equalIT.jiraExchangeConnector.impl.MyPluginComponentImpl;

import static org.junit.Assert.assertEquals;

public class MyComponentUnitTest
{
    @Test
    public void testMyName()
    {
        MyPluginComponent component = new MyPluginComponentImpl(null);
        assertEquals("names do not match!", "myComponent",component.getName());
    }
}