package org.sakaiproject.clog.dashboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sakaiproject.clog.api.ClogManager;
import org.sakaiproject.clog.api.datamodel.Post;
import org.sakaiproject.dash.entity.DashboardEntityInfo;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.util.ResourceLoader;

import lombok.Setter;

@Setter
public abstract class ClogDashboardEntityType implements DashboardEntityInfo {
    
    protected ClogManager clogManager;
    protected UserDirectoryService userDirectoryService;

    public final String getIconUrl(String subtype) {
        return "/library/image/silk/book_edit.png";
    }

    public final List<List<String>> getOrder(String entityReference, String localeCode) {

        List<List<String>> order = new ArrayList<List<String>>();
        List<String> section0 = new ArrayList<String>();
        section0.add(VALUE_TITLE);
        order.add(section0);
        List<String> section1 = new ArrayList<String>();
        section1.add("clog_metadata-label");
        order.add(section1);
        List<String> section3 = new ArrayList<String>();
        section3.add(VALUE_MORE_INFO);
        order.add(section3);
        return order;
    }

    public final Map<String, String> getProperties(String entityReference, String localeCode) {

        ResourceLoader rl = new ResourceLoader("org.sakaiproject.clog.bundle.dashboard");
        Map<String,String> props = new HashMap<String,String>();
        props.put("clog_metadata-label", rl.getString("clog.metadata"));
        return props;
    }

    /**
     * Implement this to map Sakai events to dashboard display strings
     */
    public final String getEventDisplayString(String key, String dflt) {

        ResourceLoader rl = new ResourceLoader("org.sakaiproject.clog.bundle.dashboard");
        
        if (ClogManager.CLOG_POST_CREATED.equals(key) || ClogManager.CLOG_POST_RESTORED.equals(key)) {
            return rl.getString("new_blog_post");
        } else if (ClogManager.CLOG_COMMENT_CREATED.equals(key)) {
            return rl.getString("new_blog_comment");
        }
        
        return null;
    }

    public final boolean isAvailable(String entityReference) {
        return true;
    }

    public List<String> getUsersWithAccess(String reference) {

        List<String> users = new ArrayList<String>();
        String[] parts = reference.split(Entity.SEPARATOR);
            
        String postId = "";
        if (parts.length >= 5) {
            postId = parts[4];
        }

        try {
            Post post = clogManager.getPostHeader(postId);
            if (post.isPrivate()) {
                users.add(post.getCreatorId());
                return users;
            }
            String siteId = post.getSiteId();
            users.addAll(SiteService.getSite(siteId).getUsers());
            return users;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean isUserPermitted(String sakaiUserId, String entityReference, String contextId) {
        return true;
    }
}
