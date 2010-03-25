var BlogUtils;

(function()
{
	if(BlogUtils == null)
		BlogUtils = new Object();
		
	BlogUtils.getCurrentUser = function() {
		var user = null;
		jQuery.ajax(
		{
	 		url : "/direct/user/current.json",
	   		dataType : "json",
	   		async : false,
	   		cache : false,
		   	success : function(u) {
				user = u;
			},
			error : function(xmlHttpRequest,stat,error) {
				alert("Failed to get the current user. Status: " + stat + ". Error: " + error);
			}
	  	});

		return user;
	}

	BlogUtils.showSearchResults = function(searchTerms)
	{
    	jQuery.ajax(
		{
			url : "/portal/tool/" + yaftPlacementId + "/data/search/" + searchTerms,
        	dataType : "json",
        	async : false,
			cache: false,
        	success : function(results)
       	 	{
        		var params = new Object();
        		params["results"] = results;
				params["searchTerms"] = searchTerms;
				YaftUtils.render('yaft_search_results_content_template',params,'yaft_content');
	 			$(document).ready(function() {setMainFrameHeight(window.frameElement.id);});
        	},
        	error : function(xmlHttpRequest,status,error)
			{
			}
		});
	}

	BlogUtils.getPreferences = function() {
		var prefs = null;
		jQuery.ajax(
		{
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

		jQuery.ajax(
		{
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

	BlogUtils.parsePermissions = function() {
		var perms = [];

		jQuery.ajax( {
	       	url : "/direct/site/" + blogSiteId + "/perms/blog.json",
	       	dataType : "json",
	       	async : false,
			cache: false,
		   	success : function(p) {
				for(role in p.data) {
					var permSet = {'role':role};

					for(var i=0,j=p.data[role].length;i<j;i++) {
						var perm = p.data[role][i].replace(/\./g,"_");
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

	BlogUtils.savePermissions = function() {
		var boxes = $('.blog_permission_checkbox');
		var myData = {};
		for(var i=0,j=boxes.length;i<j;i++) {
			var box = boxes[i];
			if(box.checked)
				myData[box.id] = 'true';
			else
				myData[box.id] = 'false';
		}

		jQuery.ajax( {
	 		url : "/direct/site/" + blogSiteId + "/setPerms",
			type : 'POST',
			data : myData,
			timeout: 30000,
			async : false,
			dataType: 'text',
		   	success : function(result) {
				switchState('viewAllPosts');
			},
			error : function(xmlHttpRequest,status,error) {
				alert("Failed to create meeting. Status: " + status + '. Error: ' + error);
			}
	  	});


		return false;
	}
	

	BlogUtils.getProfileMarkup = function(userId) {
		var profile = '';

		jQuery.ajax( {
	       	url : "/direct/profile/" + userId + "/formatted",
	       	dataType : "html",
	       	async : false,
			cache: false,
		   	success : function(p) {
				profile = p;
			},
			error : function(xmlHttpRequest,stat,error) {
				alert("Failed to get profile markup. Status: " + stat + ". Error: " + error);
			}
	   	});

		return profile;
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

		var jsonData = JSON.stringify(post);

		jQuery.ajax( {
	 		url : "/direct/blog-post/new",
			type : 'POST',
			data : {'json':jsonData},
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
				'content':FCKeditorAPI.GetInstance('blog_content_editor').GetXHTML(true)
				};
				
		var jsonData = JSON.stringify(comment);

		jQuery.ajax( {
	 		url : "/direct/blog-comment/new",
			type : 'POST',
			data : {'json':jsonData,'siteId':blogSiteId},
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
		
	BlogUtils.getParameters = function() {
		var arg = new Object();
		var href = document.location.href;

		if ( href.indexOf( "?") != -1) {
			var paramString = href.split( "?")[1];
			
			if(paramString.indexOf("#") != -1)
				paramString = paramString.split("#")[0];
				
			var params = paramString.split("&");

			for (var i = 0; i < params.length; ++i) {
				var name = params[i].split( "=")[0];
				var value = params[i].split( "=")[1];
				arg[name] = unescape(value);
			}
		}
	
		return arg;
	}

	BlogUtils.getCurrentUserPermissions = function() {
		var permissions = null;
		jQuery.ajax( {
	 		url : "/direct/site/" + blogSiteId + "/userPerms/blog.json",
	   		dataType : "json",
	   		async : false,
	   		cache : false,
		   	success : function(perms,status) {
				permissions = perms;
			},
			error : function(xmlHttpRequest,stat,error) {
				alert("Failed to get the current user permissions. Status: " + stat + ". Error: " + error);
			}
	  	});
	  	
	  	return permissions;
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
	
	BlogUtils.render = function(templateName,contextObject,output) {
		var templateNode = document.getElementById(templateName);
		var firstNode = templateNode.firstChild;
		var template = null;

		if ( firstNode && ( firstNode.nodeType === 8 || firstNode.nodeType === 4))
  			template = templateNode.firstChild.data.toString();
		else
   			template = templateNode.innerHTML.toString();

		var trimpathTemplate = TrimPath.parseTemplate(template,templateName);

   		var render = trimpathTemplate.process(contextObject);

		if (output)
			document.getElementById(output).innerHTML = render;

		return render;
	}

	BlogUtils.setupEditor = function(textarea_id,width,height,toolbarSet,siteId) {
		var oFCKeditor = new FCKeditor(textarea_id);

		oFCKeditor.BasePath = "/library/editor/FCKeditor/";
		oFCKeditor.Width  = width;
		oFCKeditor.Height = height;
		oFCKeditor.ToolbarSet = toolbarSet;
		
		var collectionId = "/group/" + siteId + "/";
		
		oFCKeditor.Config['ImageBrowserURL'] = oFCKeditor.BasePath + "editor/filemanager/browser/default/browser.html?Connector=/sakai-fck-connector/filemanager/connector&Type=Image&CurrentFolder=" + collectionId;
		oFCKeditor.Config['LinkBrowserURL'] = oFCKeditor.BasePath + "editor/filemanager/browser/default/browser.html?Connector=/sakai-fck-connector/filemanager/connector&Type=Link&CurrentFolder=" + collectionId;
		oFCKeditor.Config['FlashBrowserURL'] = oFCKeditor.BasePath + "editor/filemanager/browser/default/browser.html?Connector=/sakai-fck-connector/filemanager/connector&Type=Flash&CurrentFolder=" + collectionId;
		oFCKeditor.Config['ImageUploadURL'] = oFCKeditor.BasePath + "/sakai-fck-connector/filemanager/connector?Type=Image&Command=QuickUpload&Type=Image&CurrentFolder=" + collectionId;
		oFCKeditor.Config['FlashUploadURL'] = oFCKeditor.BasePath + "/sakai-fck-connector/filemanager/connector?Type=Flash&Command=QuickUpload&Type=Flash&CurrentFolder=" + collectionId;
		oFCKeditor.Config['LinkUploadURL'] = oFCKeditor.BasePath + "/sakai-fck-connector/filemanager/connector?Type=File&Command=QuickUpload&Type=Link&CurrentFolder=" + collectionId;

		oFCKeditor.Config['CurrentFolder'] = collectionId;

		oFCKeditor.Config['CustomConfigurationsPath'] = "/library/editor/FCKeditor/config.js";
		oFCKeditor.ReplaceTextarea();
	}

}) ();
