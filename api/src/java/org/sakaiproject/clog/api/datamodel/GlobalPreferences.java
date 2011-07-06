package org.sakaiproject.clog.api.datamodel;

public class GlobalPreferences {
	
	private Boolean showBody = true;
    private String userId;

	public void setShowBody(Boolean showBody) {
		this.showBody = showBody;
	}

	public Boolean isShowBody() {
		return showBody;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getUserId() {
		return userId;
	}
}
