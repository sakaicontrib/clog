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
import java.util.Set;

import org.sakaiproject.authz.api.SecurityAdvisor;
import org.sakaiproject.entity.api.EntityProducer;
import org.sakaiproject.clog.api.BlogMember;
import org.sakaiproject.clog.api.SakaiProxy;

public interface SakaiProxy
{
	public String getCurrentSiteId();

	public String getCurrentUserId();
	
	public Connection borrowConnection() throws SQLException;
	
	public void returnConnection(Connection connection);

	public String getCurrentUserDisplayName();
	
	public String getVendor();

	public String getDisplayNameForTheUser(String userId);
	
    public boolean isMaintainer(String userId,String siteId);

	public boolean isCurrentUserMaintainer(String siteId);
	
	public boolean isCurrentUserAdmin();

	public String getSakaiProperty(String string);

	public BlogMember getMember(String memberId);
	
	public boolean isAutoDDL();

	public List<BlogMember> getSiteMembers(String siteId);
	
	public String getServerUrl();
	
	public String getPortalUrl();
	
	public String getAccessUrl();

	public void registerEntityProducer(EntityProducer entityProducer);

	public void registerFunction(String function);

	public boolean isAllowedFunction(String function,String siteId);

	public void sendEmailWithMessage(String creatorId, String subject, String string);
	
	public void sendEmailWithMessage(Set<String> emails, String subject,String string);
	
	public void addDigestMessage(String userId,String subject, String message);
	
	public void addDigestMessage(Set<String> users,String subject, String message);

	public void registerSecurityAdvisor(SecurityAdvisor securityAdvisor);

	public void postEvent(String event, String entityId, String siteId);

	public Set<String> getSiteUsers(String siteId);

	public String getSiteTitle(String siteId);

	public String getBlogPageId(String siteId);
	
	public String getBlogToolId(String siteId);

	public String storeResource(byte[] blob,String siteId,String creatorId);
}
