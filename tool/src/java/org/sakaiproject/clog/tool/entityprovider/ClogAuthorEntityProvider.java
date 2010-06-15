package org.sakaiproject.clog.tool.entityprovider;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.sakaiproject.clog.api.ClogManager;
import org.sakaiproject.clog.api.ClogMember;
import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.entitybroker.entityprovider.CoreEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.AutoRegisterEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.CollectionResolvable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Describeable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Outputable;
import org.sakaiproject.entitybroker.entityprovider.extension.Formats;
import org.sakaiproject.entitybroker.entityprovider.search.Restriction;
import org.sakaiproject.entitybroker.entityprovider.search.Search;
import org.sakaiproject.entitybroker.util.AbstractEntityProvider;
import org.sakaiproject.user.api.UserDirectoryService;

public class ClogAuthorEntityProvider extends AbstractEntityProvider implements CoreEntityProvider, AutoRegisterEntityProvider, 
	Outputable, Describeable, CollectionResolvable
{
	private ClogManager blogManager;
	
	private UserDirectoryService userDirectoryService = null;
	  
	public final static String ENTITY_PREFIX = "clog-author";

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

	/**
	 * No intention of implementing this. Forced to due to the fact that
	 * CollectionsResolvable extends Resolvable
	 */
	public Object getEntity(EntityReference ref)
	{
		if(LOG.isDebugEnabled()) LOG.debug("getEntity(" + ref.getId() + ")");
		
		LOG.warn("getEntity is unimplemented. Returning null ...");
		
		return null;
	}

	public Object getSampleEntity()
	{
		return new ClogMember();
	}

	public String getEntityPrefix()
	{
		return ENTITY_PREFIX;
	}

	public ClogManager getBlogManager()
	{
		return blogManager;
	}
	
	public void setBlogManager(ClogManager blogManager)
	{
		this.blogManager = blogManager;
	}

	public String[] getHandledOutputFormats() {
	    return new String[] { Formats.JSON };
	}

	public List<ClogMember> getEntities(EntityReference ref, Search search) {

		List<ClogMember> authors = new ArrayList<ClogMember>();
		
		Restriction locRes = search.getRestrictionByProperty(CollectionResolvable.SEARCH_LOCATION_REFERENCE);
		
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
