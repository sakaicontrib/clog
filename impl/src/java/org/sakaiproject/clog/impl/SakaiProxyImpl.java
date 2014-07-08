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
package org.sakaiproject.clog.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.sakaiproject.authz.api.AuthzGroup;
import org.sakaiproject.authz.api.AuthzGroupService;
import org.sakaiproject.authz.api.AuthzPermissionException;
import org.sakaiproject.authz.api.FunctionManager;
import org.sakaiproject.authz.api.GroupNotDefinedException;
import org.sakaiproject.authz.api.Role;
import org.sakaiproject.authz.api.SecurityAdvisor;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.clog.api.ClogFunctions;
import org.sakaiproject.clog.api.ClogManager;
import org.sakaiproject.clog.api.ClogMember;
import org.sakaiproject.clog.api.SakaiProxy;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.content.api.ContentResourceEdit;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.email.api.DigestService;
import org.sakaiproject.email.api.EmailService;
import org.sakaiproject.entity.api.EntityManager;
import org.sakaiproject.entity.api.EntityProducer;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.event.api.NotificationEdit;
import org.sakaiproject.event.api.NotificationService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.search.api.InvalidSearchQueryException;
import org.sakaiproject.search.api.SearchList;
import org.sakaiproject.search.api.SearchResult;
import org.sakaiproject.search.api.SearchService;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.Tool;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.user.api.AuthenticationManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.util.BaseResourceProperties;
import org.sakaiproject.util.FormattedText;

public class SakaiProxyImpl implements SakaiProxy {

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

	private DigestService digestService;

	private SearchService searchService;

	private NotificationService notificationService;

