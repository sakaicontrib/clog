package org.sakaiproject.clog.api;

import org.sakaiproject.user.api.User;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ClogMember {

	private int numberOfPosts = 0;

	private int numberOfComments = 0;

	private long dateOfLastPost = -1L;

	private long dateOfLastComment = -1L;

	private String lastCommentCreator = "";

	private transient User sakaiUser = null;

	public ClogMember() {
	}

	public ClogMember(User user) {
		this.sakaiUser = user;
	}

	public String getUserId() {
		return sakaiUser.getId();
	}

	public String getUserEid() {
		return sakaiUser.getEid();
	}

	public String getUserDisplayName() {
		return sakaiUser.getLastName() + ", " + sakaiUser.getFirstName();
	}
}
