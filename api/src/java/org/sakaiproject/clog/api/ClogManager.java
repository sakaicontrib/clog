package org.sakaiproject.clog.api;

import java.util.*;

import org.sakaiproject.clog.api.datamodel.Comment;
import org.sakaiproject.clog.api.datamodel.Post;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.EntityProducer;

public interface ClogManager extends EntityProducer {
    public static final String ENTITY_PREFIX = "clog";
    public static final String REFERENCE_ROOT = Entity.SEPARATOR + ENTITY_PREFIX;
    public static final String CLOG_POST_CREATED = "clog.post.created";
    public static final String CLOG_POST_DELETED = "clog.post.deleted";
    public static final String CLOG_COMMENT_CREATED = "clog.comment.created";
    public static final String CLOG_COMMENT_DELETED = "clog.comment.deleted";
    public static final String CLOG_POST_RECYCLED = "clog.post.recycled";
    public static final String CLOG_POST_RESTORED = "clog.post.restored";
    public static final String CLOG_POST_WITHDRAWN = "clog.post.withdrawn";

    public Post getPost(String postId) throws Exception;
    
    // Used by Dashboard integration
    public Post getPostHeader(String postId) throws Exception;

    public List<Post> getPosts(String placementId) throws Exception;

    public List<Post> getPosts(QueryBean query) throws Exception;

    public boolean savePost(Post post);

    public boolean deletePost(String postId);
    
	public Comment getComment(String commentId) throws Exception;

    public boolean saveComment(Comment comment);

    public boolean deleteComment(String commentId);

    public boolean recyclePost(String postId);

    public boolean restorePost(String postId);

    public List<ClogMember> getAuthors(String siteId);

    public boolean deleteAutosavedCopy(String postId);


}
