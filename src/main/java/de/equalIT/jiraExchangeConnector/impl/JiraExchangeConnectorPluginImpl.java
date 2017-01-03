package de.equalIT.jiraExchangeConnector.impl;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.IssueService.CreateValidationResult;
import com.atlassian.jira.bc.issue.IssueService.DeleteValidationResult;
import com.atlassian.jira.bc.issue.IssueService.IssueResult;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.attachment.CreateAttachmentParamsBean;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.ErrorCollection.Reason;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.sun.mail.imap.IMAPFolder;

import de.equalIT.jiraExchangeConnector.api.JiraExchangeConnectorPlugin;

/**
 * This class contains the core functionality. It initializes the plugin, polls Exchange and processes the mails.
 * 
 * @author Volker Gronau
 *
 */
@ExportAsService({JiraExchangeConnectorPlugin.class})
@Named("JiraExchangeConnectorPluginComponent")
public class JiraExchangeConnectorPluginImpl implements Runnable, JiraExchangeConnectorPlugin {

	/**
	 * We use standard Jira logging (Log4j). The advantage is, all logging is configured in Jira. The disadvantage is,
	 * all logging is configured in Jira.
	 */
	protected static final Logger logger = LogManager.getLogger("atlassian.plugin.jiraExchangeConnectorPlugin.JiraExchangeConnectorPluginImpl");

	@ComponentImport
	private final ApplicationProperties applicationProperties;

	@ComponentImport
	protected PluginSettingsFactory pluginSettingsFactory;

	@ComponentImport
	protected PluginEventManager pluginEventManager;

	protected Thread checkMailThread;

	/**
	 * The constructor is called by Jira, this is the entry point of our plugin. All the parameters are injected
	 * automatically. You can add other Jira components to it, they will be automatically injected.
	 * 
	 */
	@Inject
	public JiraExchangeConnectorPluginImpl(final PluginEventManager pluginEventManager, final ApplicationProperties applicationProperties, final PluginSettingsFactory pluginSettingsFactory) {
		this.pluginEventManager = pluginEventManager;
		this.applicationProperties = applicationProperties;
		this.pluginSettingsFactory = pluginSettingsFactory;

		logger.info("-------------------------------------------");
		logger.info(getName() + " initialized.");

		if (pluginEventManager != null) {
			logger.info("Registering for plugin events.");
			PluginListener.install(pluginEventManager, "de.equalIT.jiraExchangeConnector.jira-exchange-connector", new Runnable() {

				@Override
				public void run() {
					start();
				}
			}, new Runnable() {

				@Override
				public void run() {
					stop();
				}
			});
		}

		start();
		logger.info("-------------------------------------------");
	}

	public void start() {
		if (checkMailThread == null) {
			checkMailThread = new Thread(this);
			checkMailThread.setName("CheckMailThread");
			checkMailThread.setDaemon(false);
			checkMailThread.start();
			logger.info(getName() + " started.");
		}
	}

	public void stop() {
		if (checkMailThread != null) {
			logger.info(getName() + " stopped.");
			checkMailThread.interrupt();
			checkMailThread = null;
		}
	}

	@Override
	public String getName() {
		return "Jira Exchange Connector Plugin";
	}

	/**
	 * Runs in the context of "checkMailThread". Calls the function "pollExchange" every 10 seconds.
	 */
	@Override
	public void run() {
		logger.info(getName() + " Starting.");
		try {
			while (true) {
				try {
					SettingsWrapper settingsWrapper = new SettingsWrapper(pluginSettingsFactory);

					logger.info("Polling IMAP server.");
					pollExchange(settingsWrapper);
				} catch (Exception e) {
					logger.error("Error polling IMAP server.", e);
				}
				Thread.sleep(10000);
			}
		} catch (InterruptedException e) {
			// eat
		}
		logger.info(getName() + " Stopping.");
	}

