/* Stuff that we always expect to be setup */
var clogSiteId = null;
var clogPlacementId = null;
var clogCurrentUserPermissions = null;
var clogCurrentUserGlobalPreferences = null;
var clogCurrentPost = null;
var clogCurrentPosts = null;
var clogCurrentState = null;
var clogCurrentUser = null;
var clogHomeState = null;
var clogOnMyWorkspace = false;
var clogOnGateway = false;
var clogPublicAllowed = false;
var clogTitleChanged = false;
var clogTextAreaChanged = false;
var wysiwygEditor = 'fckEditor'; //default

var autosave_id = null;

var clogBaseDataUrl = "";

var clogInPDA = false;

(function () {

	var arg = SakaiUtils.getParameters();

	if (arg.editor) {
		wysiwygEditor = arg.editor;
	}
	
	if(!arg || !arg.placementId || !arg.siteId) {
		alert('The placement id and site id MUST be supplied as page parameters');
		return;
	}
	
	clogSiteId = arg.siteId;
	clogPlacementId = arg.placementId;

	if('true' === arg.publicAllowed) clogPublicAllowed = true;

	var href = document.location.href;

    if(href.indexOf("portal/pda") != -1) {
        clogBaseDataUrl = "/portal/pda/" + clogSiteId + "/tool/" + clogPlacementId + "/";
        clogInPDA = true;
        wysiwygEditor = 'none';
    } else {
        clogBaseDataUrl = "/portal/tool/" + clogPlacementId + "/";
        clogInPDA = false;
    }

    // CLOG-70. To support the en_US_DEBUG locales we need to do this as jquery.localisation
    // seems to drop variants from locale strings. Needs to be fixed in jquery.localisation; the
    // author has been contacted.
    if('en_US_DEBUG' === arg['language']) arg['language'] = 'db_DB';

    if(arg['language']) {
        $.localise('clog-translations',{language:arg['language'],loadBase: true});
    }
    else {
        $.localise('clog-translations');
    }

	// We need the toolbar in a template so we can swap in the translations
    if(clogInPDA && screen.width < 800) {
	    SakaiUtils.renderTrimpathTemplate('clog_pda_toolbar_template',{},'clog_toolbar');
        $('#clog_toolbar_dropdown').change(function () {
            if(clog_menu_label != this.value) { 
                switchState(this.value);
            }
        });
    } else {
	    SakaiUtils.renderTrimpathTemplate('clog_toolbar_template',{},'clog_toolbar');

	    $('#clog_home_link').click(function(e) {
		    return switchState('home');
	    });

	    $('#clog_view_authors_link').click(function(e) {
		    return switchState('viewMembers');
	    });

	    $('#clog_my_clog_link').click(function(e) {
		    return switchState('userPosts');
    	});

	    if(clogPublicAllowed) {
		    $('#clog_my_public_posts_link').click(function(e) {
			    return switchState('myPublicPosts');
		    });
	    }

	    $('#clog_create_post_link').click(function(e) {
		    return switchState('createPost');
	    });

	    $('#clog_permissions_link').click(function(e) {
		    return switchState('permissions');
	    });

	    $('#clog_recycle_bin_link').click(function(e) {
		    return switchState('viewRecycled');
	    });
    }

	$('#clog_search_field').change(function(e) {
		ClogUtils.showSearchResults();
	});
	
	$('#clog_search_button').click(function(e) {
		ClogUtils.showSearchResults();
	});
	

	clogHomeState = 'viewAllPosts';

	if(clogSiteId.match(/^~/)) clogOnMyWorkspace = true;

	// If we are on a My Workspace type site (characterised by a tilde as the
	// first character in the site id), show the user's posts by default.
	if(clogOnMyWorkspace) {
		arg.state = 'userPosts';
		clogHomeState = 'userPosts';
		$("#clog_view_authors_link").hide();
		$("#clog_my_clog_link").hide();
		
		if(clogPublicAllowed) $("#clog_my_public_posts_link").show();
	}

	if('!gateway' === clogSiteId) clogOnGateway = true;

	clogCurrentUser = SakaiUtils.getCurrentUser();
	
	if(!clogCurrentUser) {
		alert("No current user. Have you logged in?");
		return;
	}

	if(!clogOnGateway) {
		clogCurrentUserGlobalPreferences = ClogUtils.getGlobalPreferences();
		
		clogCurrentUserPermissions = new ClogPermissions(ClogUtils.getCurrentUserPermissions());
	
		if(clogCurrentUserPermissions == null) return;
	
		if(clogCurrentUserPermissions.modifyPermissions) {
			$("#clog_permissions_link").show();
			$("#clog_recycle_bin_link").show();
		}
		else {
			$("#clog_permissions_link").hide();
			$("#clog_recycle_bin_link").hide();
		}
	}
	else {
		$("#clog_permissions_link").hide();
		$("#clog_my_clog_link").hide();
		$('#clog_recycle_bin_link').hide();
		clogCurrentUserPermissions = new ClogPermissions();
	}

	if(window.frameElement)
		window.frameElement.style.minHeight = '600px';
	
	// Now switch into the requested state
	switchState(arg.state,arg);

})();

