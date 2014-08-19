/* Stuff that we always expect to be setup */
clog.currentUserPermissions = null;
clog.settings = {};
clog.currentPost = null;
clog.currentPosts = null;
clog.currentState = null;
clog.homeState = null;
clog.onMyWorkspace = false;
clog.onGateway = false;
clog.titleChanged = false;

clog.autosave_id = null;

clog.LOCAL_STORAGE_KEY = 'clog';

clog.states = {
    GROUPS: 'groups',
    GROUP_POSTS: 'groupPosts'
};

clog.fitFrame = function () {

    try {
        if (window.frameElement) {
            setMainFrameHeight(window.frameElement.id);
        }
    } catch (err) { }
};

clog.switchState = function (state,arg) {
	
	// Clear the autosave interval
	if (clog.autosave_id) {
		clearInterval(clog.autosave_id);
	}

	clog.currentState = state;

	// Just in case we have a floating cluetip hanging about
	$('#cluetip').hide();

	if (!clog.onGateway && clog.currentUserPermissions.postCreate) {
		$("#clog_create_post_link").show();
	} else {
		$("#clog_create_post_link").hide();
    }
	
	if ('home' === state) {
	    $('#clog_toolbar > li > span').removeClass('current');
	    $('#clog_home_link > span').addClass('current');
		clog.switchState(clog.homeState, arg);
	} else if ('viewAllPosts' === state) {

	    $('#clog_toolbar > li > span').removeClass('current');
	    $('#clog_home_link > span').addClass('current');

        $('#clog_toolbar_dropdown').val(clog.i18n.home_label);

		clog.utils.setCurrentPosts();
	 			
		clog.utils.renderTemplate('all_posts', { posts: clog.currentPosts,
                                                    onGateway: clog.onGateway,
                                                    siteId: clog.siteId,
                                                    showBody: clog.settings.showBody }, 'clog_content');

	 	$(document).ready(function () {

		    clog.currentPosts.forEach(function (p) {
			    clog.utils.renderPost(p, 'post_' + p.id);
            });

	 	    $(document).ready(function () {

		        clog.utils.attachProfilePopup();
                clog.fitFrame();
            });

            if (!clog.settings.showBody) {
                $('.clog_body').hide();
            }
        });
	} else if (clog.states.GROUPS === state) {
	    $('#clog_toolbar > li > span').removeClass('current');
	    $('#clog_groups_link > span').addClass('current');

		clog.utils.renderTemplate('groups', {groups: clog.groups, hasGroups: clog.groups.length > 0}, 'clog_content');

        $(document).ready(function () {

            $("#clog_group_table").tablesorter({
                widgets: ['zebra'],
                cssHeader:'clogSortableTableHeader',
                cssAsc:'clogSortableTableHeaderSortUp',
                cssDesc:'clogSortableTableHeaderSortDown',
                textExtraction: 'complex',	
                sortList: [[0,0]],
                headers: {
                    2: { sorter: "clogDate" }
                } });

            clog.fitFrame();
        });
	} else if (clog.states.GROUP_POSTS === state) {
        if (!arg || !arg.groupId) return;

        clog.currentGroupId = arg.groupId;
        clog.currentGroupTitle = arg.groupTitle;

        var url = "/direct/clog-post.json?siteId=" + clog.siteId + "&groupId=" + arg.groupId;

		jQuery.ajax( {
	       	'url': url,
	       	dataType: "json",
			cache: false,
		   	success: function (data) {

				clog.currentPosts = data['clog-post_collection'];

                clog.utils.addFormattedDatesToPosts(clog.currentPosts);
	 			
				clog.utils.renderTemplate('group_posts', { groupId: arg.groupId,
                                                            groupTitle: arg.groupTitle,
                                                            posts: clog.currentPosts,
                                                            showRSS: true,
                                                            siteId: clog.siteId,
                                                            showBody: clog.settings.showBody }, 'clog_content');

	 			$(document).ready(function () {

	 			    clog.currentPosts.forEach(function (p) {
					    clog.utils.renderPost(p, 'post_' + p.id);
                    });

	 			    $(document).ready(function () {

                        if (!clog.settings.showBody) {
                            $('.clog_body').hide();
                        }

                        clog.fitFrame();
				    });
                });
			},
			error: function (xmlHttpRequest, textStatus, errorThrown) {
				alert("Failed to get group posts. Reason: " + errorThrown);
			}
	   	});
	} else if ('viewMembers' === state) {
	    $('#clog_toolbar > li > span').removeClass('current');
	    $('#clog_view_authors_link > span').addClass('current');
		if (!clog.onGateway && clog.currentUserPermissions.postCreate) {
			$("#clog_create_post_link").show();
        } else {
			$("#clog_create_post_link").hide();
        }

		jQuery.ajax({
	    	url: "/direct/clog-author.json?siteId=" + clog.siteId,
	      	dataType: "json",
			cache: false,
		   	success: function (data) {

				var authors = data['clog-author_collection'];

                authors.forEach(function (a) {
                    a.formattedDateOfLastPost = clog.utils.formatDate(a.dateOfLastPost);
                });

				clog.utils.renderTemplate('authors', {'authors': authors, onGateway: clog.onGateway, siteId: clog.siteId}, 'clog_content');

 				$(document).ready(function () {

 					clog.utils.attachProfilePopup();

  					$("#clog_author_table").tablesorter({
							widgets: ['zebra'],
	 						cssHeader:'clogSortableTableHeader',
	 						cssAsc:'clogSortableTableHeaderSortUp',
	 						cssDesc:'clogSortableTableHeaderSortDown',
							textExtraction: 'complex',	
							sortList: [[0,0]],
	 						headers: {
	 							2: { sorter: "clogDate" },
                                4: { sorter: false }
	 						} }).tablesorterPager({ container: $("#clogAuthorPager"), positionFixed: false });

                    $('.pagedisplay').prop('disabled', true);

                    clog.fitFrame();
	   			});

			},
			error : function (xmlHttpRequest, status, errorThrown) {
				alert("Failed to get authors. Reason: " + errorThrown);
			}
	   	});
	} else if ('userPosts' === state) {

	    $('#clog_toolbar > li > span').removeClass('current');
	    $('#clog_my_clog_link > span').addClass('current');

		// Default to using the current session user id ...
		var userId = clog.userId;
		
		// ... but override it with any supplied one
		if (arg && arg.userId) {
			userId = arg.userId;
        }

		var url = "/direct/clog-post.json?siteId=" + clog.siteId + "&creatorId=" + userId + "&autosaved=true";

		jQuery.ajax( {
	       	'url': url,
	       	dataType: "json",
	       	async: false,
			cache: false,
		   	success: function (data) {

				var profileMarkup = clog.sakai.getProfileMarkup(userId);

				clog.currentPosts = data['clog-post_collection'];

                clog.utils.addFormattedDatesToPosts(clog.currentPosts);
	 			
		        var showRSS = userId !== clog.userId && !clog.onGateway;
				clog.utils.renderTemplate('user_posts', { creatorId: userId,
                                                            posts: clog.currentPosts,
                                                            showRSS: showRSS,
                                                            onGateway: clog.onGateway,
                                                            siteId: clog.siteId,
                                                            showBody: clog.settings.showBody }, 'clog_content');

	 			$(document).ready(function () {

				    $('#clog_author_profile').html(profileMarkup);

	 			    clog.currentPosts.forEach(function (p) {
					    clog.utils.renderPost(p, 'post_' + p.id);
                    });

	 			    $(document).ready(function () {

                        if (!clog.settings.showBody) {
                            $('.clog_body').hide();
                        }

                        clog.fitFrame();
				    });
                });
			},
			error: function (xmlHttpRequest, textStatus, errorThrown) {
				alert("Failed to get posts. Reason: " + errorThrown);
			}
	   	});
	} else if ('myPublicPosts' === state) {

		$('#clog_toolbar > li > span').removeClass('current');
		$('#clog_my_public_posts_link > span').addClass('current');
		// Default to using the current session user id ...
		var userId = clog.userId;

		var url = "/direct/clog-post.json?siteId=" + clog.siteId + "&creatorId=" + userId + "&visibilities=PUBLIC";

		// TODO: Factor this into a method. Same as above ...
		jQuery.ajax( {
	       	'url': url,
	       	dataType: "json",
	       	async: false,
			cache: false,
		   	success: function (data) {

				var profileMarkup = clog.sakai.getProfileMarkup(userId);

				clog.currentPosts = data['clog-post_collection'];

                clog.utils.addFormattedDatesToPosts(clog.currentPosts);
	 			
				clog.utils.renderTemplate('user_posts', {'creatorId': userId,'posts': clog.currentPosts}, 'clog_content');
				$('#clog_author_profile').html(profileMarkup);
                clog.currentPosts.forEach(function (p) {
                    clog.utils.renderPost(p, 'post_' + p.id);
                });

                clog.fitFrame();
			},
			error : function (xmlHttpRequest,status,errorThrown) {
				alert("Failed to get posts. Reason: " + errorThrown);
			}
	   	});
	} else if ('post' === state) {
		if (arg && arg.postId) {
			clog.currentPost = clog.utils.findPost(arg.postId);
        }

		if (!clog.currentPost) {
			return false;
        }
			
		clog.utils.addFormattedDatesToCurrentPost();

        var cp = clog.currentPost;
	 			
		clog.utils.renderTemplate('post_page', cp, 'clog_content');

		clog.utils.renderPost(cp, 'post_' + clog.currentPost.id);

	 	$(document).ready(function () {

			$('#clog_user_posts_link').click(function (e) {
				clog.switchState('userPosts',{'userId' : clog.currentPost.creatorId});
			});

			$('.content').show();

			if (clog.currentPost.comments.length > 0) $('.comments').show();

            clog.fitFrame();
	 	});
	} else if ('createPost' === state) {
	    $('#clog_toolbar > li > span').removeClass('current');
	    $('#clog_create_post_link > span').addClass('current');

		clog.currentPost = {id: '', title: '', content: '', commentable: true, groups: []};

		if (arg && arg.postId) {
			clog.currentPost = clog.utils.findPost(arg.postId);
			if (clog.currentPost.autosavedVersion) {
				if (confirm(clog.i18n.autosaved_copy_question)) {
					clog.currentPost = clog.currentPost.autosavedVersion;
				}
			}
		}

		clog.utils.renderTemplate('create_post', {post: clog.currentPost, onMyWorkspace: clog.onMyWorkspace, groups: clog.groups, hasGroups: clog.groups.length > 0}, 'clog_content');

	 	$(document).ready(function () {

	 		$('#clog_title_field').bind('keypress', function (e) {
				clog.titleChanged = true;	 		
	 		});

            $('#clog_visibility_maintainer,#clog_visibility_site').click(function (e) {

                $('#clog-group-fieldset').hide();
                clog.fitFrame();
            });

            if (clog.currentPost.groups.length > 0) {
                $('#clog-visibility-group').prop('checked', true);
                $('#clog-group-fieldset').show();
                clog.currentPost.groups.forEach(function (groupId) {
                    $('#clog-group-' + groupId).prop('selected', true);
                });
            }
            
            $('#clog-visibility-group').click(function (e) {
                $('#clog-group-fieldset').show();
                clog.fitFrame();
            });

			$('#clog_save_post_button').click(function () {
				clog.utils.savePostAsDraft(clog.editor);
			});

			$('#clog_make_post_public_button').click(function () {
				clog.utils.publicisePost(clog.editor);
			});

			$('#clog_publish_post_button').click(function () {
				clog.utils.publishPost(clog.editor);
			});

			$('#clog_cancel_button').click(function (e) {

				// If the current post has neither been saved or published, delete the autosaved copy
				if (!clog.currentPost.visibility) {
					clog.utils.deleteAutosavedCopy(clog.currentPost.id);
				}
				clog.switchState('home');
			});

            if ("MAINTAINER" === clog.currentPost.visibility) {
                $('#clog_visibility_maintainer').attr("checked","true");
            }
			
			// Start the auto saver
			clog.autosave_id = setInterval(function () {
					clog.utils.autosavePost(clog.editor);
				}, 10000);

 			clog.sakai.setupWysiwygEditor(clog.editor,'clog_content_editor',600,400);
	 	});
	} else if ('createComment' === state) {
		if (!arg || !arg.postId)
			return;

		clog.currentPost = clog.utils.findPost(arg.postId);

		var comment = {id: '',postId: arg.postId,content: ''};

		var currentIndex = -1;

		if (arg.commentId) {
			var comments = clog.currentPost.comments;

			comments.forEach(function (c) {

				if (c.id == arg.commentId) {
					comment = c;
				}
			});
		}

		clog.utils.renderTemplate('create_comment', comment, 'clog_content');

		$(document).ready(function () {

			clog.utils.renderPost(clog.currentPost, 'clog_post_' + arg.postId);

			$('#clog_save_comment_button').click(function () {
				clog.utils.saveComment(clog.editor);
			});
			
			clog.sakai.setupWysiwygEditor(clog.editor,'clog_content_editor',600,400);
		});
	} else if ('permissions' === state) {
	    $('#clog_toolbar > li > span').removeClass('current');
	    $('#clog_permissions_link > span').addClass('current');
		var perms = clog.utils.getSitePermissionMatrix();
		clog.utils.renderTemplate('permissions',{'perms': perms},'clog_content');

	 	$(document).ready(function () {

            clog.fitFrame();

			$('#clog_permissions_save_button').bind('click',clog.utils.savePermissions);
		});
	} else if ('viewRecycled' === state) {
	    $('#clog_toolbar >  li > span').removeClass('current');
	    $('#clog_recycle_bin_link > span').addClass('current');
		jQuery.ajax( {
	       	url: "/direct/clog-post.json?siteId=" + clog.siteId + "&visibilities=RECYCLED",
	       	dataType: "json",
	       	async: false,
			cache: false,
		   	success: function (data) {

				var posts = data['clog-post_collection'];

				clogRecycledPosts = posts;
                clog.utils.addFormattedDatesToPosts(clogRecycledPosts);
	 			
				clog.utils.renderTemplate('recycled_posts', {'posts': posts}, 'clog_content');
	 			posts.forEach(function (p) {
					clog.utils.renderPost(p, 'post_' + p.id);
                });

                clog.fitFrame();

				if (posts.length > 0) {
					$('#clog_really_delete_button').click(clog.utils.deleteSelectedPosts);
					$('#clog_restore_button').click(clog.utils.restoreSelectedPosts);
				}
				else {
					$('#clog_really_delete_button').prop('disabled', true);
					$('#clog_restore_button').prop('disabled', true);
				}
			},
			error : function (xmlHttpRequest, status, errorThrown) {
				alert("Failed to get posts. Reason: " + errorThrown);
			}
	   	});
	}
};

