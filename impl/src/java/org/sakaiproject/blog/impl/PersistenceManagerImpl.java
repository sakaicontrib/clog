package org.sakaiproject.blog.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.sakaiproject.blog.api.BlogMember;
import org.sakaiproject.blog.api.BlogSecurityManager;
import org.sakaiproject.blog.api.PersistenceManager;
import org.sakaiproject.blog.api.QueryBean;
import org.sakaiproject.blog.api.SakaiProxy;
import org.sakaiproject.blog.api.datamodel.Comment;
import org.sakaiproject.blog.api.datamodel.Post;
import org.sakaiproject.blog.api.datamodel.Preferences;
import org.sakaiproject.blog.impl.sql.HiperSonicGenerator;
import org.sakaiproject.blog.api.sql.ISQLGenerator;
import org.sakaiproject.blog.impl.sql.MySQLGenerator;
import org.sakaiproject.blog.impl.sql.OracleSQLGenerator;

public class PersistenceManagerImpl implements PersistenceManager
{
	private Logger logger = Logger.getLogger(PersistenceManagerImpl.class);

	private ISQLGenerator sqlGenerator;

	private SakaiProxy sakaiProxy;

	private BlogSecurityManager securityManager;

	public void init()
	{
		if (logger.isDebugEnabled()) logger.debug("init()");

		String vendor = sakaiProxy.getVendor();

		// TODO load the proper class using reflection. We can use a named based system to locate the correct SQLGenerator
		if (vendor.equals("mysql"))
			sqlGenerator = new MySQLGenerator();
		else if (vendor.equals("oracle"))
			sqlGenerator = new OracleSQLGenerator();
		else if (vendor.equals("hsqldb"))
			sqlGenerator = new HiperSonicGenerator();
		else
		{
			logger.error("Unknown database vendor:" + vendor + ". Defaulting to HypersonicDB ...");
			sqlGenerator = new HiperSonicGenerator();
		}

		if (sakaiProxy.isAutoDDL())
		{
			if(!setupTables())
				logger.error("Failed to setup the tables");
		}
	}

	public boolean setupTables()
	{
		if (logger.isDebugEnabled())
			logger.debug("setupTables()");

		Connection connection = null;
		Statement statement = null;

		try
		{
			connection = getConnection();
			boolean oldAutoCommitFlag = connection.getAutoCommit();
			connection.setAutoCommit(false);
			
			statement = connection.createStatement();

			try
			{
				List<String> statements = sqlGenerator.getCreateStatementsForPost();

				for (String sql : statements)
					statement.executeUpdate(sql);

				connection.commit();
				
				return true;
			}
			catch (Exception e)
			{
				logger.error("Caught exception whilst setting up tables. Rolling back ...", e);
				connection.rollback();
			}
			finally
			{
				connection.setAutoCommit(oldAutoCommitFlag);
			}
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst setting up tables", e);
		}
		finally
		{
			if(statement != null)
			{
				try
				{
					statement.close();
				}
				catch (SQLException e) {}
			}
			
			releaseConnection(connection);
		}
		
		return false;
	}

	public boolean existPost(String OID) throws Exception
	{
		if (logger.isDebugEnabled())
			logger.debug("existPost(" + OID + ")");

		Connection connection = null;
		Statement statement = null;

		try
		{
			connection = getConnection();
			statement = connection.createStatement();
			String sql = sqlGenerator.getSelectPost(OID);
			ResultSet rs = statement.executeQuery(sql);
			boolean exists = rs.next();
			rs.close();
			return exists;
		}
		finally
		{
			if(statement != null)
			{
				try
				{
					statement.close();
				}
				catch (SQLException e) {}
			}
			
			releaseConnection(connection);
		}
	}

	public List<Post> getAllPost(String placementId) throws Exception
	{
		return getAllPost(placementId, false);
	}

