package de.equalIT.jiraExchangeConnector.adminUI;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.atlassian.plugin.spring.scanner.annotation.component.JiraComponent;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.common.collect.Maps;
import com.google.gson.Gson;

import de.equalIT.jiraExchangeConnector.impl.SettingsWrapper;

/**
 * For the configuration in the browser.
 * 
 * @author vjay@gmx.net
 *
 */
@SuppressWarnings("serial")
@JiraComponent
public class AdminServlet extends HttpServlet {

	protected static final Logger logger = LogManager.getLogger("atlassian.plugin");

	protected UserManager userManager;

	protected LoginUriProvider loginUriProvider;

	protected TemplateRenderer renderer;

	protected PluginSettingsFactory pluginSettingsFactory;

	@Autowired
	public AdminServlet(@ComponentImport PluginSettingsFactory pluginSettingsFactory, @ComponentImport TemplateRenderer renderer, @ComponentImport UserManager userManager, @ComponentImport LoginUriProvider loginUriProvider) {
		this.userManager = userManager;
		this.loginUriProvider = loginUriProvider;
		this.renderer = renderer;
		this.pluginSettingsFactory = pluginSettingsFactory;
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		String username = userManager.getRemoteUsername(request);
		if (username == null || !userManager.isSystemAdmin(username)) {
			redirectToLogin(request, response);
			return;
		}
		if (Boolean.parseBoolean(request.getParameter("config"))) {
			SettingsWrapper settingsWrapper = new SettingsWrapper(pluginSettingsFactory);
			Map<String, Object> result = Maps.newHashMap();
			result.put("active", settingsWrapper.isActive());
			result.put("imapDeleteMessage", settingsWrapper.isImapDeleteMessage());
			result.put("imapServer", settingsWrapper.getImapServer());
			result.put("imapUserName", settingsWrapper.getImapUserName());
			result.put("imapPassword", settingsWrapper.getImapPassword());
			result.put("imapInboxName", settingsWrapper.getImapInboxName());
			result.put("projectName", settingsWrapper.getProjectName());
			result.put("issueOwner", settingsWrapper.getIssueOwner());
			result.put("issueStatus", settingsWrapper.getIssueStatus());
			result.put("issueType", settingsWrapper.getIssueType());

			String resultString = new Gson().toJson(result);

			logger.debug("Sending:" + resultString);
			response.setContentType("text/json;charset=utf-8");
			response.getWriter().print(resultString);
			response.getWriter().flush();
			return;
		}

		response.setContentType("text/html;charset=utf-8");
		renderer.render("admin.vm", response.getWriter());
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Map<String, Object> config = new Gson().fromJson(request.getReader(), HashMap.class);
		SettingsWrapper settingsWrapper = new SettingsWrapper(pluginSettingsFactory);
		settingsWrapper.setActive((Boolean) config.get("active"));
		settingsWrapper.setImapDeleteMessage((Boolean) config.get("imapDeleteMessage"));
		settingsWrapper.setImapServer((String) config.get("imapServer"));
		settingsWrapper.setImapUserName((String) config.get("imapUserName"));
		settingsWrapper.setImapPassword((String) config.get("imapPassword"));
		settingsWrapper.setImapInboxName((String) config.get("imapInboxName"));
		settingsWrapper.setProjectName((String) config.get("projectName"));
		settingsWrapper.setIssueOwner((String) config.get("issueOwner"));
		settingsWrapper.setIssueStatus((String) config.get("issueStatus"));
		settingsWrapper.setIssueType((String) config.get("issueType"));

		response.getWriter().print("OK");
		response.getWriter().flush();
	}

	private void redirectToLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.sendRedirect(loginUriProvider.getLoginUri(getUri(request)).toASCIIString());
	}

	private URI getUri(HttpServletRequest request) {
		StringBuffer builder = request.getRequestURL();
		if (request.getQueryString() != null) {
			builder.append("?");
			builder.append(request.getQueryString());
		}
		return URI.create(builder.toString());
	}
}