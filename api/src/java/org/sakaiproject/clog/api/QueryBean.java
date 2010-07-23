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

import java.util.ArrayList;
import java.util.List;

public class QueryBean
{
	private List<String> visibilities;

	private String _user;

	private String siteId;
	
	private String caller;
	
	private boolean searchAutoSaved = false;

	public QueryBean()
	{
		visibilities = new ArrayList<String>(); // this mean no filter by visibility
		_user = "";
		siteId = "";
		caller = "";
	}

	public boolean hasConditions()
	{
		return siteId.length() > 0 || visibilities.size() > 0;
	}

	public boolean queryBySiteId()
	{
		return !siteId.equals("");
	}

	public boolean queryByVisibility()
	{
		return visibilities.size() > 0;
	}

	public void setVisibilities(String[] visibilities)
	{
		this.visibilities.clear();
		
		for(String v : visibilities)
		{
			this.visibilities.add(v);
		}
	}

	public List<String> getVisibilities()
	{
		return visibilities;
	}

	public void setCreator(String user)
	{
		this._user = user;
	}

	public String getCreator()
	{
		return _user;
	}

	public void setSiteId(String siteId)
	{
		this.siteId = siteId;
	}

	public String getSiteId()
	{
		return siteId;
	}

	public boolean queryByCreator()
	{
		return ! _user.trim().equals("");
	}

	public void setCaller(String caller)
	{
		this.caller = caller;
	}

	public String getCaller()
	{
		return caller;
	}

	public void setSearchAutoSaved(boolean searchAutoSaved)
	{
		this.searchAutoSaved = searchAutoSaved;
	}

	public boolean isSearchAutoSaved()
	{
		return searchAutoSaved;
	}
}
