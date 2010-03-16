/*************************************************************************************
 * Copyright 2006, 2008 Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 *
 *       http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.

 *************************************************************************************/
package org.sakaiproject.blog.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Observer;
import java.util.Set;

import org.apache.log4j.Logger;
import org.sakaiproject.authz.api.AuthzGroup;
import org.sakaiproject.authz.api.AuthzGroupService;
import org.sakaiproject.authz.api.FunctionManager;
import org.sakaiproject.authz.api.Role;
import org.sakaiproject.authz.api.SecurityAdvisor;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.email.api.DigestService;
import org.sakaiproject.email.api.EmailService;
import org.sakaiproject.entity.api.EntityManager;
import org.sakaiproject.entity.api.EntityProducer;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.event.cover.NotificationService;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.blog.api.SakaiProxy;
import org.sakaiproject.user.api.AuthenticationManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.blog.api.BlogMember;

public class SakaiProxyImpl implements SakaiProxy
{
	private Logger logger = Logger.getLogger(SakaiProxyImpl.class);

	private ToolManager toolManager;

	private SessionManager sessionManager;

	private AuthzGroupService authzGroupService;

	private ServerConfigurationService serverConfigurationService;

	private SiteService siteService;

	private AuthenticationManager authenticationManager;

	private SecurityService securityService;

	private UserDirectoryService userDirectoryService;

	private ContentHostingService contentHostingService;

	private EntityManager entityManager;

	private SqlService sqlService;

	private FunctionManager functionManager;

	private EventTrackingService eventTrackingService;

	private EmailService emailService;

	private DigestService digestService;

	/** Inject this in your components.xml */
	private String fromAddress = "sakai-blog@sakai.lancs.ac.uk";

	public void init() {}

	public void destroy() {}

	public String getCurrentSiteId()
	{
		return toolManager.getCurrentPlacement().getContext(); // equivalent to PortalService.getCurrentSiteId();
	}

	public String getCurrentUserId()
	{

		Session session = sessionManager.getCurrentSession();
		String userId = session.getUserId();
		return userId;
	}

	public Connection borrowConnection() throws SQLException
	{
		return sqlService.borrowConnection();
	}

	public void returnConnection(Connection connection)
	{
		sqlService.returnConnection(connection);
	}

	public String getCurrentUserDisplayName()
	{
		return getDisplayNameForTheUser(getCurrentUserId());
	}

	public String getVendor()
	{
		return sqlService.getVendor();
	}

	public String getDisplayNameForTheUser(String userId)
	{
		try
		{
			User sakaiUser = userDirectoryService.getUser(userId);
			return sakaiUser.getDisplayName();
		}
		catch (Exception e)
		{
			return userId; // this can happen if the user does not longer exist in the system
		}
	}

	private String getEmailForTheUser(String userId)
	{
		try
		{
			User sakaiUser = userDirectoryService.getUser(userId);
			return sakaiUser.getEmail();
		}
		catch (Exception e)
		{
			return ""; // this can happen if the user does not longer exist in the system
		}

	}

	public boolean isMaintainer(String userId, String siteId)
	{
		try
		{
			if (userId == null || siteId == null)
			{
				return false;
			}

			Site site = siteService.getSite(siteId);
			AuthzGroup realm = authzGroupService.getAuthzGroup(site.getReference());
			User sakaiUser = userDirectoryService.getUser(userId);
			Role r = realm.getUserRole(sakaiUser.getId());
			if (r.getId().equals(realm.getMaintainRole())) // This bit could be wrong
			{
				return true;
			}
			else
			{
				return false;
			}

		}
		catch (Exception e)
		{
			logger.error("Exception thrown whilst checking for maintainer status",e);
			return false;
		}
	}

	public boolean isCurrentUserMaintainer(String siteId)
	{
		return isMaintainer(getCurrentUserId(), siteId);
	}

	public boolean isCurrentUserAdmin()
	{
		String userId = getCurrentUserId();
		return userId.equals(userDirectoryService.ADMIN_ID);
	}

	public String getSakaiProperty(String string)
	{
		return serverConfigurationService.getString(string);
	}

	public BlogMember getMember(String memberId)
	{
		User user;
		try
		{
			user = userDirectoryService.getUser(memberId);
			BlogMember member = new BlogMember(user);

			return member;
		}
		catch (UserNotDefinedException e)
		{
			e.printStackTrace();
			return null;
		}
	}