	public List<Post> getAllPost(String placementId, boolean populate) throws Exception
	{
		if (logger.isDebugEnabled())
			logger.debug("getAllPost(" + placementId + ")");

		List<Post> result = new ArrayList<Post>();

		Connection connection = null;
		Statement statement = null;
		
		try
		{
			connection = getConnection();
			statement = connection.createStatement();
			ResultSet rs = statement.executeQuery(sqlGenerator.getSelectAllPost(placementId));
			result = transformResultSetInPostCollection(rs, connection);
			rs.close();
		}
		finally
		{
			if(statement != null)
			{
				try
				{
					statement.close();
				}
				catch (SQLException e) {}
			}
			
			releaseConnection(connection);
		}

		return result;
	}

	public boolean saveComment(Comment comment)
	{
		Connection connection = null;
		List<PreparedStatement> statements = null;

		try
		{
			connection = getConnection();
			boolean oldAutoCommit = connection.getAutoCommit();
			connection.setAutoCommit(false);

			try
			{

				statements = sqlGenerator.getSaveStatementsForComment(comment, connection);
				for(PreparedStatement st : statements)
					st.executeUpdate();
				
				connection.commit();
				
				return true;
			}
			catch (Exception e)
			{
				logger.error("Caught exception whilst saving comment. Rolling back ...",e);
				connection.rollback();
			}
			finally
			{
				connection.setAutoCommit(oldAutoCommit);
			}
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst saving comment.", e);
		}
		finally
		{
			if(statements != null)
			{
				for(PreparedStatement st: statements)
				{
					try
					{
						st.close();
					}
					catch (SQLException e) {}
				}
			}
			
			releaseConnection(connection);
		}
		
		return false;
	}

	public boolean deleteComment(String commentId)
	{
		Connection connection = null;
		List<PreparedStatement> statements = null;

		try
		{
			connection = getConnection();
			boolean oldAutoCommit = connection.getAutoCommit();
			connection.setAutoCommit(false);

			try
			{

				statements = sqlGenerator.getDeleteStatementsForComment(commentId, connection);
				for(PreparedStatement st : statements)
					st.executeUpdate();
				
				connection.commit();
				
				return true;
			}
			catch (Exception e)
			{
				logger.error("Caught exception whilst deleting comment. Rolling back ...",e);
				connection.rollback();
			}
			finally
			{
				connection.setAutoCommit(oldAutoCommit);
			}
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst deleting comment.", e);
		}
		finally
		{
			if(statements != null)
			{
				for(PreparedStatement st: statements)
				{
					try
					{
						st.close();
					}
					catch (SQLException e) {}
				}
			}
			
			releaseConnection(connection);
		}
		
		return false;
	}

	public boolean savePost(Post post)
	{
		if (logger.isDebugEnabled()) logger.debug("createPost()");

		Connection connection = null;
		List<PreparedStatement> statements = null;

		try
		{
			connection = getConnection();
			boolean oldAutoCommit = connection.getAutoCommit();
			connection.setAutoCommit(false);

			try
			{
				statements = sqlGenerator.getInsertStatementsForPost(post, connection);
				
				for(PreparedStatement st : statements)
					st.executeUpdate();
				
				connection.commit();
				
				post.setModifiedDate(new Date().getTime());
				
				return true;
			}
			catch (Exception e)
			{
				logger.error("Caught exception whilst saving post. Rolling back ...",e);
				connection.rollback();
			}
			finally
			{
				connection.setAutoCommit(oldAutoCommit);
			}
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst saving post.", e);
		}
		finally
		{
			if(statements != null)
			{
				for(PreparedStatement st: statements)
				{
					try
					{
						st.close();
					}
					catch (SQLException e) {}
				}
			}
			
			releaseConnection(connection);
		}
		
		return false;
	}

	public boolean deletePost(Post post)
	{
		if (logger.isDebugEnabled())
			logger.debug("deletePost(" + post.getId() + ")");

		Connection connection = null;
		List<PreparedStatement> statements = null;
		
		try
		{
			connection = getConnection();
			boolean oldAutoCommit = connection.getAutoCommit();
			connection.setAutoCommit(false);
			
			try
			{
				statements = sqlGenerator.getDeleteStatementsForPost(post, connection);
				
				for(PreparedStatement st : statements)
					st.executeUpdate();

				connection.commit();
				
				return true;
			}
			catch (Exception e)
			{
				logger.error("Caught exception whilst deleting post. Rolling back ...",e);
				connection.rollback();
			}
			finally
			{
				connection.setAutoCommit(oldAutoCommit);
			}
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst deleting post.", e);
		}
		finally
		{
			if(statements != null)
			{
				for(PreparedStatement st : statements)
				{
					try
					{
						st.close();
					}
					catch (Exception e) {}
				}
			}

			releaseConnection(connection);
		}
		
		return false;
	}

