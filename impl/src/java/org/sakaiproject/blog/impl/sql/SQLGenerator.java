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

package org.sakaiproject.blog.impl.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.sakaiproject.blog.api.QueryBean;
import org.sakaiproject.blog.api.sql.ISQLGenerator;
import org.sakaiproject.blog.api.datamodel.Comment;
import org.sakaiproject.blog.api.datamodel.Post;
import org.sakaiproject.blog.api.datamodel.Preferences;
import org.sakaiproject.blog.api.datamodel.Visibilities;

public class SQLGenerator implements ISQLGenerator
{
	// by default, oracle values
	public String BLOB = "BLOB";

	public String BIGINT = "NUMBER";

	public String CLOB = "CLOB";

	public String TIMESTAMP = "DATETIME";

	public String AUTO_TIMESTAMP = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP";

	public String VARCHAR = "VARCHAR";

	public String TEXT = "TEXT";

	/*
	 * (non-Javadoc)
	 * 
	 * @see uk.ac.lancs.e_science.sakaiproject.component.blogger.persistence.sql.util.ISQLGenerator#getCreateStatementsForPost(java.lang.String)
	 */
	public List<String> getCreateTablesStatements()
	{
		ArrayList result = new ArrayList();

		result.add(doTableForPost());
		result.add(doTableForComments());
		result.add(doTableForAuthor());
		result.add(doTableForPreferences());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see uk.ac.lancs.e_science.sakaiproject.component.blogger.persistence.sql.util.ISQLGenerator#getSelectStatementForQuery(java.lang.String,
	 *      uk.ac.lancs.e_science.sakaiproject.service.blogger.searcher.QueryBean, java.lang.String)
	 */
	public List<String> getSelectStatementsForQuery(QueryBean query)
	{
		List<String> statements = new ArrayList<String>();

		String queryString = query.getQueryString();

		if (queryString != null && queryString.length() > 0)
		{
			String sql = "SELECT *" + " FROM " + TABLE_POST + " WHERE " + TITLE + " LIKE '%" + queryString + "%'";

			if (query.queryBySiteId())
				sql += " AND " + SITE_ID + " = '" + query.getSiteId() + "'";

			sql += " ORDER BY " + CREATED_DATE + " DESC";

			statements.add(sql);

			sql = "SELECT DISTINCT " + TABLE_POST + ".*" + " FROM " + TABLE_POST + "," + TABLE_COMMENT + " WHERE " + TABLE_POST + "." + POST_ID + " = " + TABLE_COMMENT + "." + POST_ID;

			if (query.queryBySiteId())
				sql += " AND " + TABLE_POST + "." + SITE_ID + " = '" + query.getSiteId() + "'";

			sql += " AND " + TABLE_COMMENT + "." + CONTENT + " like '%" + queryString + "%'" + " ORDER BY " + CREATED_DATE + " DESC";

			statements.add(sql);

			return statements;
		}

		StringBuilder statement = new StringBuilder();
		statement.append("SELECT * FROM ").append(TABLE_POST);

		if (query.hasConditions())
			statement.append(" WHERE ");

		// we know that there are conditions. Build the statement
		if (query.queryBySiteId())
			statement.append(SITE_ID).append(" = '").append(query.getSiteId()).append("' AND ");

		if (query.queryByCreator())
			statement.append(CREATOR_ID).append(" = '").append(query.getCreator()).append("' AND");

		if (query.queryByVisibility())
		{
			statement.append("(");

			List<String> visibilities = query.getVisibilities();
			int length = visibilities.size();
			for (int i = 0; i < length; i++)
			{
				statement.append(VISIBILITY).append("='").append(visibilities.get(i)).append("'");

				if (i < (length - 1) )
				{
					statement.append(" OR ");
				}
			}

			statement.append(") AND ");
		}

		if (query.queryByInitDate())
			statement.append(CREATED_DATE).append(">='").append(query.getInitDate()).append("' AND ");

		if (query.queryByEndDate())
			statement.append(CREATED_DATE).append("<='").append(query.getEndDate()).append("' AND ");

		// in this point, we know that there is a AND at the end of the statement. Remove it.
		statement = new StringBuilder(statement.toString().substring(0, statement.length() - 4)); // 4 is the length of AND with the last space
		statement.append(" ORDER BY ").append(CREATED_DATE).append(" DESC ");

		statements.add(statement.toString());
		return statements;
	}

