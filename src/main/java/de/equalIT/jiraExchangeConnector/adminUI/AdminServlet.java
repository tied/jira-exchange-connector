package de.equalIT.jiraExchangeConnector.adminUI;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;

import com.atlassian.plugin.spring.scanner.annotation.component.JiraComponent;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.common.collect.Maps;
import com.google.gson.Gson;

/**
 * For the later configuration in the browser.
 * 
 * @author vjay@gmx.net
 *
 */
@SuppressWarnings("serial")
@JiraComponent
public class AdminServlet extends HttpServlet {

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
		System.out.println("GOT " + request.getRequestURI());

		String username = userManager.getRemoteUsername(request);
		if (username == null || !userManager.isSystemAdmin(username)) {
			redirectToLogin(request, response);
			return;
		}
		if (Boolean.parseBoolean(request.getParameter("config"))) {
			PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
			Map<String, Object> result = Maps.newHashMap();
			result.put("name", settings.get("Config.name"));
			result.put("time", settings.get("Config.time"));

			String resultString = new Gson().toJson(result);

			System.out.println("Sending:" + resultString);
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
		System.out.println("doPost " + request.getRequestURI());

		try (Reader reader = request.getReader();) {
			Map<String, Object> config = new Gson().fromJson(reader, HashMap.class);
			PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
			settings.put("Config.name", config.get("name"));
			settings.put("Config.time", config.get("time"));
		}
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