	public boolean recyclePost(Post post)
	{
		if (logger.isDebugEnabled())
			logger.debug("recyclePost(" + post.getId() + ")");

		Connection connection = null;
		List<PreparedStatement> statements = null;
		
		try
		{
			connection = getConnection();
			boolean oldAutoCommit = connection.getAutoCommit();
			connection.setAutoCommit(false);

			try
			{
				statements = sqlGenerator.getRecycleStatementsForPost(post,connection);
				for(PreparedStatement st : statements)
					st.executeUpdate();
				connection.commit();
				return true;
			}
			catch (Exception e)
			{
				logger.error("Caught exception whilst recycling post. Rolling back ...",e);
				connection.rollback();
			}
			finally
			{
				connection.setAutoCommit(oldAutoCommit);
			}
		}
		catch(Exception e)
		{
			logger.error("Caught exception whilst recycling post.",e);
			return false;
		}
		finally
		{
			if(statements != null)
			{
				for(PreparedStatement st: statements)
				{
					try
					{
						st.close();
					}
					catch (SQLException e) {}
				}
			}

			releaseConnection(connection);
		}
		
		return false;
	}
	
	public boolean restorePost(Post post)
	{
		if (logger.isDebugEnabled())
			logger.debug("restore(" + post.getId() + ")");

		Connection connection = null;
		List<PreparedStatement> statements = null;
		
		try
		{
			connection = getConnection();
			boolean oldAutoCommit = connection.getAutoCommit();
			connection.setAutoCommit(false);

			try
			{
				statements = sqlGenerator.getRestoreStatementsForPost(post,connection);
				for(PreparedStatement st : statements)
					st.executeUpdate();
				connection.commit();
				return true;
			}
			catch (Exception e)
			{
				logger.error("Caught exception whilst recycling post. Rolling back ...",e);
				connection.rollback();
			}
			finally
			{
				connection.setAutoCommit(oldAutoCommit);
			}
		}
		catch(Exception e)
		{
			logger.error("Caught exception whilst recycling post.",e);
			return false;
		}
		finally
		{
			if(statements != null)
			{
				for(PreparedStatement st: statements)
				{
					try
					{
						st.close();
					}
					catch (SQLException e) {}
				}
			}

			releaseConnection(connection);
		}
		
		return false;
	}

	public List<Post> getPosts(QueryBean query) throws Exception
	{
		List<Post> posts = new ArrayList<Post>();

		Connection connection = null;
		Statement st = null;
		
		try
		{
			connection = getConnection();
			st = connection.createStatement();
			List<String> sqlStatements = sqlGenerator.getSelectStatementsForQuery(query);
			for (String sql : sqlStatements)
			{
				ResultSet rs = st.executeQuery(sql);
				posts.addAll(transformResultSetInPostCollection(rs, connection));
				rs.close();
			}
		}
		finally
		{
			if(st != null)
			{
				try
				{
					st.close();
				}
				catch (Exception e) {}
			}

			releaseConnection(connection);
		}

		return posts;
	}

	public Post getPost(String postId) throws Exception
	{
		if (logger.isDebugEnabled())
			logger.debug("getPost(" + postId + ")");

		Connection connection = null;
		Statement st = null;
		try
		{
			connection = getConnection();
			st = connection.createStatement();
			String sql = sqlGenerator.getSelectPost(postId);
			ResultSet rs = st.executeQuery(sql);
			List<Post> posts = transformResultSetInPostCollection(rs, connection);
			rs.close();

			if (posts.size() == 0)
				throw new Exception("getPost: Unable to find post with id:" + postId);
			if (posts.size() > 1)
				throw new Exception("getPost: there is more than one post with id:" + postId);

			return posts.get(0);
		}
		finally
		{
			if(st != null)
			{
				try
				{
					st.close();
				}
				catch (Exception e) {}
			}

			releaseConnection(connection);
		}
	}

