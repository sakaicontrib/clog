package org.sakaiproject.clog.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.sakaiproject.clog.api.datamodel.Comment;
import org.sakaiproject.clog.api.sql.ISQLGenerator;
import org.sakaiproject.clog.api.datamodel.Post;
import org.sakaiproject.clog.impl.sql.HiperSonicGenerator;
import org.sakaiproject.clog.impl.sql.MySQLGenerator;
import org.sakaiproject.clog.impl.sql.OracleSQLGenerator;
import org.sakaiproject.clog.api.ClogMember;
import org.sakaiproject.clog.api.QueryBean;
import org.sakaiproject.clog.api.SakaiProxy;

public class PersistenceManager {

	private Logger logger = Logger.getLogger(PersistenceManager.class);

	private ISQLGenerator sqlGenerator;

	private SakaiProxy sakaiProxy;

	public PersistenceManager(SakaiProxy sakaiProxy) {
		this.sakaiProxy = sakaiProxy;

		String vendor = sakaiProxy.getVendor();

		// TODO load the proper class using reflection. We can use a named based
		// system to locate the correct SQLGenerator
		if (vendor.equals("mysql"))
			sqlGenerator = new MySQLGenerator();
		else if (vendor.equals("oracle"))
			sqlGenerator = new OracleSQLGenerator();
		else if (vendor.equals("hsqldb"))
			sqlGenerator = new HiperSonicGenerator();
		else {
			logger.error("Unknown database vendor:" + vendor + ". Defaulting to HypersonicDB ...");
			sqlGenerator = new HiperSonicGenerator();
		}

		if (sakaiProxy.isAutoDDL()) {
			if (!setupTables()) {
				logger.error("Failed to setup the tables");
			}
		}
	}

	public boolean setupTables() {

        logger.debug("setupTables()");

		Connection connection = null;
		Statement statement = null;

		try {
			connection = sakaiProxy.borrowConnection();
			boolean oldAutoCommitFlag = connection.getAutoCommit();
			connection.setAutoCommit(false);

			statement = connection.createStatement();

			try {
				List<String> statements = sqlGenerator.getCreateTablesStatements();

				for (String sql : statements) {
					statement.executeUpdate(sql);
                }

				connection.commit();

				return true;
			} catch (Exception e) {
				logger.error("Caught exception whilst setting up tables. Message: " + e.getMessage() + ". Rolling back ...");
				connection.rollback();
			} finally {
				connection.setAutoCommit(oldAutoCommitFlag);
			}
		} catch (Exception e) {
			logger.error("Caught exception whilst setting up tables. Message: " + e.getMessage());
		} finally {
			if (statement != null) {
				try {
					statement.close();
				} catch (Exception e) {
				}
			}

			sakaiProxy.returnConnection(connection);
		}

		return false;
	}

	public boolean existPost(String OID) throws Exception {
		if (logger.isDebugEnabled())
			logger.debug("existPost(" + OID + ")");

		Connection connection = null;
		Statement statement = null;
		ResultSet rs = null;

		try {
			connection = sakaiProxy.borrowConnection();
			statement = connection.createStatement();
			String sql = sqlGenerator.getSelectPost(OID);
			rs = statement.executeQuery(sql);
			boolean exists = rs.next();
			return exists;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
				}
			}
			if (statement != null) {
				try {
					statement.close();
				} catch (SQLException e) {
				}
			}

