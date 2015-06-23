package org.sakaiproject.clog.tool.entityprovider;

import lombok.Getter;

import org.sakaiproject.clog.api.datamodel.Post;

/**
 * A JSON friendly version of a CLOG post for the summary list.
 * 
 * @author Adrian Fish <adrian.r.fish@gmail.com>
 */
@Getter
public class SparsePost {
	
	private String id = "";
	private long createdDateMillis = -1L;
	private long modifiedDateMillis = -1L;
	private String creatorDisplayName = "";
	private String title = "";
	private String url = "";

	public SparsePost(Post post) {
		
		this.id = post.getId();
		this.createdDateMillis = post.getCreatedDate();
		this.modifiedDateMillis = post.getModifiedDate();
		this.creatorDisplayName = post.getCreatorDisplayName();
		this.title = post.getTitle();
		this.url = post.getUrl();
	}
}
