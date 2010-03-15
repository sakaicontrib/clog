function BlogPermissions(data) {

	for(var i=0,j=data.length;i<j;i++) {
		if('blog.post.create' === data[i])
			this.postCreate = true;
		else if('blog.post.delete.any' === data[i])
			this.postDeleteAny = true;
		else if('blog.post.delete.own' === data[i])
			this.postDeleteOwn = true;
		else if('blog.post.update.any' === data[i])
			this.postUpdateAny = true;
		else if('blog.post.update.own' === data[i])
			this.postUpdateOwn = true;
		else if('blog.comment.create' === data[i])
			this.commentCreate = true;
		else if('blog.comment.delete.any' === data[i])
			this.commentDeleteAny = true;
		else if('blog.comment.delete.own' === data[i])
			this.commentDeleteOwn = true;
		else if('blog.comment.update.any' === data[i])
			this.commentUpdateAny = true;
		else if('blog.comment.update.own' === data[i])
			this.commentUpdateOwn = true;
	}
}
