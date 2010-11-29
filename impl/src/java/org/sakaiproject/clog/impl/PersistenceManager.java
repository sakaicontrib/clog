package org.sakaiproject.clog.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.sakaiproject.clog.api.datamodel.Comment;
import org.sakaiproject.clog.api.sql.ISQLGenerator;
import org.sakaiproject.clog.api.datamodel.Post;
import org.sakaiproject.clog.api.datamodel.Preferences;
import org.sakaiproject.clog.api.datamodel.Visibilities;
import org.sakaiproject.clog.impl.sql.HiperSonicGenerator;
import org.sakaiproject.clog.impl.sql.MySQLGenerator;
import org.sakaiproject.clog.impl.sql.OracleSQLGenerator;
import org.sakaiproject.clog.api.ClogMember;
import org.sakaiproject.clog.api.QueryBean;
import org.sakaiproject.clog.api.SakaiProxy;

public class PersistenceManager
{
	private Logger logger = Logger.getLogger(PersistenceManager.class);

	private ISQLGenerator sqlGenerator;

	private SakaiProxy sakaiProxy;

	public PersistenceManager(SakaiProxy sakaiProxy)
	{
		this.sakaiProxy = sakaiProxy;
		
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
			{
				logger.error("Failed to setup the tables");
			}
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
			connection = sakaiProxy.borrowConnection();
			boolean oldAutoCommitFlag = connection.getAutoCommit();
			connection.setAutoCommit(false);
			
			statement = connection.createStatement();

			try
			{
				List<String> statements = sqlGenerator.getCreateTablesStatements();

				for (String sql : statements)
					statement.executeUpdate(sql);

				connection.commit();
				
				return true;
			}
			catch (Exception e)
			{
				logger.error("Caught exception whilst setting up tables. Message: " + e.getMessage() + ". Rolling back ...");
				connection.rollback();
			}
			finally
			{
				connection.setAutoCommit(oldAutoCommitFlag);
			}
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst setting up tables. Message: " + e.getMessage());
		}
		finally
		{
			if(statement != null)
			{
				try
				{
					statement.close();
				}
				catch (Exception e) {}
			}
			
			sakaiProxy.returnConnection(connection);
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
			connection = sakaiProxy.borrowConnection();
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
			
			sakaiProxy.returnConnection(connection);
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
		ResultSet rs = null;
		
		try
		{
			connection = sakaiProxy.borrowConnection();
			statement = connection.createStatement();
			rs = statement.executeQuery(sqlGenerator.getSelectAllPost(placementId));
			result = transformResultSetInPostCollection(rs, connection);
		}
		finally
		{
			if(rs != null)
			{
				try
				{
					rs.close();
				}
				catch(Exception e) {}
			}
			
			if(statement != null)
			{
				try
				{
					statement.close();
				}
				catch(Exception e) {}
			}
			
			sakaiProxy.returnConnection(connection);
		}

		return result;
	}

	public boolean saveComment(Comment comment)
	{
		Connection connection = null;
		List<PreparedStatement> statements = null;

		try
		{
			connection = sakaiProxy.borrowConnection();
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
			
			sakaiProxy.returnConnection(connection);
		}
		
		return false;
	}

