package org.sakaiproject.clog.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.sakaiproject.clog.api.ClogFunctions;
import org.sakaiproject.clog.api.ClogManager;
import org.sakaiproject.clog.api.ClogMember;
import org.sakaiproject.clog.api.QueryBean;
import org.sakaiproject.clog.api.SakaiProxy;
import org.sakaiproject.clog.api.XmlDefs;
import org.sakaiproject.clog.api.datamodel.Comment;
import org.sakaiproject.clog.api.datamodel.GlobalPreferences;
import org.sakaiproject.clog.api.datamodel.Post;
import org.sakaiproject.clog.api.datamodel.Visibilities;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.HttpAccess;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.entity.api.ResourceProperties;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ClogManagerImpl implements ClogManager {
	private Logger logger = Logger.getLogger(ClogManagerImpl.class);

	private PersistenceManager persistenceManager;

	private ClogSecurityManager securityManager;

	private SakaiProxy sakaiProxy;

	private boolean importBlog1Data = false;
	private boolean importBlog2Data = false;

	public void init() {
		if (logger.isDebugEnabled())
			logger.debug("init()");

		logger.info("Registering Clog functions ...");

		sakaiProxy.registerFunction(ClogFunctions.CLOG_POST_CREATE);
		sakaiProxy.registerFunction(ClogFunctions.CLOG_POST_READ_ANY);
		sakaiProxy.registerFunction(ClogFunctions.CLOG_POST_UPDATE_ANY);
		sakaiProxy.registerFunction(ClogFunctions.CLOG_POST_UPDATE_OWN);
		sakaiProxy.registerFunction(ClogFunctions.CLOG_POST_DELETE_ANY);
		sakaiProxy.registerFunction(ClogFunctions.CLOG_POST_DELETE_OWN);
		sakaiProxy.registerFunction(ClogFunctions.CLOG_COMMENT_CREATE);
		sakaiProxy.registerFunction(ClogFunctions.CLOG_COMMENT_READ_ANY);
		sakaiProxy.registerFunction(ClogFunctions.CLOG_COMMENT_READ_OWN);
		sakaiProxy.registerFunction(ClogFunctions.CLOG_COMMENT_UPDATE_ANY);
		sakaiProxy.registerFunction(ClogFunctions.CLOG_COMMENT_UPDATE_OWN);
		sakaiProxy.registerFunction(ClogFunctions.CLOG_COMMENT_DELETE_ANY);
		sakaiProxy.registerFunction(ClogFunctions.CLOG_COMMENT_DELETE_OWN);
		sakaiProxy.registerFunction(ClogFunctions.CLOG_MODIFY_PERMISSIONS);

		logger.info("Registered Clog functions ...");

		sakaiProxy.registerEntityProducer(this);

		persistenceManager = new PersistenceManager(sakaiProxy);

		if (importBlog1Data)
			persistenceManager.importBlog1Data();

		if (importBlog2Data)
			persistenceManager.importBlog2Data();

		securityManager = new ClogSecurityManager(sakaiProxy);
	}

	public Post getPost(String postId) throws Exception {
		if (logger.isDebugEnabled())
			logger.debug("getPost(" + postId + ")");

		Post post = persistenceManager.getPost(postId);
		if (securityManager.canCurrentUserReadPost(post))
			return post;
		else
			throw new Exception("The current user does not have permissions to read this post.");
	}

	public List<Post> getPosts(String placementId) throws Exception {
		// Get all the posts for the supplied site and filter them through the
		// security manager
		List<Post> filtered;
		List<Post> unfiltered = persistenceManager.getAllPost(placementId);
		filtered = securityManager.filter(unfiltered);
		return filtered;
	}

	public List<Post> getPosts(QueryBean query) throws Exception {
		// Get all the posts for the supplied site and filter them through the
		// security manager
		List<Post> filtered;
		List<Post> unfiltered = persistenceManager.getPosts(query);
		if (query.isSkipFilter()) {
			return unfiltered;
		} else {
			filtered = securityManager.filter(unfiltered);
			return filtered;
		}
	}

	public boolean savePost(Post post) {
		try {
			// Post asPost = persistenceManager.getAutosavedPost(post.getId());

			// if(asPost != null)
			// post.setId("");

			return persistenceManager.savePost(post);
		} catch (Exception e) {
			logger.error("Caught exception whilst creating post", e);
		}

		return false;
	}

	public boolean deletePost(String postId) {
		try {
			Post post = persistenceManager.getPost(postId);
			if (securityManager.canCurrentUserDeletePost(post))
				return persistenceManager.deletePost(post);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	public boolean saveComment(Comment comment) {
		try {
			return persistenceManager.saveComment(comment);
		} catch (Exception e) {
			logger.error("Caught exception whilst saving comment", e);
		}

		return false;
	}

	public boolean deleteComment(String commentId) {
		try {
			if (persistenceManager.deleteComment(commentId)) {
				return true;
			}
		} catch (Exception e) {
			logger.error("Caught exception whilst deleting comment.", e);
		}

		return false;
	}

	public boolean recyclePost(String postId) {
		try {
			Post post = persistenceManager.getPost(postId);

			if (securityManager.canCurrentUserDeletePost(post)) {
				if (persistenceManager.recyclePost(post)) {
					post.setVisibility(Visibilities.RECYCLED);
					return true;
				}
			}
		} catch (Exception e) {
			logger.error("Caught an exception whilst recycling post '" + postId + "'");
		}

		return false;
	}

	public ClogSecurityManager getSecurityManager() {
		return securityManager;
	}

	public void setSecurityManager(ClogSecurityManager securityManager) {
		this.securityManager = securityManager;
	}

	public void setPersistenceManager(PersistenceManager pm) {
		this.persistenceManager = pm;
	}

	private String serviceName() {
		return ClogManager.class.getName();
	}

	public String archive(String siteId, Document doc, Stack stack, String archivePath, List attachments) {
		if (logger.isDebugEnabled())
			logger.debug("archive(siteId:" + siteId + ",archivePath:" + archivePath + ")");

		StringBuilder results = new StringBuilder();

		results.append(getLabel() + ": Started.\n");

		int postCount = 0;

		try {
			// start with an element with our very own (service) name
			Element element = doc.createElement(serviceName());
			element.setAttribute("version", "2.5.x");
			((Element) stack.peek()).appendChild(element);
			stack.push(element);

			Element clog = doc.createElement("clog");
			List<Post> posts = getPosts(siteId);
			if (posts != null && posts.size() > 0) {
				for (Post post : posts) {
					Element postElement = post.toXml(doc, stack);
					clog.appendChild(postElement);
					postCount++;
				}
			}

			((Element) stack.peek()).appendChild(clog);
			stack.push(clog);

			stack.pop();

			results.append(getLabel() + ": Finished. " + postCount + " post(s) archived.\n");
		} catch (Exception any) {
			results.append(getLabel() + ": exception caught. Message: " + any.getMessage());
			logger.warn(getLabel() + " exception caught. Message: " + any.getMessage());
		}

		stack.pop();

		return results.toString();
	}

	/**
	 * From EntityProducer
	 */
	public String merge(String siteId, Element root, String archivePath, String fromSiteId, Map attachmentNames, Map userIdTrans, Set userListAllowImport) {
		logger.debug("merge(siteId:" + siteId + ",root tagName:" + root.getTagName() + ",archivePath:" + archivePath + ",fromSiteId:" + fromSiteId);

		StringBuilder results = new StringBuilder();

		int postCount = 0;

		NodeList postNodes = root.getElementsByTagName(XmlDefs.POST);
		final int numberPosts = postNodes.getLength();

		for (int i = 0; i < numberPosts; i++) {
			Node child = postNodes.item(i);
			if (child.getNodeType() != Node.ELEMENT_NODE) {
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

		return results.toString();
	}

	/**
	 * From EntityProducer
	 */
	public Entity getEntity(Reference ref) {
		if (logger.isDebugEnabled())
			logger.debug("getEntity(Ref ID:" + ref.getId() + ")");

		Entity rv = null;

		try {
			String reference = ref.getReference();

			int lastIndex = reference.lastIndexOf(Entity.SEPARATOR);
			String postId = reference.substring(lastIndex + 1);
			rv = getPost(postId);
		} catch (Exception e) {
			logger.warn("getEntity(): " + e);
		}

		return rv;
	}

	/**
	 * From EntityProducer
	 */
	public Collection getEntityAuthzGroups(Reference ref, String userId) {
		if (logger.isDebugEnabled())
			logger.debug("getEntityAuthzGroups(Ref ID:" + ref.getId() + "," + userId + ")");

		List ids = new ArrayList();
		ids.add("/site/" + ref.getContext());
		return ids;
	}

	public String getEntityDescription(Reference arg0) {
		return null;
	}

	public ResourceProperties getEntityResourceProperties(Reference ref) {
		try {
			String reference = ref.getReference();

			int lastIndex = reference.lastIndexOf(Entity.SEPARATOR);
			String postId = reference.substring(lastIndex, reference.length() - lastIndex);
			Entity entity = getPost(postId);
			return entity.getProperties();
		} catch (Exception e) {
			logger.warn("getEntity(): " + e);
			return null;
		}
	}

	/**
	 * From EntityProducer
	 */
	public String getEntityUrl(Reference ref) {
		return getEntity(ref).getUrl();
	}

	/**
	 * From EntityProducer
	 */
	public HttpAccess getHttpAccess() {
		return null;
	}

	/**
	 * From EntityProducer
	 */
	public String getLabel() {
		return "clog";
	}

	/**
	 * From EntityProducer
	 */
	public boolean parseEntityReference(String referenceString, Reference reference) {

		String[] parts = referenceString.split(Entity.SEPARATOR);

		if (parts.length < 2 || !parts[1].equals("clog")) // Leading slash adds
														  // an empty element
			return false;

		if (parts.length == 2) {
			reference.set("sakai:clog", "", "", null, "");
			return true;
		}

		String siteId = parts[2];
		String subType = parts[3];
		String entityId = parts[4];

		if ("posts".equals(subType)) {
			reference.set("clog", "posts", entityId, null, siteId);
			return true;
		}

		return false;
	}

	public boolean willArchiveMerge() {
		return true;
	}

	public String getEntityPrefix() {
		return ClogManager.ENTITY_PREFIX;
	}

	public boolean entityExists(String id) {
		String postId = id.substring(id.lastIndexOf(Entity.SEPARATOR));

		try {
			if (persistenceManager.postExists(postId))
				return true;
		} catch (Exception e) {
			logger.error("entityExists threw an exception", e);
		}

		return false;
	}

	public List<ClogMember> getAuthors(String siteId) {
		List<ClogMember> authors = new ArrayList<ClogMember>();

		if ("!gateway".equals(siteId)) {
			authors = persistenceManager.getPublicBloggers();
		} else {
			authors = sakaiProxy.getSiteMembers(siteId);
			for (ClogMember author : authors)
				persistenceManager.populateAuthorData(author, siteId);
		}

		return authors;
	}

	public boolean restorePost(String postId) {
		try {
			Post post = persistenceManager.getPost(postId);
			return persistenceManager.restorePost(post);
		} catch (Exception e) {
			logger.error("Caught an exception whilst restoring post '" + postId + "'");
		}

		return false;
	}

	public boolean saveGlobalPreferences(GlobalPreferences preferences) {
		return persistenceManager.saveGlobalPreferences(preferences);
	}

	public GlobalPreferences getGlobalPreferences(String userId) {
		if (userId == null)
			userId = sakaiProxy.getCurrentUserId();

		return persistenceManager.getGlobalPreferences(userId);
	}

	public void setSakaiProxy(SakaiProxy sakaiProxy) {
		this.sakaiProxy = sakaiProxy;
	}

	public SakaiProxy getSakaiProxy() {
		return sakaiProxy;
	}

	public void setImportBlog1Data(boolean importBlog1Data) {
		this.importBlog1Data = importBlog1Data;
	}

	public boolean isImportBlog1Data() {
		return importBlog1Data;
	}

	public void importBlog1Data() {
		persistenceManager.importBlog1Data();
	}

	public void importBlog2Data() {
		persistenceManager.importBlog2Data();
	}

	public void setImportBlog2Data(boolean importBlog2Data) {
		this.importBlog2Data = importBlog2Data;
	}

	public boolean isImportBlog2Data() {
		return importBlog2Data;
	}

	public boolean deleteAutosavedCopy(String postId) {
		return persistenceManager.deleteAutosavedCopy(postId);
	}
}