			sakaiProxy.returnConnection(connection);
		}
	}

	public List<Post> getAllPost(String placementId) throws Exception {
		return getAllPost(placementId, false);
	}

	public List<Post> getAllPost(String placementId, boolean populate) throws Exception {
		if (logger.isDebugEnabled())
			logger.debug("getAllPost(" + placementId + ")");

		List<Post> result = new ArrayList<Post>();

		Connection connection = null;
		Statement statement = null;
		ResultSet rs = null;

		try {
			connection = sakaiProxy.borrowConnection();
			statement = connection.createStatement();
			rs = statement.executeQuery(sqlGenerator.getSelectAllPost(placementId));
			result = transformResultSetInPostCollection(rs, connection);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {
				}
			}

			if (statement != null) {
				try {
					statement.close();
				} catch (Exception e) {
				}
			}

			sakaiProxy.returnConnection(connection);
		}

		return result;
	}

	public boolean saveComment(Comment comment) {
		Connection connection = null;
		List<PreparedStatement> statements = null;

		try {
			connection = sakaiProxy.borrowConnection();
			boolean oldAutoCommit = connection.getAutoCommit();
			connection.setAutoCommit(false);

			try {

				statements = sqlGenerator.getSaveStatementsForComment(comment, connection);
				for (PreparedStatement st : statements)
					st.executeUpdate();

				connection.commit();

				return true;
			} catch (Exception e) {
				logger.error("Caught exception whilst saving comment. Rolling back ...", e);
				connection.rollback();
			} finally {
				connection.setAutoCommit(oldAutoCommit);
			}
		} catch (Exception e) {
			logger.error("Caught exception whilst saving comment.", e);
		} finally {
			if (statements != null) {
				for (PreparedStatement st : statements) {
					try {
						st.close();
					} catch (SQLException e) {
					}
				}
			}

			sakaiProxy.returnConnection(connection);
		}

		return false;
	}

	public boolean deleteComment(String commentId) {
		Connection connection = null;
		List<PreparedStatement> statements = null;

		try {
			connection = sakaiProxy.borrowConnection();
			boolean oldAutoCommit = connection.getAutoCommit();
			connection.setAutoCommit(false);

			try {

				statements = sqlGenerator.getDeleteStatementsForComment(commentId, connection);
				for (PreparedStatement st : statements)
					st.executeUpdate();

				connection.commit();

				return true;
			} catch (Exception e) {
				logger.error("Caught exception whilst deleting comment. Rolling back ...", e);
				connection.rollback();
			} finally {
				connection.setAutoCommit(oldAutoCommit);
			}
		} catch (Exception e) {
			logger.error("Caught exception whilst deleting comment.", e);
		} finally {
			if (statements != null) {
				for (PreparedStatement st : statements) {
					try {
						st.close();
					} catch (SQLException e) {
					}
				}
			}

			sakaiProxy.returnConnection(connection);
		}

		return false;
	}

	public boolean savePost(Post post) {

        logger.debug("savePost()");

		Connection connection = null;
		List<PreparedStatement> statements = null;

		try {
			connection = sakaiProxy.borrowConnection();
			boolean oldAutoCommit = connection.getAutoCommit();
			connection.setAutoCommit(false);

			try {
				if (post.isAutoSave()) {
					statements = sqlGenerator.getInsertStatementsForAutoSavedPost(post, connection);
                } else {
					Post currentPost = null;
                    try {
                        currentPost = getPost(post.getId()); 
                    } catch (Exception e1) {}

                    statements = sqlGenerator.getInsertStatementsForPost(post, currentPost, connection);
                }

				for (PreparedStatement st : statements) {
					st.executeUpdate();
                }

				if (post.getSiteId().startsWith("~")) {

					// This post is on a MyWorkspace site

					String accessUrl = sakaiProxy.getAccessUrl();
					String content = post.getContent();
					Pattern p = Pattern.compile(accessUrl + "/content/user/" + sakaiProxy.getCurrentUserEid() + "/([^\"]*)\"");
					Matcher m = p.matcher(content);
					while (m.find()) {
						String contentId = m.group(1);
						if (post.isPublic()) {
							if (!sakaiProxy.setResourcePublic("/user/" + sakaiProxy.getCurrentUserId() + "/" + contentId, true)) {
								throw new Exception("Failed to make embedded resource public");
                            }
						} else {
							if (!sakaiProxy.setResourcePublic("/user/" + sakaiProxy.getCurrentUserId() + "/" + contentId, false)) {
								throw new Exception("Failed to make embedded resource public");
                            }

						}
					}
				}

				connection.commit();
				return true;
			} catch (Exception e) {
				logger.error("Caught exception whilst saving post. Rolling back ...", e);
				connection.rollback();
			} finally {
				connection.setAutoCommit(oldAutoCommit);
			}
		} catch (Exception e) {
			logger.error("Caught exception whilst saving post.", e);
		} finally {
			if (statements != null) {
				for (PreparedStatement st : statements) {
					try {
						st.close();
					} catch (SQLException e) {
					}
				}
			}

			sakaiProxy.returnConnection(connection);
		}

		return false;
	}

	public boolean deletePost(Post post) {

		if (logger.isDebugEnabled()) {
			logger.debug("deletePost(" + post.getId() + ")");
        }

		Connection connection = null;
		List<PreparedStatement> statements = null;

		try {
			connection = sakaiProxy.borrowConnection();
			boolean oldAutoCommit = connection.getAutoCommit();
			connection.setAutoCommit(false);

			try {
				statements = sqlGenerator.getDeleteStatementsForPost(post, connection);

				for (PreparedStatement st : statements)
					st.executeUpdate();

				connection.commit();

				return true;
			} catch (Exception e) {
				logger.error("Caught exception whilst deleting post. Rolling back ...", e);
				connection.rollback();
			} finally {
				connection.setAutoCommit(oldAutoCommit);
			}
		} catch (Exception e) {
			logger.error("Caught exception whilst deleting post.", e);
		} finally {
			if (statements != null) {
				for (PreparedStatement st : statements) {
					try {
						st.close();
					} catch (Exception e) {
					}
				}
			}

			sakaiProxy.returnConnection(connection);
		}

		return false;
	}

	public boolean recyclePost(Post post) {
		if (logger.isDebugEnabled())
			logger.debug("recyclePost(" + post.getId() + ")");

		Connection connection = null;
		List<PreparedStatement> statements = null;

		try {
			connection = sakaiProxy.borrowConnection();
			boolean oldAutoCommit = connection.getAutoCommit();
			connection.setAutoCommit(false);

			try {
				statements = sqlGenerator.getRecycleStatementsForPost(post, connection);
				for (PreparedStatement st : statements)
					st.executeUpdate();
				connection.commit();
				return true;
			} catch (Exception e) {
				logger.error("Caught exception whilst recycling post. Rolling back ...", e);
				connection.rollback();
			} finally {
				connection.setAutoCommit(oldAutoCommit);
			}
		} catch (Exception e) {
			logger.error("Caught exception whilst recycling post.", e);
			return false;
		} finally {
			if (statements != null) {
				for (PreparedStatement st : statements) {
					try {
						st.close();
					} catch (SQLException e) {
					}
				}
			}

			sakaiProxy.returnConnection(connection);
		}

		return false;
	}

	public boolean restorePost(Post post) {
		if (logger.isDebugEnabled())
			logger.debug("restore(" + post.getId() + ")");

		Connection connection = null;
		List<PreparedStatement> statements = null;

		try {
			connection = sakaiProxy.borrowConnection();
			boolean oldAutoCommit = connection.getAutoCommit();
			connection.setAutoCommit(false);

			try {
				statements = sqlGenerator.getRestoreStatementsForPost(post, connection);
				for (PreparedStatement st : statements)
					st.executeUpdate();
				connection.commit();
				return true;
			} catch (Exception e) {
				logger.error("Caught exception whilst recycling post. Rolling back ...", e);
				connection.rollback();
			} finally {
				connection.setAutoCommit(oldAutoCommit);
			}
		} catch (Exception e) {
			logger.error("Caught exception whilst recycling post.", e);
			return false;
		} finally {
			if (statements != null) {
				for (PreparedStatement st : statements) {
					try {
						st.close();
					} catch (SQLException e) {
					}
				}
			}

			sakaiProxy.returnConnection(connection);
		}

		return false;
	}

	public List<Post> getPosts(QueryBean query) throws Exception {
		List<Post> posts = new ArrayList<Post>();

		Connection connection = null;
		Statement st = null;
		ResultSet rs = null;

		try {
			connection = sakaiProxy.borrowConnection();
			st = connection.createStatement();
			List<String> sqlStatements = sqlGenerator.getSelectStatementsForQuery(query);
			for (String sql : sqlStatements) {
				rs = st.executeQuery(sql);
				posts.addAll(transformResultSetInPostCollection(rs, connection));
			}
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {
				}
			}

			if (st != null) {
				try {
					st.close();
				} catch (Exception e) {
				}
			}

			sakaiProxy.returnConnection(connection);
		}

		return posts;
	}

	public Post getPost(String postId) throws Exception {

		if (logger.isDebugEnabled()) {
			logger.debug("getPost(" + postId + ")");
        }

		Connection connection = null;
		Statement st = null;
		ResultSet rs = null;
		try {
			connection = sakaiProxy.borrowConnection();
			st = connection.createStatement();
			String sql = sqlGenerator.getSelectPost(postId);
			rs = st.executeQuery(sql);
			List<Post> posts = transformResultSetInPostCollection(rs, connection);

			if (posts.size() == 0)
				throw new Exception("getPost: Unable to find post with id:" + postId);
			if (posts.size() > 1)
				throw new Exception("getPost: there is more than one post with id:" + postId);

			return posts.get(0);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {
				}
			}

			if (st != null) {
				try {
					st.close();
				} catch (Exception e) {
				}
			}

			sakaiProxy.returnConnection(connection);
		}
	}

	public Post getAutosavedPost(String postId) {
		if (logger.isDebugEnabled())
			logger.debug("getAutosavedPost(" + postId + ")");

		Connection connection = null;
		PreparedStatement st = null;
		ResultSet rs = null;
		try {
			connection = sakaiProxy.borrowConnection();
			st = sqlGenerator.getSelectAutosavedPost(postId, connection);
			rs = st.executeQuery();
			List<Post> posts = transformResultSetInPostCollection(rs, connection);

			if (posts.size() == 0) {
				return null;
			}
			if (posts.size() > 1) {
				logger.error("getAutosavedPost: there is more than one post with id:" + postId);
				return null;
			}

			return posts.get(0);
		} catch (Exception e) {
			logger.error("Caught exception whilst getting autosaved post", e);
			return null;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {
				}
			}

			if (st != null) {
				try {
					st.close();
				} catch (Exception e) {
				}
			}

			sakaiProxy.returnConnection(connection);
		}
	}

	private List<Post> transformResultSetInPostCollection(ResultSet rs, Connection connection) throws Exception {

		List<Post> result = new ArrayList<Post>();

		if (rs == null) {
			return result;
        }

		Statement extrasST = null;

		try {
			extrasST = connection.createStatement();

			while (rs.next()) {
				Post post = new Post();

				String postId = rs.getString(ISQLGenerator.POST_ID);
				post.setId(postId);

				String visibility = rs.getString(ISQLGenerator.VISIBILITY);
				post.setVisibility(visibility);

				if (!post.isAutoSave()) {
					post.setAutosavedVersion(getAutosavedPost(postId));
                }

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

				String keywordsText = rs.getString(ISQLGenerator.KEYWORDS);
				post.setKeywordsText(keywordsText);

				int allowComments = rs.getInt(ISQLGenerator.ALLOW_COMMENTS);
				post.setCommentable(allowComments == 1);

				String sql = sqlGenerator.getSelectComments(postId);
				ResultSet extrasRS = extrasST.executeQuery(sql);
				post.setComments(transformResultSetInCommentCollection(extrasRS));
				extrasRS.close();

				String groupsSql = sqlGenerator.getSelectGroups(postId);
				extrasRS  = extrasST.executeQuery(groupsSql);
				post.setGroups(transformResultSetInGroupCollection(extrasRS));
				extrasRS.close();

				post.setCreatorDisplayName(sakaiProxy.getDisplayNameForTheUser(post.getCreatorId()));

				result.add(post);
			}
		} finally {

			if (extrasST != null) {
				try {
					extrasST.close();
				} catch (Exception e) { }
			}
		}

		return result;
	}

	private List<Comment> transformResultSetInCommentCollection(ResultSet rs) throws Exception {

		List<Comment> result = new ArrayList<Comment>();

		if (rs == null) {
			return result;
        }

		while (rs.next()) {
			Comment comment = new Comment();
			comment.setId(rs.getString(ISQLGenerator.COMMENT_ID));
			comment.setPostId(rs.getString(ISQLGenerator.POST_ID));
			comment.setSiteId(rs.getString("SITE_ID"));
			comment.setCreatorId(rs.getString(ISQLGenerator.CREATOR_ID));
			comment.setCreatedDate(rs.getTimestamp(ISQLGenerator.CREATED_DATE).getTime());
			comment.setContent(rs.getString(ISQLGenerator.CONTENT));
			comment.setModifiedDate(rs.getTimestamp(ISQLGenerator.MODIFIED_DATE).getTime());
			comment.setCreatorDisplayName(sakaiProxy.getDisplayNameForTheUser(comment.getCreatorId()));

			result.add(comment);
		}

		return result;
	}

	private List<String> transformResultSetInGroupCollection(ResultSet rs) throws Exception {

		List<String> result = new ArrayList<String>();

		if (rs == null) {
			return result;
        }

		while (rs.next()) {
			result.add(rs.getString(ISQLGenerator.GROUP_ID));
		}

		return result;
	}

	public Comment getComment(String commentId) throws Exception {
		if (logger.isDebugEnabled())
			logger.debug("getComment(" + commentId + ")");

		Connection connection = null;
		Statement st = null;
		ResultSet rs = null;
		try {
			connection = sakaiProxy.borrowConnection();
			st = connection.createStatement();
			String sql = sqlGenerator.getSelectComment(commentId);
			rs = st.executeQuery(sql);
			List<Comment> comments = transformResultSetInCommentCollection(rs);
			if (comments.size() < 1) {
				logger.error("Failed to find comment with id '" + commentId + "'");
				return null;
			}

			return comments.get(0);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {
				}
			}

			if (st != null) {
				try {
					st.close();
				} catch (Exception e) {
				}
			}

			sakaiProxy.returnConnection(connection);
		}
	}

	public void setSakaiProxy(SakaiProxy sakaiProxy) {
		this.sakaiProxy = sakaiProxy;
	}

	public SakaiProxy getSakaiProxy() {
		return sakaiProxy;
	}

	public List<ClogMember> getPublicBloggers() {
		List<ClogMember> members = new ArrayList<ClogMember>();

		Connection connection = null;
		Statement publicST = null;
		Statement countST = null;
		ResultSet rs = null;
		ResultSet countRS = null;

		try {
			connection = sakaiProxy.borrowConnection();
			publicST = connection.createStatement();
			countST = connection.createStatement();
			String sql = sqlGenerator.getSelectPublicBloggers();
			rs = publicST.executeQuery(sql);

			while (rs.next()) {
				String userId = rs.getString(ISQLGenerator.CREATOR_ID);
				ClogMember member = sakaiProxy.getMember(userId);
				String countPostsQuery = sqlGenerator.getCountPublicPosts(userId);
				countRS = countST.executeQuery(countPostsQuery);

				countRS.next();

				int count = countRS.getInt("NUMBER_POSTS");

				countRS.close();
				member.setNumberOfPosts(count);

				members.add(member);
			}
		} catch (Exception e) {
			logger.error("Caught exception whilst getting public bloggers.", e);
		} finally {
			if (countRS != null) {
				try {
					countRS.close();
				} catch (Exception e) {
				}
			}

			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {
				}
			}

			if (publicST != null) {
				try {
					publicST.close();
				} catch (Exception e) {
				}
			}

			if (countST != null) {
				try {
					countST.close();
				} catch (Exception e) {
				}
			}

			sakaiProxy.returnConnection(connection);
		}
		return members;
	}

	public boolean postExists(String postId) throws Exception {
		Connection connection = null;
		Statement st = null;
		ResultSet rs = null;

		try {
			connection = sakaiProxy.borrowConnection();
			st = connection.createStatement();
			String sql = sqlGenerator.getSelectPost(postId);
			rs = st.executeQuery(sql);
			boolean exists = rs.next();
			return exists;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {
				}
			}

			if (st != null) {
				try {
					st.close();
				} catch (Exception e) {
				}
			}

			sakaiProxy.returnConnection(connection);
		}
	}

	public boolean populateAuthorData(ClogMember author, String siteId) {
		Connection connection = null;
		Statement st = null;
		ResultSet rs = null;

		try {
			connection = sakaiProxy.borrowConnection();
			st = connection.createStatement();
			String sql = sqlGenerator.getSelectAuthorStatement(author.getUserId(), siteId);
			rs = st.executeQuery(sql);
			if (rs.next()) {
				int totalPosts = rs.getInt(ISQLGenerator.TOTAL_POSTS);
				int totalComments = rs.getInt(ISQLGenerator.TOTAL_COMMENTS);
				long lastPostDate = -1L;
				Timestamp ts = rs.getTimestamp(ISQLGenerator.LAST_POST_DATE);
				if (ts != null) {
					lastPostDate = rs.getTimestamp(ISQLGenerator.LAST_POST_DATE).getTime();
				}
				author.setNumberOfPosts(totalPosts);
				author.setNumberOfComments(totalComments);
				author.setDateOfLastPost(lastPostDate);
			}

			return true;
		} catch (Exception e) {
			logger.error("Failed to populate author data.", e);
			return false;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {
				}
			}

			if (st != null) {
				try {
					st.close();
				} catch (Exception e) {
				}
			}

			sakaiProxy.returnConnection(connection);
		}
	}

	public boolean deleteAutosavedCopy(String postId) {
		Connection connection = null;
		PreparedStatement st = null;

		try {
			connection = sakaiProxy.borrowConnection();
			st = sqlGenerator.getDeleteAutosavedCopyStatement(postId, connection);
			st.executeUpdate();
			return true;
		} catch (Exception e) {
			logger.error("Caught exception whilst deleting autosaved copy.", e);
		} finally {
			if (st != null) {
				try {
					st.close();
				} catch (Exception e) {
				}
			}

			sakaiProxy.returnConnection(connection);
		}

		return false;
	}
}
