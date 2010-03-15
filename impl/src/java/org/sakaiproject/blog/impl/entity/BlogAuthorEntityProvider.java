package org.sakaiproject.blog.impl.entity;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.sakaiproject.blog.api.BlogManager;
import org.sakaiproject.blog.api.BlogMember;
import org.sakaiproject.blog.api.QueryBean;
import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.entitybroker.entityprovider.CoreEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.AutoRegisterEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.CollectionResolvable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Describeable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Outputable;
import org.sakaiproject.entitybroker.entityprovider.extension.Formats;
import org.sakaiproject.entitybroker.entityprovider.search.Restriction;
import org.sakaiproject.entitybroker.entityprovider.search.Search;
import org.sakaiproject.user.api.UserDirectoryService;

public class BlogAuthorEntityProvider implements CoreEntityProvider, AutoRegisterEntityProvider, 
	Outputable, Describeable, CollectionResolvable
{
	private BlogManager blogManager;
	
	private UserDirectoryService userDirectoryService = null;
	  
	public final static String ENTITY_PREFIX = "blog-author";

	protected final Logger LOG = Logger.getLogger(getClass());
	
	public boolean entityExists(String id)
	{
		if(LOG.isDebugEnabled()) LOG.debug("entityExists("  + id + ")");

		if (id == null) {
			return false;
		}
		
		if ("".equals(id))
			return false;
		
		try
		{
			userDirectoryService.getUser(id);
			return true;
		}
		catch(Exception e)
		{
			LOG.error("Caught exception whilst getting user.",e);
			return false;
		}
	}

	public Object getEntity(EntityReference ref)
	{
		if(LOG.isDebugEnabled()) LOG.debug("getEntity(" + ref.getId() + ")");

		String id = ref.getId();
		
		if (id == null || "".equals(id))
	         return new BlogMember();
		  
		BlogMember author = null;
		
		try
		{
			//author = blogManager.getAuthor(id);
		}
		catch (Exception e)
		{
			LOG.error("Caught exception whilst getting author.",e);
		}

		if (author == null) {
			throw new IllegalArgumentException("Author not found");
		}
		
		// TODO: Security !!!!
		
		return author;
	}

	public Object getSampleEntity()
	{
		return new BlogMember();
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

	public List<BlogMember> getEntities(EntityReference ref, Search search) {

		List<BlogMember> authors = new ArrayList<BlogMember>();
		
		Restriction locRes = search.getRestrictionByProperty(CollectionResolvable.SEARCH_LOCATION_REFERENCE);
		
		QueryBean query = new QueryBean();
		
        if (locRes != null)
        {
        	String location = locRes.getStringValue();
        	String context = new EntityReference(location).getId();
        
        	try
        	{
        		authors = blogManager.getAuthors(context);
        	}
        	catch (Exception e)
        	{
        		LOG.error("Caught exception whilst getting posts.",e);
        	}
        }
        
		return authors;
	}

	public void setUserDirectoryService(UserDirectoryService userDirectoryService)
	{
		this.userDirectoryService = userDirectoryService;
	}

	public UserDirectoryService getUserDirectoryService()
	{
		return userDirectoryService;
	}
}
