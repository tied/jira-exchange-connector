package de.equalIT.jiraExchangeConnector.impl;

import java.util.Collection;
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
import com.atlassian.jira.bc.issue.IssueService.IssueResult;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueInputParameters;
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

import de.equalIT.jiraExchangeConnector.api.MyPluginComponent;

@ExportAsService({MyPluginComponent.class})
@Named("myPluginComponent")
public class MyPluginComponentImpl implements Runnable, MyPluginComponent {
	protected static final Logger logger = LogManager.getLogger("atlassian.plugin");

	@ComponentImport
	private final ApplicationProperties applicationProperties;

	@ComponentImport
	protected PluginSettingsFactory pluginSettingsFactory;

	@ComponentImport
	protected PluginEventManager pluginEventManager;

	protected Thread checkMailThread;

	@Inject
	public MyPluginComponentImpl(final PluginEventManager pluginEventManager, final ApplicationProperties applicationProperties, final PluginSettingsFactory pluginSettingsFactory) {
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
		if (null != applicationProperties) {
			return applicationProperties.getDisplayName();
		}

		return "Jira Exchange Connector Plugin";
	}

	@Override
	public void run() {
		logger.info(getName() + "Starting.");
		try {
			while (true) {
				try {
					SettingsWrapper settingsWrapper = new SettingsWrapper(pluginSettingsFactory);

					logger.info(getName() + "Polling Exchange.");
					pollExchange(settingsWrapper);
				} catch (Exception e) {
					logger.error(getName() + "Error polling Exchange.", e);
				}
				Thread.sleep(10000);
			}
		} catch (InterruptedException e) {
			// eat
		}
		logger.info(getName() + "Stopping.");
	}

	protected void pollExchange(SettingsWrapper settingsWrapper) throws Exception {
		String server = settingsWrapper.getImapServer();//  "imap.gmx.net";//"192.168.22.3";
		String username = settingsWrapper.getImapUserName(); // "vjay@gmx.net";//"Beuth05";
		String password = settingsWrapper.getImapPassword();//"ungumeiu%57";
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

		Properties systemProperties = System.getProperties();
		systemProperties.setProperty("mail.store.protocol", "imaps");
		systemProperties.put("mail.imaps.ssl.trust", "*");
		Session session = Session.getDefaultInstance(systemProperties, null);
		Store store = session.getStore("imaps");
		try {
			logger.info("Connecting to IMAP server: " + server);
			store.connect(server, username, password);

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
				 * Now we fetch the message from the IMAP folder in descending order.
				 *
				 * This way the new mails arrive with the first chunks and older mails
				 * afterwards.
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
					long beforeTime;//= System.nanoTime();
					Message[] messages = folder.getMessagesByUID(start, end);
					totalNumberOfMessages += messages.length;
					//					logger.info("found " + messages.length + " messages (took " + (System.nanoTime() - beforeTime) / 1000 / 1000 + " ms)");

					//					for (Message message : messages) {
					//						boolean isRead = message.isSet(Flags.Flag.SEEN);
					//
					//						if (!isRead) {
					//							processMessage(message);
					//						}
					//					}

					/*
					 * If we would access e.g. the subject of a message right away
					 * it would be fetched from the IMAP server lazily.
					 *
					 * Fetching the subjects of all messages one by one would
					 * produce many requests to the IMAP server and take too
					 * much time.
					 *
					 * Instead with the following lines we load some information
					 * for all messages with one single request to save some
					 * time here.
					 */
					beforeTime = System.nanoTime();
					// this instance could be created outside the loop as well
					FetchProfile metadataProfile = new FetchProfile();
					// load flags, such as SEEN (read), ANSWERED, DELETED, ...
					metadataProfile.add(FetchProfile.Item.FLAGS);
					// also load From, To, Cc, Bcc, ReplyTo, Subject and Date
					metadataProfile.add(FetchProfile.Item.ENVELOPE);
					// we could as well load the entire messages (headers and body, including all "attachments")
					// metadataProfile.add(IMAPFolder.FetchProfileItem.MESSAGE);
					folder.fetch(messages, metadataProfile);
					//					logger.info("loaded messages (took " + (System.nanoTime() - beforeTime) / 1000 / 1000 + " ms)");

					/*
					 * Now that we have all the information we need, let's print some mails.
					 * This should be wicked fast.
					 */
					beforeTime = System.nanoTime();
					for (int i = messages.length - 1; i >= 0; i--) {
						Message message = messages[i];
						//						long uid = folder.getUID(message);
						boolean isRead = message.isSet(Flags.Flag.SEEN);
						if (!isRead) {
							processMessage(settingsWrapper, message);
						}
					}
				}
			} finally {
				if (folder.isOpen()) {
					folder.close(true);
				}
			}

