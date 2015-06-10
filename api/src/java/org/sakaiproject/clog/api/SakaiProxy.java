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
package org.sakaiproject.clog.api;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sakaiproject.entity.api.EntityProducer;
import org.sakaiproject.search.api.InvalidSearchQueryException;
import org.sakaiproject.search.api.SearchResult;
import org.sakaiproject.site.api.Site;

public interface SakaiProxy {

	public String getCurrentSiteId();

    /**
     * Returns the locale_string property of the current site.
     *
     * @return the locale_string property of the current site.
     */
    public String getCurrentSiteLocale();

    /**
     *  Returns a map of group title against group id for the groups
     *  in the current site.
     */
    public Map<String, String> getSiteGroupsForCurrentUser(String siteId);

	public String getCurrentToolId();

	public String getCurrentToolTitle();

	public String getCurrentUserId();

	public Connection borrowConnection() throws SQLException;

	public void returnConnection(Connection connection);

	public String getCurrentUserDisplayName();

	public String getVendor();

	public String getDisplayNameForTheUser(String userId);

	public boolean isCurrentUserMaintainer(String siteId);

	public boolean isCurrentUserTutor(String siteId);

	public boolean isCurrentUserAdmin();

	public String getSakaiProperty(String string);

	public ClogMember getMember(String memberId);

	public boolean isAutoDDL();

	public List<ClogMember> getSiteMembers(String siteId);

	public String getServerUrl();

	public String getServiceName();

	public String getPortalUrl();

	public String getAccessUrl();

	public void registerEntityProducer(EntityProducer entityProducer);

	public void registerFunction(String function);

	public boolean isAllowedFunction(String function, String siteId);

	public void postEvent(String event, String entityId, String siteId);

	public Set<String> getSiteUsers(String siteId);

	public String getSiteTitle(String siteId);

	public String getClogPageId(String siteId);

	public String getClogToolId(String siteId);

	public String storeResource(byte[] blob, String displayName, String siteId, String creatorId);

	public List<SearchResult> searchInCurrentSite(String searchTerms) throws InvalidSearchQueryException;

	public Set<String> getSitePermissionsForCurrentUser(String siteId);

	public Map<String, Set<String>> getSitePermissions(String siteId);

	public boolean setPermissionsForSite(String siteId, Map<String, Object> params);

	public boolean isPublicAllowed();

	public String getCurrentUserEid();

	public boolean setResourcePublic(String contentId, boolean isPublic);

	public boolean isCurrentUserMemberOfSite(String siteId) throws Exception;

	public String getWysiwygEditor();
}
