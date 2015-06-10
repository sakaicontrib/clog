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

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import org.sakaiproject.clog.api.datamodel.Visibilities;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class QueryBean {

	private List<String> visibilities
         = Arrays.asList(new String [] {Visibilities.SITE,Visibilities.TUTOR,Visibilities.PRIVATE});
	private String creator = "";
	private String siteId = "";
	private String title = "";
	private String keyword = "";
    private String group = "";
	private String caller = "";
	private boolean searchAutoSaved = false;
	private int page = 0;

	public boolean hasConditions() {

		return creator.length() > 0 || siteId.length() > 0 || visibilities.size() > 0
                || title.length() > 0 || keyword.length() > 0 || group.length() > 0;
	}

	public boolean queryBySiteId() {
		return !siteId.equals("");
	}

	public boolean queryByVisibility() {
		return visibilities.size() > 0;
	}

	public boolean queryByTitle() {
		return title.length() > 0;
	}

	public boolean queryByKeyword() {
		return keyword.length() > 0;
	}

	public boolean queryByCreator() {
		return !creator.trim().equals("");
	}

    public boolean queryByGroup() {
		return !group.trim().equals("");
    }

    public boolean byPublic() {
        return visibilities.contains(Visibilities.PUBLIC);
    }
}
