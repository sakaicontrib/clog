package org.sakaiproject.clog.tool.entityprovider;

import lombok.Getter;

import org.sakaiproject.clog.api.datamodel.Post;

/**
 * A JSON friendly version of a CLOG post for the summary list.
 * 
 * @author Adrian Fish <adrian.r.fish@gmail.com>
 */
public class SparsePost {
	
	@Getter
	private String id = "";
	
	@Getter
	private long createdDate = -1L;
	
	@Getter
	private long modifiedDate = -1L;
	
	@Getter
	private String creatorDisplayName = "";
	
	@Getter
	private String title = "";
	
	@Getter
	private String url = "";
	
	public SparsePost(Post post) {
		
		this.id = post.getId();
		this.createdDate = post.getCreatedDate() / 1000;
		this.modifiedDate = post.getModifiedDate() / 1000;
		this.creatorDisplayName = post.getCreatorDisplayName();
		this.title = post.getTitle();
		this.url = post.getUrl();
	}
}
