package org.sakaiproject.clog.tool.entityprovider;

import java.util.Map;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.sakaiproject.clog.api.datamodel.Comment;
import org.sakaiproject.clog.api.datamodel.Post;
import org.sakaiproject.clog.api.ClogManager;
import org.sakaiproject.clog.api.SakaiProxy;
import org.sakaiproject.entitybroker.DeveloperHelperService;
import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.entitybroker.entityprovider.CoreEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.EntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.*;
import org.sakaiproject.entitybroker.entityprovider.extension.Formats;
import org.sakaiproject.entitybroker.util.AbstractEntityProvider;
import org.sakaiproject.util.ResourceLoader;

public class ClogCommentEntityProvider extends AbstractEntityProvider implements CoreEntityProvider, AutoRegisterEntityProvider, Inputable, Outputable, Createable, Describeable, Deleteable {

	private ClogManager clogManager;

	public void setClogManager(ClogManager clogManager) {
		this.clogManager = clogManager;
	}

	private DeveloperHelperService developerService = null;

	public final static String ENTITY_PREFIX = "clog-comment";

	protected final Logger LOG = Logger.getLogger(getClass());

	private SakaiProxy sakaiProxy = null;

	public void setSakaiProxy(SakaiProxy sakaiProxy) {
		this.sakaiProxy = sakaiProxy;
	}

	public String createEntity(EntityReference ref, Object entity, Map<String, Object> params) {

		if (LOG.isDebugEnabled())
			LOG.debug("createEntity");

		String userId = developerService.getCurrentUserId();

		String id = (String) params.get("id");
		String postId = (String) params.get("postId");
		String content = (String) params.get("content");
		String siteId = (String) params.get("siteId");

		Comment comment = new Comment();
		comment.setId(id);
		comment.setSiteId(siteId);
		comment.setPostId(postId);
		comment.setCreatorId(userId);
		comment.setContent(content);

		boolean isNew = "".equals(comment.getId());

		if (clogManager.saveComment(comment)) {
			if (isNew) {
				String reference = ClogManager.REFERENCE_ROOT + "/" + siteId + "/posts/" + postId + "/comments/" + comment.getId();
				sakaiProxy.postEvent(ClogManager.CLOG_COMMENT_CREATED, reference, siteId);
			}

			return comment.getId();
		} else
			return "FAIL";
	}

	public boolean entityExists(String id) {
		if (LOG.isDebugEnabled())
			LOG.debug("entityExists(" + id + ")");

		if (id == null) {
			return false;
		}

		if ("".equals(id))
			return false;

		try {
			return (clogManager.getComment(id) != null);
		} catch (Exception e) {
			LOG.error("Caught exception whilst getting post.", e);
			return false;
		}
	}

	public Object getEntity(EntityReference ref) {

		if (LOG.isDebugEnabled())
			LOG.debug("getEntity(" + ref.getId() + ")");

		String id = ref.getId();

		if (id == null || "".equals(id)) {
			return new Comment();
		}

		Comment comment = null;

		try {
			comment = clogManager.getComment(id);
		} catch (Exception e) {
			LOG.error("Caught exception whilst getting comment.", e);
		}

		if (comment == null) {
			throw new IllegalArgumentException("Comment not found");
		}

		// TODO: Security !!!!

		return comment;
	}

	public Object getSampleEntity() {
		return new Comment();
	}

	public String getEntityPrefix() {
		return ENTITY_PREFIX;
	}

	public String[] getHandledOutputFormats() {
		return new String[] { Formats.JSON };
	}

	public String[] getHandledInputFormats() {
		return new String[] { Formats.HTML, Formats.JSON, Formats.FORM };
	}

	public void deleteEntity(EntityReference ref, Map<String, Object> params) {

		if (LOG.isDebugEnabled())
			LOG.debug("deleteEntity");

		String siteId = (String) params.get("siteId");

		if (clogManager.deleteComment(ref.getId())) {
			String reference = ClogManager.REFERENCE_ROOT + "/" + siteId + "/posts/gdfgdsfgdfg/comments/" + ref.getId();
			sakaiProxy.postEvent(ClogManager.CLOG_COMMENT_DELETED, reference, siteId);
		}
	}

	public void setDeveloperService(DeveloperHelperService developerService) {
		this.developerService = developerService;
	}
}