	/**
	 * Checks the exchange server for new mails and calls the function "processMessage" for every unread mail.
	 * 
	 * @throws Exception
	 */
	protected void pollExchange(SettingsWrapper settingsWrapper) throws Exception {

		// Read all settings an throw exception if something is not configured.
		String server = settingsWrapper.getImapServer();
		String username = settingsWrapper.getImapUserName();
		String password = settingsWrapper.getImapPassword();
		String folderName = settingsWrapper.getImapInboxName();

		if (Strings.isNullOrEmpty(server)) {
			throw new Exception("IMAP server not configured.");
		}
		if (Strings.isNullOrEmpty(username)) {
			throw new Exception("User name not configured.");
		}
		if (Strings.isNullOrEmpty(password)) {
			throw new Exception("Password not configured.");
		}
		if (Strings.isNullOrEmpty(folderName)) {
			throw new Exception("Inbox name not configured.");
		}

		// Initialize javax.mail
		Properties properties = new Properties();
		properties.putAll(System.getProperties());
		properties.setProperty("mail.store.protocol", "imaps"); // we want to use imaps(ecure)
		properties.put("mail.imaps.ssl.trust", "*"); // We allow self signed certificates

		Session session = Session.getDefaultInstance(properties, null);
		Store store = session.getStore("imaps");
		try {
			logger.info("Connecting to IMAP server: " + server);
			store.connect(server, username, password);

			// we open the configured inbox folder
			IMAPFolder folder = (IMAPFolder) store.getFolder(folderName);
			if (folder == null) {
				throw new Exception("Did not find folder " + folderName);
			}

			long afterFolderSelectionTime = System.nanoTime();
			int totalNumberOfMessages = 0;
			try {
				if (!folder.isOpen()) {
					folder.open(Folder.READ_WRITE);
				}

				/*
				 * Now we fetch the messages from the IMAP folder .
				 */
				long largestUid = folder.getUIDNext() - 1;
				int chunkSize = 500;
				for (long offset = 0; offset < largestUid; offset += chunkSize) {
					long start = Math.max(1, largestUid - offset - chunkSize + 1);
					long end = Math.max(1, largestUid - offset);

					/*
					 * The next line fetches the existing messages within the
					 * given range from the server.
					 *
					 * The messages are not loaded entirely and contain hardly
					 * any information. The Message-instances are mostly empty.
					 */

					Message[] messages = folder.getMessagesByUID(start, end);
					totalNumberOfMessages += messages.length;

					/*
					 * If we would access e.g. the subject of a message right away
					 * it would be fetched from the IMAP server lazily.
					 *
					 * Fetching the flags of all messages one by one would
					 * produce many requests to the IMAP server and take too
					 * much time.
					 *
					 * Instead with the following lines we load that information
					 * for all messages with one single request to save some
					 * time here.
					 */

					// load flags, such as SEEN (read), ANSWERED, DELETED, ...
					FetchProfile metadataProfile = new FetchProfile();
					metadataProfile.add(FetchProfile.Item.FLAGS);
					folder.fetch(messages, metadataProfile);

					/*
					 * Now that we have all the information we need, we can iterate over them very fast.
					 */
					for (int i = messages.length - 1; i >= 0; i--) {
						Message message = messages[i];
						boolean isRead = message.isSet(Flags.Flag.SEEN);
						if (!isRead) {
							try {
								processMessage(settingsWrapper, message);
							} catch (Exception e) {
								logger.error("Error processing message " + message, e);
								message.setFlag(Flags.Flag.SEEN, false); // set it as unseen to be processed again
							}
						}
					}
				}
			} finally {
				if (folder.isOpen()) {
					folder.close(true);
				}
			}

			logger.info("Processing " + totalNumberOfMessages + " messages (took " + (System.nanoTime() - afterFolderSelectionTime) / 1000 / 1000 + " ms)");
		} finally {
			store.close();
		}
	}

