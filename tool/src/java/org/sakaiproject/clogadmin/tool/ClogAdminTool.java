package org.sakaiproject.clogadmin.tool;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.log4j.Logger;
import org.sakaiproject.clog.api.ClogManager;
import org.sakaiproject.clog.api.QueryBean;
import org.sakaiproject.clog.api.SakaiProxy;
import org.sakaiproject.clog.api.datamodel.Comment;
import org.sakaiproject.clog.api.datamodel.Post;
import org.sakaiproject.clog.api.datamodel.Visibilities;
import org.sakaiproject.clog.api.sql.ISQLGenerator;
import org.sakaiproject.component.api.ComponentManager;

/**
 * @author Adrian Fish (a.fish@lancaster.ac.uk)
 */
public class ClogAdminTool extends HttpServlet {
	private Logger logger = Logger.getLogger(getClass());

	private SakaiProxy sakaiProxy;
	private ClogManager clogManager;

	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		ComponentManager componentManager = org.sakaiproject.component.cover.ComponentManager.getInstance();
		sakaiProxy = (SakaiProxy) componentManager.get(SakaiProxy.class);
		clogManager = (ClogManager) componentManager.get(ClogManager.class);
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		if (sakaiProxy == null)
			throw new ServletException("sakaiProxy MUST be initialised.");
		
		if(!sakaiProxy.isCurrentUserAdmin()) {
			throw new ServletException("CLOG admin can only be used by Sakai super users.");
		}
		
		request.setAttribute("skin", sakaiProxy.getSakaiSkin());
		request.setAttribute("toolId", sakaiProxy.getCurrentToolId());
		
		response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        RequestDispatcher dispatcher = request.getRequestDispatcher("/admin.jsp");
        dispatcher.include(request,response);
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String blogwow =  request.getParameter("blogwow");
		String blogger =  request.getParameter("blogger");
		String blog2 =  request.getParameter("blog2");
		
		int numberImported = 0;
		
		if(blogwow != null) {
			numberImported += importBlogWowData();
		}
		
		if(blogger != null) {
			numberImported += importBlog1Data();
		}
		
		if(blog2 != null) {
			numberImported += importBlog2Data();
		}
		
		request.setAttribute("skin", sakaiProxy.getSakaiSkin());
		request.setAttribute("toolId", sakaiProxy.getCurrentToolId());
		request.setAttribute("numberImported", numberImported);
		
