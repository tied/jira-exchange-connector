package de.equalIT.jiraExchangeConnector.impl;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.IssueService.CreateValidationResult;
import com.atlassian.jira.bc.issue.IssueService.IssueResult;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.ErrorCollection.Reason;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.ApplicationProperties;
import de.equalIT.jiraExchangeConnector.api.MyPluginComponent;

import java.util.Timer;
import java.util.TimerTask;

import javax.inject.Inject;
import javax.inject.Named;

@ExportAsService({MyPluginComponent.class})
@Named("myPluginComponent")
public class MyPluginComponentImpl implements MyPluginComponent {
	@ComponentImport
	private final ApplicationProperties applicationProperties;

	@Inject
	public MyPluginComponentImpl(final ApplicationProperties applicationProperties) {
		this.applicationProperties = applicationProperties;

		// This creates an issue, for copy & paste later
		//        Timer timer = new Timer();
		//		timer.schedule(new TimerTask() {
		//
		//			@Override
		//			public void run() {
		//				System.out.println("YOOYOYOYOYOYO YOYOYOO");
		//				System.out.println("YOOYOYOYOYOYO YOYOYOO");
		//				System.out.println("YOOYOYOYOYOYO YOYOYOO");
		//				System.out.println("YOOYOYOYOYOYO YOYOYOO");
		//
		//				try {
		//					// ComponentAccessor.getIssueManager().create
		//					for (Project project : ComponentAccessor.getProjectManager().getProjectObjects()) {
		//						System.out.println(project.getName());
		//					}
		//					Project project = ComponentAccessor.getProjectManager().getProjectByCurrentKeyIgnoreCase("TEST");
		//					System.out.println("project: " + project.getId());
		//					// Files.write(Paths.get("/tmp/test.log"),
		//					// "Test".getBytes());
		//
		//					IssueService issueService = ComponentAccessor.getIssueService();
		//
		//					IssueInputParameters issueInputParameters = issueService.newIssueInputParameters()
		//							.setProjectId(project.getId()).setSummary("This is a summary")
		//							.setDescription("I am a description").setIssueTypeId("10000").setReporterId("admin")
		//							.setStatusId("10000");
		//
		//					JiraAuthenticationContext jAC = ComponentAccessor.getJiraAuthenticationContext();
		//					jAC.setLoggedInUser(ComponentAccessor.getUserManager().getUser("admin"));
		//					ApplicationUser user = jAC.getLoggedInUser();
		//
		//					System.out.println("User is: " + user);
		//					CreateValidationResult createValidationResult = issueService.validateCreate(user,
		//							issueInputParameters);
		//					System.out.println("createValidationResult: " + createValidationResult.isValid());
		//					if (createValidationResult.isValid()) {
		//						IssueResult createResult = issueService.create(user, createValidationResult);
		//						System.out.println("createResult: " + createResult.isValid());
		//						if (createResult.isValid()) {
		//							System.out.println("createResult: " + createResult.getIssue());
		//							System.out.println("createResult: " + createResult.getIssue().getId());
		//						} else {
		//							System.out.println("hasAnyErrors: " + createResult.getErrorCollection().hasAnyErrors());
		//							for (Reason reason : createResult.getErrorCollection().getReasons()) {
		//								System.out.println("reason: " + reason);
		//							}
		//							for (String error : createResult.getErrorCollection().getErrorMessages()) {
		//								System.out.println("errors: " + error);
		//							}
		//						}
		//					} else {
		//						System.out
		//								.println("hasAnyErrors: " + createValidationResult.getErrorCollection().hasAnyErrors());
		//						for (Reason reason : createValidationResult.getErrorCollection().getReasons()) {
		//							System.out.println("reason: " + reason);
		//						}
		//						for (String error : createValidationResult.getErrorCollection().getErrorMessages()) {
		//							System.out.println("errors: " + error);
		//						}
		//					}
		//
		//					IssueResult i = ComponentAccessor.getIssueService().getIssue(user, "TEST-1");
		//					System.out.println("i: " + i);
		//					System.out.println("i: " + i.getIssue());
		//					System.out.println("i issuetypeid: " + i.getIssue().getIssueTypeId());
		//					System.out.println("i assid: " + i.getIssue().getAssigneeId());
		//					System.out.println("i repid: " + i.getIssue().getReporterId());
		//					System.out.println("i projectid: " + i.getIssue().getProjectId());
		//					System.out.println("i sid: " + i.getIssue().getStatusId());
		//					System.out.println("i pid: " + i.getIssue().getPriority().getId());
		//					System.out.println("i pid: " + i.getIssue().getResolutionId());
		//
		//				} catch (Exception e) {
		//					// TODO Auto-generated catch block
		//					e.printStackTrace();
		//				}
		//
		//				System.out.println("YOOYOYOYOYOYO YOYOYOO");
		//				System.out.println("YOOYOYOYOYOYO YOYOYOO");
		//				System.out.println("YOOYOYOYOYOYO YOYOYOO");
		//				System.out.println("YOOYOYOYOYOYO YOYOYOO");
		//			}
		//		}, 60000);
	}

	public String getName() {
		if (null != applicationProperties) {
			return "myComponent:" + applicationProperties.getDisplayName();
		}

		return "myComponent";
	}
}