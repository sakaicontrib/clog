package org.sakaiproject.clog.tool.entityprovider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import lombok.Setter;

import org.apache.log4j.Logger;
import org.sakaiproject.clog.api.datamodel.Post;
import org.sakaiproject.clog.api.ClogManager;
import org.sakaiproject.clog.api.QueryBean;
import org.sakaiproject.clog.api.SakaiProxy;
import org.sakaiproject.entitybroker.EntityView;
import org.sakaiproject.entitybroker.entityprovider.annotations.EntityCustomAction;
import org.sakaiproject.entitybroker.entityprovider.capabilities.ActionsExecutable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.AutoRegisterEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Describeable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Inputable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Outputable;
import org.sakaiproject.entitybroker.entityprovider.extension.Formats;
import org.sakaiproject.entitybroker.util.AbstractEntityProvider;

public class ClogRSSEntityProvider extends AbstractEntityProvider implements AutoRegisterEntityProvider, Inputable, Outputable, Describeable, ActionsExecutable {
	
	public final static String ENTITY_PREFIX = "clog-rss";
	
	protected final Logger LOG = Logger.getLogger(getClass());

	@Setter
	private ClogManager clogManager;
	
	@Setter
	private SakaiProxy sakaiProxy = null;

	private SimpleDateFormat rfc822DateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");

	public Object getSampleEntity() {
		return new Post();
	}

	public String getEntityPrefix() {
		return ENTITY_PREFIX;
	}

	public String[] getHandledOutputFormats() {
		return new String[] { Formats.RSS };
	}

	public String[] getHandledInputFormats() {
		return new String[] { Formats.HTML, Formats.FORM };
	}

	@EntityCustomAction(action = "user", viewKey = EntityView.VIEW_LIST)
	public String getFeedForUser(EntityView view, Map<String, Object> params) {
		
        String authorId = view.getPathSegment(2);

		if (authorId == null) {
			throw new IllegalArgumentException("Invalid path provided: expect to receive the author id");
		}
		
		String currentUserId = developerHelperService.getCurrentUserId();

		QueryBean qb = new QueryBean();
		qb.setCreator(authorId);

		String siteId = (String) params.get("siteId");

		if (siteId != null && siteId.length() > 0 && !"!gateway".equals(siteId)) {
			qb.setSiteId(siteId);
		}

		List<Post> posts = null;

		try {
			posts = clogManager.getPosts(qb);
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid post id");
		}

		String authorDisplayName = sakaiProxy.getDisplayNameForTheUser(authorId);

		StringBuilder rssXml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<rss version=\"2.0\" xmlns:content=\"http://purl.org/rss/1.0/modules/content/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n<channel>\n");
		rssXml.append("\n<title>").append(authorDisplayName).append("</title>");
		rssXml.append("\n<link>").append(sakaiProxy.getServerUrl()).append("/direct/clog-rss/").append(authorId).append("/authors.xml?siteId%3D").append(siteId).append("</link>");
		rssXml.append("\n<description>").append("Blog posts for " + authorDisplayName).append("</description>");
		rssXml.append("\n<language>en</language>");
		for (Post post : posts) {
			String encodedUrl = developerHelperService.getServerURL() + "/direct/clog-post/" + post.getId() + ".html";
			rssXml.append("\n<item>");
			rssXml.append("\n<title>").append(post.getTitle()).append("</title>");
			rssXml.append("\n<link>").append(encodedUrl).append("</link>");
			rssXml.append("\n<comments>").append(encodedUrl).append("</comments>");
			rssXml.append("\n<pubDate>").append(rfc822DateFormat.format(new Date(post.getCreatedDate()))).append("</pubDate>");
			rssXml.append("\n<dc:creator>").append(authorDisplayName).append("</dc:creator>");
			rssXml.append("\n<category>Uncategorised</category>");
			rssXml.append("\n<guid isPermaLink=\"false\">").append(encodedUrl).append("</guid>");
			rssXml.append("\n<description></description>");
			rssXml.append("\n</item>");
		}
		rssXml.append("\n</channel>\n</rss>");

		return rssXml.toString();
	}

	@EntityCustomAction(action = "site", viewKey = EntityView.VIEW_LIST)
	public String getFeedForSite(EntityView view) {
		
        String siteId = view.getPathSegment(2);
		
		if (siteId == null) {
			throw new IllegalArgumentException("Invalid path provided: expect to receive the site id");
		}

		QueryBean qb = new QueryBean();
		qb.setSiteId(siteId);

		List<Post> posts = null;

		try {
			posts = clogManager.getPosts(qb);
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid post id");
		}

		String siteTitle = sakaiProxy.getSiteTitle(siteId);

		StringBuilder rssXml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<rss version=\"2.0\" xmlns:content=\"http://purl.org/rss/1.0/modules/content/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n<channel>\n");
		rssXml.append("\n<title>").append(siteTitle).append("</title>");
		rssXml.append("\n<link>").append(sakaiProxy.getServerUrl()).append("/direct/clog-rss/").append(siteId).append("/allSite.xml").append(siteId).append("</link>");
		rssXml.append("\n<description>").append("Blog posts for " + siteTitle).append("</description>");
		rssXml.append("\n<language>en</language>");
		for (Post post : posts) {
			String encodedUrl = developerHelperService.getServerURL() + "/direct/clog-post/" + post.getId() + ".html";
			rssXml.append("\n<item>");
			rssXml.append("\n<title>").append(post.getTitle()).append("</title>");
			rssXml.append("\n<link>").append(encodedUrl).append("</link>");
			rssXml.append("\n<comments>").append(encodedUrl).append("</comments>");
			rssXml.append("\n<pubDate>").append(rfc822DateFormat.format(new Date(post.getCreatedDate()))).append("</pubDate>");
			rssXml.append("\n<dc:creator>").append(siteTitle).append("</dc:creator>");
			rssXml.append("\n<category>Uncategorised</category>");
			rssXml.append("\n<guid isPermaLink=\"false\">").append(encodedUrl).append("</guid>");
			rssXml.append("\n<description></description>");
			rssXml.append("\n</item>");
		}
		rssXml.append("\n</channel>\n</rss>");

		return rssXml.toString();
	}
}
