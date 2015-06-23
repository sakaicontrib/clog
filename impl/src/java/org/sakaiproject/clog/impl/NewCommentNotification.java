package org.sakaiproject.clog.impl;

import java.util.List;

import org.sakaiproject.clog.api.ClogManager;
import org.sakaiproject.clog.api.SakaiProxy;
import org.sakaiproject.clog.api.datamodel.Comment;
import org.sakaiproject.clog.api.datamodel.Post;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.entity.api.Entity;
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

import lombok.Setter;

public class NewCommentNotification extends SiteEmailNotification {

	private final static ResourceLoader rb = new ResourceLoader("newcomment");

    @Setter
	private SakaiProxy sakaiProxy;

	public NewCommentNotification() {
	}

	public NewCommentNotification(String siteId) {
		super(siteId);
	}

	protected String getFromAddress(Event event) {
		String userEmail = "no-reply@" + ServerConfigurationService.getServerName();
		String userDisplay = ServerConfigurationService.getString("ui.service", "Sakai");
		String no_reply = "From: \"" + userDisplay + "\" <" + userEmail + ">";
		String from = getFrom(event);
		// get the message
		Reference ref = EntityManager.newReference(event.getResource());
		Comment msg = (Comment) ref.getEntity();
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
		Comment comment = (Comment) ref.getEntity();

		ref = EntityManager.newReference(ClogManager.REFERENCE_ROOT + Entity.SEPARATOR + comment.getSiteId() + Entity.SEPARATOR + "posts" + Entity.SEPARATOR + comment.getPostId());
		Post post = (Post) ref.getEntity();

		String creatorName = "";
		try {
			creatorName = UserDirectoryService.getUser(comment.getCreatorId()).getDisplayName();
		} catch (UserNotDefinedException e) {
			e.printStackTrace();
		}

		return rb.getFormattedMessage("noti.body", new Object[] { creatorName, post.getTitle(), post.getUrl() });
	}

	protected String getSubject(Event event) {
		Reference ref = EntityManager.newReference(event.getResource());
		Comment comment = (Comment) ref.getEntity();

		String siteTitle = "";
		try {
			siteTitle = SiteService.getSite(comment.getSiteId()).getTitle();
		} catch (IdUnusedException e) {
			e.printStackTrace();
		}

		return rb.getFormattedMessage("noti.subject", new Object[] { siteTitle });
	}

	protected String getTag(String title, boolean shouldUseHtml) {
		return rb.getFormattedMessage("noti.tag", new Object[] { ServerConfigurationService.getString("ui.service", "Sakai"), ServerConfigurationService.getPortalUrl(), title });
	}

	protected List getHeaders(Event event) {
		List rv = super.getHeaders(event);
		rv.add("Subject: " + getSubject(event));
		rv.add(getFromAddress(event));
		rv.add(getTo(event));
		return rv;
	}
}
