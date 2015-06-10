package org.sakaiproject.clog.tool.entityprovider;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import lombok.Setter;

import org.apache.log4j.Logger;
import org.sakaiproject.clog.api.datamodel.Post;
import org.sakaiproject.clog.api.datamodel.Visibilities;
import org.sakaiproject.clog.api.ClogManager;
import org.sakaiproject.clog.api.QueryBean;
import org.sakaiproject.clog.api.SakaiProxy;
import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.entitybroker.EntityView;
import org.sakaiproject.entitybroker.entityprovider.CoreEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.annotations.EntityCustomAction;
import org.sakaiproject.entitybroker.entityprovider.annotations.EntityURLRedirect;
import org.sakaiproject.entitybroker.entityprovider.capabilities.ActionsExecutable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.AutoRegisterEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.CollectionResolvable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Createable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Deleteable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Describeable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Inputable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Outputable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Redirectable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Statisticable;
import org.sakaiproject.entitybroker.entityprovider.extension.ActionReturn;
import org.sakaiproject.entitybroker.entityprovider.extension.Formats;
import org.sakaiproject.entitybroker.entityprovider.search.Restriction;
import org.sakaiproject.entitybroker.entityprovider.search.Search;
import org.sakaiproject.entitybroker.exception.EntityException;
import org.sakaiproject.entitybroker.util.AbstractEntityProvider;
import org.sakaiproject.entitybroker.util.TemplateParseUtil;
import org.sakaiproject.util.ResourceLoader;

public class ClogPostEntityProvider extends AbstractEntityProvider implements CoreEntityProvider, AutoRegisterEntityProvider, Inputable, Outputable, /*Createable,*/ Describeable, CollectionResolvable, ActionsExecutable, Redirectable, Statisticable {

	private static final String[] EVENT_KEYS = new String[] { ClogManager.CLOG_POST_CREATED, ClogManager.CLOG_POST_DELETED, ClogManager.CLOG_POST_RECYCLED, ClogManager.CLOG_POST_RESTORED, ClogManager.CLOG_COMMENT_CREATED, ClogManager.CLOG_COMMENT_DELETED };

	@Setter
	private ClogManager clogManager;

	@Setter
	private SakaiProxy sakaiProxy = null;

	public final static String ENTITY_PREFIX = "clog-post";

	protected final Logger LOG = Logger.getLogger(getClass());

	public boolean entityExists(String id) {

		if (LOG.isDebugEnabled()) {
			LOG.debug("entityExists(" + id + ")");
        }

		if (id == null) {
			return false;
		}

		if ("".equals(id)) {
			return false;
        }

		try {
			return clogManager.getPost(id) != null;
		} catch (Exception e) {
			LOG.error("Caught exception whilst getting post.", e);
			return false;
		}
	}

	public Object getEntity(EntityReference ref) {

		if (LOG.isDebugEnabled()) {
			LOG.debug("getEntity(" + ref.getId() + ")");
        }

		String postId = ref.getId();

		if (postId == null || "".equals(postId)) {
			return new Post();
		}

		Post post = null;

		try {
			post = clogManager.getPost(postId);
		} catch (Exception e) {
			LOG.error("Caught exception whilst getting post with id '" + postId + "'", e);
		}

		if (post == null) {
			throw new EntityException("No post with id '" + postId + "'", ref.getReference(), HttpServletResponse.SC_BAD_REQUEST);
		}

		// TODO: Security !!!!

		return post;
	}

