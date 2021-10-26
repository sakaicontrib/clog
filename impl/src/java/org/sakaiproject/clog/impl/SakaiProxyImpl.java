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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.sakaiproject.authz.api.AuthzGroup;
import org.sakaiproject.authz.api.AuthzGroupService;
import org.sakaiproject.authz.api.FunctionManager;
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
import org.sakaiproject.entity.api.EntityManager;
import org.sakaiproject.entity.api.EntityProducer;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.event.api.NotificationEdit;
import org.sakaiproject.event.api.NotificationService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.memory.api.Cache;
import org.sakaiproject.memory.api.MemoryService;
import org.sakaiproject.memory.api.SimpleConfiguration;
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

import lombok.Setter;

@Setter
public class SakaiProxyImpl implements SakaiProxy {

    private final Logger logger = Logger.getLogger(SakaiProxyImpl.class);

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

    private MemoryService memoryService;

    private SqlService sqlService;

    private FunctionManager functionManager;

    private EventTrackingService eventTrackingService;

    private DigestService digestService;

    private SearchService searchService;

    private NotificationService notificationService;

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

    public String getCurrentSiteId() {
        return toolManager.getCurrentPlacement().getContext(); // equivalent to
    }

    public Site getSiteOrNull(String siteId) {

        Site site = null;

        try {
            site = siteService.getSite(siteId);
        } catch (IdUnusedException idue) {
            logger.warn("No site with id '" + siteId + "'");
        }

        return site;
    }

    /**
     * {@inheritDoc}
     */
    public String getCurrentSiteLocale() {

        String siteId = toolManager.getCurrentPlacement().getContext();

        Site currentSite = getSiteOrNull(siteId);

        if (currentSite != null) {
            String locale = currentSite.getProperties().getProperty("locale_string");
            if (locale != null) {
                return locale;
            }
        }

        return null;
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

    private boolean isMaintainer(String userId, String siteId) {

        try {
            if (userId == null || siteId == null) {
                return false;
            }

            if (isCurrentUserAdmin()) {
                return true;
            }

            AuthzGroup realm = authzGroupService.getAuthzGroup("/site/" + siteId);
            Role r = realm.getUserRole(userId);
            if (r != null && r.getId().equals(realm.getMaintainRole())) {
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

    public boolean isCurrentUserTutor(String siteId) {

        final String userId = getCurrentUserId();

        try {
            if (userId == null || siteId == null) {
                return false;
            }

            if (isCurrentUserAdmin()) {
                return true;
            }

            AuthzGroup realm = authzGroupService.getAuthzGroup("/site/" + siteId);
            Role r = realm.getUserRole(userId);
            if (r != null && r.isAllowed(ClogFunctions.CLOG_TUTOR)) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            logger.error("Exception thrown whilst checking for tutor status", e);
            return false;
        }
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

    public boolean isAutoDDL() {
        String autoDDL = serverConfigurationService.getString("auto.ddl");
        return autoDDL.equals("true");
    }

    public Map<String, String> getSiteGroupsForCurrentUser(String siteId) {

        Map<String, String> map = new HashMap<String, String>();

        try {
            Collection<Group> groups
                = siteService
                    .getSite(siteId)
                        .getGroupsWithMember(getCurrentUserId());

            for (Group group : groups) {
                map.put(group.getId(), group.getTitle());
            }
        } catch (IdUnusedException idue) {
            // This should never happen. Really.
            logger.error("Invalid site id. No groups will be returned.");
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

    public void registerEntityProducer(EntityProducer entityProducer) {
        entityManager.registerEntityProducer(entityProducer, "/clog");
    }

    public void registerFunction(String function) {
        List functions = functionManager.getRegisteredFunctions("clog.");

        if (!functions.contains(function)) {
            functionManager.registerFunction(function, true);
        }
    }

    public boolean isAllowedFunction(String function, String siteId) {

        try {
            Site site = siteService.getSite(siteId);
            Role role = site.getUserRole(getCurrentUserId());
            return isAllowedFunction(function, role);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isAllowedFunction(String function, Role role) {

        try {
            if (isCurrentUserAdmin()) {
                return true;
            }

            if (role == null) {
                return false;
            }

            return role.isAllowed(function);
        } catch (Exception e) {
            logger.error("Caught exception while performing function test", e);
        }

        return false;
    }

    private void enableSecurityAdvisor(SecurityAdvisor securityAdvisor) {
        securityService.pushAdvisor(securityAdvisor);
    }

    private void disableSecurityAdvisor(SecurityAdvisor securityAdvisor) {
        securityService.popAdvisor(securityAdvisor);
    }

    public void postEvent(String event, String reference, String siteId) {
        eventTrackingService.post(eventTrackingService.newEvent(event, reference, null, true, NotificationService.NOTI_OPTIONAL));
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

            if (functions.contains("realm.upd") && functions.contains("site.upd")) {
                filteredFunctions.add(ClogFunctions.CLOG_MODIFY_PERMISSIONS);
            }
        }

        return filteredFunctions;
    }

    private String getFromAddress() {
        return serverConfigurationService.getString("setup.request", "sakai-clog@sakai.lancs.ac.uk");
    }

    public boolean isPublicAllowed() {
        return serverConfigurationService.getBoolean("clog.allowPublic", false);
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

    public Cache getCache(String cache) {

        try {
            Cache c = memoryService.getCache(cache);
            if (c == null) {
                c = memoryService.createCache(cache, new SimpleConfiguration(0));
            }
            return c;
        } catch (Exception e) {
            logger.error("Exception whilst retrieving '" + cache + "' cache. Returning null ...", e);
            return null;
        }
    }

    public void addToolToToolConfig(ToolConfiguration tool) {
        tool.setTool("sakai.clog", toolManager.getTool("sakai.clog"));
        tool.setTitle(toolManager.getTool("sakai.clog").getTitle());
    }
}