	private List<Post> transformResultSetInPostCollection(ResultSet rs, Connection connection) throws Exception
	{
		List<Post> result = new ArrayList<Post>();
		
		if (rs == null)
			return result;
		
		Statement commentST = null;
		
		try
		{
			commentST = connection.createStatement();
			
			while (rs.next())
			{
				Post post = new Post();

				String postId = rs.getString(ISQLGenerator.POST_ID);
				post.setId(postId);
				
				String siteId = rs.getString(ISQLGenerator.SITE_ID);
				post.setSiteId(siteId);

				String title = rs.getString(ISQLGenerator.TITLE);
				post.setTitle(title);
				
				String content = rs.getString(ISQLGenerator.CONTENT);
				post.setContent(content);

				Date postCreatedDate = rs.getTimestamp(ISQLGenerator.CREATED_DATE);
				post.setCreatedDate(postCreatedDate.getTime());

				Date postModifiedDate = rs.getTimestamp(ISQLGenerator.MODIFIED_DATE);
				post.setModifiedDate(postModifiedDate.getTime());

				String postCreatorId = rs.getString(ISQLGenerator.CREATOR_ID);
				post.setCreatorId(postCreatorId);
				
				String keywords = rs.getString(ISQLGenerator.KEYWORDS);
				post.setKeywords(keywords);

				int allowComments = rs.getInt(ISQLGenerator.ALLOW_COMMENTS);
				post.setCommentable(allowComments == 1);
				
				String visibility = rs.getString(ISQLGenerator.VISIBILITY);
				post.setVisibility(visibility);

				String sql = sqlGenerator.getSelectComments(postId);
				ResultSet commentRS = commentST.executeQuery(sql);

				while (commentRS.next())
				{
					String commentId = commentRS.getString(ISQLGenerator.COMMENT_ID);
					String commentCreatorId = commentRS.getString(ISQLGenerator.CREATOR_ID);
					Date commentCreatedDate = commentRS.getTimestamp(ISQLGenerator.CREATED_DATE);
					Date commentModifiedDate = commentRS.getTimestamp(ISQLGenerator.MODIFIED_DATE);
					String commentContent = commentRS.getString(ISQLGenerator.CONTENT);

					Comment comment = new Comment();
					comment.setId(commentId);
					comment.setPostId(post.getId());
					comment.setCreatorId(commentCreatorId);
					comment.setCreatedDate(commentCreatedDate.getTime());
					comment.setContent(commentContent);
					comment.setModifiedDate(commentModifiedDate.getTime());
					comment.setCreatorDisplayName(sakaiProxy.getDisplayNameForTheUser(comment.getCreatorId()));

					post.addComment(comment);
				}

				commentRS.close();
				
				post.setCreatorDisplayName(sakaiProxy.getDisplayNameForTheUser(post.getCreatorId()));

				result.add(post);
			}
		}
		finally
		{
			if(commentST != null)
			{
				try
				{
					commentST.close();
				}
				catch (Exception e) {}
			}
		}

		return result;
	}

	private void releaseConnection(Connection connection)
	{
		if (logger.isDebugEnabled())
			logger.debug("releaseConnection()");

		try
		{
			sakaiProxy.returnConnection(connection);
		}
		catch (Exception e) {}
	}

	private Connection getConnection() throws Exception
	{
		if (logger.isDebugEnabled())
			logger.debug("getConnection()");

		return sakaiProxy.borrowConnection();
	}

	public void setSakaiProxy(SakaiProxy sakaiProxy)
	{
		this.sakaiProxy = sakaiProxy;
	}

	public SakaiProxy getSakaiProxy()
	{
		return sakaiProxy;
	}

	public void setSecurityManager(BlogSecurityManager securityManager)
	{
		this.securityManager = securityManager;
	}

	public BlogSecurityManager getSecurityManager()
	{
		return securityManager;
	}