	public void setToolManager(ToolManager toolManager)
	{
		this.toolManager = toolManager;
	}

	public ToolManager getToolManager()
	{
		return toolManager;
	}

	public void setSessionManager(SessionManager sessionManager)
	{
		this.sessionManager = sessionManager;
	}

	public SessionManager getSessionManager()
	{
		return sessionManager;
	}

	public void setAuthzGroupService(AuthzGroupService authzGroupService)
	{
		this.authzGroupService = authzGroupService;
	}

	public AuthzGroupService getAuthzGroupService()
	{
		return authzGroupService;
	}

	public boolean isAutoDDL()
	{
		String autoDDL = serverConfigurationService.getString("auto.ddl");
		return autoDDL.equals("true");
	}

	/*
	 * public List<String> getEidMaintainerSiteMembers() { // TODO Auto-generated method stub return null; }
	 */

	public List<BlogMember> getSiteMembers(String siteId)
	{
		ArrayList<BlogMember> result = new ArrayList<BlogMember>();
		try
		{
			Site site = siteService.getSite(siteId);
			Set<String> userIds = site.getUsers();
			for (String userId : userIds)
			{
				try
				{
					User sakaiUser = userDirectoryService.getUser(userId);
					BlogMember member = new BlogMember(sakaiUser);
					result.add(member);
				}
				catch (UserNotDefinedException unde)
				{
					logger.error("Failed to get site member details", unde);
				}
			}
		}
		catch (Exception e)
		{
			logger.error("Exception thrown whilst getting site members",e);
		}

		return result;
	}

	public String getPortalUrl()
	{
		return serverConfigurationService.getPortalUrl();
	}

	public String getServerUrl()
	{
		return serverConfigurationService.getServerUrl();
	}

	public String getAccessUrl()
	{
		return serverConfigurationService.getAccessUrl();
	}

	public void setContentHostingService(ContentHostingService contentHostingService)
	{
		this.contentHostingService = contentHostingService;
	}

	public ContentHostingService getContentHostingService()
	{
		return contentHostingService;
	}

	public void setSiteService(SiteService siteService)
	{
		this.siteService = siteService;
	}

	public SiteService getSiteService()
	{
		return siteService;
	}

	public void setServerConfigurationService(ServerConfigurationService serverConfigurationService)
	{
		this.serverConfigurationService = serverConfigurationService;
	}

	public ServerConfigurationService getServerConfigurationService()
	{
		return serverConfigurationService;
	}

	public void setAuthenticationManager(AuthenticationManager authenticationManager)
	{
		this.authenticationManager = authenticationManager;
	}

	public AuthenticationManager getAuthenticationManager()
	{
		return authenticationManager;
	}

	public void setUserDirectoryService(UserDirectoryService userDirectoryService)
	{
		this.userDirectoryService = userDirectoryService;
	}

	public UserDirectoryService getUserDirectoryService()
	{
		return userDirectoryService;
	}

	public void registerEntityProducer(EntityProducer entityProducer)
	{
		entityManager.registerEntityProducer(entityProducer, "blog");
	}

	public void setEntityManager(EntityManager entityManager)
	{
		this.entityManager = entityManager;
	}

	public EntityManager getEntityManager()
	{
		return entityManager;
	}

	public void setSqlService(SqlService sqlService)
	{
		this.sqlService = sqlService;
	}

	public SqlService getSqlService()
	{
		return sqlService;
	}

	public void registerFunction(String function)
	{
		List functions = functionManager.getRegisteredFunctions("blog.");

		if (!functions.contains(function))
		{
			functionManager.registerFunction(function);
		}
	}

	public void setFunctionManager(FunctionManager functionManager)
	{
		this.functionManager = functionManager;
	}

	public FunctionManager getFunctionManager()
	{
		return functionManager;
	}

	public boolean isAllowedFunction(String function, String siteId)
	{
		try
		{
			Site site = siteService.getSite(siteId);
			Role r = site.getUserRole(getCurrentUserId());

			if (r == null)
			{
				return false;
			}

			return r.isAllowed(function);
		}
		catch (Exception e)
		{
			logger.error("Caught exception while performing function test", e);
		}

		return false;
	}

	public void setEventTrackingService(EventTrackingService eventTrackingService)
	{
		this.eventTrackingService = eventTrackingService;
	}

