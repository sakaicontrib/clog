package org.sakaiproject.clog.tool.entityprovider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.log4j.Logger;
import org.sakaiproject.clog.api.datamodel.Post;
import org.sakaiproject.clog.api.datamodel.Visibilities;
import org.sakaiproject.clog.api.ClogManager;
import org.sakaiproject.clog.api.QueryBean;
import org.sakaiproject.clog.api.SakaiProxy;
import org.sakaiproject.entitybroker.DeveloperHelperService;
import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.entitybroker.EntityView;
import org.sakaiproject.entitybroker.entityprovider.CoreEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.annotations.EntityCustomAction;
import org.sakaiproject.entitybroker.entityprovider.capabilities.ActionsExecutable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.AutoRegisterEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.CollectionResolvable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Createable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Deleteable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Describeable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Inputable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Outputable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Statisticable;
import org.sakaiproject.entitybroker.entityprovider.extension.Formats;
import org.sakaiproject.entitybroker.entityprovider.search.Restriction;
import org.sakaiproject.entitybroker.entityprovider.search.Search;
import org.sakaiproject.entitybroker.exception.EntityException;
import org.sakaiproject.entitybroker.util.AbstractEntityProvider;
import org.sakaiproject.util.ResourceLoader;

public class ClogPostEntityProvider extends AbstractEntityProvider implements CoreEntityProvider, AutoRegisterEntityProvider, Inputable, Outputable, Createable, Describeable, Deleteable, CollectionResolvable, ActionsExecutable, Statisticable {
	private static final String[] EVENT_KEYS = new String[] { ClogManager.CLOG_POST_CREATED, ClogManager.CLOG_POST_DELETED, ClogManager.CLOG_POST_RECYCLED, ClogManager.CLOG_POST_RESTORED, ClogManager.CLOG_COMMENT_CREATED, ClogManager.CLOG_COMMENT_DELETED };

	private ClogManager clogManager;

	public void setClogManager(ClogManager clogManager) {
		this.clogManager = clogManager;
	}

	private DeveloperHelperService developerService = null;

	private SakaiProxy sakaiProxy = null;

	public final static String ENTITY_PREFIX = "clog-post";

	protected final Logger LOG = Logger.getLogger(getClass());

	private boolean allowImportAction = false;

	public boolean entityExists(String id) {
		if (LOG.isDebugEnabled())
			LOG.debug("entityExists(" + id + ")");

		if (id == null) {
			return false;
		}

		if ("".equals(id))
			return false;

		try {
			return (clogManager.getPost(id) != null);
		} catch (Exception e) {
			LOG.error("Caught exception whilst getting post.", e);
			return false;
		}
	}

	public Object getEntity(EntityReference ref) {

		if (LOG.isDebugEnabled())
			LOG.debug("getEntity(" + ref.getId() + ")");

		String id = ref.getId();

		if (id == null || "".equals(id)) {
			return new Post();
		}

		Post post = null;

		try {
			post = clogManager.getPost(id);
		} catch (Exception e) {
			LOG.error("Caught exception whilst getting post.", e);
		}

		if (post == null) {
			throw new IllegalArgumentException("Post not found");
		}

		// TODO: Security !!!!

		return post;
	}

	public String createEntity(EntityReference ref, Object entity, Map<String, Object> params) {
		if (LOG.isDebugEnabled())
			LOG.debug("createEntity");

		String userId = developerService.getCurrentUserId();

		String id = (String) params.get("id");
		String visibility = (String) params.get("visibility");
		String title = (String) params.get("title");
		String content = (String) params.get("content");
		String siteId = (String) params.get("siteId");
		boolean commentable = Boolean.parseBoolean((String) params.get("commentable"));
		String mode = (String) params.get("mode");

		Post post = new Post();
		post.setId(id);
		post.setVisibility(visibility);
		post.setCreatorId(userId);
		post.setSiteId(siteId);
		post.setTitle(title);
		post.setContent(content);
		post.setCommentable(commentable);

		boolean isNew = "".equals(post.getId());

		if (clogManager.savePost(post)) {
			if ((isNew || (mode != null && "publish".equals(mode))) && post.isReady() && !post.isAutoSave()) {
				String reference = ClogManager.REFERENCE_ROOT + "/" + siteId + "/post/" + post.getId();
				sakaiProxy.postEvent(ClogManager.CLOG_POST_CREATED, reference, post.getSiteId());

				// Send an email to all site participants apart from the author
				clogManager.sendNewPostAlert(post);
			}

			return post.getId();
		} else
			return "FAIL";
	}

	public Object getSampleEntity() {
		return new Post();
	}

	public String getEntityPrefix() {
		return ENTITY_PREFIX;
	}

	public String[] getHandledOutputFormats() {
		return new String[] { Formats.JSON };
	}

	public String[] getHandledInputFormats() {
		return new String[] { Formats.HTML, Formats.JSON, Formats.FORM };
	}

