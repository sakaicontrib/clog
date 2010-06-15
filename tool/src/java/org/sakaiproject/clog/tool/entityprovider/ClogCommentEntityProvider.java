package org.sakaiproject.clog.tool.entityprovider;

import java.util.Map;

import org.apache.log4j.Logger;
import org.sakaiproject.clog.api.datamodel.Comment;
import org.sakaiproject.clog.api.ClogManager;
import org.sakaiproject.clog.api.SakaiProxy;
import org.sakaiproject.entitybroker.DeveloperHelperService;
import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.entitybroker.entityprovider.EntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.AutoRegisterEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Createable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Deleteable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Describeable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Inputable;
import org.sakaiproject.entitybroker.entityprovider.extension.Formats;
import org.sakaiproject.entitybroker.util.AbstractEntityProvider;

public class ClogCommentEntityProvider extends AbstractEntityProvider implements EntityProvider, AutoRegisterEntityProvider, 
	Inputable, Createable, Describeable, Deleteable
{
	private ClogManager blogManager;
	
	private DeveloperHelperService developerService = null;
	  
	public final static String ENTITY_PREFIX = "clog-comment";

	protected final Logger LOG = Logger.getLogger(getClass());
	
	private SakaiProxy sakaiProxy = null;
	
	public String createEntity(EntityReference ref, Object entity,
            Map<String, Object> params)
	{
		if(LOG.isDebugEnabled()) LOG.debug("createEntity");
		
		String userId = developerService.getCurrentUserId();
		
		String id = (String) params.get("id");
		String postId = (String) params.get("postId");
		String content = (String) params.get("content");
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
				String reference = ClogManager.REFERENCE_ROOT + "/" + siteId + "/comment/" + postId;
				sakaiProxy.postEvent(ClogManager.BLOG_COMMENT_CREATED,reference,siteId);
				
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
	
	public void setBlogManager(ClogManager blogManager)
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
			sakaiProxy.postEvent(ClogManager.BLOG_COMMENT_DELETED,ref.getId(),siteId);
	}

	public void setDeveloperService(DeveloperHelperService developerService)
	{
		this.developerService = developerService;
	}

	public void setSakaiProxy(SakaiProxy sakaiProxy)
	{
		this.sakaiProxy = sakaiProxy;
	}
}