		response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        RequestDispatcher dispatcher = request.getRequestDispatcher("/result.jsp");
        dispatcher.include(request,response);
	}
	
	public int importBlog1Data() {
		if (logger.isDebugEnabled()) {
			logger.debug("Starting import of Blogger data ...");
		}

		Connection connection = null;
		Statement postST = null;
		Statement imageST = null;
		Statement fileST = null;
		ResultSet postRS = null;

		int numberImported = 0;

		try {
			connection = sakaiProxy.borrowConnection();

			ImporterSaxParser saxParser = new ImporterSaxParser(connection, sakaiProxy);

			postST = connection.createStatement();
			imageST = connection.createStatement();
			fileST = connection.createStatement();

			postRS = postST.executeQuery("SELECT * FROM BLOGGER_POST");

			while (postRS.next()) {
				String siteId = postRS.getString(ISQLGenerator.SITE_ID);

				String title = postRS.getString(ISQLGenerator.TITLE);
				
				QueryBean query = new QueryBean();
				query.setSiteId(siteId);
				query.setTitle(title);
				query.setKeyword("imported_from_blogger");
				
				List<Post> posts = clogManager.getPosts(query);
				
				if(posts.size() > 0) {
					// Already imported. Skip it.
					continue;
				}

				String postCreatorId = postRS.getString("IDCREATOR");

				long createdDate = postRS.getLong("DATEPOST");

				String xml = postRS.getString("XML");

				Post post = new Post();
				post.addKeyword("imported_from_blogger");
				post.setSiteId(siteId);

				saxParser.populatePost(xml, post);

				if ("".equals(post.getCreatorId()) || post.getCreatorId() == null)
					post.setCreatorId(postCreatorId);

				if ("".equals(post.getTitle()) || post.getTitle() == null)
					post.setTitle(title);

				if (-1 == post.getCreatedDate())
					post.setCreatedDate(createdDate);

				post.setSiteId(siteId);

				if (clogManager.savePost(post)) {
					List<Comment> comments = post.getComments();

					for (Comment comment : comments) {
						comment.setPostId(post.getId());
						clogManager.saveComment(comment);
					}

					numberImported++;
				}
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Finished import of previous Blogger data. " + numberImported + " posts imported.");
			}

			return numberImported;
		} catch (Exception e) {
			logger.error("Exception thrown whilst importing Blogger data", e);
			return 0;
		} finally {
			if (postRS != null) {
				try {
					postRS.close();
				} catch (Exception e) {
				}
			}

			if (postST != null) {
				try {
					postST.close();
				} catch (Exception e) {
				}
			}
			if (imageST != null) {
				try {
					imageST.close();
				} catch (Exception e) {
				}
			}
			if (fileST != null) {
				try {
					fileST.close();
				} catch (Exception e) {
				}
			}
		}
	}
	
	public int importBlog2Data() {
		if (logger.isDebugEnabled()) {
			logger.debug("Starting import of blog 2 data ...");
		}

		Connection connection = null;
		Statement postST = null;
		Statement testST = null;
		Statement postElementST = null;
		Statement elementST = null;
		Statement commentST = null;
		ResultSet postRS = null;
		ResultSet testRS = null;
		ResultSet postElementRS = null;
		ResultSet elementRS = null;
		ResultSet commentRS = null;

		int numberImported = 0;

		try {
			connection = sakaiProxy.borrowConnection();

			postST = connection.createStatement();
			testST = connection.createStatement();
			postElementST = connection.createStatement();
			elementST = connection.createStatement();
			commentST = connection.createStatement();

			postRS = postST.executeQuery("SELECT * FROM BLOG_POST");

			while (postRS.next()) {
				String siteId = postRS.getString(ISQLGenerator.SITE_ID);

				testRS = testST.executeQuery("SELECT * FROM BLOG_OPTIONS WHERE SITE_ID = '" + siteId + "'");

				if (testRS.next() && "LEARNING_LOG".equals(testRS.getString("BLOGMODE"))) {
					testRS.close();
					continue;
				}

				boolean brokenPost = false;

				Post post = new Post();

				String postId = postRS.getString(ISQLGenerator.POST_ID);
				// post.setId(postId);

				post.setSiteId(siteId);

				String title = postRS.getString(ISQLGenerator.TITLE);
				post.setTitle(title);
				
				QueryBean query = new QueryBean();
				query.setSiteId(siteId);
				query.setTitle(title);
				query.setKeyword("imported_from_blog");
				
				List<Post> posts = clogManager.getPosts(query);
				
				if(posts.size() > 0) {
					// Already imported. Skip it.
					continue;
				}
				
				post.addKeyword("imported_from_blog");

				Date postCreatedDate = postRS.getTimestamp(ISQLGenerator.CREATED_DATE);
				post.setCreatedDate(postCreatedDate.getTime());

				Date postModifiedDate = postRS.getTimestamp(ISQLGenerator.MODIFIED_DATE);
				post.setModifiedDate(postModifiedDate.getTime());

				String postCreatorId = postRS.getString(ISQLGenerator.CREATOR_ID);
				post.setCreatorId(postCreatorId);

				String keywordsText = postRS.getString(ISQLGenerator.KEYWORDS);
				post.setKeywordsText(keywordsText);

				int allowComments = postRS.getInt(ISQLGenerator.ALLOW_COMMENTS);
				post.setCommentable(allowComments == 1);

				String visibility = postRS.getString(ISQLGenerator.VISIBILITY);

				if ("PUBLIC".equals(visibility) || "READY".equals(visibility))
					visibility = "SITE";

				post.setVisibility(visibility);

				String shortText = postRS.getString("SHORT_TEXT");

				String collectedMarkup = "<i>" + shortText + "</i><br /><br />";

				postElementRS = postElementST.executeQuery("SELECT * FROM BLOG_POST_ELEMENT WHERE POST_ID = '" + postId + "' ORDER BY POSITION");
				while (postElementRS.next()) {
					String elementId = postElementRS.getString("ELEMENT_ID");
					String elementType = postElementRS.getString("ELEMENT_TYPE").trim();
					String displayName = postElementRS.getString("DISPLAY_NAME");

					if ("PARAGRAPH".equals(elementType)) {
						elementRS = elementST.executeQuery("SELECT CONTENT FROM BLOG_PARAGRAPH WHERE PARAGRAPH_ID = '" + elementId + "'");
						if (!elementRS.next()) {
							logger.error("Inconsistent Database. Post ID: " + postId + ". Post Title: " + post.getTitle() + ". No paragraph element found for post element with id '" + elementId + "'. Skipping this post ...");
							brokenPost = true;
							continue;
						}

						String content = elementRS.getString("CONTENT");
						collectedMarkup += content + "<br /><br />";
						elementRS.close();
					} else if ("LINK".equals(elementType)) {
						elementRS = elementST.executeQuery("SELECT URL FROM BLOG_LINK WHERE LINK_ID = '" + elementId + "'");
						if (!elementRS.next()) {
							logger.error("Inconsistent Database. Post ID: " + postId + ". Post Title: " + post.getTitle() + ". No link element found for post element with id '" + elementId + "'. Skipping this post ...");
							brokenPost = true;
							continue;
						}

						String href = elementRS.getString("URL");
						String link = "<a href=\"" + href + "\">" + displayName + "</a><br /><br />";
						collectedMarkup += link;
						elementRS.close();
					} else if ("IMAGE".equals(elementType)) {
						elementRS = elementST.executeQuery("SELECT * FROM BLOG_IMAGE WHERE IMAGE_ID = '" + elementId + "'");
						if (!elementRS.next()) {
							logger.error("Inconsistent Database. Post ID: " + postId + ". Post Title: " + post.getTitle() + ". No image element found for post element with id '" + elementId + "'. Skipping this post ...");
							brokenPost = true;
							continue;
						}

						String fullResourceId = elementRS.getString("FULL_RESOURCE_ID");
						String webResourceId = elementRS.getString("WEB_RESOURCE_ID");
						String fullUrl = sakaiProxy.getServerUrl() + "/access/content" + fullResourceId;
						String webUrl = sakaiProxy.getServerUrl() + "/access/content" + webResourceId;

						String img = "<img src=\"" + webUrl + "\" onclick=\"window.open('" + fullUrl + "','Full Image','width=400,height=300,status=no,resizable=yes,location=no,scrollbars=yes');\"/><br />";

						collectedMarkup += img + "<br />";
						elementRS.close();
					} else if ("FILE".equals(elementType)) {
						elementRS = elementST.executeQuery("SELECT * FROM BLOG_FILE WHERE FILE_ID = '" + elementId + "'");
						if (!elementRS.next()) {
							logger.error("Inconsistent Database. Post ID: " + postId + ". Post Title: " + post.getTitle() + ". No file element found for post element with id '" + elementId + "'. Skipping this post ...");
							brokenPost = true;
							continue;
						}

						String resourceId = elementRS.getString("RESOURCE_ID");

						String file = "<a href=\"" + sakaiProxy.getServerUrl() + "/access/content" + resourceId + "\">" + displayName + "</a><br /><br />";

						collectedMarkup += file;
						elementRS.close();
					}
				} // while(postElementRS.next())

				postElementRS.close();

				post.setContent(collectedMarkup);

				if (!brokenPost && clogManager.savePost(post)) {
					numberImported++;

					commentRS = commentST.executeQuery("SELECT * FROM BLOG_COMMENT WHERE POST_ID = '" + postId + "'");

					while (commentRS.next()) {
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

						clogManager.saveComment(comment);
					} // while(commentRS.next())

					commentRS.close();
				}
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Finished import of blog 2 data. " + numberImported + " posts imported.");
			}

			return numberImported;
		} catch (Exception e) {
			logger.error("Exception thrown whilst importing blog 2 data", e);
			return 0;
		} finally {
			if (commentRS != null) {
				try {
					commentRS.close();
				} catch (Exception e) {
				}
			}

			if (elementRS != null) {
				try {
					elementRS.close();
				} catch (Exception e) {
				}
			}

			if (postElementRS != null) {
				try {
					postElementRS.close();
				} catch (Exception e) {
				}
			}

			if (testRS != null) {
				try {
					testRS.close();
				} catch (Exception e) {
				}
			}

			if (postRS != null) {
				try {
					postRS.close();
				} catch (Exception e) {
				}
			}

			if (postST != null) {
				try {
					postST.close();
				} catch (Exception e) {
				}
			}

			if (testST != null) {
				try {
					testST.close();
				} catch (Exception e) {
				}
			}

			if (postElementST != null) {
				try {
					postElementST.close();
				} catch (Exception e) {
				}
			}

			if (elementST != null) {
				try {
					elementST.close();
				} catch (Exception e) {
				}
			}

			if (commentST != null) {
				try {
					commentST.close();
				} catch (Exception e) {
				}
			}

			sakaiProxy.returnConnection(connection);
		}
	}
	
	private int importBlogWowData() {
		
		Connection connection = null;
		Statement postST = null;
		ResultSet postRS = null;
		Statement commentST = null;
		ResultSet commentRS = null;

		int numberImported = 0;

		try {
			connection = sakaiProxy.borrowConnection();

			postST = connection.createStatement();
			commentST = connection.createStatement();
			postRS = postST.executeQuery("SELECT blogwow_entry.*,location FROM blogwow_entry,blogwow_blog where blogwow_entry.blog_id = blogwow_blog.id");
			while(postRS.next()) {
				String id = postRS.getString("id");
				String title = postRS.getString("title");
				String text = postRS.getString("text");
				Date created = postRS.getTimestamp("dateCreated");
				Date modified = postRS.getTimestamp("dateModified");
				String privacySetting = postRS.getString("privacySetting");
				String ownerId = postRS.getString("ownerId");
				String location = postRS.getString("location");
				
				Post post = new Post();
				
				if(location.startsWith("/site/")) {
					String siteId = location.substring(location.lastIndexOf("/") + 1);
					post.setSiteId(siteId);
				}
				
				QueryBean query = new QueryBean();
				query.setSiteId(post.getSiteId());
				query.setTitle(title);
				query.setKeyword("imported_from_blogwow");
				
				List<Post> posts = clogManager.getPosts(query);
				
				if(posts.size() > 0) {
					// Already imported. Skip it.
					continue;
				}
				
				post.addKeyword("imported_from_blogwow");
				
				post.setTitle(title);
				post.setCreatedDate(created.getTime());
				post.setModifiedDate(modified.getTime());
				post.setCreatorId(ownerId);
				post.setContent(text);
				
				if("private".equals(privacySetting)) {
					post.setVisibility(Visibilities.PRIVATE);
				} else if("group".equals(privacySetting)) {
					post.setVisibility(Visibilities.SITE);
				} else if("public".equals(privacySetting)) {
					post.setVisibility(Visibilities.PUBLIC);
				}
				
				if(clogManager.savePost(post)) {
				
					commentRS = commentST.executeQuery("SELECT * FROM blogwow_comment WHERE entry_id = '" + id + "'");
					while(commentRS.next()) {
						String commentText = commentRS.getString("text");
						Date commentCreated = commentRS.getTimestamp("dateCreated");
						Date commentModified = commentRS.getTimestamp("dateModified");
						String commentOwnerId = commentRS.getString("ownerId");
					
						Comment comment = new Comment();
						comment.setCreatorId(commentOwnerId);
						comment.setCreatedDate(commentCreated.getTime());
						comment.setModifiedDate(commentModified.getTime());
						comment.setContent(commentText);
						comment.setPostId(post.getId());

						clogManager.saveComment(comment);
					}
					commentRS.close();
					numberImported++;
				}
				
			}
		} catch (Exception e) {
			logger.error("Exception thrown whilst importing blog wow data", e);
			return 0;
		} finally {
			if (commentRS != null) {
				try {
					commentRS.close();
				} catch (Exception e) {
				}
			}
			if (postRS != null) {
				try {
					postRS.close();
				} catch (Exception e) {
				}
			}
			if (commentST != null) {
				try {
					commentST.close();
				} catch (Exception e) {
				}
			}
			if (postST != null) {
				try {
					postST.close();
				} catch (Exception e) {
				}
			}

			sakaiProxy.returnConnection(connection);
		}
		
		return numberImported;
	}
}