	@EntityCustomAction(action = "store", viewKey = EntityView.VIEW_NEW)
	public ActionReturn handleStore(Map<String, Object> params) {

        LOG.debug("handleStore");

		String userId = developerHelperService.getCurrentUserId();

		String id = (String) params.get("id");
		String visibility = (String) params.get("visibility");
		String title = (String) params.get("title");
		String content = (String) params.get("content");
		String siteId = (String) params.get("siteId");
		boolean commentable = Boolean.parseBoolean((String) params.get("commentable"));
		String mode = (String) params.get("mode");
		String groupsString = (String) params.get("groups");

		Post post = new Post();
		post.setId(id);
		post.setVisibility(visibility);
		post.setCreatorId(userId);
		post.setSiteId(siteId);
		post.setTitle(title);

        if (groupsString != null && groupsString.length() > 0) {
            post.setGroups(Arrays.asList(groupsString.split(",")));
        }

		post.setContent(content);
		post.setCommentable(commentable);

		boolean isNew = "".equals(post.getId());

		boolean isWithdrawn = false;

		boolean isBeingPublished = false;

		if (!isNew && !post.isAutoSave()) {
			try {
				Post oldPost = clogManager.getPostHeader(post.getId());
				if (oldPost.isReady() && post.isPrivate()) {
					isWithdrawn = true;
				} else if (oldPost.isPrivate() && post.isReady()) {
					isBeingPublished = true;
				}
			} catch (Exception e) {
				LOG.info("Failed to get post with id '" + post.getId() + "'. This could happen if this is the first time a post has been saved or published.", e);
			}
		}

		if (clogManager.savePost(post)) {
			if ((isNew || (mode != null && "publish".equals(mode))) && post.isReady() && !post.isAutoSave()) {
				sakaiProxy.postEvent(ClogManager.CLOG_POST_CREATED, post.getReference(), post.getSiteId());
			}

			if (isWithdrawn) {
				sakaiProxy.postEvent(ClogManager.CLOG_POST_WITHDRAWN, post.getReference(), post.getSiteId());
			}

			if (isBeingPublished) {
				sakaiProxy.postEvent(ClogManager.CLOG_POST_RESTORED, post.getReference(), post.getSiteId());
			}

            if (!post.isAutoSave()) {
                return new ActionReturn(clogManager.getSiteGroupsForCurrentUser(siteId));
            } else {
			    return new ActionReturn(post.getId());
            }
		} else {
			return new ActionReturn("FAIL");
        }
	}

	public Object getSampleEntity() {
		return new Post();
	}

	public String getEntityPrefix() {
		return ENTITY_PREFIX;
	}

	public String[] getHandledOutputFormats() {
		return new String[] { Formats.JSON, Formats.HTML };
	}

	public String[] getHandledInputFormats() {
		return new String[] { Formats.HTML, Formats.JSON, Formats.FORM };
	}

    @EntityURLRedirect(value = "/{prefix}/{postId}")
    public String mapPostUrl(String incomingUrl, Map<String, String> tokens) {

        String postId = tokens.get("postId");

        if (postId == null) {
            return null;
        }

        String extension = tokens.get(TemplateParseUtil.EXTENSION);

        if (extension.equalsIgnoreCase("html")) {
            try {
                Post post = clogManager.getPost(postId);

                if (post == null) {
                    return null;
                } else {
                    return post.getUrl();
                }
            } catch (Exception e) {
                LOG.error("Caught exception whilst getting post.", e);
                return null;
            }
        } else {
            return null;
        }
    }

