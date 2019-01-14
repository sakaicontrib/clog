/* Stuff that we always expect to be setup */
clog.currentUserPermissions = null;
clog.settings = {};
clog.currentPost = null;
clog.currentPosts = [];
clog.currentState = null;
clog.homeState = null;
clog.onMyWorkspace = false;
clog.onGateway = false;
clog.titleChanged = false;
clog.autosave_id = null;
clog.page = 0;
clog.postsTotal = 0;
clog.postsRendered = 0;

// Sorting keys start
clog.SORT_NAME_UP = 'sortnameup';
clog.SORT_NAME_DOWN = 'sortnamedown';
clog.sortByName = clog.SORT_NAME_DOWN;
clog.SORT_POSTS_UP = 'sortpostsup';
clog.SORT_POSTS_DOWN = 'sortpostsdown';
clog.sortByPosts = clog.SORT_POSTS_DOWN;
clog.SORT_LAST_POST_UP = 'sortlastpostup';
clog.SORT_LAST_POST_DOWN = 'sortlastpostdown';
clog.sortByLastPost = clog.SORT_LAST_POST_DOWN;
clog.SORT_COMMENTS_UP = 'sortcommentsup';
clog.SORT_COMMENTS_DOWN = 'sortcommentsdown';
clog.sortByComments = clog.SORT_COMMENTS_DOWN;
// Sorting keys end
//
clog.LOCAL_STORAGE_KEY = 'clog';
clog.AJAX_TIMEOUT = 5000;

clog.states = {
    GROUPS: 'groups',
    GROUP_POSTS: 'groupPosts'
};