	public void setNotificationService(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	public void init() {

		NotificationEdit ne = notificationService.addTransientNotification();
		ne.setResourceFilter(ClogManager.REFERENCE_ROOT);
		ne.setFunction(ClogManager.CLOG_POST_CREATED);
		NewPostNotification yn = new NewPostNotification();
		yn.setSakaiProxy(this);
		ne.setAction(yn);

		NotificationEdit ne2 = notificationService.addTransientNotification();
		ne2.setResourceFilter(ClogManager.REFERENCE_ROOT);
		ne2.setFunction(ClogManager.CLOG_COMMENT_CREATED);
		NewCommentNotification cn = new NewCommentNotification();
		cn.setSakaiProxy(this);
		ne2.setAction(cn);
	}

	public void destroy() {
	}

	public String getCurrentSiteId() {
		return toolManager.getCurrentPlacement().getContext(); // equivalent to
	}

	public String getCurrentToolId() {
		return toolManager.getCurrentPlacement().getId();
	}

	public String getCurrentToolTitle() {
		Tool tool = toolManager.getCurrentTool();
		if (tool != null)
			return tool.getTitle();
		else
			return "Clog";
	}

	public String getCurrentUserId() {
		Session session = sessionManager.getCurrentSession();
		String userId = session.getUserId();
		return userId;
	}

	public String getCurrentUserEid() {
		Session session = sessionManager.getCurrentSession();
		String userEid = session.getUserEid();
		return userEid;
	}

	public Connection borrowConnection() throws SQLException {
		return sqlService.borrowConnection();
	}

	public void returnConnection(Connection connection) {
		sqlService.returnConnection(connection);
	}

	public String getCurrentUserDisplayName() {
		return getDisplayNameForTheUser(getCurrentUserId());
	}

	public String getVendor() {
		return sqlService.getVendor();
	}

	public String getDisplayNameForTheUser(String userId) {
		try {
			User sakaiUser = userDirectoryService.getUser(userId);
			// CLOG-24
			return FormattedText.escapeHtmlFormattedText(sakaiUser.getDisplayName());
		} catch (Exception e) {
			return userId; // this can happen if the user does not longer exist
			// in the system
		}
	}

	private String getEmailForTheUser(String userId) {
		try {
			User sakaiUser = userDirectoryService.getUser(userId);
			return sakaiUser.getEmail();
		} catch (Exception e) {
			return ""; // this can happen if the user does not longer exist in
			// the system
		}

	}

	public boolean isMaintainer(String userId, String siteId) {

		try {
			if (userId == null || siteId == null) {
				return false;
			}

			if (isCurrentUserAdmin()) {
				return true;
			}

			Site site = siteService.getSite(siteId);
			AuthzGroup realm = authzGroupService.getAuthzGroup(site.getReference());
			User sakaiUser = userDirectoryService.getUser(userId);
			Role r = realm.getUserRole(sakaiUser.getId());
			if (r.getId().equals(realm.getMaintainRole())) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			logger.error("Exception thrown whilst checking for maintainer status", e);
			return false;
		}
	}

	public boolean isCurrentUserMaintainer(String siteId) {
		return isMaintainer(getCurrentUserId(), siteId);
	}

	public boolean isCurrentUserAdmin() {
		return securityService.isSuperUser();
	}

	public String getSakaiProperty(String string) {
		return serverConfigurationService.getString(string);
	}

	public ClogMember getMember(String memberId) {
		User user;
		try {
			user = userDirectoryService.getUser(memberId);
			ClogMember member = new ClogMember(user);

			return member;
		} catch (UserNotDefinedException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void setToolManager(ToolManager toolManager) {
		this.toolManager = toolManager;
	}

	public ToolManager getToolManager() {
		return toolManager;
	}

	public void setSessionManager(SessionManager sessionManager) {
		this.sessionManager = sessionManager;
	}

	public SessionManager getSessionManager() {
		return sessionManager;
	}

	public void setAuthzGroupService(AuthzGroupService authzGroupService) {
		this.authzGroupService = authzGroupService;
	}

	public AuthzGroupService getAuthzGroupService() {
		return authzGroupService;
	}

	public boolean isAutoDDL() {
		String autoDDL = serverConfigurationService.getString("auto.ddl");
		return autoDDL.equals("true");
	}

    public Map<String, String> getCurrentSiteGroups() {

        Map<String, String> map = new HashMap<String, String>();

        try {
            Collection<Group> groups = siteService.getSite(getCurrentSiteId()).getGroups();

            for (Group group : groups) {
                map.put(group.getId(), group.getTitle());
            }
        } catch (IdUnusedException idue) {
            // This should never happen. Really.
            logger.error("getCurrentSiteId returned an invalid site id. No groups will be returned.");
        }

        return map;
    }

	/*
	 * public List<String> getEidMaintainerSiteMembers() { // TODO
	 * Auto-generated method stub return null; }
	 */

	public List<ClogMember> getSiteMembers(String siteId) {
		ArrayList<ClogMember> result = new ArrayList<ClogMember>();
		try {
			Site site = siteService.getSite(siteId);
			Set<String> userIds = site.getUsers();
			for (String userId : userIds) {
				try {
					User sakaiUser = userDirectoryService.getUser(userId);
					ClogMember member = new ClogMember(sakaiUser);
					result.add(member);
				} catch (UserNotDefinedException unde) {
					logger.error("Failed to get site member details", unde);
				}
			}
		} catch (Exception e) {
			logger.error("Exception thrown whilst getting site members", e);
		}

		return result;
	}

	public String getPortalUrl() {
		// don't use serverConfigurationService.getPortalUrl() as it can return
		// 'sakai-entitybroker-direct' instead of 'portal'
		return getServerUrl() + serverConfigurationService.getString("portalPath");
	}

	public String getServerUrl() {
		return serverConfigurationService.getServerUrl();
	}

	public String getServiceName() {
		return serverConfigurationService.getString("ui.service", "Sakai");
	}

	public String getAccessUrl() {
		return serverConfigurationService.getAccessUrl();
	}

	public void setContentHostingService(ContentHostingService contentHostingService) {
		this.contentHostingService = contentHostingService;
	}

	public ContentHostingService getContentHostingService() {
		return contentHostingService;
	}

	public void setSiteService(SiteService siteService) {
		this.siteService = siteService;
	}

	public SiteService getSiteService() {
		return siteService;
	}

	public void setServerConfigurationService(ServerConfigurationService serverConfigurationService) {
		this.serverConfigurationService = serverConfigurationService;
	}

	public ServerConfigurationService getServerConfigurationService() {
		return serverConfigurationService;
	}

	public void setAuthenticationManager(AuthenticationManager authenticationManager) {
		this.authenticationManager = authenticationManager;
	}

	public AuthenticationManager getAuthenticationManager() {
		return authenticationManager;
	}

	public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
		this.userDirectoryService = userDirectoryService;
	}

	public UserDirectoryService getUserDirectoryService() {
		return userDirectoryService;
	}

	public void registerEntityProducer(EntityProducer entityProducer) {
		entityManager.registerEntityProducer(entityProducer, "clog");
	}

	public void setEntityManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public EntityManager getEntityManager() {
		return entityManager;
	}

	public void setSqlService(SqlService sqlService) {
		this.sqlService = sqlService;
	}

	public SqlService getSqlService() {
		return sqlService;
	}

	public void registerFunction(String function) {
		List functions = functionManager.getRegisteredFunctions("clog.");

		if (!functions.contains(function)) {
			functionManager.registerFunction(function);
		}
	}

	public void setFunctionManager(FunctionManager functionManager) {
		this.functionManager = functionManager;
	}

	public FunctionManager getFunctionManager() {
		return functionManager;
	}

	public boolean isAllowedFunction(String function, String siteId) {
		try {
			if (isCurrentUserAdmin())
				return true;

			Site site = siteService.getSite(siteId);
			Role r = site.getUserRole(getCurrentUserId());

			if (r == null) {
				return false;
			}

			return r.isAllowed(function);
		} catch (Exception e) {
			logger.error("Caught exception while performing function test", e);
		}

		return false;
	}

	public void setEventTrackingService(EventTrackingService eventTrackingService) {
		this.eventTrackingService = eventTrackingService;
	}

	public EventTrackingService getEventTrackingService() {
		return eventTrackingService;
	}

	private void enableSecurityAdvisor(SecurityAdvisor securityAdvisor) {
		securityService.pushAdvisor(securityAdvisor);
	}

	private void disableSecurityAdvisor(SecurityAdvisor securityAdvisor) {
		securityService.popAdvisor(securityAdvisor);
	}

	public void setSecurityService(SecurityService securityService) {
		this.securityService = securityService;
	}

	public SecurityService getSecurityService() {
		return securityService;
	}

	public void postEvent(String event, String reference, String siteId) {
		eventTrackingService.post(eventTrackingService.newEvent(event, reference, true));
	}

	public Set<String> getSiteUsers(String siteId) {
		try {
			Site site = siteService.getSite(siteId);
			return site.getUsers();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public void setDigestService(DigestService digestService) {
		this.digestService = digestService;
	}

	public DigestService getDigestService() {
		return digestService;
	}

	public String getSiteTitle(String siteId) {
		try {
			return siteService.getSite(siteId).getTitle();
		} catch (Exception e) {
			logger.error("Caught exception whilst getting site title", e);
		}

		return "";
	}

	public String getClogPageId(String siteId) {
		try {
			Site site = siteService.getSite(siteId);
			ToolConfiguration tc = site.getToolForCommonId("sakai.clog");
			return tc.getPageId();
		} catch (Exception e) {
			return "";
		}
	}

	public String getClogToolId(String siteId) {
		try {
			Site site = siteService.getSite(siteId);
			ToolConfiguration tc = site.getToolForCommonId("sakai.clog");
			return tc.getId();
		} catch (Exception e) {
			return "";
		}
	}

	/**
	 * Used by the blog 1 and 2 data importers
	 */
	public String storeResource(byte[] blob, String displayName, String siteId, String creatorId) {
		ContentResourceEdit resource = null;
		ResourceProperties props = null;

		String id = UUID.randomUUID().toString();

		String resourceId = "/group/" + siteId + "/clog-files/" + id;

		// CLOG-61 get a security advisor so we can add resources
		SecurityAdvisor securityAdvisor = new SecurityAdvisor() {

			public SecurityAdvice isAllowed(String userId, String function, String reference) {
				return SecurityAdvice.ALLOWED;
			}
		};

		enableSecurityAdvisor(securityAdvisor);

		try {
			resource = contentHostingService.addResource(resourceId);
			props = new BaseResourceProperties();

			if (blob.length > 0) {
				resource.setContent(blob);
				props.addProperty(ResourceProperties.PROP_CREATOR, creatorId);
				props.addProperty(ResourceProperties.PROP_DISPLAY_NAME, displayName);
			}

			resource.getPropertiesEdit().addAll(props);

			contentHostingService.commitResource(resource, NotificationService.NOTI_NONE);

			return resourceId;

		} catch (PermissionException pe) {
			logger.error("Caught permission exception whilst storing resource. Returning null ...", pe);
		} catch (Exception e) {
			logger.error("Caught an exception whilst storing resource. Returning null ...", e);
		} finally {
			disableSecurityAdvisor(securityAdvisor);
		}
		return null;
	}

	public List<SearchResult> searchInCurrentSite(String searchTerms) throws InvalidSearchQueryException {
		List<SearchResult> results = new ArrayList<SearchResult>();

		List<String> contexts = new ArrayList<String>(1);
		contexts.add(getCurrentSiteId());

		try {
			SearchList sl = searchService.search(searchTerms, contexts, 0, 50, "normal", "normal");
			for (Iterator i = sl.iterator(0); i.hasNext();) {
				SearchResult sr = (SearchResult) i.next();

				if ("Clog".equals(sr.getTool()))
					results.add(sr);
			}
		} catch (InvalidSearchQueryException isqe) {
			throw isqe;
		} catch (Exception e) {
			logger.error("Caught exception whilst searching", e);
		}

		return results;
	}

	public Set<String> getSitePermissionsForCurrentUser(String siteId) {

		String userId = getCurrentUserId();

		if (userId == null) {
			throw new SecurityException("This action (userPerms) is not accessible to anon and there is no current user.");
		}

		Set<String> filteredFunctions = new TreeSet<String>();

		if (securityService.isSuperUser(userId)) {
			// Special case for the super admin
			filteredFunctions.addAll(functionManager.getRegisteredFunctions("clog"));
		} else {
			Site site = null;
			AuthzGroup siteHelperRealm = null;

			try {
				site = siteService.getSite(siteId);
				siteHelperRealm = authzGroupService.getAuthzGroup("!site.helper");
			} catch (Exception e) {
				// This should probably be logged but not rethrown.
			}

			Role currentUserRole = site.getUserRole(userId);

			Role siteHelperRole = siteHelperRealm.getRole(currentUserRole.getId());

			Set<String> functions = currentUserRole.getAllowedFunctions();

			if (siteHelperRole != null) {
				// Merge in all the functions from the same role in !site.helper
				functions.addAll(siteHelperRole.getAllowedFunctions());
			}

			for (String function : functions) {
				if (function.startsWith("clog"))
					filteredFunctions.add(function);
			}

			if (functions.contains("site.upd")) {
				filteredFunctions.add(ClogFunctions.CLOG_MODIFY_PERMISSIONS);
			}
		}

		return filteredFunctions;
	}

	public Map<String, Set<String>> getSitePermissions(String siteId) {

		Map<String, Set<String>> perms = new HashMap<String, Set<String>>();

		String userId = getCurrentUserId();

		if (userId == null) {
			throw new SecurityException("This action (perms) is not accessible to anon and there is no current user.");
		}

		try {
			Site site = siteService.getSite(siteId);

			Set<Role> roles = site.getRoles();
			for (Role role : roles) {
				Set<String> functions = role.getAllowedFunctions();
				Set<String> filteredFunctions = new TreeSet<String>();
				for (String function : functions) {
					if (function.startsWith("clog"))
						filteredFunctions.add(function);
				}

				perms.put(role.getId(), filteredFunctions);
			}
		} catch (Exception e) {
			logger.error("Failed to get current site permissions.", e);
		}

		return perms;
	}

	public boolean setPermissionsForSite(String siteId, Map<String, Object> params) {

		String userId = getCurrentUserId();

		if (userId == null)
			throw new SecurityException("This action (setPerms) is not accessible to anon and there is no current user.");

        Site site = null;

		try {
			site = siteService.getSite(siteId);
		} catch (IdUnusedException ide) {
			logger.warn(userId + " attempted to update CLOG permissions for unknown site " + siteId);
			return false;
		}

		boolean admin = securityService.isSuperUser(userId);

		try {

			AuthzGroup authzGroup = authzGroupService.getAuthzGroup(site.getReference());

			// admin can update permissions. check for anyone else
			if (!securityService.isSuperUser()) {

				Role siteRole = authzGroup.getUserRole(userId);
				AuthzGroup siteHelperAuthzGroup = authzGroupService.getAuthzGroup("!site.helper");
				Role siteHelperRole = siteHelperAuthzGroup.getRole(siteRole.getId());

				if (!siteRole.isAllowed(ClogFunctions.CLOG_MODIFY_PERMISSIONS) && !siteRole.isAllowed("site.upd")) {
					if (siteHelperRole == null || !siteHelperRole.isAllowed(ClogFunctions.CLOG_MODIFY_PERMISSIONS)) {
						logger.warn(userId + " attempted to update CLOG permissions for site " + site.getTitle());
						return false;
					}
				}
			}

			boolean changed = false;

			for (String name : params.keySet()) {
				if (!name.contains(":")) {
					continue;
                }

				String value = (String) params.get(name);

				String roleId = name.substring(0, name.indexOf(":"));

				Role role = authzGroup.getRole(roleId);
				if (role == null) {
					throw new IllegalArgumentException("Invalid role id '" + roleId + "' provided in POST parameters.");
				}
				String function = name.substring(name.indexOf(":") + 1);

				if ("true".equals(value)) {
					role.allowFunction(function);
                } else {
					role.disallowFunction(function);
                }

				changed = true;
			}

			if (changed) {
				try {
					authzGroupService.save(authzGroup);
				} catch (AuthzPermissionException ape) {
					throw new SecurityException("The permissions for this site (" + siteId + ") cannot be updated by the current user.");
				}
			}

			return true;
		} catch (GroupNotDefinedException gnde) {
			logger.error("No realm defined for site (" + siteId + ").");
		}

		return false;
	}

	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}

	public SearchService getSearchService() {
		return searchService;
	}

	private String getFromAddress() {
		return serverConfigurationService.getString("setup.request", "sakai-clog@sakai.lancs.ac.uk");
	}

	public boolean isPublicAllowed() {
		return "true".equals(serverConfigurationService.getString("clog.allowPublic", "false"));
	}

	public boolean setResourcePublic(String contentId, boolean isPublic) {
		try {
			contentHostingService.setPubView(contentId, isPublic);
			return contentHostingService.isPubView(contentId) == isPublic;
		} catch (Throwable t) {
			t.printStackTrace();
			return false;
		}
	}

	public boolean isCurrentUserMemberOfSite(String siteId) throws Exception {
		if (securityService.isSuperUser()) {
			return true;
		}
		Site site = siteService.getSite(siteId);
		return site.getMember(getCurrentUserId()) != null;
	}

	public String getWysiwygEditor() {
		return serverConfigurationService.getString("wysiwyg.editor");
	}
}