	public List<Post> getEntities(EntityReference ref, Search search) {

		QueryBean query = new QueryBean();
        query.setVisibilities(Arrays.asList(new String[] { Visibilities.SITE, Visibilities.TUTOR, Visibilities.PRIVATE }));

		Restriction creatorRes = search.getRestrictionByProperty("creatorId");
		if (creatorRes != null) {
			query.setCreator(creatorRes.getStringValue());
        }

		Restriction groupRes = search.getRestrictionByProperty("groupId");
        if (groupRes != null) {
            query.setGroup(groupRes.getStringValue());
		    query.setVisibilities(Arrays.asList(new String[] { Visibilities.GROUP }));
        }

		Restriction locRes = search.getRestrictionByProperty(CollectionResolvable.SEARCH_LOCATION_REFERENCE);
		Restriction visibilities = search.getRestrictionByProperty("visibilities");

		Restriction autosaveRes = search.getRestrictionByProperty("autosaved");

		if (visibilities != null) {
			String visibilitiesValue = visibilities.getStringValue();
			String[] values = visibilitiesValue.split(",");
			query.setVisibilities(Arrays.asList(values));
		}

		if (locRes != null) {
			String location = locRes.getStringValue();
			String context = new EntityReference(location).getId();

			query.setSiteId(context);

			if ("!gateway".equals(context)) {
				query.setVisibilities(Arrays.asList(new String[] { Visibilities.PUBLIC }));
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

		if (autosaveRes != null) {
			query.setSearchAutoSaved(true);
        }

		try {
			return clogManager.getPosts(query);
		} catch (Exception e) {
			LOG.error("Caught exception whilst getting posts. Returning an empty list ...", e);
		}

        return new ArrayList<Post>();
	}

	public void deleteEntity(EntityReference ref, Map<String, Object> params) {

		LOG.debug("deleteEntity");

		String siteId = (String) params.get("siteId");

		if (clogManager.deletePost(ref.getId())) {
			String reference = ClogManager.REFERENCE_ROOT + "/" + siteId + "/posts/" + ref.getId();
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
			String reference = ClogManager.REFERENCE_ROOT + "/" + post.getSiteId() + "/posts/" + ref.getId();
			sakaiProxy.postEvent(ClogManager.CLOG_POST_RECYCLED, reference, post.getSiteId());

			return "SUCCESS";
		} else {
			return "FAIL";
		}
	}

	@EntityCustomAction(action = "restore", viewKey = EntityView.VIEW_LIST)
	public String handleRestore(Map<String,Object> params) {

		String userId = developerHelperService.getCurrentUserId();
		
		if (userId == null) {
			throw new EntityException("You must be logged in to restore posts","",HttpServletResponse.SC_UNAUTHORIZED);
		}
		
		if (!params.containsKey("posts")) {
			throw new EntityException("Bad request: a posts param must be supplied","",HttpServletResponse.SC_BAD_REQUEST);
		}
		
		String postIdsString = (String) params.get("posts");
		
		String[] postIds = postIdsString.split(",");
		
		for (String postId : postIds) {
			Post post = null;

			try {
				post = clogManager.getPost(postId);
			} catch (Exception e) {
				LOG.error("Failed to retrieve post with id '" + postId + "' during restore operation. Skipping restore ...",e);
				continue;
			}

			if (post == null) {
				LOG.info("Post id '" + postId + "' is invalid. Skipping restore ...");
				continue;
			}

			if (clogManager.restorePost(postId)) {
				String reference = ClogManager.REFERENCE_ROOT + "/" + post.getSiteId() + "/posts/" + postId;
				sakaiProxy.postEvent(ClogManager.CLOG_POST_RESTORED, reference, post.getSiteId());
			}
		}
		
		return "SUCCESS";
	}
	
	@EntityCustomAction(action = "remove", viewKey = EntityView.VIEW_LIST)
	public String handleRemove(Map<String,Object> params) {
		
		String userId = developerHelperService.getCurrentUserId();
		
		if(userId == null) {
			throw new EntityException("You must be logged in to delete posts","",HttpServletResponse.SC_UNAUTHORIZED);
		}
		
		if(!params.containsKey("posts")) {
			throw new EntityException("Bad request: a posts param must be supplied","",HttpServletResponse.SC_BAD_REQUEST);
		}
		
		String siteId = (String) params.get("site");
		
		if(siteId == null) {
			throw new EntityException("Bad request: a site param must be supplied","",HttpServletResponse.SC_BAD_REQUEST);
		}
		
		String postIdsString = (String) params.get("posts");
		
		String[] postIds = postIdsString.split(",");
		
		for(String postId : postIds) {

			if (clogManager.deletePost(postId)) {
				String reference = ClogManager.REFERENCE_ROOT + "/" + siteId + "/posts/" + postId;
				sakaiProxy.postEvent(ClogManager.CLOG_POST_DELETED, reference, siteId);
			}
		}
		
		return "SUCCESS";
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
		ResourceLoader msgs = new ResourceLoader("org.sakaiproject.clog.bundle.Events");
		msgs.setContextLocale(locale);
		for (int i = 0; i < EVENT_KEYS.length; i++) {
			localeEventNames.put(EVENT_KEYS[i], msgs.getString(EVENT_KEYS[i]));
		}
		return localeEventNames;
	}
}
