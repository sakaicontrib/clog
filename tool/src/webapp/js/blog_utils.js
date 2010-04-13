var BlogUtils;

(function()
{
	if(BlogUtils == null)
		BlogUtils = new Object();

	BlogUtils.showSearchResults = function(searchTerms) {
    	jQuery.ajax( {
			url : "/direct/search.json?tool=blog&contexts=" + blogSiteId + "&searchTerms=" + searchTerms,
        	dataType : "json",
        	async : false,
			cache: false,
        	success : function(results) {
        		switchState('searchResults',{'results':results["search_collection"]});
        	},
        	error : function(xmlHttpRequest,status,error) {
				alert("Failed to search. Status: " + status + ". Error: " + error);
			}
		});
	}

	BlogUtils.getPreferences = function() {
		var prefs = null;
		jQuery.ajax( {
	 		url : "/direct/blog-preferences/" + blogSiteId + ".json",
	   		dataType : "json",
	   		async : false,
	   		cache : false,
		   	success : function(p) {
				prefs = p;
			},
			error : function(xmlHttpRequest,stat,error) {
				alert("Failed to get the current user preferences. Status: " + stat + ". Error: " + error);
			}
	  	});

		return prefs;

		return {emailFrequency:'digest'};
	}

	BlogUtils.savePreferences = function() {
		var emailFrequency = $('.blog_email_option:checked').val();
		var myData = {'siteId':blogSiteId,'emailFrequency':emailFrequency};

		jQuery.ajax( {
	 		url : "/direct/blog-preferences/new",
			type : 'POST',
			data : myData,
			timeout: 30000,
			async : false,
			dataType: 'text',
		   	success : function(result) {
		   		blogCurrentUserPreferences = myData;
				switchState('viewAllPosts');
			},
			error : function(xmlHttpRequest,status,error) {
				alert("Failed to save preferences. Status: " + status + '. Error: ' + error);
			}
	  	});

		return false;
	}

	BlogUtils.setPostsForCurrentSite = function() {

		jQuery.ajax( {
	       	url : "/direct/blog-post.json?siteId=" + blogSiteId,
	       	dataType : "json",
	       	async : false,
			cache: false,
		   	success : function(data) {
				blogCurrentPosts = data['blog-post_collection'];
			},
			error : function(xmlHttpRequest,status,errorThrown) {
				alert("Failed to get posts. Reason: " + errorThrown);
			}
	   	});
	}

	BlogUtils.savePostAsDraft = function() {
		BlogUtils.storePost('PRIVATE');
	}

	BlogUtils.publishPost = function() {
		BlogUtils.storePost('READY');
	}

	BlogUtils.publicisePost = function() {
		BlogUtils.storePost('PUBLIC');
	}
		
	BlogUtils.storePost = function(visibility) {

		var post = {
				'id':$('#blog_post_id_field').val(),
				'visibility':visibility,
				'commentable':$('#blog_commentable_checkbox').attr('checked'),
				'title':$('#blog_title_field').val(),
				'content':FCKeditorAPI.GetInstance('blog_content_editor').GetXHTML(true),
				'siteId':blogSiteId
				};

		jQuery.ajax( {
	 		url : "/direct/blog-post/new",
			type : 'POST',
			data : post,
			timeout: 30000,
			async : false,
			dataType: 'text',
		   	success : function(id) {
				switchState(blogHomeState);
			},
			error : function(xmlHttpRequest,status,error) {
				alert("Failed to store post. Status: " + status + '. Error: ' + error);
			}
	  	});
	}
	
	BlogUtils.saveComment = function() {

		var comment = {
				'id':$('#blog_comment_id_field').val(),
				'postId':blogCurrentPost.id,
				'content':FCKeditorAPI.GetInstance('blog_content_editor').GetXHTML(true),
				'siteId':blogSiteId
				};

		jQuery.ajax( {
	 		url : "/direct/blog-comment/new",
			type : 'POST',
			data : comment,
			timeout: 30000,
			async : false,
			dataType: 'text',
		   	success : function(id) {
				switchState('viewAllPosts');
			},
			error : function(xmlHttpRequest,status,error) {
				alert("Failed to save comment. Status: " + status + '. Error: ' + error);
			}
	  	});

		return false;
	}

	BlogUtils.recyclePost = function(postId) {
		if(!confirm(blog_delete_post_message))
			return false;

		jQuery.ajax( {
	 		url : "/direct/blog-post/" + postId + "/recycle",
			dataType : 'text',
			async : false,
		   	success : function(result) {
				switchState('viewAllPosts');
			},
			error : function(xmlHttpRequest,status,error) {
				alert("Failed to recycle post. Status: " + status + '. Error: ' + error);
			}
	  	});

		return false;
	}

	BlogUtils.deleteSelectedPosts = function() {
		var selected = $('.blog_recycled_post_checkbox:checked');

		var commands = '';

		for(var i=0,j=selected.length;i<j;i++) {
			commands += "/direct/blog-post/" + selected[i].id + "?siteId=" + blogSiteId;
			if(i < (j - 1)) commands += ",";
		}

		jQuery.ajax( {
	 		url : "/direct/batch?_refs=" + commands,
			dataType : 'text',
			type:'DELETE',
			async : false,
		   	success : function(result) {
				switchState('viewAllPosts');
			},
			error : function(xmlHttpRequest,status,error) {
				alert("Failed to delete selected posts. Status: " + status + '. Error: ' + error);
			}
	  	});

		return false;
	}

	BlogUtils.restoreSelectedPosts = function() {
		var selected = $('.blog_recycled_post_checkbox:checked');

		var commands = '';

		for(var i=0,j=selected.length;i<j;i++) {
			commands += "/direct/blog-post/" + selected[i].id + "/restore";
			if(i < (j - 1)) commands += ",";
		}

		jQuery.ajax( {
	 		url : "/direct/batch?_refs=" + commands,
			dataType : 'text',
			async : false,
		   	success : function(result) {
				switchState('viewAllPosts');
			},
			error : function(xmlHttpRequest,status,error) {
				alert("Failed to restore selected posts. Status: " + status + '. Error: ' + error);
			}
	  	});

		return false;
	}

	BlogUtils.findPost = function(postId) {
		var post = null;
		
		if(!blogCurrentPosts) {
			jQuery.ajax( {
	 			url : "/direct/blog-post/" + postId + ".json",
	   			dataType : "json",
	   			async : false,
	   			cache : false,
		   		success : function(p,status) {
					post = p;
				},
				error : function(xmlHttpRequest,stat,error) {
					alert("Failed to get the post. Status: " + stat + ". Error: " + error);
				}
	  		});
	  	}
		else {
			for(var i=0,j=blogCurrentPosts.length;i<j;i++) {
				if(blogCurrentPosts[i].id === postId)
					post = blogCurrentPosts[i];
			}
		}

		return post;
	}

	BlogUtils.deleteComment = function(commentId) {
		if(!confirm(blog_delete_comment_message))
			return false;
		
		jQuery.ajax( {
	 		url : "/direct/blog-comment/" + commentId + "?siteId=" + blogSiteId,
	   		async : false,
			type:'DELETE',
		   	success : function(text,status) {
				switchState('viewAllPosts');
			},
			error : function(xmlHttpRequest,status,error) {
				alert("Failed to delete comment. Status: " + status + ". Error: " + error);
			}
	  	});
	  	
	  	return false;
	}
}) ();