			logger.info("Listed all " + totalNumberOfMessages + " messages (took " + (System.nanoTime() - afterFolderSelectionTime) / 1000 / 1000 + " ms)");
		} finally {
			store.close();
		}
	}

	protected void processMessage(SettingsWrapper settingsWrapper, Message message) throws Exception {
		Project project = ComponentAccessor.getProjectManager().getProjectByCurrentKeyIgnoreCase(settingsWrapper.getProjectName());
		if (project == null) {
			logger.error("Did not find project with the name " + settingsWrapper.getProjectName());
			for (Project project2 : ComponentAccessor.getProjectManager().getProjectObjects()) {
				logger.error(project2.getName());
			}
		} else {
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

			boolean success = false;

			IssueService issueService = ComponentAccessor.getIssueService();

			JiraAuthenticationContext jAC = ComponentAccessor.getJiraAuthenticationContext();
			jAC.setLoggedInUser(ComponentAccessor.getUserManager().getUser(settingsWrapper.getIssueOwner()));
			ApplicationUser user = jAC.getLoggedInUser();
			logger.info("Creating with user: " + user);

			//			try {
			//				IssueResult i = ComponentAccessor.getIssueService().getIssue(user, "TEST-1");
			//				System.out.println("i: " + i);
			//				System.out.println("i: " + i.getIssue());
			//				System.out.println("i issuetypeid: " + i.getIssue().getIssueTypeId());
			//				System.out.println("i assid: " + i.getIssue().getAssigneeId());
			//				System.out.println("i repid: " + i.getIssue().getReporterId());
			//				System.out.println("i projectid: " + i.getIssue().getProjectId());
			//				System.out.println("i sid: " + i.getIssue().getStatusId());
			//				System.out.println("i pid: " + i.getIssue().getPriority().getId());
			//			} catch (Exception e) {
			//			}

			Collection<IssueType> issueTypes = ComponentAccessor.getConstantsManager().getAllIssueTypeObjects();
			IssueType issueType = issueTypes.iterator().next();
			for (IssueType issueType2 : issueTypes) {
				if (settingsWrapper.getIssueType().equalsIgnoreCase(issueType2.getName())) {
					issueType = issueType2;
				}
				logger.info("Available issue type: " + issueType2.getId() + " - " + issueType2.getName() + " - " + issueType2.getDescription());
			}

			logger.info("Using issue type: " + issueType.getId() + " - " + issueType.getName() + " - " + issueType.getDescription());

			Collection<Status> statusTypes = ComponentAccessor.getConstantsManager().getStatusObjects();
			Status status = statusTypes.iterator().next();
			for (Status status2 : statusTypes) {
				if (settingsWrapper.getIssueStatus().equalsIgnoreCase(status2.getName())) {
					status = status2;
				}
				logger.info("Available status type: " + status2.getId() + " - " + status2.getName() + " - " + status2.getDescription());
			}

			logger.info("Using status type: " + status.getId() + " - " + status.getName() + " - " + status.getDescription());

			IssueInputParameters issueInputParameters = issueService.newIssueInputParameters().setProjectId(project.getId()).setSummary(subject).setDescription(bodyText).setIssueTypeId(issueType.getId()).setReporterId(user.getUsername()).setAssigneeId(user.getUsername()).setStatusId(status.getId());

			CreateValidationResult createValidationResult = issueService.validateCreate(user, issueInputParameters);
			logger.info("createValidationResult: " + createValidationResult.isValid());
			if (createValidationResult.isValid()) {
				IssueResult createResult = issueService.create(user, createValidationResult);
				logger.info("createResult: " + createResult.isValid());
				if (createResult.isValid()) {
					Issue issue = createResult.getIssue();
					logger.info("Issue created with Id: " + issue.getId());
					success = true;
				} else {
					logger.error("Creation of issue failed");
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
				logger.error("Properties: " + Joiner.on(';').withKeyValueSeparator("=").join(createValidationResult.getProperties()));
				logger.error("FieldValues: " + Joiner.on(';').withKeyValueSeparator("=").join(createValidationResult.getFieldValuesHolder()));
			}

			if (success) {
				if (settingsWrapper.isImapDeleteMessage()) {
					message.setFlag(Flags.Flag.DELETED, true);
				}
			} else {
				message.setFlag(Flags.Flag.SEEN, false);
			}

		}

	}
}