	public List<Post> getEntities(EntityReference ref, Search search) {
		List<Post> posts = new ArrayList<Post>();

		Restriction creatorRes = search.getRestrictionByProperty("creatorId");

		Restriction locRes = search.getRestrictionByProperty(CollectionResolvable.SEARCH_LOCATION_REFERENCE);
		Restriction visibilities = search.getRestrictionByProperty("visibilities");

		Restriction autosaveRes = search.getRestrictionByProperty("autosaved");

		QueryBean query = new QueryBean();
		query.setVisibilities(new String[] { Visibilities.SITE, Visibilities.MAINTAINER, Visibilities.PRIVATE });

		if (visibilities != null) {
			String visibilitiesValue = visibilities.getStringValue();
			String[] values = visibilitiesValue.split(",");
			query.setVisibilities(values);
		}

		if (locRes != null) {
			String location = locRes.getStringValue();
			String context = new EntityReference(location).getId();

			query.setSiteId(context);

			if ("!gateway".equals(context)) {
				query.setVisibilities(new String[] { Visibilities.PUBLIC });
				query.setSiteId("");
			} else if (context.startsWith("~") && query.getVisibilities().equals(Arrays.asList(Visibilities.PUBLIC))) {
				// We are on a MyWorkspace and PUBLIC has been requested. PUBLIC
				// posts always
				// retain the site ID of the site they were originally created
				// in so a site id
				// query for the MyWorkspace will fail. We need to flatten the
				// site id.
				query.setSiteId("");
			}
		}

		if (creatorRes != null)
			query.setCreator(creatorRes.getStringValue());

		if (autosaveRes != null)
			query.setSearchAutoSaved(true);

		try {
			posts = clogManager.getPosts(query);
		} catch (Exception e) {
			LOG.error("Caught exception whilst getting posts.", e);
		}

		return posts;
	}

	public void deleteEntity(EntityReference ref, Map<String, Object> params) {
		if (LOG.isDebugEnabled())
			LOG.debug("deleteEntity");

		String siteId = (String) params.get("siteId");

		if (clogManager.deletePost(ref.getId())) {
			String reference = ClogManager.REFERENCE_ROOT + "/" + siteId + "/post/" + ref.getId();
			sakaiProxy.postEvent(ClogManager.CLOG_POST_DELETED, reference, siteId);
		}
	}

	@EntityCustomAction(action = "recycle", viewKey = EntityView.VIEW_SHOW)
	public String handleRecycle(EntityReference ref) {
		String postId = ref.getId();

		if (postId == null)
			throw new IllegalArgumentException("Invalid path provided: expect to receive the post id");

		Post post = null;

		try {
			post = clogManager.getPost(postId);
		} catch (Exception e) {
		}

		if (post == null)
			throw new IllegalArgumentException("Invalid post id");

		if (clogManager.recyclePost(postId)) {
			String reference = ClogManager.REFERENCE_ROOT + "/" + post.getSiteId() + "/post/" + ref.getId();
			sakaiProxy.postEvent(ClogManager.CLOG_POST_RECYCLED, reference, post.getSiteId());

			return "SUCCESS";
		} else {
			return "FAIL";
		}
	}

	@EntityCustomAction(action = "restore", viewKey = EntityView.VIEW_SHOW)
	public String handleRestore(EntityReference ref) {
		String postId = ref.getId();

		if (postId == null) {
			throw new IllegalArgumentException("Invalid path provided: expect to receive the post id");
		}

		Post post = null;

		try {
			post = clogManager.getPost(postId);
		} catch (Exception e) {
		}

		if (post == null)
			throw new IllegalArgumentException("Invalid post id");

		if (clogManager.restorePost(postId)) {
			String reference = ClogManager.REFERENCE_ROOT + "/" + post.getSiteId() + "/post/" + ref.getId();
			sakaiProxy.postEvent(ClogManager.CLOG_POST_RESTORED, reference, post.getSiteId());

			return "SUCCESS";
		} else {
			return "FAIL";
		}
	}

	@EntityCustomAction(action = "deleteAutosavedCopy", viewKey = EntityView.VIEW_SHOW)
	public String handleDeleteAutosavedCopy(EntityReference ref) {
		String postId = ref.getId();

		if (postId == null) {
			throw new IllegalArgumentException("Invalid path provided: expect to receive the post id");
		}

		if (clogManager.deleteAutosavedCopy(postId))
			return "SUCCESS";
		else
			throw new EntityException("Failed to delete the autosaved copy.", postId);
	}

	@EntityCustomAction(action = "import1", viewKey = EntityView.VIEW_LIST)
	public String handleImport1(EntityReference ref) {
		if (allowImportAction)
			clogManager.importBlog1Data();
		return "SUCCESS";
	}

	@EntityCustomAction(action = "import2", viewKey = EntityView.VIEW_LIST)
	public String handleImport2(EntityReference ref) {
		if (allowImportAction)
			clogManager.importBlog2Data();
		return "SUCCESS";
	}

	/**
	 * From Statisticable
	 */
	public String getAssociatedToolId() {
		return "sakai.clog";
	}

	/**
	 * From Statisticable
	 */
	public String[] getEventKeys() {
		String[] temp = new String[EVENT_KEYS.length];
		System.arraycopy(EVENT_KEYS, 0, temp, 0, EVENT_KEYS.length);
		return temp;
	}

	/**
	 * From Statisticable
	 */
	public Map<String, String> getEventNames(Locale locale) {
		Map<String, String> localeEventNames = new HashMap<String, String>();
		ResourceLoader msgs = new ResourceLoader("Events");
		msgs.setContextLocale(locale);
		for (int i = 0; i < EVENT_KEYS.length; i++) {
			localeEventNames.put(EVENT_KEYS[i], msgs.getString(EVENT_KEYS[i]));
		}
		return localeEventNames;
	}

	public void setDeveloperService(DeveloperHelperService developerService) {
		this.developerService = developerService;
	}

	public void setSakaiProxy(SakaiProxy sakaiProxy) {
		this.sakaiProxy = sakaiProxy;
	}

	public void setAllowImportAction(boolean allowImportAction) {
		this.allowImportAction = allowImportAction;
	}
}
