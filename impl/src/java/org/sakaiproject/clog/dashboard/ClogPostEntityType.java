package org.sakaiproject.clog.dashboard;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sakaiproject.clog.api.datamodel.Post;
import org.sakaiproject.dash.entity.DashboardEntityInfo;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.util.ResourceLoader;

public class ClogPostEntityType extends ClogDashboardEntityType{

    public String getGroupTitle(int numberOfItems, String contextTitle, String labelKey) {
        return null;
    }

    public String getIdentifier() {
        return ClogPostCreatedEventProcessor.IDENTIFIER;
    }

    public Map<String, Object> getValues(String entityReference, String localeCode) {

        ResourceLoader rl = new ResourceLoader("org.sakaiproject.clog.bundle.dashboard");
        String postId = entityReference.substring(entityReference.lastIndexOf("/") + 1);
        Post post;
        try {
            post = clogManager.getPostHeader(postId);
        } catch (Exception e) {
            return null;
        }
        List<Map<String,String>> infoList = new ArrayList<Map<String,String>>();
        Map<String,String> infoItem = new HashMap<String,String>();
        infoItem.put(VALUE_INFO_LINK_URL, post.getUrl());
        infoItem.put(VALUE_INFO_LINK_TITLE, rl.getString("view_in_site"));
        infoItem.put(VALUE_INFO_LINK_TARGET, "_top");
        infoList.add(infoItem);
        Map<String, Object> values = new HashMap<String, Object>();
        DateFormat df = DateFormat.getDateTimeInstance();
        values.put(VALUE_NEWS_TIME, df.format(new Date(post.getCreatedDate())));
        values.put(VALUE_MORE_INFO, infoList);
        values.put(VALUE_TITLE,post.getTitle());
        try {
            values.put(VALUE_USER_NAME, userDirectoryService.getUser(post.getCreatorId()).getDisplayName());
        } catch (UserNotDefinedException unde) {
        }
        values.put(DashboardEntityInfo.VALUE_ENTITY_TYPE, ClogPostCreatedEventProcessor.IDENTIFIER);
        return values;
    }
}
