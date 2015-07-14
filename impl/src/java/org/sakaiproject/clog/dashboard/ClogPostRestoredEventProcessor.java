package org.sakaiproject.clog.dashboard;

import org.sakaiproject.clog.api.ClogManager;
import org.sakaiproject.clog.api.datamodel.Post;
import org.sakaiproject.dash.model.NewsItem;
import org.sakaiproject.dash.model.SourceType;
import org.sakaiproject.event.api.Event;

public class ClogPostRestoredEventProcessor extends ClogDashboardEventProcessor{
    
    public static final String IDENTIFIER = "clog-post";
    
    private SourceType sourceType;
    
    public void init() {
        sourceType = dashboardLogic.getSourceType(IDENTIFIER);
    }
    
    public String getEventIdentifer() {
        return ClogManager.CLOG_POST_RESTORED;
    }

    /**
     * Process the clog.post.restored event. The aim is to create an appropriate
     * Dashboard NewsItem for it to render. 
     */
    public void processEvent(Event event) {

        String resource = event.getResource();
        String postId = resource.substring(resource.lastIndexOf("/") + 1);
        Post post;
        try {
            post = clogManager.getPostHeader(postId);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        NewsItem newsItem = dashboardLogic.createNewsItem(post.getTitle()
                                        ,event.getEventTime()
                                        ,ClogManager.CLOG_POST_RESTORED
                                        ,resource
                                        ,dashboardLogic.getContext(event.getContext())
                                        ,sourceType
                                        ,null);
        dashboardLogic.createNewsLinks(newsItem);
    }
}