	public EventTrackingService getEventTrackingService()
	{
		return eventTrackingService;
	}

	public void sendEmailWithMessage(String user, String subject, String message)
	{
		Set<String> users = new HashSet<String>(1);
		users.add(user);
		sendEmailWithMessage(users, subject, message);

	}

	public void sendEmailWithMessage(Set<String> users, String subject, String message)
	{
		sendEmailToParticipants(fromAddress, users, subject, message);
	}

	public void addDigestMessage(String userId, String subject, String message)
	{
		try
		{
			digestService.digest(userId, subject, message);
		}
		catch (Exception e)
		{
			logger.error("Failed to add message to digest.", e);
		}
	}

	public void addDigestMessage(Set<String> users, String subject, String message)
	{
		for (String userId : users)
		{
			try
			{
				digestService.digest(userId, subject, message);
			}
			catch (Exception e)
			{
				logger.error("Failed to add message to digest.", e);
			}
		}
	}

	public void setEmailService(EmailService emailService)
	{
		this.emailService = emailService;
	}

	public EmailService getEmailService()
	{
		return emailService;
	}

	private void sendEmailToParticipants(String from, Set<String> to, String subject, String text)
	{
		class EmailSender implements Runnable
		{
			private Thread runner;

			private String sender;

			private String subject;

			private String text;

			private Set<String> participants;

			public EmailSender(String from, Set<String> to, String subject, String text)
			{
				this.sender = from;
				this.participants = to;
				this.text = text;
				this.subject = subject;
				runner = new Thread(this, "Blog Emailer Thread");
				runner.start();
			}

			public synchronized void run()
			{
				String emailText = "<html><body>";
				emailText += text;
				emailText += "</body></html>";

				List<String> additionalHeader = new ArrayList<String>();
				additionalHeader.add("Content-Type: text/html; charset=ISO-8859-1");
				// aditionalHeader.add("Content-Type: text/html; charset=UTF-8");

				String emailSender = getEmailForTheUser(sender);
				if (emailSender == null || emailSender.trim().equals(""))
				{
					emailSender = getDisplayNameForTheUser(sender);
				}

				for (String userId : participants)
				{
					String emailParticipant = getEmailForTheUser(userId);
					try
					{
						// TODO: This should all be parameterised and internationalised.
						// logger.info("Sending email to " + participantId + " ...");
						emailService.send(emailSender, emailParticipant, subject, emailText, emailParticipant/* participantEid */, sender, additionalHeader);
					}
					catch (Exception e)
					{
						System.out.println("Failed to send email to '" + userId + "'. Message: " + e.getMessage());
					}
				}
			}
		}

		new EmailSender(from, to, subject, text);
	}

	public void setFromAddress(String fromAddress)
	{
		this.fromAddress = fromAddress;
	}

	public String getFromAddress()
	{
		return fromAddress;
	}

	public void registerSecurityAdvisor(SecurityAdvisor securityAdvisor)
	{
		securityService.pushAdvisor(securityAdvisor);
	}

	public void setSecurityService(SecurityService securityService)
	{
		this.securityService = securityService;
	}

	public SecurityService getSecurityService()
	{
		return securityService;
	}

	public void postEvent(String event, String entityId, String siteId)
	{
		eventTrackingService.post(eventTrackingService.newEvent(event, entityId, siteId, true, NotificationService.NOTI_OPTIONAL));
	}

	public Set<String> getSiteUsers(String siteId)
	{
		try
		{
			Site site = siteService.getSite(siteId);
			return site.getUsers();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return null;
	}

	public void setDigestService(DigestService digestService)
	{
		this.digestService = digestService;
	}

	public DigestService getDigestService()
	{
		return digestService;
	}

	public String getSiteTitle(String siteId)
	{
		try
		{
			return siteService.getSite(siteId).getTitle();
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst getting site title", e);
		}

		return "";
	}

	public String getBlog2PageId(String siteId)
	{
		try
		{
			Site site = siteService.getSite(siteId);
			ToolConfiguration tc = site.getToolForCommonId("blog2");
			return tc.getPageId();
		}
		catch (Exception e)
		{
			return "";
		}
	}

	public String getBlog2ToolId(String siteId)
	{
		try
		{
			Site site = siteService.getSite(siteId);
			ToolConfiguration tc = site.getToolForCommonId("blog2");
			return tc.getId();
		}
		catch (Exception e)
		{
			return "";
		}
	}
}
