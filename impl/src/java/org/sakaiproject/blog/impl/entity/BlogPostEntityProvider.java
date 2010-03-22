package org.sakaiproject.blog.impl.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.sakaiproject.blog.api.BlogManager;
import org.sakaiproject.blog.api.QueryBean;
import org.sakaiproject.blog.api.SakaiProxy;
import org.sakaiproject.blog.api.datamodel.Post;
import org.sakaiproject.blog.api.datamodel.Visibilities;
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
import org.sakaiproject.util.ResourceLoader;

public class BlogPostEntityProvider implements CoreEntityProvider, AutoRegisterEntityProvider, Inputable, Outputable, Createable, Describeable, Deleteable, CollectionResolvable, ActionsExecutable, Statisticable
{
	private static final String[] EVENT_KEYS
		= new String[] {
			BlogManager.BLOG_POST_CREATED,
			BlogManager.BLOG_POST_DELETED,
			BlogManager.BLOG_COMMENT_CREATED,
			BlogManager.BLOG_COMMENT_DELETED};
	
	private BlogManager blogManager;

	private DeveloperHelperService developerService = null;
	
	private SakaiProxy sakaiProxy  = null;

	public final static String ENTITY_PREFIX = "blog-post";

	protected final Logger LOG = Logger.getLogger(getClass());
	
	private boolean allowImportAction = false;

	public boolean entityExists(String id)
	{
		if (LOG.isDebugEnabled())
			LOG.debug("entityExists(" + id + ")");

		if (id == null)
		{
			return false;
		}

		if ("".equals(id))
			return false;

		try
		{
			return (blogManager.getPost(id) != null);
		}
		catch (Exception e)
		{
			LOG.error("Caught exception whilst getting post.", e);
			return false;
		}
	}

	public Object getEntity(EntityReference ref)
	{

		if (LOG.isDebugEnabled())
			LOG.debug("getEntity(" + ref.getId() + ")");

		String id = ref.getId();

		if (id == null || "".equals(id))
		{
			return new Post();
		}

		Post post = null;

		try
		{
			post = blogManager.getPost(id);
		}
		catch (Exception e)
		{
			LOG.error("Caught exception whilst getting post.", e);
		}

		if (post == null)
		{
			throw new IllegalArgumentException("Post not found");
		}

		// TODO: Security !!!!

		return post;
	}

	public String createEntity(EntityReference ref, Object entity, Map<String, Object> params)
	{
		if (LOG.isDebugEnabled())
			LOG.debug("createEntity");

		String userId = developerService.getCurrentUserId();

		String json = (String) params.get("json");

		JSONObject jsonObject = JSONObject.fromObject(json);

		String id = jsonObject.getString("id");
		String visibility = jsonObject.getString("visibility");
		String title = jsonObject.getString("title");
		String content = jsonObject.getString("content");
		String siteId = jsonObject.getString("siteId");
		boolean commentable = jsonObject.getBoolean("commentable");

		Post post = new Post();
		post.setId(id);
		post.setVisibility(visibility);
		post.setCreatorId(userId);
		post.setSiteId(siteId);
		post.setTitle(title);
		post.setContent(content);
		post.setCommentable(commentable);
		
		boolean isNew = "".equals(post.getId());

		if (blogManager.savePost(post))
		{
			if(isNew)
			{
				sakaiProxy.postEvent(BlogManager.BLOG_POST_CREATED,post.getId(),post.getSiteId());
				
				// Send an email to all site participants apart from the author
				blogManager.sendNewPostAlert(post);
			}
			
			return post.getId();
		}
		else
			return "FAIL";
	}

	public Object getSampleEntity()
	{
		return new Post();
	}

	public String getEntityPrefix()
	{
		return ENTITY_PREFIX;
	}

	public BlogManager getBlogManager()
	{
		return blogManager;
	}

	public void setBlogManager(BlogManager blogManager)
	{
		this.blogManager = blogManager;
	}

	public String[] getHandledOutputFormats()
	{
		return new String[] { Formats.JSON };
	}

	public String[] getHandledInputFormats()
	{
		return new String[] { Formats.HTML, Formats.JSON, Formats.FORM };
	}

