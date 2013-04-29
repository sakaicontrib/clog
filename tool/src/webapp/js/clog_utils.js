var ClogUtils;

(function()
{
	if(ClogUtils == null)
		ClogUtils = new Object();

	ClogUtils.showSearchResults = function() {
	
		var searchTerms = $('#clog_search_field').val();

        if(searchTerms.length == 0) {
            return;
        }
		
    	jQuery.ajax( {
			url : "/portal/tool/" + clogPlacementId + "/search",
			type : 'POST',
        	dataType : "json",
        	async : false,
			cache: false,
        	data : {'searchTerms':searchTerms},
        	success : function(results) {
        		var hits = results;
				for(var i=0,j=hits.length;i<j;i++) {
					var postId = hits[i].id;
					hits[i].url = "javascript: switchState('post',{postId:'" + postId + "'});";
				}
        		switchState('searchResults',{'results':hits});
        	},
        	error : function(xhr,textStatus,errorThrown) {
				alert(xhr.responseText);
			}
		});
	}
	
	ClogUtils.getCurrentUserPermissions = function() {
		var permissions = null;
		jQuery.ajax( {
	 		url : clogBaseDataUrl + "userPerms.json",
	   		dataType : "json",
	   		async : false,
	   		cache : false,
		   	success : function(perms) {
				permissions = perms;
			},
			error : function(xmlHttpRequest,stat,error) {
				alert("Failed to get the current user permissions. Status: " + stat + ". Error: " + error);
			}
	  	});
	  	
	  	return permissions;
	}
	
	ClogUtils.getSitePermissionMatrix = function() {
        var perms = [];

        jQuery.ajax( {
	 		url : clogBaseDataUrl + "perms.json",
            //url : "/portal/tool/" + clogPlacementId + "/perms",
            dataType : "json",
            async : false,
            cache: false,
            success : function(p) {
                for(role in p) {
                    var permSet = {'role':role};

                    for(var i=0,j=p[role].length;i<j;i++) {
                        var perm = p[role][i].replace(/\./g,"_");
                        eval("permSet." + perm + " = true");
                    }

                    perms.push(permSet);
                }
            },
            error : function(xmlHttpRequest,stat,error) {
                alert("Failed to get permissions. Status: " + stat + ". Error: " + error);
            }
        });

        return perms;
    }
    
    ClogUtils.savePermissions = function() {
        var boxes = $('.clog_permission_checkbox');
        var myData = {};
        for(var i=0,j=boxes.length;i<j;i++) {
            var box = boxes[i];
            if(box.checked)
                myData[box.id] = 'true';
            else
                myData[box.id] = 'false';
        }

        jQuery.ajax( {
            url : "/portal/tool/" + clogPlacementId + "/setPerms",
            type : 'POST',
            data : myData,
            timeout: 30000,
            async : false,
            dataType: 'text',
            success : function(result) {
                switchState('viewAllPosts');
            },
            error : function(xmlHttpRequest,status,error) {
                alert("Failed to save permissions. Status: " + status + '. Error: ' + error);
            }
        });

        return false;
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

    ClogUtils.addFormattedDatesToPosts = function (posts) {
        for(var i=0,j=posts.length;i<j;i++) {
        	ClogUtils.addFormattedDateToPost(posts[i]);
        }
    }
    
    ClogUtils.addFormattedDateToPost = function(post) {
            var d = new Date(post.createdDate);
            var formattedCreatedDate = d.getDate() + " " + clog_month_names[d.getMonth()] + " " + d.getFullYear() + " @ " + d.getHours() + ":" + d.getMinutes();
            post.formattedCreatedDate = formattedCreatedDate;

            d = new Date(post.modifiedDate);
            var formattedModifiedDate = d.getDate() + " " + clog_month_names[d.getMonth()] + " " + d.getFullYear() + " @ " + d.getHours() + ":" + d.getMinutes();
            post.formattedModifiedDate = formattedModifiedDate;

            for(var k=0,m=post.comments.length;k<m;k++) {
                d = new Date(post.comments[k].createdDate);
                formattedCreatedDate = d.getDate() + " " + clog_month_names[d.getMonth()] + " " + d.getFullYear() + " @ " + d.getHours() + ":" + d.getMinutes();
                post.comments[k].formattedCreatedDate = formattedCreatedDate;

                d = new Date(post.comments[k].modifiedDate);
                var formattedModifiedDate = d.getDate() + " " + clog_month_names[d.getMonth()] + " " + d.getFullYear() + " @ " + d.getHours() + ":" + d.getMinutes();
                post.comments[k].formattedModifiedDate = formattedModifiedDate;
            }
        }

    ClogUtils.addFormattedDatesToCurrentPost = function () {
    	ClogUtils.addFormattedDateToPost(clogCurrentPost);
	}

	ClogUtils.setCurrentPosts = function() {

		jQuery.ajax( {
	       	url : "/direct/clog-post.json?siteId=" + clogSiteId + "&autosaved=true",
	       	dataType : "json",
	       	async : false,
			cache: false,
		   	success : function(data) {
				clogCurrentPosts = data['clog-post_collection'];
                ClogUtils.addFormattedDatesToPosts(clogCurrentPosts);
			},
			error : function(xmlHttpRequest,status,errorThrown) {
				alert("Failed to get posts. Reason: " + errorThrown);
			}
	   	});
	}
	
	ClogUtils.autosavePost = function(wysiwygEditor) {
		
		if(!SakaiUtils.isEditorDirty(wysiwygEditor,'clog_content_editor') && !clogTitleChanged) {
			return;
		}
	
		return ClogUtils.storePost('AUTOSAVE',null,wysiwygEditor);
	}

	ClogUtils.savePostAsDraft = function(wysiwygEditor) {
		return ClogUtils.storePost('PRIVATE',null,wysiwygEditor);
	}

	ClogUtils.publishPost = function(wysiwygEditor) {
		return ClogUtils.storePost('READY',true,wysiwygEditor);
	}

	ClogUtils.publicisePost = function(wysiwygEditor) {
		if(confirm(clog_public_question))
			return ClogUtils.storePost('PUBLIC',null,wysiwygEditor);
	}
		
	ClogUtils.storePost = function(visibility,isPublish,wysiwygEditor) {

	    var title = $('#clog_title_field').val();

		if(title.length < 4) {
            if('AUTOSAVE' !== visibility) {
                alert(clog_short_title_warning);
            }
			return 0;
		}

		var success = false;
		
		if('READY' === visibility) {
			visibility = ($('#clog_visibility_maintainer').attr('checked')) ? 'MAINTAINER':'SITE';
		}

		var post = {
				'id':$('#clog_post_id_field').val(),
				'visibility':visibility,
				'commentable':$('#clog_commentable_checkbox').attr('checked') === 'checked',
				'title':title,
				'content':SakaiUtils.getEditorData(wysiwygEditor,'clog_content_editor'),
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
		   		if('AUTOSAVE' !== visibility) {
					switchState(clogHomeState);
				}
				else {
					clogCurrentPost.id = id;
					$('#clog_post_id_field').val(id);
				}

				success = true;
				clogTitleChanged = false;
                SakaiUtils.resetEditor(wysiwygEditor,'clog_content_editor');
			},
			error : function(xmlHttpRequest,status,error) {
				alert("Failed to store post. Status: " + status + '. Error: ' + error);
			}
	  	});

		return success;
	}
	
	ClogUtils.saveComment = function(wysiwygEditor) {
		
		var comment = {
				'id':$('#clog_comment_id_field').val(),
				'postId':clogCurrentPost.id,
				'content':SakaiUtils.getEditorData(wysiwygEditor,'clog_content_editor'),
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

	ClogUtils.deleteAutosavedCopy = function(postId) {

        if(postId === '') {
            // CLOG-19 The post hasn't been autosaved yet. If it had been it would have an id
            return;
        }

		jQuery.ajax( {
	 		url : "/direct/clog-post/" + postId + "/deleteAutosavedCopy",
			timeout: 30000,
			async : false,
			dataType: 'text',
		   	success : function(result) {
				switchState('viewAllPosts');
			},
			error : function(xmlHttpRequest,status,error) {
				alert("Failed to delete autosaved copy. Status: " + status + '. Error: ' + error);
			}
	  	});
	}

	ClogUtils.recyclePost = function(postId) {
		if(!confirm(clog_delete_post_message))
			return false;

		jQuery.ajax( {
	 		url : "/direct/clog-post/" + postId + "/recycle",
			dataType : 'text',
			async : false,
			cache: false,
		   	success : function(result) {
				switchState(clogCurrentState);
			},
			error : function(xmlHttpRequest,status,error) {
				alert("Failed to recycle post. Status: " + status + '. Error: ' + error);
			}
	  	});

		return false;
	}

	ClogUtils.deleteSelectedPosts = function() {
	
		if(!confirm(clog_really_delete_post_message)) {
			return false;
		}
		
		var selected = $('.clog_recycled_post_checkbox:checked');

        if(selected.length <= 0) {
            // No posts selected for deletion
            return;
        }
        
		var postIds = '';

		for(var i=0,j=selected.length;i<j;i++) {
			postIds += selected[i].id;
			if(i < (j - 1)) postIds += ",";
		}

		jQuery.ajax( {
	 		url : "/direct/clog-post/remove?posts=" + postIds + "&site=" + clogSiteId,
			dataType : 'text',
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

        // CLOG-29
        if(selected.length <= 0) {
            // No posts selected for restoration
            return;
        }

		var postIds = '';

		for(var i=0,j=selected.length;i<j;i++) {
			postIds += selected[i].id;
			if(i < (j - 1)) postIds += ",";
		}

		jQuery.ajax( {
	 		url : "/direct/clog-post/restore?posts=" + postIds,
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
