package org.sakaiproject.blog.api;

import java.util.List;
import java.util.Set;

import org.sakaiproject.blog.api.datamodel.Comment;
import org.sakaiproject.blog.api.datamodel.Post;
import org.sakaiproject.blog.api.datamodel.Preferences;

public interface PersistenceManager
{
	public boolean setupTables();

	public boolean existPost(String OID) throws Exception;

	public List<Post> getAllPost(String placementId) throws Exception;

	public List<Post> getAllPost(String placementId, boolean populate) throws Exception;

	public boolean saveComment(Comment comment);

	public boolean deleteComment(String commentId);

	public boolean savePost(Post post);

	public boolean deletePost(Post post);

	public boolean recyclePost(Post post);
	
	public boolean restorePost(Post post);

	public List<Post> getPosts(QueryBean query) throws Exception;

	public Post getPost(String postId) throws Exception;

	public void setSakaiProxy(SakaiProxy sakaiProxy);

	public SakaiProxy getSakaiProxy();

	public void setSecurityManager(BlogSecurityManager securityManager);

	public BlogSecurityManager getSecurityManager();

	public List<BlogMember> getPublicBloggers();

	public Preferences getPreferences(String siteId,String userId);

	public boolean savePreferences(Preferences preferences);

	public boolean postExists(String postId) throws Exception;

	public boolean populateAuthorData(BlogMember author,String siteId);
}