	public boolean deleteComment(String commentId)
	{
		Connection connection = null;
		List<PreparedStatement> statements = null;

		try
		{
			connection = sakaiProxy.borrowConnection();
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
			
			sakaiProxy.returnConnection(connection);
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
			connection = sakaiProxy.borrowConnection();
			boolean oldAutoCommit = connection.getAutoCommit();
			connection.setAutoCommit(false);

			try
			{
				if(post.isAutoSave())
					statements = sqlGenerator.getInsertStatementsForAutoSavedPost(post, connection);
				else
					statements = sqlGenerator.getInsertStatementsForPost(post, connection);
				
				for(PreparedStatement st : statements)
					st.executeUpdate();
				
				if(post.isPublic()) {
					String accessUrl = sakaiProxy.getAccessUrl();
					String content = post.getContent();
					Pattern p = Pattern.compile(accessUrl + "/content(/user/" + sakaiProxy.getCurrentUserEid() + "[^\"]*)\"");
					Matcher m = p.matcher(content);
					while(m.find()) {
						String contentId = m.group(1);
						if(!sakaiProxy.makeResourcePublic(contentId))
							throw new Exception("Failed to make embedded resource public");
					}
				}
				
				connection.commit();
				
				//post.setModifiedDate(new Date().getTime());
				
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
			
			sakaiProxy.returnConnection(connection);
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
			connection = sakaiProxy.borrowConnection();
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

			sakaiProxy.returnConnection(connection);
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
			connection = sakaiProxy.borrowConnection();
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

			sakaiProxy.returnConnection(connection);
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
			connection = sakaiProxy.borrowConnection();
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

			sakaiProxy.returnConnection(connection);
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
			connection = sakaiProxy.borrowConnection();
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

			sakaiProxy.returnConnection(connection);
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
			connection = sakaiProxy.borrowConnection();
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

			sakaiProxy.returnConnection(connection);
		}
	}
	
	public Post getAutosavedPost(String postId)
	{
		if (logger.isDebugEnabled())
			logger.debug("getAutosavedPost(" + postId + ")");

		Connection connection = null;
		PreparedStatement st = null;
		try
		{
			connection = sakaiProxy.borrowConnection();
			st = sqlGenerator.getSelectAutosavedPost(postId,connection);
			ResultSet rs = st.executeQuery();
			List<Post> posts = transformResultSetInPostCollection(rs, connection);
			rs.close();

			if (posts.size() == 0)
			{
				if(logger.isInfoEnabled()) logger.info("getAutosavedPost: Unable to find post with id:" + postId);
				return null;
			}
			if (posts.size() > 1)
			{
				logger.error("getAutosavedPost: there is more than one post with id:" + postId);
				return null;
			}

			return posts.get(0);
		}
		catch(Exception e)
		{
			logger.error("Caught exception whilst getting autosaved post",e);
			return null;
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

			sakaiProxy.returnConnection(connection);
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
				
				String visibility = rs.getString(ISQLGenerator.VISIBILITY);
				post.setVisibility(visibility);
				
				if(!post.isAutoSave())
					post.setAutosavedVersion(getAutosavedPost(postId));
				
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

	public void setSakaiProxy(SakaiProxy sakaiProxy)
	{
		this.sakaiProxy = sakaiProxy;
	}

	public SakaiProxy getSakaiProxy()
	{
		return sakaiProxy;
	}

	public List<ClogMember> getPublicBloggers()
	{
		List<ClogMember> members = new ArrayList<ClogMember>();

		Connection connection = null;
		Statement publicST = null;
		Statement countST = null;

		try
		{
			connection = sakaiProxy.borrowConnection();
			publicST = connection.createStatement();
			countST = connection.createStatement();
			String sql = sqlGenerator.getSelectPublicBloggers();
			ResultSet rs = publicST.executeQuery(sql);

			while (rs.next())
			{
				String userId = rs.getString(ISQLGenerator.CREATOR_ID);
				ClogMember member = sakaiProxy.getMember(userId);
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
			
			sakaiProxy.returnConnection(connection);
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
			connection = sakaiProxy.borrowConnection();
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
			
			sakaiProxy.returnConnection(connection);
		}

		return preferences;
	}

	public boolean savePreferences(Preferences preferences)
	{
		Connection connection = null;
		PreparedStatement st = null;
		
		try
		{
			connection = sakaiProxy.borrowConnection();
			st = sqlGenerator.getSavePreferencesStatement(preferences, connection);
			st.executeUpdate();
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
			
			sakaiProxy.returnConnection(connection);
		}
	}

	public boolean postExists(String postId) throws Exception
	{
		Connection connection = null;
		Statement st = null;
		
		try
		{
			connection = sakaiProxy.borrowConnection();
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
			
			sakaiProxy.returnConnection(connection);
		}
	}

	public boolean populateAuthorData(ClogMember author,String siteId)
	{
		Connection connection = null;
		Statement st = null;
		
		try
		{
			connection = sakaiProxy.borrowConnection();
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
			
			sakaiProxy.returnConnection(connection);
		}
	}
	
	public boolean importBlog2Data()
	{
		if(logger.isDebugEnabled())
		{
			logger.debug("Starting import of blog 2 data ...");
		}
		
		Connection connection = null;
		Statement postST = null;
		Statement testST = null;
		Statement postElementST = null;
		Statement elementST = null;
		Statement commentST = null;
		
		int numberImported = 0;
		
		try
		{
			connection = sakaiProxy.borrowConnection();
			
			postST = connection.createStatement();
			testST = connection.createStatement();
			postElementST = connection.createStatement();
			elementST = connection.createStatement();
			commentST = connection.createStatement();
			
			ResultSet rs = postST.executeQuery("SELECT * FROM BLOG_POST");
			
			while(rs.next())
			{
				String siteId = rs.getString(ISQLGenerator.SITE_ID);
				
				ResultSet testRS = testST.executeQuery("SELECT * FROM BLOG_OPTIONS WHERE SITE_ID = '" + siteId + "'");
				
				if(testRS.next() && "LEARNING_LOG".equals(testRS.getString("BLOGMODE")))
				{
					testRS.close();
					continue;
				}
				
				boolean brokenPost = false;
				
				Post post = new Post();

				String postId = rs.getString(ISQLGenerator.POST_ID);
				//post.setId(postId);
				
				post.setSiteId(siteId);

				String title = rs.getString(ISQLGenerator.TITLE);
				post.setTitle(title);

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
				
				if("PUBLIC".equals(visibility))
					visibility = "READY";
				
				post.setVisibility(visibility);
				
				String shortText = rs.getString("SHORT_TEXT");
				
				String collectedMarkup = "<i>" + shortText + "</i><br /><br />";
				
				ResultSet postElementRS = postElementST.executeQuery("SELECT * FROM BLOG_POST_ELEMENT WHERE POST_ID = '" + postId + "' ORDER BY POSITION");
				while(postElementRS.next())
				{
					String elementId = postElementRS.getString("ELEMENT_ID");
					String elementType = postElementRS.getString("ELEMENT_TYPE").trim();
					String displayName = postElementRS.getString("DISPLAY_NAME");
					
					if("PARAGRAPH".equals(elementType))
					{
						ResultSet elementRS = elementST.executeQuery("SELECT CONTENT FROM BLOG_PARAGRAPH WHERE PARAGRAPH_ID = '" + elementId + "'");
						if(!elementRS.next())
						{
							logger.error("Inconsistent Database. Post ID: " + postId + ". Post Title: " + post.getTitle() + ". No paragraph element found for post element with id '" + elementId + "'. Skipping this post ...");
							brokenPost = true;
							continue;
						}
						
						String content = elementRS.getString("CONTENT");
						collectedMarkup += content + "<br /><br />";
						elementRS.close();
					}
					else if("LINK".equals(elementType))
					{
						ResultSet elementRS = elementST.executeQuery("SELECT URL FROM BLOG_LINK WHERE LINK_ID = '" + elementId + "'");
						if(!elementRS.next())
						{
							logger.error("Inconsistent Database. Post ID: " + postId + ". Post Title: " + post.getTitle() + ". No link element found for post element with id '" + elementId + "'. Skipping this post ...");
							brokenPost = true;
							continue;
						}
						
						String href = elementRS.getString("URL");
						String link = "<a href=\"" + href + "\">" + displayName + "</a><br /><br />";
						collectedMarkup += link;
						elementRS.close();
					}
					else if("IMAGE".equals(elementType))
					{
						ResultSet elementRS = elementST.executeQuery("SELECT * FROM BLOG_IMAGE WHERE IMAGE_ID = '" + elementId + "'");
						if(!elementRS.next())
						{
							logger.error("Inconsistent Database. Post ID: " + postId + ". Post Title: " + post.getTitle() + ". No image element found for post element with id '" + elementId + "'. Skipping this post ...");
							brokenPost = true;
							continue;
						}
						
						String fullResourceId = elementRS.getString("FULL_RESOURCE_ID");
						String webResourceId = elementRS.getString("WEB_RESOURCE_ID");
						String fullUrl = sakaiProxy.getServerUrl() + "/access/content" + fullResourceId;
						String webUrl = sakaiProxy.getServerUrl() + "/access/content" + webResourceId;
						
						String img = "<img src=\"" + webUrl + "\" onclick=\"window.open('"+ fullUrl + "','Full Image','width=400,height=300,status=no,resizable=yes,location=no,scrollbars=yes');\"/><br />";
						
						collectedMarkup += img + "<br />";
					}
					else if("FILE".equals(elementType))
					{
						ResultSet elementRS = elementST.executeQuery("SELECT * FROM BLOG_FILE WHERE FILE_ID = '" + elementId + "'");
						if(!elementRS.next())
						{
							logger.error("Inconsistent Database. Post ID: " + postId + ". Post Title: " + post.getTitle() + ". No file element found for post element with id '" + elementId + "'. Skipping this post ...");
							brokenPost = true;
							continue;
						}
						
						String resourceId = elementRS.getString("RESOURCE_ID");
						
						String file = "<a href=\"" + sakaiProxy.getServerUrl() + "/access/content" + resourceId + "\">" + displayName + "</a><br /><br />";
						
						collectedMarkup += file;
					}
				} // while(postElementRS.next())
				
				postElementRS.close();
				
				post.setContent(collectedMarkup);
				
				if(!brokenPost && savePost(post))
				{
					numberImported++;
					
					ResultSet commentRS = commentST.executeQuery("SELECT * FROM BLOG_COMMENT WHERE POST_ID = '" + postId + "'");
				
					while(commentRS.next())
					{
						Comment comment = new Comment();
					
						String creatorId = commentRS.getString(ISQLGenerator.CREATOR_ID);
						comment.setCreatorId(creatorId);
						Date commentCreatedDate = commentRS.getTimestamp(ISQLGenerator.CREATED_DATE);
						comment.setCreatedDate(commentCreatedDate.getTime());

						Date commentModifiedDate = commentRS.getTimestamp(ISQLGenerator.MODIFIED_DATE);
						comment.setModifiedDate(commentModifiedDate.getTime());
					
						String content = commentRS.getString("CONTENT");
						comment.setContent(content);
					
						comment.setPostId(post.getId());
					
						saveComment(comment);
					} // while(commentRS.next())
				
					commentRS.close();
				}
			}
			
			rs.close();
			
			if(logger.isDebugEnabled())
			{
				logger.debug("Finished import of blog 2 data. " + numberImported + " posts imported.");
			}
			
			return true;
		}
		catch(Exception e)
		{
			logger.error("Exception thrown whilst importing blog 2 data",e);
			return false;
		}
		finally
		{
			if(postST != null)
			{
				try
				{
					postST.close();
				}
				catch (Exception e) {}
			}
			
			if(testST != null)
			{
				try
				{
					testST.close();
				}
				catch (Exception e) {}
			}
			
			if(postElementST != null)
			{
				try
				{
					postElementST.close();
				}
				catch (Exception e) {}
			}
			
			if(elementST != null)
			{
				try
				{
					elementST.close();
				}
				catch (Exception e) {}
			}
			
			if(commentST != null)
			{
				try
				{
					commentST.close();
				}
				catch (Exception e) {}
			}
			
			sakaiProxy.returnConnection(connection);
		}
	}
	
	public boolean importBlog1Data()
	{
		if(logger.isDebugEnabled())
		{
			logger.debug("Starting import of previous blog 1 data ...");
		}
		
		Connection connection = null;
		Statement postST = null;
		Statement imageST = null;
		Statement fileST = null;
		
		int numberImported = 0;
		
		try
		{
			connection = sakaiProxy.borrowConnection();
			
			ImporterSaxParser saxParser = new ImporterSaxParser(connection,sakaiProxy);
			
			postST = connection.createStatement();
			imageST = connection.createStatement();
			fileST = connection.createStatement();
			
			ResultSet rs = postST.executeQuery("SELECT * FROM BLOGGER_POST");
			
			while(rs.next())
			{
				String siteId = rs.getString(ISQLGenerator.SITE_ID);
				
				String title = rs.getString(ISQLGenerator.TITLE);
				
				String postCreatorId = rs.getString("IDCREATOR");
				
				long createdDate = rs.getLong("DATEPOST");
				
				int visibility = rs.getInt("VISIBILITY");
				
				String xml = rs.getString("XML");
				
				Post post = new Post();
				post.setSiteId(siteId);
				
				saxParser.populatePost(xml,post);
				
				if("".equals(post.getCreatorId()) || post.getCreatorId() == null)
					post.setCreatorId(postCreatorId);
				
				if("".equals(post.getTitle()) || post.getTitle() == null)
					post.setTitle(title);
				
				if(-1 == post.getCreatedDate())
					post.setCreatedDate(createdDate);
				
				post.setSiteId(siteId);
				
				if(savePost(post))
				{
					List<Comment> comments = post.getComments();
					
					for(Comment comment : comments)
					{
						comment.setPostId(post.getId());
						saveComment(comment);
					}
					
					numberImported++;
				}
			}
			
			if(logger.isDebugEnabled())
			{
				logger.debug("Finished import of previous blog 1 data. " + numberImported + " posts imported.");
			}
			
			return true;
		}
		catch(Exception e)
		{
			logger.error("Exception thrown whilst importing old blog 1 data",e);
			return false;
		}
		finally
		{
			if(postST != null)
			{
				try
				{
					postST.close();
				}
				catch (Exception e) {}
			}
			if(imageST != null)
			{
				try
				{
					imageST.close();
				}
				catch (Exception e) {}
			}
			if(fileST != null)
			{
				try
				{
					fileST.close();
				}
				catch (Exception e) {}
			}
		}
	}

	public boolean deleteAutosavedCopy(String postId)
	{
		Connection connection = null;
		PreparedStatement st = null;
		
		try
		{
			connection = sakaiProxy.borrowConnection();
			st = sqlGenerator.getDeleteAutosavedCopyStatement(postId,connection);
			st.executeUpdate();
			return true;
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst deleting autosaved copy.", e);
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
			
			sakaiProxy.returnConnection(connection);
		}

		return false;
	}
}
