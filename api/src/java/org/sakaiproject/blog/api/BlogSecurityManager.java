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

package org.sakaiproject.blog.api;

import java.util.List;

import org.sakaiproject.blog.api.SakaiProxy;
import org.sakaiproject.blog.api.BlogSecurityManager;
import org.sakaiproject.blog.api.datamodel.Comment;
import org.sakaiproject.blog.api.datamodel.Post;

public interface BlogSecurityManager
{
    public boolean canCurrentUserCommentOnPost(Post post);
	
	public boolean canCurrentUserDeletePost(Post post) throws SecurityException;
	
	public boolean canCurrentUserEditPost(Post post);

	public List<Post> filter(List<Post> posts);
	
	public boolean canCurrentUserReadPost(Post post);
	
	public boolean canCurrentUserDeleteComment(Post post,Comment comment);

	public void setSakaiProxy(SakaiProxy sakaiProxy);

	public SakaiProxy getSakaiProxy();

	public boolean canCurrentUserEditComment(Post post, Comment comment);

	public void setPersistenceManager(PersistenceManager persistenceManager);

    public PersistenceManager getPersistenceManager();

    public boolean canCurrentUserSearch();
}
