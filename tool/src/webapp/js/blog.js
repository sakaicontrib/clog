/* Stuff that we always expect to be setup */
var blogSiteId = null;
var blogCurrentUserPermissions = null;
var blogCurrentUserPreferences = null;
var blogCurrentPost = null;
var blogCurrentPosts = null;
var blogCurrentUser = null;
var blogHomeState = null;
var blogOnMyWorkspace = false;

(function()
{
	// We need the toolbar in a template so we can swap in the translations
	BlogUtils.render('blog_toolbar_template',{},'blog_toolbar');

	$('#blog_home_link').bind('click',function(e) {
		return switchState('home');
	});

	$('#blog_view_authors_link').bind('click',function(e) {
		return switchState('viewMembers');
	});

	$('#blog_my_blog_link').bind('click',function(e) {
		return switchState('userPosts');
	});

	$('#blog_create_post_link').bind('click',function(e) {
		return switchState('createPost');
	});

	$('#blog_permissions_link').bind('click',function(e) {
		return switchState('permissions');
	});
	
	$('#blog_preferences_link').bind('click',function(e) {
		return switchState('preferences');
	});

	$('#blog_recycle_bin_link').bind('click',function(e) {
		return switchState('viewRecycled');
	});
	
	var arg = BlogUtils.getParameters();
	
	if(!arg || !arg.siteId) {
		alert('The site id  MUST be supplied as a page parameter');
		return;
	}

	blogHomeState = 'viewAllPosts';
	
	blogSiteId = arg.siteId;

	if(blogSiteId.match(/^~/)) blogOnMyWorkspace = true;

	// If we are on a My Workspace type site (characterised by a tilde as the
	// first character in the site id), show the user's posts by default.
	if(blogOnMyWorkspace) {
		arg.state = 'userPosts';
		blogHomeState = 'userPosts';
		$("#blog_view_authors_link").hide();
		$("#blog_my_blog_link").hide();
	}

	blogCurrentUser = BlogUtils.getCurrentUser();
	
	if(!blogCurrentUser) {
		alert("No current user. Have you logged in?");
		return;
	}

	blogCurrentUserPreferences = BlogUtils.getPreferences();
	
	blogCurrentUserPermissions = new BlogPermissions(BlogUtils.getCurrentUserPermissions().data);
	
	if(blogCurrentUserPermissions == null) return;
	
	// Site maintainers are the only ones who can change permissions
	if(blogCurrentUser.membership.memberRole === 'maintain') {
		$("#blog_permissions_link").show();
		$("#blog_recycle_bin_link").show();
	}
	else {
		$("#blog_permissions_link").hide();
		$("#blog_recycle_bin_link").hide();
	}

	if(window.frameElement)
		window.frameElement.style.minHeight = '600px';
	
	// Now switch into the requested state
	switchState(arg.state,arg);
})();