	/**
	 * This function creates a Jira issue of a message(email).
	 * 
	 * @throws Exception
	 */
	protected void processMessage(SettingsWrapper settingsWrapper, Message message) throws Exception {

		// Retrieving and checking the Jira issue service.

		IssueService issueService = ComponentAccessor.getIssueService();
		if (issueService == null) {
			throw new Exception("Could not retrieve IssueService.");
		}

		// Retrieving and checking the project in which the issue should be created.

		Project project = ComponentAccessor.getProjectManager().getProjectByCurrentKeyIgnoreCase(settingsWrapper.getProjectName());
		if (project == null) {
			StringBuilder projectNames = new StringBuilder();
			for (Project project2 : ComponentAccessor.getProjectManager().getProjectObjects()) {
				if (projectNames.length() > 0) {
					projectNames.append(", ");
				}
				projectNames.append(project2.getName());
			}
			throw new Exception("Did not find project with the name: " + settingsWrapper.getProjectName() + ", available names are: " + projectNames);
		}

		// Retrieving and checking the user to create the tickets with.

		JiraAuthenticationContext jiraAuthenticationContext = ComponentAccessor.getJiraAuthenticationContext();
		if (jiraAuthenticationContext == null) {
			throw new Exception("Could not retrieve JiraAuthenticationContext.");
		}
		jiraAuthenticationContext.setLoggedInUser(ComponentAccessor.getUserManager().getUser(settingsWrapper.getIssueOwner()));
		ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();
		if (user == null) {
			throw new Exception("Could not login user " + settingsWrapper.getIssueOwner() + ".");
		}
		logger.info("Creating issue with user: " + user);

		// Retrieving and checking the configured issue type.

		Collection<IssueType> issueTypes = ComponentAccessor.getConstantsManager().getAllIssueTypeObjects();
		IssueType issueType = issueTypes.iterator().next();
		for (IssueType issueType2 : issueTypes) {
			if (settingsWrapper.getIssueType().equalsIgnoreCase(issueType2.getName())) {
				issueType = issueType2;
			}
			logger.info("Available issue type: " + issueType2.getId() + " - " + issueType2.getName() + " - " + issueType2.getDescription());
		}
		if (issueType == null) {
			throw new Exception("Error, issue type " + settingsWrapper.getIssueType() + " does not exist.");
		}
		logger.info("Using issue type: " + issueType.getId() + " - " + issueType.getName() + " - " + issueType.getDescription());

		// Retrieving and checking the configured status type

		Collection<Status> statusTypes = ComponentAccessor.getConstantsManager().getStatusObjects();
		Status status = statusTypes.iterator().next();
		for (Status status2 : statusTypes) {
			if (settingsWrapper.getIssueStatus().equalsIgnoreCase(status2.getName())) {
				status = status2;
			}
			logger.info("Available status type: " + status2.getId() + " - " + status2.getName() + " - " + status2.getDescription());
		}
		if (status == null) {
			throw new Exception("Error, status type " + settingsWrapper.getIssueStatus() + " does not exist.");
		}
		logger.info("Using status type: " + status.getId() + " - " + status.getName() + " - " + status.getDescription());

		// Processing the message.

		MessageWrapper messageWrapper = new MessageWrapper(message);
		String subject = message.getSubject();
		if (Strings.isNullOrEmpty(subject)) {
			subject = "Mail had empty subject.";
		}
		String bodyText = messageWrapper.getBodyText();
		if (Strings.isNullOrEmpty(bodyText)) {
			bodyText = "Mail had empty body.";
		}
		logger.info("Processing message: " + subject + " from: " + Joiner.on(',').join(message.getFrom()));
		logger.info("Body: " + bodyText);

		// Validating the issue.

		CreateValidationResult createValidationResult = issueService.validateCreate(user, issueService.newIssueInputParameters().setProjectId(project.getId()).setSummary(subject).setDescription(bodyText).setIssueTypeId(issueType.getId()).setReporterId(user.getUsername()).setAssigneeId(user.getUsername()).setStatusId(status.getId()));
		logger.info("createValidationResult: " + createValidationResult.isValid());
		if (createValidationResult.isValid()) {

			// Creating the issue.
			IssueResult createResult = issueService.create(user, createValidationResult);
			logger.info("createResult: " + createResult.isValid());
			if (createResult.isValid()) {
				Issue issue = createResult.getIssue();
				logger.info("Issue created with Id: " + issue.getId());

				try {

					// Attaching files if necessary.
					Map<File, String> attachments = messageWrapper.getAttachments();
					try {
						for (Entry<File, String> attachment : attachments.entrySet()) {
							logger.info("Adding attachment: " + attachment.getValue());
							ComponentAccessor.getAttachmentManager().createAttachment(new CreateAttachmentParamsBean.Builder(attachment.getKey(), attachment.getValue(), "application/octet-stream", user, issue).build());
						}
					} finally {
						// Delete the temporary files
						for (File file : attachments.keySet()) {
							file.delete();
						}
					}

					// Success, we remove the message if configured to do so.
					if (settingsWrapper.isImapDeleteMessage()) {
						message.setFlag(Flags.Flag.DELETED, true);
					}

					// Done.

				} catch (Exception e) {
					logger.error("Error adding attachment, trying to removing issue again.", e);
					try {
						DeleteValidationResult deleteValidationResult = issueService.validateDelete(user, issue.getId());
						issueService.delete(user, deleteValidationResult);
					} catch (Exception e2) {
						logger.error("Remove failed.", e2);
					}
					throw e;
				}

			} else {
				logger.error("Creation of issue failed.");
				for (Reason reason : createResult.getErrorCollection().getReasons()) {
					logger.error("Reason: " + reason);
				}
				if (createResult.hasWarnings()) {
					for (String reason : createResult.getWarningCollection().getWarnings()) {
						logger.error("Warning: " + reason);
					}
				}
				for (String error : createResult.getErrorCollection().getErrorMessages()) {
					logger.error("Error: " + error);
				}
				throw new Exception("Creation of issue failed.");
			}
		} else {
			logger.error("Validation of issue failed");
			for (Reason reason : createValidationResult.getErrorCollection().getReasons()) {
				logger.error("Reason: " + reason);
			}
			if (createValidationResult.hasWarnings()) {
				for (String reason : createValidationResult.getWarningCollection().getWarnings()) {
					logger.error("Warning: " + reason);
				}
			}
			for (String error : createValidationResult.getErrorCollection().getErrorMessages()) {
				logger.error("Error: " + error);
			}
			throw new Exception("Validation of issue failed.");
		}

	}
}