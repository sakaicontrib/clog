package org.sakaiproject.clog.api;

import java.util.*;

import org.sakaiproject.clog.api.datamodel.Comment;
import org.sakaiproject.clog.api.datamodel.Post;
import org.sakaiproject.clog.api.datamodel.Preferences;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.EntityProducer;

public interface ClogManager extends EntityProducer
{
	public static final String ENTITY_PREFIX = "blog";
	public static final String REFERENCE_ROOT = Entity.SEPARATOR + ENTITY_PREFIX;
	public static final String BLOG_POST_CREATED = "blog.post.created";
	public static final String BLOG_POST_DELETED = "blog.post.deleted";
	public static final String BLOG_COMMENT_CREATED = "blog.comment.created";
	public static final String BLOG_COMMENT_DELETED = "blog.comment.deleted";
	public static final String BLOG_POST_RECYCLED = "blog.post.recycled";
	public static final String BLOG_POST_RESTORED = "blog.post.restored";
	
    public Post getPost(String postId) throws Exception;

    public List<Post> getPosts(String placementId) throws Exception;

    public List<Post> getPosts(QueryBean query) throws Exception;

    public boolean savePost(Post post);

    public boolean deletePost(String postId);

    public boolean saveComment(Comment comment);

    public boolean deleteComment(String commentId);

    public boolean recyclePost(String postId);
    
	public boolean restorePost(String postId);

	public List<ClogMember> getAuthors(String siteId);

	public boolean savePreferences(Preferences preferences);

	public Preferences getPreferences(String siteId,String userId);

	public void sendNewPostAlert(Post post);
	
	public void sendNewCommentAlert(Comment comment);

	public void importBlog1Data();
	
	public void importBlog2Data();
}
