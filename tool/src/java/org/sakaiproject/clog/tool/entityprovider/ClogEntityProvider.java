package org.sakaiproject.clog.tool.entityprovider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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
import org.sakaiproject.clog.api.ClogSecurityManager;
import org.sakaiproject.clog.api.QueryBean;
import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.entitybroker.EntityView;
import org.sakaiproject.entitybroker.entityprovider.annotations.EntityCustomAction;
import org.sakaiproject.entitybroker.entityprovider.capabilities.ActionsExecutable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.AutoRegisterEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Describeable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Outputable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Statisticable;
import org.sakaiproject.entitybroker.entityprovider.extension.Formats;
import org.sakaiproject.entitybroker.exception.EntityException;
import org.sakaiproject.entitybroker.util.AbstractEntityProvider;
import org.sakaiproject.util.ResourceLoader;

public class ClogEntityProvider extends AbstractEntityProvider implements AutoRegisterEntityProvider, Outputable, Describeable, ActionsExecutable {
	
	public final static String ENTITY_PREFIX = "clog";

	private final Logger LOG = Logger.getLogger(getClass());

	@Setter
	private ClogManager clogManager;
	
	@Setter
	private ClogSecurityManager clogSecurityManager;

	public Object getSampleEntity() {
		return new Post();
	}

	public String getEntityPrefix() {
		return ENTITY_PREFIX;
	}

	public String[] getHandledOutputFormats() {
		return new String[] { Formats.JSON,Formats.XML };
	}

	@EntityCustomAction(action = "site", viewKey = EntityView.VIEW_LIST)
	public List<SparsePost> getPostsForSite(EntityView view, Map<String, Object> params) {
		
		String userId = developerHelperService.getCurrentUserId();
		
		if(userId == null) {
			throw new EntityException("You must be logged in to retrieve a post","",HttpServletResponse.SC_UNAUTHORIZED);
		}
		
        String siteId = view.getPathSegment(2);
        
        if(siteId == null) {
        	throw new EntityException("Bad request: To get the posts in a site you need a url like '/direct/clog/site/SITEID.json'"
        									,"",HttpServletResponse.SC_BAD_REQUEST);
        }
        
        if(!clogSecurityManager.canAccessSiteAndTool(siteId)) {
			throw new EntityException("Access denied.","",HttpServletResponse.SC_UNAUTHORIZED);
        }
		
		List<Post> posts = new ArrayList<Post>();

		QueryBean query = new QueryBean();
		query.setVisibilities(new String[] { Visibilities.SITE, Visibilities.MAINTAINER, Visibilities.PRIVATE });
		query.setSiteId(siteId);

		if ("!gateway".equals(siteId)) {
			query.setVisibilities(new String[] { Visibilities.PUBLIC });
			query.setSiteId("");
		} else if (siteId.startsWith("~") && query.getVisibilities().equals(Arrays.asList(Visibilities.PUBLIC))) {
			// We are on a MyWorkspace and PUBLIC has been requested. PUBLIC
			// posts always
			// retain the site ID of the site they were originally created
			// in so a site id
			// query for the MyWorkspace will fail. We need to flatten the
			// site id.
			query.setSiteId("");
		} else {
			// Check the site access for this user
		}

		try {
			posts = clogManager.getPosts(query);
		} catch (Exception e) {
			LOG.error("Caught exception whilst getting posts.", e);
			throw new EntityException("Failed to retrieve posts for site " + siteId,"",HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}

		List<Post> filteredPosts = clogSecurityManager.filter(posts);
		List<SparsePost> sparsePosts = new ArrayList<SparsePost>(filteredPosts.size());
		
		for(Post filteredPost : filteredPosts) {
			sparsePosts.add(new SparsePost(filteredPost));
		}
		
		return sparsePosts;
	}

	@EntityCustomAction(action = "post", viewKey = EntityView.VIEW_LIST)
	public Post handlePost(EntityView view, Map<String, Object> params) {
		
		String userId = developerHelperService.getCurrentUserId();
		
		if(userId == null) {
			throw new EntityException("You must be logged in to retrieve a post","",HttpServletResponse.SC_UNAUTHORIZED);
		}
		
        String postId = view.getPathSegment(2);

		if (postId == null) {
			throw new EntityException("Invalid path provided: expect to receive the post id.","",HttpServletResponse.SC_BAD_REQUEST);
		}

		Post post = null;

		try {
			post = clogManager.getPost(postId);
		} catch (Exception e) {
			throw new EntityException("Failed to get post for post id.","",HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		if(clogSecurityManager.canCurrentUserReadPost(post)) {
			return post;
		} else {
			throw new EntityException("Not allowed to read this post.","",HttpServletResponse.SC_UNAUTHORIZED);
		}
	}
	
	@EntityCustomAction(action = "user", viewKey = EntityView.VIEW_LIST)
	public List<Post> handleUser(EntityView view, Map<String, Object> params) {
		
		String callingUserId = developerHelperService.getCurrentUserId();
		
		if(callingUserId == null) {
			throw new EntityException("You must be logged in to retrieve a post","",HttpServletResponse.SC_UNAUTHORIZED);
		}
		
        String requestedUserId = view.getPathSegment(2);
        
        List<Post> posts = new ArrayList<Post>();

		QueryBean query = new QueryBean();
		query.setVisibilities(new String[] { Visibilities.SITE, Visibilities.MAINTAINER, Visibilities.PRIVATE });
		query.setCreator(requestedUserId);

		try {
			posts = clogManager.getPosts(query);
		} catch (Exception e) {
			LOG.error("Caught exception whilst getting posts.", e);
			throw new EntityException("Failed to retrieve posts for user " + requestedUserId,"",HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}

		List<Post> filteredPosts = clogSecurityManager.filter(posts);
		
		for(Post filteredPost : filteredPosts) {
			filteredPost.minimise();
		}
		
		return filteredPosts;
	}
}
