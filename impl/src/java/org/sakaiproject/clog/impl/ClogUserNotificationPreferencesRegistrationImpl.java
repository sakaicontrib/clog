package org.sakaiproject.clog.impl;

import org.sakaiproject.util.ResourceLoader;
import org.sakaiproject.util.UserNotificationPreferencesRegistrationImpl;

public class ClogUserNotificationPreferencesRegistrationImpl extends UserNotificationPreferencesRegistrationImpl  {
	
	public ResourceLoader getResourceLoader(String location) {
		return new ResourceLoader(location);
	}
}
