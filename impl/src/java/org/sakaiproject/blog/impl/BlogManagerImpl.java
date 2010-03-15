package org.sakaiproject.blog.impl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.sakaiproject.blog.api.BlogFunctions;
import org.sakaiproject.blog.api.BlogManager;
import org.sakaiproject.blog.api.BlogMember;
import org.sakaiproject.blog.api.QueryBean;
import org.sakaiproject.blog.api.SakaiProxy;
import org.sakaiproject.blog.api.XmlDefs;
import org.sakaiproject.blog.api.datamodel.Comment;
import org.sakaiproject.blog.api.datamodel.Post;
import org.sakaiproject.blog.api.datamodel.Preferences;
import org.sakaiproject.blog.api.datamodel.Visibilities;
import org.sakaiproject.entity.api.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class BlogManagerImpl implements BlogManager
{
	private Logger logger = Logger.getLogger(BlogManagerImpl.class);

	private PersistenceManager persistenceManager;

	private BlogSecurityManager securityManager;

	private SakaiProxy sakaiProxy;

	public void init()
	{
		if (logger.isDebugEnabled())
			logger.debug("init()");

		logger.info("Registering Blog functions ...");

		sakaiProxy.registerFunction(BlogFunctions.BLOG_POST_CREATE);
		sakaiProxy.registerFunction(BlogFunctions.BLOG_POST_READ_ANY);
		sakaiProxy.registerFunction(BlogFunctions.BLOG_POST_UPDATE_ANY);
		sakaiProxy.registerFunction(BlogFunctions.BLOG_POST_UPDATE_OWN);
		sakaiProxy.registerFunction(BlogFunctions.BLOG_POST_DELETE_ANY);
		sakaiProxy.registerFunction(BlogFunctions.BLOG_POST_DELETE_OWN);
		sakaiProxy.registerFunction(BlogFunctions.BLOG_COMMENT_CREATE);
		sakaiProxy.registerFunction(BlogFunctions.BLOG_COMMENT_READ_ANY);
		sakaiProxy.registerFunction(BlogFunctions.BLOG_COMMENT_READ_OWN);
		sakaiProxy.registerFunction(BlogFunctions.BLOG_COMMENT_UPDATE_ANY);
		sakaiProxy.registerFunction(BlogFunctions.BLOG_COMMENT_UPDATE_OWN);
		sakaiProxy.registerFunction(BlogFunctions.BLOG_COMMENT_DELETE_ANY);
		sakaiProxy.registerFunction(BlogFunctions.BLOG_COMMENT_DELETE_OWN);

		logger.info("Registered Blog functions ...");

		sakaiProxy.registerEntityProducer(this);
		
		persistenceManager = new PersistenceManager(sakaiProxy);
		
		securityManager = new BlogSecurityManager(sakaiProxy);
	}

	public Post getPost(String postId) throws Exception
	{
		if (logger.isDebugEnabled())
			logger.debug("getPost(" + postId + ")");

		Post post = persistenceManager.getPost(postId);
		if (securityManager.canCurrentUserReadPost(post))
			return post;
		else
			throw new Exception("The current user does not have permissions to read this post.");
	}

	public List<Post> getPosts(String placementId) throws Exception
	{
		// Get all the posts for the supplied site and filter them through the
		// security manager
		List<Post> filtered;
		List<Post> unfiltered = persistenceManager.getAllPost(placementId);
		filtered = securityManager.filter(unfiltered);
		return filtered;
	}

	public List<Post> getPosts(QueryBean query) throws Exception
	{
		// Get all the posts for the supplied site and filter them through the
		// security manager
		List<Post> filtered;
		List<Post> unfiltered = persistenceManager.getPosts(query);
		filtered = securityManager.filter(unfiltered);
		return filtered;
	}

	public boolean savePost(Post post)
	{
		try
		{
			return persistenceManager.savePost(post);
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst creating post", e);
		}

		return false;
	}

	public boolean deletePost(String postId)
	{
		try
		{
			Post post = persistenceManager.getPost(postId);
			if (securityManager.canCurrentUserDeletePost(post))
				return persistenceManager.deletePost(post);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return false;
	}

	public boolean saveComment(Comment comment)
	{
		try
		{
			boolean newComment = "".equals(comment.getId());

			Post post = getPost(comment.getPostId());

			if (persistenceManager.saveComment(comment) && newComment)
			{
				/*
				 * String portalUrl = sakaiProxy.getPortalUrl(); String siteId = post.getSiteId(); String pageId = sakaiProxy.getCurrentPageId(); String toolId = sakaiProxy.getCurrentToolId();
				 * 
				 * String url = portalUrl + "/site/" + siteId + "/page/" + pageId + "?toolstate-" + toolId +
				 * "=%2Fhome%3Fwicket%3AbookmarkablePage%3D%3Aorg.sakaiproject.blog.tool.pages.PostPage%26postId%3D" + comment.getPostId();
				 * 
				 * sakaiProxy.sendEmailWithMessage(post.getCreatorId(), "New/Updated Blog Comment", sakaiProxy.getDisplayNameForTheUser(comment.getCreatorId()) + " commented on your post, titled '<a
				 * href=\"" + url + "\">" + post.getTitle() + "</a>'<br /><br />'<i>" + comment.getContent() + "</i>'");
				 */

				return true;
			}
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst saving comment", e);
		}

		return false;
	}

	public boolean deleteComment(String commentId)
	{
		try
		{
			if (persistenceManager.deleteComment(commentId))
			{
				// sakaiProxy.postEvent(BLOG_COMMENT_DELETED,commentId(),post.getSiteId());
				return true;
			}
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst deleting comment.", e);
		}

		return false;
	}

	public boolean recyclePost(String postId)
	{
		try
		{
			Post post = persistenceManager.getPost(postId);

			if (securityManager.canCurrentUserDeletePost(post))
			{
				try
				{
					persistenceManager.recyclePost(post);
					post.setVisibility(Visibilities.RECYCLED);
					return true;
				}
				catch (Exception e)
				{
					logger.error("The persistence manager threw an Exception whilst recycling post '" + postId + "'");
				}
			}
		}
		catch (Exception e)
		{
			logger.error("Caught an exception whilst recycling post '" + postId + "'");
		}

		return false;
	}

	public BlogSecurityManager getSecurityManager()
	{
		return securityManager;
	}

	public void setSecurityManager(BlogSecurityManager securityManager)
	{
		this.securityManager = securityManager;
	}

	public void setPersistenceManager(PersistenceManager pm)
	{
		this.persistenceManager = pm;
	}

	private String serviceName()
	{
		return BlogManager.class.getName();
	}

	public String archive(String siteId, Document doc, Stack stack, String archivePath, List attachments)
	{
		if (logger.isDebugEnabled())
			logger.debug("archive(siteId:" + siteId + ",archivePath:" + archivePath + ")");

		StringBuilder results = new StringBuilder();

		results.append(getLabel() + ": Started.\n");

		int postCount = 0;

		try
		{
			// start with an element with our very own (service) name
			Element element = doc.createElement(serviceName());
			element.setAttribute("version", "2.5.x");
			((Element) stack.peek()).appendChild(element);
			stack.push(element);

			Element blog = doc.createElement("blog");
			List<Post> posts = getPosts(siteId);
			if (posts != null && posts.size() > 0)
			{
				for (Post post : posts)
				{
					Element postElement = post.toXml(doc, stack);
					blog.appendChild(postElement);
					postCount++;
				}
			}

			((Element) stack.peek()).appendChild(blog);
			stack.push(blog);

			stack.pop();

			results.append(getLabel() + ": Finished. " + postCount + " post(s) archived.\n");
		}
		catch (Exception any)
		{
			results.append(getLabel() + ": exception caught. Message: " + any.getMessage());
			logger.warn(getLabel() + " exception caught. Message: " + any.getMessage());
		}

		stack.pop();

		return results.toString();
	}

	/**
	 * From EntityProducer
	 */
	public String merge(String siteId, Element root, String archivePath, String fromSiteId, Map attachmentNames, Map userIdTrans, Set userListAllowImport)
	{
		logger.debug("merge(siteId:" + siteId + ",root tagName:" + root.getTagName() + ",archivePath:" + archivePath + ",fromSiteId:" + fromSiteId);

		StringBuilder results = new StringBuilder();

		try
		{

			int postCount = 0;

			NodeList postNodes = root.getElementsByTagName(XmlDefs.POST);
			final int numberPosts = postNodes.getLength();

			for (int i = 0; i < numberPosts; i++)
			{
				Node child = postNodes.item(i);
				if (child.getNodeType() != Node.ELEMENT_NODE)
				{
					// Problem
					continue;
				}

				Element postElement = (Element) child;

				Post post = new Post();
				post.fromXml(postElement);
				post.setSiteId(siteId);

				savePost(post);
				postCount++;
			}

			results.append("Stored " + postCount + " posts.");

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return results.toString();
	}

	/**
	 * From EntityProducer
	 */
	public Entity getEntity(Reference ref)
	{
		if (logger.isDebugEnabled())
			logger.debug("getEntity(Ref ID:" + ref.getId() + ")");

		Entity rv = null;

		try
		{
			String reference = ref.getReference();

			int lastIndex = reference.lastIndexOf(Entity.SEPARATOR);
			String postId = reference.substring(lastIndex, reference.length() - lastIndex);
			rv = getPost(postId);
		}
		catch (Exception e)
		{
			logger.warn("getEntity(): " + e);
		}

		return rv;
	}

	/**
	 * From EntityProducer
	 */
	public Collection getEntityAuthzGroups(Reference ref, String userId)
	{
		if (logger.isDebugEnabled())
			logger.debug("getEntityAuthzGroups(Ref ID:" + ref.getId() + "," + userId + ")");

		// TODO Auto-generated method stub
		return null;
	}

	public String getEntityDescription(Reference arg0)
	{
		return null;
	}

	public ResourceProperties getEntityResourceProperties(Reference ref)
	{
		try
		{
			String reference = ref.getReference();

			int lastIndex = reference.lastIndexOf(Entity.SEPARATOR);
			String postId = reference.substring(lastIndex, reference.length() - lastIndex);
			Entity entity = getPost(postId);
			return entity.getProperties();
		}
		catch (Exception e)
		{
			logger.warn("getEntity(): " + e);
			return null;
		}
	}

	/**
	 * From EntityProducer
	 */
	public String getEntityUrl(Reference ref)
	{
		return getEntity(ref).getUrl();
	}

	/**
	 * From EntityProducer
	 */
	public HttpAccess getHttpAccess()
	{
		return new HttpAccess()
		{
			public void handleAccess(HttpServletRequest arg0, HttpServletResponse arg1, Reference arg2, Collection arg3) throws EntityPermissionException, EntityNotDefinedException, EntityAccessOverloadException, EntityCopyrightException
			{
				try
				{
					String referenceString = arg2.getReference();
					String postId = referenceString.substring(referenceString.lastIndexOf(Entity.SEPARATOR) + 1);
					Post post = getPost(postId);
					String url = "http://btc224000006.lancs.ac.uk/blog-tool/blog.html?state=post&postId=" + postId + "&siteId=" + post.getSiteId();
					logger.debug("URL:" + url);
					arg1.sendRedirect(url);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		};
	}

	/**
	 * From EntityProducer
	 */
	public String getLabel()
	{
		// TODO Auto-generated method stub
		return "blog";
	}

	/**
	 * From EntityProducer
	 */
	public boolean parseEntityReference(String reference, Reference ref)
	{
		if (!reference.startsWith(BlogManager.REFERENCE_ROOT))
			return false;

		return true;
	}

	public boolean willArchiveMerge()
	{
		return true;
	}

	public String getEntityPrefix()
	{
		return BlogManager.ENTITY_PREFIX;
	}

	public boolean entityExists(String id)
	{
		String postId = id.substring(id.lastIndexOf(Entity.SEPARATOR));

		try
		{
			if (persistenceManager.postExists(postId))
				return true;
		}
		catch (Exception e)
		{
			logger.error("entityExists threw an exception", e);
		}

		return false;
	}

	public List<BlogMember> getAuthors(String siteId)
	{
		List<BlogMember> authors = sakaiProxy.getSiteMembers(siteId);
		for (BlogMember author : authors)
			persistenceManager.populateAuthorData(author, siteId);
		return authors;
	}

	public boolean restorePost(String postId)
	{
		try
		{
			Post post = persistenceManager.getPost(postId);
			return persistenceManager.restorePost(post);
		}
		catch (Exception e)
		{
			logger.error("Caught an exception whilst restoring post '" + postId + "'");
		}

		return false;
	}

	public boolean savePreferences(Preferences preferences)
	{
		return persistenceManager.savePreferences(preferences);
	}

	public Preferences getPreferences(String siteId, String userId)
	{
		if (userId == null)
			userId = sakaiProxy.getCurrentUserId();

		return persistenceManager.getPreferences(siteId, userId);
	}

	public void sendNewPostAlert(Post post)
	{
		Set<String> eachList = new TreeSet<String>();
		Set<String> digestList = new TreeSet<String>();

		Set<String> users = sakaiProxy.getSiteUsers(post.getSiteId());

		for (String userId : users)
		{
			Preferences prefs = getPreferences(post.getSiteId(), userId);
			if (Preferences.MAIL_NEVER.equals(prefs.getEmailFrequency()))
				continue;
			else if (Preferences.MAIL_EACH.equals(prefs.getEmailFrequency()))
				eachList.add(userId);
			else if (Preferences.MAIL_DIGEST.equals(prefs.getEmailFrequency()))
				digestList.add(userId);
		}

		String siteTitle = sakaiProxy.getSiteTitle(post.getSiteId());

		String message = "<b>" + sakaiProxy.getDisplayNameForTheUser(post.getCreatorId()) + "</b>" + " create a new post titled '" + post.getTitle() + "' in " + siteTitle + "<br/><br />Click <a href=\"" + post.getUrl() + "\">here</a> to read it.";

		sakaiProxy.sendEmailWithMessage(eachList, "[ " + siteTitle + " - Blog ] New Blog Post", message);
		sakaiProxy.addDigestMessage(digestList, "[ " + siteTitle + " - Blog ] New Blog Post", message);
	}

	public void sendNewCommentAlert(Comment comment)
	{
		try
		{
			Post post = getPost(comment.getPostId());

			BlogMember author = sakaiProxy.getMember(post.getCreatorId());

			String userId = author.getUserId();

			Preferences prefs = getPreferences(post.getSiteId(), userId);
			if (Preferences.MAIL_NEVER.equals(prefs.getEmailFrequency()))
				return;

			String siteTitle = sakaiProxy.getSiteTitle(post.getSiteId());

			String message = "<b>" + sakaiProxy.getDisplayNameForTheUser(comment.getCreatorId()) + "</b>" + " commented on your post titled '" + post.getTitle() + "' in " + siteTitle + "<br/><br />Click <a href=\"" + post.getUrl() + "\">here</a> to read it.";

			if (Preferences.MAIL_EACH.equals(prefs.getEmailFrequency()))
				sakaiProxy.sendEmailWithMessage(userId, "[ " + siteTitle + " - Blog ] New Comment", message);
			else if (Preferences.MAIL_DIGEST.equals(prefs.getEmailFrequency()))
				sakaiProxy.addDigestMessage(userId, "[ " + siteTitle + " - Blog ] New Comment", message);
		}
		catch (Exception e)
		{
			logger.error("Failed to send new comment alert.", e);
		}
	}

	public void setSakaiProxy(SakaiProxy sakaiProxy)
	{
		this.sakaiProxy = sakaiProxy;
	}

	public SakaiProxy getSakaiProxy()
	{
		return sakaiProxy;
	}
}