clog.getLocalStorageSetting = function (key) {

    if (typeof localStorage !== 'undefined') {
        var settings = JSON.parse(localStorage.getItem(clog.LOCAL_STORAGE_KEY)) || {};
        return settings[key] || false;
    }
};

clog.setLocalStorageSetting = function (key, value) {

    clog.settings[key] = value;

    if (typeof localStorage !== 'undefined') {
        var settings = JSON.parse(localStorage.getItem(clog.LOCAL_STORAGE_KEY)) || {};
        settings[key] = value;
        localStorage.setItem(clog.LOCAL_STORAGE_KEY, JSON.stringify(settings));
    }
};

clog.toggleFullContent = function (v) {

	if (v.checked) {
		$('.clog_body').hide();
        // CLOG-59
        this.setLocalStorageSetting('showBody', false);
    } else {
		$('.clog_body').show();
        // CLOG-59
        this.setLocalStorageSetting('showBody', true);
    }

    $(document).ready(function () {
        clog.fitFrame();
    });
};

(function ($) {

    if (typeof localStorage !== 'undefined') {
        clog.settings = JSON.parse(localStorage.getItem(clog.LOCAL_STORAGE_KEY)) || {};
    }

	if (!clog.placementId || !clog.siteId) {
		alert('The placement id and site id MUST be supplied as page parameters');
		return;
	}

    clog.monthMappings = {};
    clog.i18n.months.forEach(function (m, i) {
        clog.monthMappings[m] = i + 1;
    });

	if ('!gateway' === clog.siteId) {
		clog.onGateway = true;
	}
	
	if (clog.siteId.match(/^~/)) {
		clog.onMyWorkspace = true;
	}

	// This comes from sakai.properties, via the ClogTool servlet 
	if ('true' === clog.publicAllowed) {
		clog.publicAllowed = true;
	}
	
	clog.homeState = 'viewAllPosts';

	// We need the toolbar in a template so we can swap in the translations
    if (screen.width < 800) {
	    clog.utils.renderTemplate('pda_toolbar',{},'clog_toolbar');
        $('#clog_toolbar_dropdown').change(function () {

            if (clog_menu_label != this.value) { 
                clog.switchState(this.value);
            }
        });
    } else {
	    clog.utils.renderTemplate('toolbar', {} ,'clog_toolbar');

        if (clog.groups.length > 0) {
            $("#clog_groups_link").show().click(function (e) {
                return clog.switchState('groups');
            });
        }

	    $('#clog_home_link>span>a').click(function (e) {
		    return clog.switchState('home');
	    });

	    $('#clog_view_authors_link>span>a').click(function (e) {
		    return clog.switchState('viewMembers');
	    });

	    $('#clog_my_clog_link>span>a').click(function (e) {
		    return clog.switchState('userPosts');
    	});

	    if (clog.publicAllowed) {
		    $('#clog_my_public_posts_link>span>a').click(function (e) {
			    return clog.switchState('myPublicPosts');
		    });
	    }

	    $('#clog_create_post_link>span>a').click(function (e) {
		    return clog.switchState('createPost');
	    });

	    $('#clog_permissions_link>span>a').click(function (e) {
		    return clog.switchState('permissions');
	    });

	    $('#clog_recycle_bin_link>span>a').click(function (e) {
		    return clog.switchState('viewRecycled');
	    });
    }

	// If we are on a My Workspace type site (characterised by a tilde as the
	// first character in the site id), show the user's posts by default.
	if (clog.onMyWorkspace) {
		clog.state = 'userPosts';
		clog.homeState = 'userPosts';
		$("#clog_view_authors_link").hide();
		$("#clog_my_clog_link").hide();
		
		if (clog.publicAllowed) {
            $("#clog_my_public_posts_link").show();
        }
	}

	if (!clog.onGateway) {
		clog.currentUserPermissions = new ClogPermissions(clog.utils.getCurrentUserPermissions());
	
		if (clog.currentUserPermissions == null) return;
	
		if (clog.currentUserPermissions.modifyPermissions) {
			$("#clog_permissions_link").show();
		} else {
			$("#clog_permissions_link").hide();
        }

		if (clog.currentUserPermissions.postDeleteAny) {
			$("#clog_recycle_bin_link").show();
		} else {
			$("#clog_recycle_bin_link").hide();
		}

		if (clog.currentUserPermissions.postReadAny) {
			$("#clog_view_authors_link").show();
			$("#clog_view_authors_link").css('display','inline');
        }
	} else {
		$("#clog_permissions_link").hide();
		$("#clog_my_clog_link").hide();
		$("#clog_home_link").hide();
		$('#clog_recycle_bin_link').hide();
		clog.currentUserPermissions = new ClogPermissions();
	}

    $.tablesorter.addParser({
        id: 'clogDate',
        is: function (s) {
            return false;
        },
        format: function (s) {

            if (s === clog.i18n.none) {
                return 0;
            }

            var matches = s.match(/^([\d]{1,2}) (\w+) ([\d]{4}) \@ ([\d]{2}):([\d]{2}).*$/);
            var d = new Date(matches[3], clog.monthMappings[matches[2]], matches[1], matches[4], matches[5], 0);
            return d.getTime();
        },
        type: 'numeric'
    });

    clog.utils.addFormattedLastPostDatesToGroups(clog.groups);

    try {
        if (window.frameElement) {
            window.frameElement.style.minHeight = '600px';
        }
    } catch (err) { }
	
	// Now switch into the requested state
	clog.switchState(clog.state, clog);

})(jQuery);

