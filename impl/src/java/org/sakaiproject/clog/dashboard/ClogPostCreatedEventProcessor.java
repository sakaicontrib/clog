package org.sakaiproject.clog.dashboard;

import org.sakaiproject.clog.api.ClogManager;
import org.sakaiproject.clog.api.datamodel.Post;
import org.sakaiproject.dash.model.NewsItem;
import org.sakaiproject.dash.model.SourceType;
import org.sakaiproject.event.api.Event;

public class ClogPostCreatedEventProcessor extends ClogDashboardEventProcessor {
    
    public static final String IDENTIFIER = "clog-post";
    
    /** Gets used by createNewsItem */
    private SourceType sourceType;
    
    public void init() {
        sourceType = dashboardLogic.createSourceType(IDENTIFIER);
    }
    
    /**
     * This tells the dashboard observer to call our processEvent method
     * when clog.post.created events are observed
     */
    public String getEventIdentifer() {
        return ClogManager.CLOG_POST_CREATED;
    }

    /**
     * Process the clog.post.created event. The aim is to create an appropriate
     * Dashboard NewsItem for it to render. 
     */
    public void processEvent(Event event) {

        String resource = event.getResource();
        
        // Parse the post id out of the entity path
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
                                        ,ClogManager.CLOG_POST_CREATED
                                        ,resource
                                        ,dashboardLogic.getContext(post.getSiteId())
                                        ,sourceType
                                        ,"");
        dashboardLogic.createNewsLinks(newsItem);
    }
}
