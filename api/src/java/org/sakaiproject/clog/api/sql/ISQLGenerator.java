package org.sakaiproject.clog.api.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

import org.sakaiproject.clog.api.datamodel.Comment;
import org.sakaiproject.clog.api.datamodel.Post;
import org.sakaiproject.clog.api.QueryBean;

public interface ISQLGenerator {

	public static final String DEFAULT_PREFIX = "CLOG_";

	public static final String TABLE_POST = DEFAULT_PREFIX + "POST";
	public static final String TABLE_AUTOSAVED_POST = DEFAULT_PREFIX + "AUTOSAVED_POST";
	public static final String TABLE_COMMENT = DEFAULT_PREFIX + "COMMENT";
	public static final String TABLE_AUTHOR = DEFAULT_PREFIX + "AUTHOR";
	public static final String TABLE_POST_GROUP = DEFAULT_PREFIX + "POST_GROUP";
	public static final String TABLE_GROUP_DATA = DEFAULT_PREFIX + "GROUP_DATA";
	public static final String TABLE_GLOBAL_PREFERENCES = DEFAULT_PREFIX + "GLOBAL_PREFERENCES";

	public static final String POST_ID = "POST_ID";

	// From CLOG_POST_GROUP
	public static final String GROUP_ID = "GROUP_ID";

	// From CLOG_POST
	public static final String TITLE = "TITLE";

	public static final String CREATED_DATE = "CREATED_DATE";
	public static final String MODIFIED_DATE = "MODIFIED_DATE";

	// From CLOG_POST
	public static final String VISIBILITY = "VISIBILITY";

	// From CLOG_AUTHOR
	public static final String TOTAL_POSTS = "TOTAL_POSTS";
	public static final String TOTAL_COMMENTS = "TOTAL_COMMENTS";
	public static final String LAST_POST_DATE = "LAST_POST_DATE";

	public static final String USER_ID = "USER_ID";
	public static final String CREATOR_ID = "CREATOR_ID";

	public static final String SHOW_BODY = "SHOW_BODY";

	// From CLOG_POST
	public static final String SITE_ID = "SITE_ID";

	// From CLOG_POST
	public static final String KEYWORDS = "KEYWORDS";

	// From CLOG_POST
	public static final String ALLOW_COMMENTS = "ALLOW_COMMENTS";

	// From CLOG_COMMENT
	public static final String COMMENT_ID = "COMMENT_ID";

	// From CLOG_COMMENT
	public static final String CONTENT = "CONTENT";

	public abstract List<String> getCreateTablesStatements();

	public abstract List<String> getSelectStatementsForQuery(QueryBean query);

	public abstract String getSelectAllPost(String siteId);

	public abstract String getSelectPost(String OID);

	public String getSelectComment(String commentId);

	public String getSelectComments(String postId);

	public String getSelectGroups(String postId);

	public abstract List<PreparedStatement> getInsertStatementsForPost(Post post, Connection connection) throws Exception;

	public abstract List<PreparedStatement> getSaveStatementsForComment(Comment comment, Connection connection) throws Exception;

	public abstract List<PreparedStatement> getDeleteStatementsForPost(Post post, Connection connection) throws Exception;

	public abstract String getSelectPublicBloggers();

	public abstract String getCountPublicPosts(String userId);

	public abstract List<PreparedStatement> getDeleteStatementsForComment(String commentId, Connection connection) throws Exception;

	public abstract List<PreparedStatement> getRecycleStatementsForPost(Post post, Connection connection) throws Exception;

	public abstract List<PreparedStatement> getRestoreStatementsForPost(Post post, Connection connection) throws Exception;

	public abstract String getSelectAuthorStatement(String userId, String siteId);

	public abstract PreparedStatement getDeleteAutosavedCopyStatement(String postId, Connection connection) throws Exception;

	public abstract PreparedStatement getSelectAutosavedPost(String postId, Connection connection) throws Exception;

	public abstract List<PreparedStatement> getInsertStatementsForAutoSavedPost(Post post, Connection connection) throws Exception;
}
