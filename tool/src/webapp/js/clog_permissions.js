function ClogPermissions(data) {

	if(!data) return;

	this.postReadAny = false;
	this.postCreate = false;
	this.postDeleteAny = false;
	this.postDeleteOwn = false;
	this.postUpdateAny = false;
	this.postUpdateOwn = false;
	this.commentCreate = false;
	this.commentDeleteAny = false;
	this.commentDeleteOwn = false;
	this.commentUpdateAny = false;
	this.commentUpdateOwn = false;
	this.modifyPermissions = false;

	for(var i=0,j=data.length;i<j;i++) {
		if('clog.post.read.any' === data[i])
			this.postReadAny = true;
		else if('clog.post.create' === data[i])
			this.postCreate = true;
		else if('clog.post.delete.any' === data[i])
			this.postDeleteAny = true;
		else if('clog.post.delete.own' === data[i])
			this.postDeleteOwn = true;
		else if('clog.post.update.any' === data[i])
			this.postUpdateAny = true;
		else if('clog.post.update.own' === data[i])
			this.postUpdateOwn = true;
		else if('clog.comment.create' === data[i])
			this.commentCreate = true;
		else if('clog.comment.delete.any' === data[i])
			this.commentDeleteAny = true;
		else if('clog.comment.delete.own' === data[i])
			this.commentDeleteOwn = true;
		else if('clog.comment.update.any' === data[i])
			this.commentUpdateAny = true;
		else if('clog.comment.update.own' === data[i])
			this.commentUpdateOwn = true;
		else if('clog.modify.permissions' === data[i])
			this.modifyPermissions = true;
	}
}