	public List<BlogMember> getPublicBloggers()
	{
		List<BlogMember> members = new ArrayList<BlogMember>();

		Connection connection = null;
		Statement publicST = null;
		Statement countST = null;

		try
		{
			connection = getConnection();
			publicST = connection.createStatement();
			countST = connection.createStatement();
			String sql = sqlGenerator.getSelectPublicBloggers();
			ResultSet rs = publicST.executeQuery(sql);

			while (rs.next())
			{
				String userId = rs.getString(ISQLGenerator.CREATOR_ID);
				BlogMember member = sakaiProxy.getMember(userId);
				String countPostsQuery = sqlGenerator.getCountPublicPosts(userId);
				ResultSet countRS = countST.executeQuery(countPostsQuery);

				countRS.next();

				int count = countRS.getInt("NUMBER_POSTS");

				countRS.close();
				member.setNumberOfPosts(count);

				members.add(member);
			}

			rs.close();
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst getting public bloggers.", e);
		}
		finally
		{
			if(publicST != null)
			{
				try
				{
					publicST.close();
				}
				catch (Exception e) {}
			}
			
			if(countST != null)
			{
				try
				{
					countST.close();
				}
				catch (Exception e) {}
			}
			
			releaseConnection(connection);
		}
		return members;
	}

	public Preferences getPreferences(String siteId,String userId)
	{
		Preferences preferences = new Preferences();

		preferences.setSiteId(siteId);
		preferences.setUserId(userId);

		Connection connection = null;
		Statement st = null;
		
		try
		{
			connection = getConnection();
			st = connection.createStatement();
			String sql = sqlGenerator.getSelectPreferencesStatement(userId, siteId);
			ResultSet rs = st.executeQuery(sql);

			if (rs.next())
			{
				String emailFrequency = rs.getString(ISQLGenerator.EMAIL_FREQUENCY);
				preferences.setEmailFrequency(emailFrequency);
			}

			rs.close();
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst getting preferences.", e);
		}
		finally
		{
			if(st != null)
			{
				try
				{
					st.close();
				}
				catch (Exception e) {}
			}
			
			releaseConnection(connection);
		}

		return preferences;
	}

	public boolean savePreferences(Preferences preferences)
	{
		Connection connection = null;
		Statement st = null;
		
		try
		{
			connection = getConnection();
			st = connection.createStatement();
			String sql = sqlGenerator.getSavePreferencesStatement(preferences, connection);
			st.executeUpdate(sql);
			return true;
		}
		catch(Exception e)
		{
			logger.error("Caught exception whilst saving preferences.", e);
			return false;
		}
		finally
		{
			if(st != null)
			{
				try
				{
					st.close();
				}
				catch (Exception e) {}
			}
			
			releaseConnection(connection);
		}
	}

	public boolean postExists(String postId) throws Exception
	{
		Connection connection = null;
		Statement st = null;
		
		try
		{
			connection = getConnection();
			st = connection.createStatement();
			String sql = sqlGenerator.getSelectPost(postId);
			ResultSet rs = st.executeQuery(sql);
			boolean exists = rs.next();
			rs.close();
			return exists;
		}
		finally
		{
			if(st != null)
			{
				try
				{
					st.close();
				}
				catch (Exception e) {}
			}
			
			releaseConnection(connection);
		}
	}

	public boolean populateAuthorData(BlogMember author,String siteId)
	{
		Connection connection = null;
		Statement st = null;
		
		try
		{
			connection = getConnection();
			st = connection.createStatement();
			String sql = sqlGenerator.getSelectAuthorStatement(author.getUserId(),siteId);
			ResultSet rs = st.executeQuery(sql);
			if(rs.next())
			{
				int totalPosts = rs.getInt(ISQLGenerator.TOTAL_POSTS);
				int totalComments = rs.getInt(ISQLGenerator.TOTAL_COMMENTS);
				long lastPostDate = rs.getTimestamp(ISQLGenerator.LAST_POST_DATE).getTime();
				author.setNumberOfPosts(totalPosts);
				author.setNumberOfComments(totalComments);
				author.setDateOfLastPost(lastPostDate);
			}
			
			rs.close();
			
			return true;
		}
		catch(Exception e)
		{
			return false;
		}
		finally
		{
			if(st != null)
			{
				try
				{
					st.close();
				}
				catch (Exception e) {}
			}
			
			releaseConnection(connection);
		}
	}
}