	protected String doTableForPost()
	{
		StringBuilder statement = new StringBuilder();
		statement.append("CREATE TABLE ").append(TABLE_POST);
		statement.append("(");
		statement.append(POST_ID + " CHAR(36) NOT NULL,");
		statement.append(SITE_ID + " " + VARCHAR + "(255), ");
		statement.append(TITLE + " " + VARCHAR + "(255) NOT NULL, ");
		statement.append(CONTENT + " " + CLOB + " NOT NULL, ");
		statement.append(CREATED_DATE + " " + TIMESTAMP + " NOT NULL" + ", ");
		statement.append(MODIFIED_DATE + " " + TIMESTAMP + ", ");
		statement.append(CREATOR_ID + " " + VARCHAR + "(255) NOT NULL, ");
		statement.append(KEYWORDS + " " + VARCHAR + "(255), ");
		statement.append(ALLOW_COMMENTS + " INT, ");
		statement.append(VISIBILITY + " " + VARCHAR + "(16) NOT NULL, ");
		statement.append("CONSTRAINT post_pk PRIMARY KEY (" + POST_ID + ")");
		statement.append(")");
		return statement.toString();
	}

	protected String doTableForComments()
	{
		StringBuilder statement = new StringBuilder();
		statement.append("CREATE TABLE ").append(TABLE_COMMENT);
		statement.append("(");
		statement.append(COMMENT_ID + " CHAR(36) NOT NULL,");
		statement.append(POST_ID + " CHAR(36) NOT NULL,");
		statement.append(CREATOR_ID + " CHAR(36) NOT NULL,");
		statement.append(CREATED_DATE + " " + TIMESTAMP + " NOT NULL,");
		statement.append(MODIFIED_DATE + " " + TIMESTAMP + " NOT NULL,");
		statement.append(CONTENT + " " + CLOB + " NOT NULL, ");
		statement.append("CONSTRAINT comment_pk PRIMARY KEY (" + COMMENT_ID + ")");
		statement.append(")");
		return statement.toString();
	}

	protected String doTableForAuthor()
	{
		StringBuilder statement = new StringBuilder();
		statement.append("CREATE TABLE ").append(TABLE_AUTHOR);
		statement.append("(");
		statement.append(USER_ID + " CHAR(36) NOT NULL,");
		statement.append(SITE_ID + " " + VARCHAR + "(255), ");
		statement.append(TOTAL_POSTS + " INT NOT NULL,");
		statement.append(LAST_POST_DATE + " " + TIMESTAMP + ",");
		statement.append(TOTAL_COMMENTS + " INT NOT NULL,");
		statement.append("CONSTRAINT blog_author_pk PRIMARY KEY (" + USER_ID + "," + SITE_ID + ")");
		statement.append(")");
		return statement.toString();
	}