function switchState(state,arg) {
	
	// Clear the autosave interval
	if(autosave_id)
		clearInterval(autosave_id);

	clogCurrentState = state;

	$('#cluetip').hide();

	if(!clogOnGateway && clogCurrentUserPermissions.postCreate)
		$("#clog_create_post_link").show();
	else
		$("#clog_create_post_link").hide();
	
	if('home' === state) {
		switchState(clogHomeState,arg);
	}
	else if('viewAllPosts' === state) {

        $('#clog_toolbar_dropdown').val(clog_home_label);

		ClogUtils.setCurrentPosts();
	 			
		SakaiUtils.renderTrimpathTemplate('clog_all_posts_template',{'posts':clogCurrentPosts},'clog_content');
	 	$(document).ready(function () {
		    for(var i=0,j=clogCurrentPosts.length;i<j;i++) {
			    SakaiUtils.renderTrimpathTemplate('clog_post_template',clogCurrentPosts[i],'post_' + clogCurrentPosts[i].id);
            }
	 	    $(document).ready(function () {
		        ClogUtils.attachProfilePopup();
		        if(window.frameElement) {
	 			    setMainFrameHeight(window.frameElement.id);
		        }
            });
            if (!clogCurrentUserGlobalPreferences.showBody) {
                $('.clog_body').hide();
            }
        });
	}
	else if('viewMembers' === state) {
		if(!clogOnGateway && clogCurrentUserPermissions.postCreate)
			$("#clog_create_post_link").show();
		else
			$("#clog_create_post_link").hide();

		jQuery.ajax({
	    	url : "/direct/clog-author.json?siteId=" + clogSiteId,
	      	dataType : "json",
	       	async : false,
			cache: false,
		   	success : function(data) {
				SakaiUtils.renderTrimpathTemplate('clog_authors_content_template',{'authors':data['clog-author_collection']},'clog_content');

 				$(document).ready(function () {
 					ClogUtils.attachProfilePopup();
  									
  					$("#clog_author_table").tablesorter({
							widgets: ['zebra'],
	 						cssHeader:'clogSortableTableHeader',
	 						cssAsc:'clogSortableTableHeaderSortUp',
	 						cssDesc:'clogSortableTableHeaderSortDown',
							textExtraction: 'complex',	
							sortList: [[0,0]],
	 						headers:
	 						{
	 							2: {sorter: "isoDate"},
	 							3: {sorter: "isoDate"},
                                5: {sorter: false}
	 						} }).tablesorterPager({container: $("#clogAuthorPager"),positionFixed: false});

                    $('.pagedisplay').attr('disabled','true');
	 						
 					if(window.frameElement)
	 					setMainFrameHeight(window.frameElement.id);
	   			});

			},
			error : function(xmlHttpRequest,status,errorThrown) {
				alert("Failed to get authors. Reason: " + errorThrown);
			}
	   	});
	}
	else if('userPosts' === state) {
		// Default to using the current session user id ...
		var userId = clogCurrentUser.id;
		
		// ... but override it with any supplied one
		if(arg && arg.userId)
			userId = arg.userId;

		var url = "/direct/clog-post.json?siteId=" + clogSiteId + "&creatorId=" + userId + "&autosaved=true";

		jQuery.ajax( {
	       	'url' : url,
	       	dataType : "json",
	       	async : false,
			cache: false,
		   	success : function(data) {

				var profileMarkup = SakaiUtils.getProfileMarkup(userId);

				clogCurrentPosts = data['clog-post_collection'];

                ClogUtils.addFormattedDatesToPosts(clogCurrentPosts);
	 			
				SakaiUtils.renderTrimpathTemplate('clog_user_posts_template',{'creatorId':userId,'posts':clogCurrentPosts},'clog_content');

	 			$(document).ready(function() {
				    $('#clog_author_profile').html(profileMarkup);
	 			    for(var i=0,j=clogCurrentPosts.length;i<j;i++) {
					    SakaiUtils.renderTrimpathTemplate('clog_post_template',clogCurrentPosts[i],'post_' + clogCurrentPosts[i].id);
                    }

	 			    $(document).ready(function() {
	 			        if(window.frameElement) {
	 					    setMainFrameHeight(window.frameElement.id);
	 				    }
                        if (!clogCurrentUserGlobalPreferences.showBody) {
                            $('.clog_body').hide();
                        }
				    });
                });
			},
			error : function(xmlHttpRequest,status,errorThrown) {
				alert("Failed to get posts. Reason: " + errorThrown);
			}
	   	});
	}
	else if('myPublicPosts' === state) {
		// Default to using the current session user id ...
		var userId = clogCurrentUser.id;

		var url = "/direct/clog-post.json?siteId=" + clogSiteId + "&creatorId=" + userId + "&visibilities=PUBLIC";

		// TODO: Factor this into a method. Same as above ...
		jQuery.ajax( {
	       	'url' : url,
	       	dataType : "json",
	       	async : false,
			cache: false,
		   	success : function(data) {

				var profileMarkup = SakaiUtils.getProfileMarkup(userId);

				clogCurrentPosts = data['clog-post_collection'];
	 			
				SakaiUtils.renderTrimpathTemplate('clog_user_posts_template',{'creatorId':userId,'posts':clogCurrentPosts},'clog_content');
				$('#clog_author_profile').html(profileMarkup);
	 			for(var i=0,j=clogCurrentPosts.length;i<j;i++)
					SakaiUtils.renderTrimpathTemplate('clog_post_template',clogCurrentPosts[i],'post_' + clogCurrentPosts[i].id);

	 			if(window.frameElement) {
	 				$(document).ready(function() {
	 					setMainFrameHeight(window.frameElement.id);
	 				});
				}
			},
			error : function(xmlHttpRequest,status,errorThrown) {
				alert("Failed to get posts. Reason: " + errorThrown);
			}
	   	});
	}
	else if('post' === state) {
		if(arg && arg.postId)
			clogCurrentPost = ClogUtils.findPost(arg.postId);

		if(!clogCurrentPost)
			return false;
			
		ClogUtils.addFormattedDatesToCurrentPost();
	 			
		SakaiUtils.renderTrimpathTemplate('clog_post_page_content_template',clogCurrentPost,'clog_content');
		SakaiUtils.renderTrimpathTemplate('clog_post_template',clogCurrentPost,'post_' + clogCurrentPost.id);

	 	$(document).ready(function() {
			$('#clog_user_posts_link').click(function (e) {
				switchState('userPosts',{'userId' : clogCurrentPost.creatorId});
			});

			$('.content').show();

			if(clogCurrentPost.comments.length > 0) $('.comments').show();

	 		if(window.frameElement)
	 			setMainFrameHeight(window.frameElement.id);
	 	});
	}
	else if('createPost' === state) {
		clogCurrentPost = {id:'',title:'',content:'',commentable:true};

		if(arg && arg.postId) {
			clogCurrentPost = ClogUtils.findPost(arg.postId);
			if(clogCurrentPost.autosavedVersion) {
				if(confirm('There is an autosaved version of this post. Do you want to use that instead?')) {
					clogCurrentPost = clogCurrentPost.autosavedVersion;
				}
			}
		}

		SakaiUtils.renderTrimpathTemplate('clog_create_post_template',clogCurrentPost,'clog_content');

	 	$(document).ready(function () {
	 		$('#clog_title_field').bind('keypress',function (e) {
				clogTitleChanged = true;	 		
	 		});

            if(clogInPDA) {
	 		    $('#clog_content_editor').bind('keypress',function (e) {
				    clogTextAreaChanged = true;	 		
	 		    });
            }
	 		
 			SakaiUtils.setupWysiwygEditor(wysiwygEditor,'clog_content_editor',600,400,'Default',clogSiteId);
	 		
			$('#clog_save_post_button').click(function () {
				ClogUtils.savePostAsDraft(wysiwygEditor);
			});

			$('#clog_make_post_public_button').click(function () {
				ClogUtils.publicisePost(wysiwygEditor);
			});

			$('#clog_publish_post_button').click(function() {
				ClogUtils.publishPost(wysiwygEditor);
			});

			$('#clog_cancel_button').click(function(e) {
				// If the current post has neither been saved or published, delete the autosaved copy
				if(!clogCurrentPost.visibility) {
					ClogUtils.deleteAutosavedCopy(clogCurrentPost.id);
				}
				switchState('home');
			});

            if("MAINTAINER" === clogCurrentPost.visibility) {
                $('#clog_visibility_maintainer').attr("checked","true");
            }
			
			// Start the auto saver
			autosave_id = setInterval(function() {
					if(ClogUtils.autosavePost(wysiwygEditor)) {
						$('#clog_autosaved_message').show();
						setTimeout(function() {
								$('#clog_autosaved_message').fadeOut(200);
							},2000);
					}
				},10000);

	 		if(window.frameElement)
	 			setMainFrameHeight(window.frameElement.id);
	 	});
	}
	else if('createComment' === state) {
		if(!arg || !arg.postId)
			return;

		clogCurrentPost = ClogUtils.findPost(arg.postId);

		var comment = {id: '',postId: arg.postId,content: ''};

		var currentIndex = -1;

		if(arg.commentId) {
			var comments = clogCurrentPost.comments;

			for(var i=0,j=comments.length;i<j;i++) {
				if(comments[i].id == arg.commentId) {
					comment = comments[i];
					currentIndex = i;
					break;
				}
			}
		}

		SakaiUtils.renderTrimpathTemplate('clog_create_comment_template',comment,'clog_content');

		$(document).ready(function() {
			SakaiUtils.setupWysiwygEditor(wysiwygEditor,'clog_content_editor',600,400,'Default',clogSiteId);
			SakaiUtils.renderTrimpathTemplate('clog_post_template',clogCurrentPost,'clog_post_' + arg.postId);

			$('#clog_save_comment_button').click(function () {
				ClogUtils.saveComment(wysiwygEditor);
			});
			
			if(window.frameElement)
				setMainFrameHeight(window.frameElement.id);
		});
	}
	else if('permissions' === state) {
		var perms = ClogUtils.getSitePermissionMatrix();
		SakaiUtils.renderTrimpathTemplate('clog_permissions_content_template',{'perms':perms},'clog_content');

	 	$(document).ready(function() {
			$('#clog_permissions_save_button').bind('click',ClogUtils.savePermissions);

			if(window.frameElement)
				setMainFrameHeight(window.frameElement.id);
		});
	}
	else if('viewRecycled' === state) {
		jQuery.ajax( {
	       	url : "/direct/clog-post.json?siteId=" + clogSiteId + "&visibilities=RECYCLED",
	       	dataType : "json",
	       	async : false,
			cache: false,
		   	success : function(data) {

				var posts = data['clog-post_collection'];

				clogRecycledPosts = posts;
                ClogUtils.addFormattedDatesToPosts(clogRecycledPosts);
	 			
				SakaiUtils.renderTrimpathTemplate('clog_recycled_posts_template',{'posts':posts},'clog_content');
	 			for(var i=0,j=posts.length;i<j;i++)
					SakaiUtils.renderTrimpathTemplate('clog_post_template',posts[i],'post_' + posts[i].id);

				if(posts.length > 0) {
					$('#clog_really_delete_button').click(ClogUtils.deleteSelectedPosts);
					$('#clog_restore_button').click(ClogUtils.restoreSelectedPosts);
				}
				else {
					$('#clog_really_delete_button').attr('disabled','disabled');
					$('#clog_restore_button').attr('disabled','disabled');
				}

	 			if(window.frameElement) {
	 				$(document).ready(function() {
	 					setMainFrameHeight(window.frameElement.id);
	 				});
				}
			},
			error : function(xmlHttpRequest,status,errorThrown) {
				alert("Failed to get posts. Reason: " + errorThrown);
			}
	   	});
	}
	else if('searchResults' === state) {
		SakaiUtils.renderTrimpathTemplate('clog_search_results_template',arg,'clog_content');
	}
}

function toggleFullContent(v)
{
 	if(window.frameElement) {
		$(document).ready(function() {
 			setMainFrameHeight(window.frameElement.id);
		});
    }
	
	if(v.checked) {
		$('.clog_body').hide();
        // CLOG-59
        clogCurrentUserGlobalPreferences.showBody = 'false';
        ClogUtils.saveGlobalPreferences();
    } else {
		$('.clog_body').show();
        // CLOG-59
        clogCurrentUserGlobalPreferences.showBody = 'true';
        ClogUtils.saveGlobalPreferences();
    }
}
