var ClogUtils;

(function()
{
	if(ClogUtils == null)
		ClogUtils = new Object();

	ClogUtils.showSearchResults = function(searchTerms) {
    	jQuery.ajax( {
			url : "/direct/search.json?tool=clog&contexts=" + clogSiteId + "&searchTerms=" + searchTerms,
        	dataType : "json",
        	async : false,
			cache: false,
        	success : function(results) {
        		var hits = results["search_collection"];
				for(var i=0,j=hits.length;i<j;i++) {
					var postId = hits[i].id;
					hits[i].url = "javascript: switchState('post',{postId:'" + postId + "'});";
				}
        		switchState('searchResults',{'results':hits});
        	},
        	error : function(xmlHttpRequest,status,error) {
				alert("Failed to search. Status: " + status + ". Error: " + error);
			}
		});
	}
	
	ClogUtils.attachProfilePopup = function() {
		$('a.profile').cluetip({
			width: '620px',
			cluetipClass: 'clog',
			sticky: true,
 			dropShadow: false,
			arrows: true,
			mouseOutClose: true,
			closeText: '<img src="/library/image/silk/cross.png" alt="close" />',
			closePosition: 'top',
			showTitle: false,
			hoverIntent: true
		});
	}

	ClogUtils.getPreferences = function() {
		var prefs = null;
		jQuery.ajax( {
	 		url : "/direct/clog-preferences/" + clogSiteId + ".json",
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

	ClogUtils.savePreferences = function() {
		var emailFrequency = $('.clog_email_option:checked').val();
		var myData = {'siteId':clogSiteId,'emailFrequency':emailFrequency};

		jQuery.ajax( {
	 		url : "/direct/clog-preferences/new",
			type : 'POST',
			data : myData,
			timeout: 30000,
			async : false,
			dataType: 'text',
		   	success : function(result) {
		   		clogCurrentUserPreferences = myData;
				switchState('viewAllPosts');
			},
			error : function(xmlHttpRequest,status,error) {
				alert("Failed to save preferences. Status: " + status + '. Error: ' + error);
			}
	  	});

		return false;
	}

	ClogUtils.setPostsForCurrentSite = function() {

		jQuery.ajax( {
	       	url : "/direct/clog-post.json?siteId=" + clogSiteId,
	       	dataType : "json",
	       	async : false,
			cache: false,
		   	success : function(data) {
				clogCurrentPosts = data['clog-post_collection'];
			},
			error : function(xmlHttpRequest,status,errorThrown) {
				alert("Failed to get posts. Reason: " + errorThrown);
			}
	   	});
	}

	ClogUtils.savePostAsDraft = function() {
		ClogUtils.storePost('PRIVATE');
	}

	ClogUtils.publishPost = function() {
		ClogUtils.storePost('READY',true);
	}

	ClogUtils.publicisePost = function() {
		ClogUtils.storePost('PUBLIC');
	}
		
	ClogUtils.storePost = function(visibility,isPublish) {

		var post = {
				'id':$('#clog_post_id_field').val(),
				'visibility':visibility,
				'commentable':$('#clog_commentable_checkbox').attr('checked'),
				'title':$('#clog_title_field').val(),
				'content':FCKeditorAPI.GetInstance('clog_content_editor').GetXHTML(true),
				'siteId':clogSiteId
				};
				
		if(isPublish) post['mode'] = 'publish';

		jQuery.ajax( {
	 		url : "/direct/clog-post/new",
			type : 'POST',
			data : post,
			timeout: 30000,
			async : false,
			dataType: 'text',
		   	success : function(id) {
				switchState(clogHomeState);
			},
			error : function(xmlHttpRequest,status,error) {
				alert("Failed to store post. Status: " + status + '. Error: ' + error);
			}
	  	});
	}
	
	ClogUtils.saveComment = function() {

		var comment = {
				'id':$('#clog_comment_id_field').val(),
				'postId':clogCurrentPost.id,
				'content':FCKeditorAPI.GetInstance('clog_content_editor').GetXHTML(true),
				'siteId':clogSiteId
				};

		jQuery.ajax( {
	 		url : "/direct/clog-comment/new",
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

	ClogUtils.recyclePost = function(postId) {
		if(!confirm(clog_delete_post_message))
			return false;

		jQuery.ajax( {
	 		url : "/direct/clog-post/" + postId + "/recycle",
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

	ClogUtils.deleteSelectedPosts = function() {
		var selected = $('.clog_recycled_post_checkbox:checked');

		var commands = '';

		for(var i=0,j=selected.length;i<j;i++) {
			commands += "/direct/clog-post/" + selected[i].id + "?siteId=" + clogSiteId;
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

	ClogUtils.restoreSelectedPosts = function() {
		var selected = $('.clog_recycled_post_checkbox:checked');

		var commands = '';

		for(var i=0,j=selected.length;i<j;i++) {
			commands += "/direct/clog-post/" + selected[i].id + "/restore";
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

	ClogUtils.findPost = function(postId) {
		var post = null;
		
		if(!clogCurrentPosts) {
			jQuery.ajax( {
	 			url : "/direct/clog-post/" + postId + ".json",
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
			for(var i=0,j=clogCurrentPosts.length;i<j;i++) {
				if(clogCurrentPosts[i].id === postId)
					post = clogCurrentPosts[i];
			}
		}

		return post;
	}

	ClogUtils.deleteComment = function(commentId) {
		if(!confirm(clog_delete_comment_message))
			return false;
		
		jQuery.ajax( {
	 		url : "/direct/clog-comment/" + commentId + "?siteId=" + clogSiteId,
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