	public List<Post> getEntities(EntityReference ref, Search search)
	{
		List<Post> posts = new ArrayList<Post>();

		Restriction creatorRes = search.getRestrictionByProperty("creatorId");

		Restriction locRes = search.getRestrictionByProperty(CollectionResolvable.SEARCH_LOCATION_REFERENCE);
		Restriction visibilities = search.getRestrictionByProperty("visibilities");

		QueryBean query = new QueryBean();
		query.setVisibilities(new String[] {Visibilities.READY,Visibilities.PRIVATE});

		if (locRes != null)
		{
			String location = locRes.getStringValue();
			String context = new EntityReference(location).getId();

			query.setSiteId(context);
		}

		if (creatorRes != null)
			query.setCreator(creatorRes.getStringValue());

		if (visibilities != null)
		{
			String visibilitiesValue = visibilities.getStringValue();
			String[] values = visibilitiesValue.split(",");
			query.setVisibilities(values);
		}

		try
		{
			posts = blogManager.getPosts(query);
		}
		catch (Exception e)
		{
			LOG.error("Caught exception whilst getting posts.", e);
		}

		return posts;
	}

	public void deleteEntity(EntityReference ref, Map<String, Object> params)
	{
		if (LOG.isDebugEnabled())
			LOG.debug("deleteEntity");
		
		String siteId = (String) params.get("siteId");
		
		if(blogManager.deletePost(ref.getId()))
		{
			sakaiProxy.postEvent(BlogManager.BLOG_POST_DELETED,ref.getId(),siteId);
		}
	}

	@EntityCustomAction(action = "recycle", viewKey = EntityView.VIEW_SHOW)
	public String handleRecycle(EntityReference ref)
	{
		String postId = ref.getId();
		
		if (postId == null)
			throw new IllegalArgumentException("Invalid path provided: expect to receive the post id");
		
		if(blogManager.recyclePost(postId))
		{
			return "SUCCESS";
		}
		else
		{
			return "FAIL";
		}
	}
	
	@EntityCustomAction(action = "restore", viewKey = EntityView.VIEW_SHOW)
	public String handleRestore(EntityReference ref)
	{
		String postId = ref.getId();
		
		if (postId == null)
		{
			throw new IllegalArgumentException("Invalid path provided: expect to receive the post id");
		}
		
		if(blogManager.restorePost(postId))
		{
			return "SUCCESS";
		}
		else
		{
			return "FAIL";
		}
	}
	
	@EntityCustomAction(action = "import", viewKey = EntityView.VIEW_LIST)
	public String handleImport(EntityReference ref)
	{
		if(allowImportAction)
			blogManager.importPreviousBlogData();
		return "SUCCESS";
	}

	/**
	 * From Statisticable
	 */
	public String getAssociatedToolId()
	{
		return "blog2";
	}

	/**
	 * From Statisticable
	 */
	public String[] getEventKeys()
	{
		String[] temp = new String[EVENT_KEYS.length];
		System.arraycopy(EVENT_KEYS, 0, temp, 0, EVENT_KEYS.length);
		return temp;
	}

	/**
	 * From Statisticable
	 */
	public Map<String, String> getEventNames(Locale locale)
	{
		Map<String, String> localeEventNames = new HashMap<String, String>();
		ResourceLoader msgs = new ResourceLoader("Events");
		msgs.setContextLocale(locale);
		for (int i = 0; i < EVENT_KEYS.length; i++)
		{
			localeEventNames.put(EVENT_KEYS[i], msgs.getString(EVENT_KEYS[i]));
		}
		return localeEventNames;
	}

	public void setDeveloperService(DeveloperHelperService developerService)
	{
		this.developerService = developerService;
	}

	public DeveloperHelperService getDeveloperService()
	{
		return developerService;
	}

	public void setSakaiProxy(SakaiProxy sakaiProxy)
	{
		this.sakaiProxy = sakaiProxy;
	}

	public SakaiProxy getSakaiProxy()
	{
		return sakaiProxy;
	}

	public void setAllowImportAction(boolean allowImportAction)
	{
		this.allowImportAction = allowImportAction;
	}
}