function switchState(state,arg) {
	$('#cluetip').hide();

	if(blogCurrentUserPermissions.postCreate)
		$("#blog_create_post_link").show();
	else
		$("#blog_create_post_link").hide();
	
	if('home' === state) {
		switchState(blogHomeState,arg);
	}
	if('viewAllPosts' === state) {

		BlogUtils.setPostsForCurrentSite();
			
		if(window.frameElement) {
	 		$(document).ready(function() {
	 			setMainFrameHeight(window.frameElement.id);
	 		});
		}
	 			
		BlogUtils.render('blog_all_posts_template',{'posts':blogCurrentPosts},'blog_content');
		for(var i=0,j=blogCurrentPosts.length;i<j;i++)
			BlogUtils.render('blog_post_template',blogCurrentPosts[i],'post_' + blogCurrentPosts[i].id);
	}
	else if('viewMembers' === state) {
		if(blogCurrentUserPermissions.postCreate)
			$("#blog_create_post_link").show();
		else
			$("#blog_create_post_link").hide();

		jQuery.ajax({
	    	url : "/direct/blog-author.json?siteId=" + blogSiteId,
	      	dataType : "json",
	       	async : false,
			cache: false,
		   	success : function(data) {
				BlogUtils.render('blog_authors_content_template',{'authors':data['blog-author_collection']},'blog_content');

 				$(document).ready(function() {
					$('a.showPostsLink').cluetip({
						width: '620px',
						cluetipClass: 'blog',
 						dropShadow: false,
						arrows: true,
						showTitle: false
						});
  									
  					$("#blog_author_table").tablesorter({
							widgets: ['zebra'],
	 						cssHeader:'blogSortableTableHeader',
	 						cssAsc:'blogSortableTableHeaderSortUp',
	 						cssDesc:'blogSortableTableHeaderSortDown',
							textExtraction: 'complex',	
							sortList: [[0,0]],
	 						headers:
	 						{
	 							2: {sorter: "isoDate"},
	 							3: {sorter: "isoDate"}
	 						} }).tablesorterPager({container: $("#blogBloggerPager"),positionFixed: false});
	 						
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
		var userId = blogCurrentUser.id;
		
		// ... but override it with any supplied one
		if(arg && arg.userId)
			userId = arg.userId;

		var url = "/direct/blog-post.json?siteId=" + blogSiteId + "&creatorId=" + userId;

		if(blogOnMyWorkspace) url += "&visibilities=PRIVATE,PUBLIC";

		jQuery.ajax( {
	       	'url' : url,
	       	dataType : "json",
	       	async : false,
			cache: false,
		   	success : function(data) {

				var profileMarkup = BlogUtils.getProfileMarkup(userId);

				var posts = data['blog-post_collection'];
	 			
				BlogUtils.render('blog_user_posts_template',{'creatorId':userId,'posts':posts},'blog_content');
				$('#blog_author_profile').html(profileMarkup);
	 			for(var i=0,j=posts.length;i<j;i++)
					BlogUtils.render('blog_post_template',posts[i],'post_' + posts[i].id);

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
			blogCurrentPost = BlogUtils.findPost(arg.postId);

		if(!blogCurrentPost)
			return false;
	 			
		BlogUtils.render('blog_post_page_content_template',blogCurrentPost,'blog_content');
		BlogUtils.render('blog_post_template',blogCurrentPost,'post_' + blogCurrentPost.id);

	 	$(document).ready(function() {
			$('#blog_user_posts_link').bind('click',function(e) {
				switchState('userPosts',{'userId' : blogCurrentPost.creatorId});
			});

			$('.content').show();

			if(blogCurrentPost.comments.length > 0) $('.comments').show();

	 		if(window.frameElement)
	 			setMainFrameHeight(window.frameElement.id);
	 	});
	}
	else if('createPost' === state) {
		var post = {id:'',title:'',content:'',commentable:true};

		if(arg && arg.postId)
			post = BlogUtils.findPost(arg.postId);

		BlogUtils.render('blog_create_post_template',post,'blog_content');
		
		BlogUtils.setupEditor('blog_content_editor',600,400,'Default',blogSiteId);

	 	$(document).ready(function() {
			$('#blog_save_post_button').bind('click',BlogUtils.savePostAsDraft);

			// If this is a My Workspace site, make the post PUBLIC when published.
			if(blogOnMyWorkspace) {
				$('#blog_publish_post_button').bind('click',BlogUtils.publicisePost);
			}
			else
				$('#blog_publish_post_button').bind('click',BlogUtils.publishPost);

			$('#blog_cancel_button').bind('click',function(e) {
				switchState('home');
			});

	 		setMainFrameHeight(window.frameElement.id);
	 	});
	}
	else if('createComment' === state) {
		if(!arg || !arg.postId)
			return;

		blogCurrentPost = BlogUtils.findPost(arg.postId);

		var comment = {id: '',postId: arg.postId,content: ''};

		var currentIndex = -1;

		if(arg.commentId) {
			var comments = blogCurrentPost.comments;

			for(var i=0,j=comments.length;i<j;i++) {
				if(comments[i].id == arg.commentId) {
					comment = comments[i];
					currentIndex = i;
					break;
				}
			}
		}

		BlogUtils.render('blog_create_comment_template',comment,'blog_content');

		$(document).ready(function() {
			BlogUtils.setupEditor('blog_content_editor',600,400,'Default',blogSiteId);
			BlogUtils.render('blog_post_template',blogCurrentPost,'blog_post_' + arg.postId);
			$('#blog_save_comment_button').bind('click',BlogUtils.saveComment);

			if(window.frameElement)
				setMainFrameHeight(window.frameElement.id);
		});
	}
	else if('permissions' === state) {
		var perms = BlogUtils.parsePermissions();
		BlogUtils.render('blog_permissions_content_template',{'perms':perms},'blog_content');

	 	$(document).ready(function() {
			$('#blog_permissions_save_button').bind('click',BlogUtils.savePermissions);

			if(window.frameElement)
				setMainFrameHeight(window.frameElement.id);
		});
	}
	else if('preferences' === state) {
		BlogUtils.render('blog_preferences_template',{},'blog_content');
	 	$(document).ready(function() {
			if('never' === blogCurrentUserPreferences.emailFrequency) {
				$('#blog_email_option_never_checkbox').attr('checked',true);
			}
			else if('each' === blogCurrentUserPreferences.emailFrequency) {
				$('#blog_email_option_each_checkbox').attr('checked','true');
			}
			else if('digest' === blogCurrentUserPreferences.emailFrequency) {
				$('#blog_email_option_digest_checkbox').attr('checked','true');
			}
			$('#blog_preferences_save_button').bind('click',BlogUtils.savePreferences);
			if(window.frameElement)
				setMainFrameHeight(window.frameElement.id);
		});
	}
	else if('viewRecycled' === state) {
		jQuery.ajax( {
	       	url : "/direct/blog-post.json?siteId=" + blogSiteId + "&visibilities=RECYCLED",
	       	dataType : "json",
	       	async : false,
			cache: false,
		   	success : function(data) {

				var posts = data['blog-post_collection'];
	 			
				BlogUtils.render('blog_recycled_posts_template',{'posts':posts},'blog_content');
	 			for(var i=0,j=posts.length;i<j;i++)
					BlogUtils.render('blog_post_template',posts[i],'post_' + posts[i].id);

				$('#blog_really_delete_button').bind('click',BlogUtils.deleteSelectedPosts);
				$('#blog_restore_button').bind('click',BlogUtils.restoreSelectedPosts);

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
}

function toggleFullContent(v)
{
 	if(window.frameElement) {
		$(document).ready(function() {
 			setMainFrameHeight(window.frameElement.id);
		});
}
	
	if(v.checked)
		$('.content').hide();
	else
		$('.content').show();
}

function toggleComments(postId)
{
	var comments = $('#' + postId + '_comments');
	if(comments.css('display') == 'none')
		comments.show();
	else
		comments.hide();

 	if(window.frameElement) {
		$(document).ready(function() {
 			setMainFrameHeight(window.frameElement.id);
		});
	}
}
