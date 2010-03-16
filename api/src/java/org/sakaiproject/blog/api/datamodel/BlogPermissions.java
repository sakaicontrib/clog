package org.sakaiproject.blog.api.datamodel;

public class BlogPermissions
{
	private String role;
	private boolean postCreate;
	
	private boolean postReadAny = false; 
	private boolean postReadOwn = false;
	private boolean postUpdateAny = false;
	private boolean postUpdateOwn = false;
	private boolean postDeleteAny = false;
	private boolean postDeleteOwn = false;
	private boolean commentCreate = false;
	private boolean commentReadAny = false;
	private boolean commentReadOwn = false;
	private boolean commentUpdateAny = false;
	private boolean commentUpdateOwn = false;
	private boolean commentDeleteAny = false;
	private boolean commentDeleteOwn = false;
	
	public void setRole(String role)
	{
		this.role = role;
	}
	public String getRole()
	{
		return role;
	}
	public void setPostCreate(boolean postCreate)
	{
		this.postCreate = postCreate;
	}
	public boolean isPostCreate()
	{
		return postCreate;
	}
	public void setPostReadAny(boolean postReadAny)
	{
		this.postReadAny = postReadAny;
	}
	public boolean isPostReadAny()
	{
		return postReadAny;
	}
	public void setPostUpdateAny(boolean postUpdateAny)
	{
		this.postUpdateAny = postUpdateAny;
	}
	public boolean isPostUpdateAny()
	{
		return postUpdateAny;
	}
	public void setPostUpdateOwn(boolean postUpdateOwn)
	{
		this.postUpdateOwn = postUpdateOwn;
	}
	public boolean isPostUpdateOwn()
	{
		return postUpdateOwn;
	}
	public void setPostDeleteAny(boolean postDeleteAny)
	{
		this.postDeleteAny = postDeleteAny;
	}
	public boolean isPostDeleteAny()
	{
		return postDeleteAny;
	}
	public void setPostDeleteOwn(boolean postDeleteOwn)
	{
		this.postDeleteOwn = postDeleteOwn;
	}
	public boolean isPostDeleteOwn()
	{
		return postDeleteOwn;
	}
	public void setCommentCreate(boolean commentCreate)
	{
		this.commentCreate = commentCreate;
	}
	public boolean isCommentCreate()
	{
		return commentCreate;
	}
	public void setCommentReadAny(boolean commentReadAny)
	{
		this.commentReadAny = commentReadAny;
	}
	public boolean isCommentReadAny()
	{
		return commentReadAny;
	}
	public void setCommentUpdateAny(boolean commentUpdateAny)
	{
		this.commentUpdateAny = commentUpdateAny;
	}
	public boolean isCommentUpdateAny()
	{
		return commentUpdateAny;
	}
	public void setCommentUpdateOwn(boolean commentUpdateOwn)
	{
		this.commentUpdateOwn = commentUpdateOwn;
	}
	public boolean isCommentUpdateOwn()
	{
		return commentUpdateOwn;
	}
	public void setCommentDeleteAny(boolean commentDeleteAny)
	{
		this.commentDeleteAny = commentDeleteAny;
	}
	public boolean isCommentDeleteAny()
	{
		return commentDeleteAny;
	}
	public void setCommentDeleteOwn(boolean commentDeleteOwn)
	{
		this.commentDeleteOwn = commentDeleteOwn;
	}
	public boolean isCommentDeleteOwn()
	{
		return commentDeleteOwn;
	}
	public void setPostReadOwn(boolean postReadOwn)
	{
		this.postReadOwn = postReadOwn;
	}
	public boolean isPostReadOwn()
	{
		return postReadOwn;
	}
	public void setCommentReadOwn(boolean commentReadOwn)
	{
		this.commentReadOwn = commentReadOwn;
	}
	public boolean isCommentReadOwn()
	{
		return commentReadOwn;
	}
}