	protected String doTableForPreferences()
	{
		StringBuilder statement = new StringBuilder();
		statement.append("CREATE TABLE ").append(TABLE_PREFERENCES);
		statement.append("(");
		statement.append(USER_ID + " " + VARCHAR + "(36), ");
		statement.append(SITE_ID + " " + VARCHAR + "(255), ");
		statement.append(EMAIL_FREQUENCY + " " + VARCHAR + "(32) NOT NULL,");
		statement.append("CONSTRAINT blog_preferences_pk PRIMARY KEY (" + USER_ID + "," + SITE_ID + ")");
		statement.append(")");
		return statement.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see uk.ac.lancs.e_science.sakaiproject.component.blogger.persistence.sql.util.ISQLGenerator#getSelectAllPost(java.lang.String)
	 */
	public String getSelectComments(String postId)
	{
		return "SELECT * FROM " + TABLE_COMMENT + " WHERE " + POST_ID + "='" + postId + "' ORDER BY " + CREATED_DATE + " ASC";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see uk.ac.lancs.e_science.sakaiproject.component.blogger.persistence.sql.util.ISQLGenerator#getSelectAllPost(java.lang.String)
	 */
	public String getSelectAllPost(String placementId)
	{
		return "SELECT * FROM " + TABLE_POST + " WHERE " + SITE_ID + "='" + placementId + "' ORDER BY " + CREATED_DATE + " DESC";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see uk.ac.lancs.e_science.sakaiproject.component.blogger.persistence.sql.util.ISQLGenerator#getSelectPost(java.lang.String)
	 */
	public String getSelectPost(String OID)
	{
		return "SELECT * FROM " + TABLE_POST + " WHERE " + POST_ID + "='" + OID + "'";
	}

	public List<PreparedStatement> getSaveStatementsForComment(Comment comment, Connection connection) throws Exception
	{
		List<PreparedStatement> statements = new ArrayList<PreparedStatement>();

		Statement testST = null;

		try
		{
			if ("".equals(comment.getId()))
			{
				comment.setId(UUID.randomUUID().toString());

				String sql = "INSERT INTO " + TABLE_COMMENT + " VALUES(?,?,?,?,?,?)";

				PreparedStatement statement = connection.prepareStatement(sql);

				statement.setString(1, comment.getId());
				statement.setString(2, comment.getPostId());
				statement.setString(3, comment.getCreatorId());
				statement.setTimestamp(4, new Timestamp(comment.getCreatedDate()));
				statement.setTimestamp(5, new Timestamp(comment.getModifiedDate()));
				statement.setString(6, comment.getContent());

				statements.add(statement);

				testST = connection.createStatement();
				ResultSet rs = testST.executeQuery("SELECT * FROM " + TABLE_POST + " WHERE " + POST_ID + " = '" + comment.getPostId() + "'");
				if (rs.next())
				{
					String blogCreatorId = rs.getString(CREATOR_ID);
					String siteId = rs.getString(SITE_ID);


					PreparedStatement authorST = connection.prepareStatement("UPDATE " + TABLE_AUTHOR + " SET " + TOTAL_COMMENTS + " = " + TOTAL_COMMENTS + " + 1 WHERE " + USER_ID + " = ? AND " + SITE_ID + " = ?");
					authorST.setString(1, blogCreatorId);
					authorST.setString(2, siteId);

					statements.add(authorST);
				}
				rs.close();
			}
			else
			{
				String sql = "UPDATE " + TABLE_COMMENT + " SET " + CONTENT + " = ?," + MODIFIED_DATE + " = ? WHERE " + COMMENT_ID + " = ?";
				PreparedStatement statement = connection.prepareStatement(sql);

				statement.setString(1, comment.getContent());
				statement.setTimestamp(2, new Timestamp(comment.getModifiedDate()));
				statement.setString(3, comment.getId());
				statements.add(statement);
			}
		}
		finally
		{
			if (testST != null)
			{
				try
				{
					testST.close();
				}
				catch (Exception e) {}
			}
		}

		return statements;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see uk.ac.lancs.e_science.sakaiproject.component.blogger.persistence.sql.util.ISQLGenerator#getDeleteStatementsForPost(java.lang.String, java.lang.String)
	 */
	public List<PreparedStatement> getDeleteStatementsForPost(Post post, Connection connection) throws Exception
	{
		List<PreparedStatement> result = new ArrayList<PreparedStatement>();

		PreparedStatement commentST = connection.prepareStatement("DELETE FROM " + TABLE_COMMENT + " WHERE " + POST_ID + " = ?");
		commentST.setString(1,post.getId());
		result.add(commentST);

		PreparedStatement postST = connection.prepareStatement("DELETE FROM " + TABLE_POST + " WHERE " + POST_ID + " = ?");
		postST.setString(1,post.getId());
		result.add(postST);

		return result;
	}

	public List<PreparedStatement> getRecycleStatementsForPost(Post post, Connection connection) throws Exception
	{
		List<PreparedStatement> statements = new ArrayList<PreparedStatement>();
		PreparedStatement st = connection.prepareStatement("UPDATE " + TABLE_POST + " SET " + VISIBILITY + " = '" + Visibilities.RECYCLED + "' WHERE " + POST_ID + " = ?");
		st.setString(1, post.getId());
		statements.add(st);
		statements.addAll(getAuthorTableStatements(post, false, connection));
		return statements;

	}

	public List<PreparedStatement> getRestoreStatementsForPost(Post post, Connection connection) throws Exception
	{
		List<PreparedStatement> statements = new ArrayList<PreparedStatement>();
		PreparedStatement st = connection.prepareStatement("UPDATE " + TABLE_POST + " SET " + VISIBILITY + " = '" + Visibilities.PRIVATE + "' WHERE " + POST_ID + " = ?");
		st.setString(1, post.getId());
		statements.add(st);
		return statements;

	}

	public List<PreparedStatement> getInsertStatementsForPost(Post post, Connection connection) throws Exception
	{
		List<PreparedStatement> statements = new ArrayList<PreparedStatement>();

		Statement testST = null;

		try
		{
			if ("".equals(post.getId()))
			{
				// Empty post id == new post

				post.setId(UUID.randomUUID().toString());
				String sql = "INSERT INTO " + TABLE_POST + " (" + POST_ID + "," + SITE_ID + "," + TITLE + "," + CONTENT + "," + CREATED_DATE + "," + MODIFIED_DATE + "," + CREATOR_ID + "," + VISIBILITY + "," + KEYWORDS + "," + ALLOW_COMMENTS + ") VALUES (?,?,?,?,?,?,?,?,?,?)";

				PreparedStatement postST = connection.prepareStatement(sql);
				postST.setString(1, post.getId());

				postST.setString(2, post.getSiteId());

				postST.setString(3, post.getTitle());

				postST.setString(4, post.getContent());

				postST.setTimestamp(5, new Timestamp(post.getCreatedDate()));

				postST.setTimestamp(6, new Timestamp(post.getModifiedDate()));

				postST.setString(7, post.getCreatorId());

				postST.setString(8, post.getVisibility());

				postST.setString(9, post.getKeywords());

				postST.setInt(10, (post.isCommentable()) ? 1 : 0);

				statements.add(postST);

				if (post.isReady() || post.isPublic())
				{
					statements.addAll(getAuthorTableStatements(post, true, connection));
				}
			}
			else
			{
				testST = connection.createStatement();
				ResultSet rs = testST.executeQuery("SELECT * FROM " + TABLE_POST + " WHERE " + POST_ID + " = '" + post.getId() + "'");
				
				if (!rs.next())
				{
					rs.close();
					throw new Exception("Failed to get data for post '" + post.getId() + "'");
				}
				
				String currentVisibility = rs.getString(VISIBILITY);
				
				rs.close();
				
				String sql = "UPDATE " + TABLE_POST + " " + "SET " + TITLE + " = ?," + CONTENT + " = ?," + VISIBILITY + " = ?," + MODIFIED_DATE + " = ?," + ALLOW_COMMENTS + " = ? WHERE " + POST_ID + " = ?";

				PreparedStatement postST = connection.prepareStatement(sql);
				postST.setString(1, post.getTitle());
				postST.setString(2, post.getContent());
				postST.setString(3, post.getVisibility());
				post.setModifiedDate(new Date().getTime());
				postST.setTimestamp(4, new Timestamp(post.getModifiedDate()));
				postST.setInt(5, (post.isCommentable()) ? 1 : 0);
				postST.setString(6, post.getId());

				statements.add(postST);

				if (post.isReady() || post.isPublic())
				{
					if (Visibilities.PRIVATE.equals(currentVisibility) || Visibilities.RECYCLED.equals(currentVisibility))
					{
						// This post has been made visible
						statements.addAll(getAuthorTableStatements(post, true, connection));
					}
				}
				else
				{
					if (Visibilities.READY.equals(currentVisibility) || Visibilities.PUBLIC.equals(currentVisibility))
					{
						// This post has been hidden
						statements.addAll(getAuthorTableStatements(post, false, connection));
					}
				}
			}
		}
		finally
		{
			if (testST != null)
			{
				try
				{
					testST.close();
				}
				catch (Exception e) {}
			}
		}

		return statements;
	}

	private List<PreparedStatement> getAuthorTableStatements(Post post, boolean increment, Connection connection) throws Exception
	{
		List<PreparedStatement> statements = new ArrayList<PreparedStatement>();

		Statement testST = null;

		try
		{
			testST = connection.createStatement();
			ResultSet testRS = testST.executeQuery("SELECT COUNT(*) FROM " + TABLE_COMMENT + " WHERE " + POST_ID + " = '" + post.getId() + "'");
			testRS.next();
			int numComments = testRS.getInt(1);
			testRS.close();
			testST.close();
			
			String postAmount = " - 1";
			String commentAmount = " - ";
			if (increment)
			{
				postAmount = " + 1";
				commentAmount = " + ";
			}
			
			commentAmount += numComments;
			
			testST = connection.createStatement();
			testRS = testST.executeQuery("SELECT * FROM " + TABLE_AUTHOR + " WHERE " + USER_ID + " = '" + post.getCreatorId() + "' AND " + SITE_ID + " = '" + post.getSiteId() + "'");

			PreparedStatement totalST = null;
			if (testRS.next())
			{
				totalST = connection.prepareStatement("UPDATE " + TABLE_AUTHOR + " SET " + TOTAL_POSTS + " = " + TOTAL_POSTS + postAmount + "," + TOTAL_COMMENTS + " = " + TOTAL_COMMENTS + commentAmount + " WHERE " + USER_ID + " = ?");
				totalST.setString(1, post.getCreatorId());
				statements.add(totalST);

				PreparedStatement dateST = connection.prepareStatement("UPDATE " + TABLE_AUTHOR + " SET " + LAST_POST_DATE + " = " + "(SELECT MAX(" + CREATED_DATE + ") FROM " + TABLE_POST + " WHERE (" + VISIBILITY + " = 'READY' OR " + VISIBILITY + " = 'PUBLIC')" + " AND " + USER_ID + " = ?" + " AND " + SITE_ID + " = ?)" + " WHERE " + USER_ID + " = ?" + " AND " + SITE_ID + " = ?");
				dateST.setString(1, post.getCreatorId());
				dateST.setString(2, post.getSiteId());
				dateST.setString(3, post.getCreatorId());
				dateST.setString(4, post.getSiteId());
				statements.add(dateST);
			}
			else
			{
				totalST = connection.prepareStatement("INSERT INTO " + TABLE_AUTHOR + " VALUES(?,?,1,?,0)");
				totalST.setString(1, post.getCreatorId());
				totalST.setString(2, post.getSiteId());
				totalST.setTimestamp(3, new Timestamp(post.getCreatedDate()));
				statements.add(totalST);
			}

			testRS.close();
		}
		finally
		{
			if (testST != null)
			{
				try
				{
					testST.close();
				}
				catch (Exception e) {}
			}
		}

		return statements;
	}

	public String getSelectPublicBloggers()
	{
		return "SELECT DISTINCT " + CREATOR_ID + " FROM " + TABLE_POST + " WHERE " + VISIBILITY + " = '" + Visibilities.PUBLIC + "'";
	}

	public String getCountPublicPosts(String creatorId)
	{
		return "SELECT COUNT(*) AS NUMBER_POSTS" + " FROM " + TABLE_POST + " WHERE " + CREATOR_ID + " = '" + creatorId + "'" + " AND " + VISIBILITY + " = '" + Visibilities.PUBLIC + "'";
	}

	public List<PreparedStatement> getDeleteStatementsForComment(String commentId, Connection connection) throws Exception
	{
		List<PreparedStatement> statements = new ArrayList<PreparedStatement>();
		Statement testST = null;

		try
		{
			testST = connection.createStatement();
			ResultSet rs = testST.executeQuery("SELECT tp.CREATOR_ID,tp.SITE_ID FROM " + TABLE_POST + " tp," + TABLE_COMMENT + " tc WHERE tp." + POST_ID + " = tc." + POST_ID + " AND " + COMMENT_ID + " = '" + commentId + "'");
			if (rs.next())
			{
				String blogCreatorId = rs.getString(CREATOR_ID);
				String siteId = rs.getString(SITE_ID);

				PreparedStatement authorST = connection.prepareStatement("UPDATE " + TABLE_AUTHOR + " SET " + TOTAL_COMMENTS + " = " + TOTAL_COMMENTS + " - 1 WHERE " + USER_ID + " = ? AND " + SITE_ID + " = ?");
				authorST.setString(1, blogCreatorId);
				authorST.setString(2, siteId);

				statements.add(authorST);
			}
			
			rs.close();
			
			PreparedStatement commentST = connection.prepareStatement("DELETE FROM " + TABLE_COMMENT + " WHERE " + COMMENT_ID + " = ?");
			commentST.setString(1, commentId);
			statements.add(commentST);
			
			return statements;
		}
		finally
		{
			if(testST != null)
			{
				try
				{
					testST.close();
				}
				catch(Exception e) {}
			}
		}
	}

	public String getSelectPreferencesStatement(String userId, String placementId)
	{
		return "SELECT * FROM " + TABLE_PREFERENCES + " WHERE " + USER_ID + " = '" + userId + "' AND " + SITE_ID + " = '" + placementId + "'";
	}

	public PreparedStatement getSavePreferencesStatement(Preferences preferences, Connection connection) throws Exception
	{
		String userId = preferences.getUserId();
		String siteId = preferences.getSiteId();

		Statement testST = null;
		
		try
		{
			testST = connection.createStatement();
			ResultSet rs = testST.executeQuery("SELECT * FROM " + TABLE_PREFERENCES + " WHERE " + USER_ID + " = '" + userId + "' AND " + SITE_ID + " = '" + siteId + "'");
			
			PreparedStatement st = null;

			if (rs.next())
			{
				String sql = "UPDATE " + TABLE_PREFERENCES + " SET " + EMAIL_FREQUENCY + " = ? WHERE " + USER_ID + " = ? AND " + SITE_ID + " = ?";
				st = connection.prepareStatement(sql);
				st.setString(1,preferences.getEmailFrequency());
				st.setString(2,userId);
				st.setString(3,siteId);
			}
			else
			{
				String sql = "INSERT INTO " + TABLE_PREFERENCES + " (" + USER_ID + "," + SITE_ID + "," + EMAIL_FREQUENCY + ") VALUES(?,?,?)";
				st = connection.prepareStatement(sql);
				st.setString(1,userId);
				st.setString(2,siteId);
				st.setString(3,preferences.getEmailFrequency());
			}

			rs.close();
			
			return st;
		}
		finally
		{
			if(testST != null)
			{
				try
				{
					testST.close();
				}
				catch(Exception e) {}
			}
		}
	}

	public String getSelectAuthorStatement(String userId, String siteId)
	{
		return "SELECT * FROM " + TABLE_AUTHOR + " WHERE USER_ID = '" + userId + "' AND " + SITE_ID + " = '" + siteId + "'";
	}
}
