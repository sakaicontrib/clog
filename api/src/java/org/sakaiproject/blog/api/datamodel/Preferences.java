package org.sakaiproject.blog.api.datamodel;

public class Preferences
{
	public static final String MAIL_EACH = "each";
	public static final String MAIL_NEVER = "never";
	public static final String MAIL_DIGEST = "digest";
	
	private String siteId;
	private String userId;
	private String emailFrequency = MAIL_EACH;
	
	public void setSiteId(String siteId)
	{
		this.siteId = siteId;
	}
	public String getSiteId()
	{
		return siteId;
	}
	public void setUserId(String userId)
	{
		this.userId = userId;
	}
	public String getUserId()
	{
		return userId;
	}
	public void setEmailFrequency(String emailFrequency)
	{
		this.emailFrequency = emailFrequency;
	}
	public String getEmailFrequency()
	{
		return emailFrequency;
	}
}