clog.switchState = function (state,arg) {

    // CLOG-200
    // We don't want infinite scroll for everything. Views that need it will switch it back on
    $(window).off('scroll.clog');

	// Clear the autosave interval
	if (clog.autosave_id) {
		clearInterval(clog.autosave_id);
	}

	clog.currentState = state;

	// Just in case we have a floating cluetip hanging about
	$('#cluetip').hide();

	if (clog.currentUserPermissions.postCreate) {
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

        var templateData = {
                onGateway: clog.onGateway,
                siteId: clog.siteId,
                showBody: clog.settings.showBody
            };

        clog.utils.renderTemplate('all_posts', templateData, 'clog_content');

        // renderPageOfPosts uses this. Set it to the start page
        clog.page = 0;
        clog.postsRendered = 0;

        clog.currentPosts = [];

        clog.utils.renderPageOfPosts();
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
        });
	} else if (clog.states.GROUP_POSTS === state) {
        if (!arg || !arg.groupId) return;

        clog.currentGroupId = arg.groupId;
        clog.currentGroupTitle = arg.groupTitle;

        var templateData = {
                groupId: arg.groupId,
                groupTitle: arg.groupTitle,
                showRSS: true,
                siteId: clog.siteId,
                showBody: clog.settings.showBody
            };

        clog.utils.renderTemplate('group_posts', templateData, 'clog_content');

        // renderPageOfPosts uses this. Set it to the start page
        clog.page = 0;
        clog.postsRendered = 0;

        clog.currentPosts = [];

        clog.utils.renderPageOfPosts({groupId: arg.groupId});
	} else if ('viewMembers' === state) {
	    $('#clog_toolbar > li > span').removeClass('current');
	    $('#clog_view_authors_link > span').addClass('current');
		if (clog.currentUserPermissions.postCreate) {
			$("#clog_create_post_link").show();
        } else {
			$("#clog_create_post_link").hide();
        }

        clog.utils.renderTemplate('authors_wrapper', {onGateway: clog.onGateway, siteId: clog.siteId}, 'clog_content');

        $(document).ready(function () {

            $('#clog-sortbyname').click(function (e) {

                if (clog.sortByName === clog.SORT_NAME_DOWN) {
                    clog.sortByName = clog.SORT_NAME_UP;
                } else {
                    clog.sortByName = clog.SORT_NAME_DOWN;
                }

                clog.page = 0;
                clog.postsRendered = 0;
                clog.utils.renderPageOfMembers({ sort: clog.sortByName });
            });
            $('#clog-sortbynumberposts').click(function (e) {
                if (clog.sortByPosts === clog.SORT_POSTS_DOWN) {
                    clog.sortByPosts = clog.SORT_POSTS_UP;
                } else {
                    clog.sortByPosts = clog.SORT_POSTS_DOWN;
                }

                clog.page = 0;
                clog.postsRendered = 0;
                clog.utils.renderPageOfMembers({ sort: clog.sortByPosts });
            });
            $('#clog-sortbylastpost').click(function (e) {
                if (clog.sortByLastPost === clog.SORT_LAST_POST_DOWN) {
                    clog.sortByLastPost = clog.SORT_LAST_POST_UP;
                } else {
                    clog.sortByLastPost = clog.SORT_LAST_POST_DOWN;
                }

                clog.page = 0;
                clog.postsRendered = 0;
                clog.utils.renderPageOfMembers({ sort: clog.sortByLastPost });
            });
            $('#clog-sortbynumbercomments').click(function (e) {
                if (clog.sortByComments === clog.SORT_COMMENTS_DOWN) {
                    clog.sortByComments = clog.SORT_COMMENTS_UP;
                } else {
                    clog.sortByComments = clog.SORT_COMMENTS_DOWN;
                }

                clog.page = 0;
                clog.postsRendered = 0;
                clog.utils.renderPageOfMembers({ sort: clog.sortByComments });
            });
        });

        // renderPageOfMembers uses this. Set it to the start page
        clog.page = 0;
        clog.postsRendered = 0;

        clog.currentPosts = [];

        clog.utils.renderPageOfMembers();
	} else if ('userPosts' === state) {

	    $('#clog_toolbar > li > span').removeClass('current');
	    $('#clog_my_clog_link > span').addClass('current');

		// Default to using the current session user id ...
		var userId = clog.userId;
		
		// ... but override it with any supplied one
		if (arg && arg.userId) {
			userId = arg.userId;
        }

        if (arg && !arg.userDisplayName) {
            arg.userDisplayName = userId;
        }

        var templateData = {
                userId: userId,
                userDisplayName: arg.userDisplayName,
                showRSS: (userId !== clog.userId && !clog.onGateway),
                onGateway: clog.onGateway,
                siteId: clog.siteId,
                showBody: clog.settings.showBody
            };

        clog.utils.renderTemplate('all_user_posts', templateData, 'clog_content');

        // renderPageOfPosts uses this. Set it to the start page
        clog.page = 0;
        clog.postsRendered = 0;

        clog.currentPosts = [];

        clog.utils.renderPageOfPosts({userId: userId});
	} else if ('myPublicPosts' === state) {

		$('#clog_toolbar > li > span').removeClass('current');
		$('#clog_my_public_posts_link > span').addClass('current');
		// Default to using the current session user id ...
		var userId = clog.userId;

        clog.utils.renderTemplate('all_user_posts', {'creatorId': userId}, 'clog_content');

        // renderPageOfPosts uses this. Set it to the start page
        clog.page = 0;
        clog.postsRendered = 0;

        clog.currentPosts = [];

        clog.utils.renderPageOfPosts({userId: userId, isPublic: true});
	} else if ('post' === state) {

		var postCallback = function (post) {

                clog.currentPost = post;

                if (!clog.currentPost) {
			        return false;
                }
			
                clog.utils.addFormattedDatesToCurrentPost();

                var cp = clog.currentPost;
                        
                clog.utils.renderTemplate('post_page', cp, 'clog_content');

                clog.utils.renderPost(cp, 'post_' + clog.currentPost.id);

                $(document).ready(function () {

                    $('#clog_user_posts_link').click(function (e) {
                        clog.switchState('userPosts',{'userId' : clog.currentPost.creatorId, userDisplayName: clog.currentPost.creatorDisplayName});
                    });

                    $('.content').show();

                    if (clog.currentPost.comments.length > 0) $('.comments').show();
                });
            };

		if (arg && arg.postId) {
			clog.utils.findPost(arg.postId, postCallback, arg.fromSamepage);
		}
	} else if ('createPost' === state) {
	    $('#clog_toolbar > li > span').removeClass('current');
	    $('#clog_create_post_link > span').addClass('current');

        var createPostCallback = function (post) {

                clog.currentPost = post;
                if (clog.currentPost.autosavedVersion) {
                    if (confirm(clog.i18n.autosaved_copy_question)) {
                        clog.currentPost = clog.currentPost.autosavedVersion;
                    }
                }

                var templateData = {
                        post: clog.currentPost,
                        onMyWorkspace: clog.onMyWorkspace,
                        publicAllowed: clog.publicAllowed,
                        groups: clog.groups,
                        hasGroups: clog.groups.length > 0
                    };

                clog.utils.renderTemplate('create_post', templateData, 'clog_content');

                $(document).ready(function () {


                    $('#clog_title_field').bind('keypress', function (e) {
                        clog.titleChanged = true;	 		
                    });

                    $('#clog_visibility_maintainer,#clog_visibility_site').click(function (e) {

                        $('#clog-group-fieldset').hide();
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
                        return false;
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
            };

		if (arg && arg.postId) {
			clog.utils.findPost(arg.postId, createPostCallback);
		} else {
			createPostCallback({ id: '', title: '', content: '', commentable: true, groups: [] });
        }
	} else if ('createComment' === state) {
		if (!arg || !arg.postId) {
			return;
        }

        var commentCallback = function (post) {

		        clog.currentPost = post;

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
                        return false;
                    });
                    
                    clog.sakai.setupWysiwygEditor(clog.editor,'clog_content_editor',600,400);
                });
            };

		clog.utils.findPost(arg.postId, commentCallback, arg.fromSamepage);
	} else if ('permissions' === state) {
	    $('#clog_toolbar > li > span').removeClass('current');
	    $('#clog_permissions_link > span').addClass('current');

        var permissionsCallback = function (perms) {

                clog.utils.renderTemplate('permissions', {'perms': perms}, 'clog_content');

                $(document).ready(function () {
                    $('#clog_permissions_save_button').bind('click', clog.utils.savePermissions);
                });
            };

		clog.utils.getSitePermissionMatrix(permissionsCallback);
	} else if ('viewRecycled' === state) {
	    $('#clog_toolbar >  li > span').removeClass('current');
	    $('#clog_recycle_bin_link > span').addClass('current');
		jQuery.ajax( {
	       	url: "/direct/clog-post/posts.json?siteId=" + clog.siteId + "&visibilities=RECYCLED",
	       	dataType: "json",
			cache: false,
            timeout: clog.AJAX_TIMEOUT,
		   	success: function (data) {

				var posts = data.posts;

				clogRecycledPosts = posts;
                clog.utils.addFormattedDatesToPosts(clogRecycledPosts);
	 			
				clog.utils.renderTemplate('recycled_posts', {'posts': posts}, 'clog_content');
	 			posts.forEach(function (p) {
					clog.utils.renderPost(p, 'post_' + p.id);
                });

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
		$('.clog-body').hide();
		$('.postOptionsPanel').hide();
        // CLOG-59
        this.setLocalStorageSetting('showBody', false);
    } else {
		$('.clog-body').show();
		$('.postOptionsPanel').show();
        // CLOG-59
        this.setLocalStorageSetting('showBody', true);
    }
};

(function ($) {

    if (typeof localStorage !== 'undefined') {
        clog.settings = JSON.parse(localStorage.getItem(clog.LOCAL_STORAGE_KEY)) || {};
    }

	if (!clog.placementId || !clog.siteId) {
		alert('The placement id and site id MUST be supplied as page parameters');
		return;
	}

    var languagesLoaded = function () {

        clog.i18n = portal.i18n.translations["clog"];

        clog.i18n.months = clog.i18n.months.split(',');

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
        if ($(document).width() < 800) {
            clog.utils.renderTemplate('pda_toolbar',{},'clog_toolbar');
            $(document).ready(function () {

                $('#clog-toolbar-dropdown').change(function () {
                    clog.switchState(this.value);
                });
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
                return clog.switchState('userPosts', {userDisplayName: clog.userDisplayName});
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

        if (!clog.onGateway) {

            var permissionsCallback = function (permissions) {

                    clog.currentUserPermissions = new ClogPermissions(permissions);

                    if (clog.currentUserPermissions == null) {
                        return;
                    }

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
                    }

                    // Now switch into the requested state
                    clog.switchState(clog.state, clog);
                };

            clog.utils.getCurrentUserPermissions(permissionsCallback);
        } else {
            $("#clog_permissions_link").hide();
            $("#clog_my_clog_link").hide();
            $("#clog_home_link").hide();
            $('#clog_recycle_bin_link').hide();
            clog.currentUserPermissions = new ClogPermissions();

            // Now switch into the requested state
            clog.switchState(clog.state, clog);
        }
    };

    $(function () {

        portal.i18n.loadProperties({
            resourceClass: 'org.sakaiproject.clog.api.ClogManager',
            resourceBundle: 'org.sakaiproject.clog.bundle.ui',
            namespace: 'clog',
            callback: function () {
              languagesLoaded();
            }
        });
    });
})(jQuery);
