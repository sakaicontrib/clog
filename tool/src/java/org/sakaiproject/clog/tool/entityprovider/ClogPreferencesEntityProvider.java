package org.sakaiproject.clog.tool.entityprovider;

import java.util.Map;

import org.apache.log4j.Logger;
import org.sakaiproject.clog.api.datamodel.Preferences;
import org.sakaiproject.clog.api.BlogManager;
import org.sakaiproject.entitybroker.DeveloperHelperService;
import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.entitybroker.entityprovider.EntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.AutoRegisterEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Createable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Describeable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Inputable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Outputable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Resolvable;
import org.sakaiproject.entitybroker.entityprovider.extension.Formats;
import org.sakaiproject.entitybroker.util.AbstractEntityProvider;

public class ClogPreferencesEntityProvider extends AbstractEntityProvider implements EntityProvider, AutoRegisterEntityProvider, 
	Inputable, Outputable, Resolvable,Createable, Describeable
{
	private BlogManager blogManager;
	
	private DeveloperHelperService developerService = null;
	  
	public final static String ENTITY_PREFIX = "clog-preferences";

	protected final Logger LOG = Logger.getLogger(getClass());
	
	public String createEntity(EntityReference ref, Object entity, Map<String, Object> params)
	{
		if(LOG.isDebugEnabled()) LOG.debug("createEntity");
		
		String userId = developerService.getCurrentUserId();
		
		String siteId = (String) params.get("siteId");
		String emailFrequency = (String) params.get("emailFrequency");
		
		Preferences preferences = new Preferences();
		preferences.setUserId(userId);
		preferences.setSiteId(siteId);
		preferences.setEmailFrequency(emailFrequency);
		
		if(blogManager.savePreferences(preferences))
		{
			return "SUCCESS";
		}
		else
			return "FAIL";
	}

	public Object getSampleEntity()
	{
		return new Preferences();
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

	public void setDeveloperService(DeveloperHelperService developerService)
	{
		this.developerService = developerService;
	}

	public DeveloperHelperService getDeveloperService()
	{
		return developerService;
	}

	public Object getEntity(EntityReference ref)
	{
		String siteId = ref.getId();
		return blogManager.getPreferences(siteId,null);
	}
}
