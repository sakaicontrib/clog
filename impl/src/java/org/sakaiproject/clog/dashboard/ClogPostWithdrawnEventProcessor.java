package org.sakaiproject.clog.dashboard;

import org.sakaiproject.clog.api.ClogManager;
import org.sakaiproject.event.api.Event;

public class ClogPostWithdrawnEventProcessor extends ClogDashboardEventProcessor{
    
    public static final String IDENTIFIER = "clog-post";
    
    public String getEventIdentifer() {
        return ClogManager.CLOG_POST_WITHDRAWN;
    }

    /**
     * Process the clog.post.withdrawn event. The aim is to remove the
     * NewsItem for this post.
     */
    public void processEvent(Event event) {

        String resource = event.getResource();
        dashboardLogic.removeNewsItem(resource);
        dashboardLogic.removeNewsLinks(resource);
    }
}
