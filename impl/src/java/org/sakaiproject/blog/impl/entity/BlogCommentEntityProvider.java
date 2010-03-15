package org.sakaiproject.blog.impl.entity;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.sakaiproject.blog.api.BlogManager;
import org.sakaiproject.blog.api.SakaiProxy;
import org.sakaiproject.blog.api.datamodel.Comment;
import org.sakaiproject.entitybroker.DeveloperHelperService;
import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.entitybroker.entityprovider.EntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.AutoRegisterEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Createable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Deleteable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Describeable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Inputable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Statisticable;
import org.sakaiproject.entitybroker.entityprovider.extension.Formats;
import org.sakaiproject.util.ResourceLoader;

public class BlogCommentEntityProvider implements EntityProvider, AutoRegisterEntityProvider, 
	Inputable, Createable, Describeable, Deleteable
{
	private BlogManager blogManager;
	
	private DeveloperHelperService developerService = null;
	  
	public final static String ENTITY_PREFIX = "blog-comment";

	protected final Logger LOG = Logger.getLogger(getClass());
	
	private SakaiProxy sakaiProxy = null;
	
	public String createEntity(EntityReference ref, Object entity,
            Map<String, Object> params)
	{
		if(LOG.isDebugEnabled()) LOG.debug("createEntity");
		
		String userId = developerService.getCurrentUserId();
		
		String json = (String) params.get("json");
		
		JSONObject jsonObject = JSONObject.fromObject(json);
		
		String id = jsonObject.getString("id");
		String postId = jsonObject.getString("postId");
		String content = jsonObject.getString("content");
		
		String siteId = (String) params.get("siteId");
		
		Comment comment = new Comment();
		comment.setId(id);
		comment.setPostId(postId);
		comment.setCreatorId(userId);
		comment.setContent(content);
		
		boolean isNew = "".equals(comment.getId());
		
		if(blogManager.saveComment(comment))
		{
			if(isNew)
			{
				sakaiProxy.postEvent(BlogManager.BLOG_COMMENT_CREATED,comment.getId(),siteId);
				
				// Send an email to the post author
				blogManager.sendNewCommentAlert(comment);
			}
			
			return comment.getId();
		}
		else
			return "FAIL";
	}

	public Object getSampleEntity()
	{
		return new Comment();
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

	public String[] getHandledOutputFormats() {
	    return new String[] { Formats.JSON };
	}
	
	public String[] getHandledInputFormats() {
        return new String[] { Formats.HTML, Formats.JSON, Formats.FORM };
    }

	public void deleteEntity(EntityReference ref, Map<String, Object> params)
	{
		if(LOG.isDebugEnabled()) LOG.debug("deleteEntity");
		
		String siteId = (String) params.get("siteId");
		
		if(blogManager.deleteComment(ref.getId()))
			sakaiProxy.postEvent(BlogManager.BLOG_COMMENT_DELETED,ref.getId(),siteId);
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
}
