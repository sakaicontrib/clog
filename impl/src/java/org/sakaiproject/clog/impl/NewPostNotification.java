package org.sakaiproject.clog.impl;

import java.util.ArrayList;
import java.util.List;

import org.sakaiproject.clog.api.ClogFunctions;
import org.sakaiproject.clog.api.SakaiProxy;
import org.sakaiproject.clog.api.datamodel.Post;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.entity.cover.EntityManager;
import org.sakaiproject.event.api.Event;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.user.cover.UserDirectoryService;
import org.sakaiproject.util.ResourceLoader;
import org.sakaiproject.util.SiteEmailNotification;

public class NewPostNotification extends SiteEmailNotification {

    private static ResourceLoader rb = new ResourceLoader("newpost");

    private SakaiProxy sakaiProxy = null;
    private String resourceAbility;

    public NewPostNotification() {
    }

    public NewPostNotification(String siteId) {
        super(siteId);
    }

    public void setSakaiProxy(SakaiProxy sakaiProxy) {
        this.sakaiProxy = sakaiProxy;
    }

    protected String getFromAddress(Event event) {
        String userEmail = "no-reply@" + ServerConfigurationService.getServerName();
        String userDisplay = ServerConfigurationService.getString("ui.service", "Sakai");
        String no_reply = "From: \"" + userDisplay + "\" <" + userEmail + ">";
        String from = getFrom(event);
        // get the message
        Reference ref = EntityManager.newReference(event.getResource());
        Post msg = (Post) ref.getEntity();
        String userId = msg.getCreatorId();

        // checks if "from" email id has to be included? and whether the
        // notification is a delayed notification?. SAK-13512
        if ((ServerConfigurationService.getString("emailFromReplyable@org.sakaiproject.event.api.NotificationService").equals("true")) && from.equals(no_reply) && userId != null) {

            try {
                User u = UserDirectoryService.getUser(userId);
                userDisplay = u.getDisplayName();
                userEmail = u.getEmail();
                if ((userEmail != null) && (userEmail.trim().length()) == 0)
                    userEmail = null;

            } catch (UserNotDefinedException e) {
            }

            // some fallback positions
            if (userEmail == null)
                userEmail = "no-reply@" + ServerConfigurationService.getServerName();
            if (userDisplay == null)
                userDisplay = ServerConfigurationService.getString("ui.service", "Sakai");
            from = "From: \"" + userDisplay + "\" <" + userEmail + ">";
        }

        return from;
    }

    protected String plainTextContent(Event event) {
        Reference ref = EntityManager.newReference(event.getResource());
        Post post = (Post) ref.getEntity();

        String creatorName = "";
        try {
            creatorName = UserDirectoryService.getUser(post.getCreatorId()).getDisplayName();
        } catch (UserNotDefinedException e) {
            e.printStackTrace();
        }

        return rb.getFormattedMessage("noti.body", new Object[] { creatorName, post.getTitle(), post.getUrl() });
    }

    protected String getSubject(Event event) {
        Reference ref = EntityManager.newReference(event.getResource());
        Post post = (Post) ref.getEntity();

        String siteTitle = "";
        try {
            siteTitle = SiteService.getSite(post.getSiteId()).getTitle();
        } catch (IdUnusedException e) {
            e.printStackTrace();
        }

        return rb.getFormattedMessage("noti.subject", new Object[] { siteTitle });
    }

    protected String getTag(String title, boolean shouldUseHtml) {

        final String tpl = (shouldUseHtml) ? "noti.tag.html" : "noti.tag";
        return rb.getFormattedMessage(tpl, new Object[] {
                                ServerConfigurationService.getString("ui.service", "Sakai"),
                                ServerConfigurationService.getPortalUrl(),
                                title });
    }

    protected List getHeaders(Event event) {
        List rv = super.getHeaders(event);
        rv.add("Subject: " + getSubject(event));
        rv.add(getFromAddress(event));
        rv.add(getTo(event));
        return rv;
    }

    @Override
    protected List<User> getRecipients(Event event) {

        Reference ref = EntityManager.newReference(event.getResource());
        Post post = (Post) ref.getEntity();
        List<User> recipients = new ArrayList<>();

        if (post.isVisibleToTutors()) {
            // Is the notification dispatcher thread safe?
            this.resourceAbility = ClogFunctions.CLOG_TUTOR;
        }

        try {
            recipients = super.getRecipients(event);
        }
        finally {
            this.resourceAbility = null;
        }

        if (post.isGroup()) {
            List<User> usersInGroups = sakaiProxy.getUsersInGroups(post.getSiteId(), post.getGroups());
            recipients.retainAll(usersInGroups);
        }

        return recipients;
    }

    @Override
    protected String getResourceAbility() {
        return this.resourceAbility;
    }
}
