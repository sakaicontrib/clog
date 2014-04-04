<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html  
      xmlns="http://www.w3.org/1999/xhtml"  
      xml:lang="${isolanguage}"
      lang="${isolanguage}">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<script type="text/javascript">

    var clog = {
    	userId:'${userId}',
    	siteId:'${siteId}',
    	placementId:'${placementId}',
    	state:'${state}',
    	editor:'${editor}',
    	publicAllowed:'${publicAllowed}',
        postId:'${postId}',
        i18n: {
            <c:forEach items="${i18n}" var="i" varStatus="is">
            ${i.key}: "${i.value}"<c:if test="${!is.last}">,</c:if>
            </c:forEach>
        },
    	onPDAPortal:'${onPDAPortal}'
    };
    
</script>
${sakaiHtmlHead}
    <link rel="stylesheet" type="text/css" href="/library/js/jquery/jquery-ui/css/smoothness/jquery-ui.css" media="all"/>
    <link href="/profile2-tool/css/profile2-profile-entity.css" type="text/css" rel="stylesheet" media="all" />
    <link rel="stylesheet" type="text/css" href="/clog-tool/css/clog.css"  media="all"/>
    <script type="text/javascript" src="/clog-tool/lib/jquery.js"></script>
    <script type="text/javascript" src="/clog-tool/lib/jquery-ui.js"></script>
    <script type="text/javascript" src="/clog-tool/lib/handlebars.runtime-v1.3.0.js"></script>
    <script type="text/javascript" src="/clog-tool/templates/all.handlebars"></script>
    <script type="text/javascript" src="/clog-tool/lib/trimpath-template-latest.js"></script>
    <script type="text/javascript" src="/clog-tool/lib/jquery.hoverIntent.js"></script>
    <script type="text/javascript" src="/clog-tool/lib/jquery.cluetip.min.js"></script>
    <script type="text/javascript" src="/clog-tool/lib/jquery.tablesorter.min.js"></script>
    <script type="text/javascript" src="/clog-tool/lib/jquery.tablesorter.pager.js"></script>
    <script type="text/javascript" src="/clog-tool/lib/sakai_utils.js"></script>
    <script type="text/javascript" src="/clog-tool/js/clog_utils.js"></script>
    <script type="text/javascript" src="/clog-tool/clog-translations-en_GB.js"></script>
    <script type="text/javascript" src="/clog-tool/clog-translations-${isolanguage}.js"></script>
    <script type="text/javascript" src="/clog-tool/js/clog_permissions.js"></script>
    <script type="text/javascript" src="/profile2-tool/javascript/profile2-eb.js"></script>
</head>
<body>

<div class="portletBody">

<ul id="clog_toolbar" class="navIntraTool actionToolBar" role="menu"></ul>

<div id="clogMainContainer">
	<div id="clog_content"></div>
</div>

<!--  Templates Start -->

<div id="clog_create_comment_template" style="display:none"><!--
	<div id="clog_post_${postId}" style="width:100%"></div>

	<h3>${clog_add_your_comment_label}:</h3>

	<div style="height: 20px;"></div>

	<textarea name="text" id="clog_content_editor" cols="45" rows="10">${content}</textarea>
	<br />
	<br />
    <input id="clog_comment_id_field" type="hidden" value="${id}"/>
    <input id="clog_save_comment_button" type="button" value="${save}"/>
    <input type="button" value="${cancel}" onclick="switchState('viewAllPosts');"/>
-->
</div>

<div id="clog_authors_content_template" style="display:none"><!--
	<h2>${clog_authors_label}</h2>
	<div id="clogAuthorPager">
	<form>
		<img class="first" src="/library/image/silk/control_start_blue.png"/>
		<img class="prev" src="/library/image/silk/control_rewind_blue.png"/>
		<input class="pagedisplay" type="text"/>
		<img class="next" src="/library/image/silk/control_fastforward_blue.png"/>
		<img class="last" src="/library/image/silk/control_end_blue.png"/>
		<select class="pagesize">
			<option selected="selected"  value="10">10</option>
			<option value="20">20</option>
			<option value="30">30</option>
			<option  value="40">40</option>
		</select>
		
	</form>
	</div>
    <table id="clog_author_table" cols="4" class="listHier speciallistHier">
		<thead>
		<tr>
			<th width="20%"><div>${clog_author_label}</div></th>
			<th width="10%"><div>${posts}</div></th>
			<th width="20%"><div>${dateOfLastPostHeader}</div></th>
			<th width="45%"><div>${clog_number_of_comments_label}</div></th>
			{if !clogOnGateway}<th width="5%"><div>RSS</div></th>{/if}
		</tr>
		</thead>
		<tbody>
		{for m in authors}
		<tr>
			<td><img src="/direct/profile/${m.userId}/image" width="20" alt="User"/>&nbsp;<a href="javascript:;" onclick="switchState('userPosts',{userId:'${m.userId}'});" rel="/direct/profile/${m.userId}/formatted" class="icon profile">${m.userDisplayName}</a></td>
			<td>${m.numberOfPosts}</td>
			<td>{if m.dateOfLastPost > 0}${m.formattedDateOfLastPost}{else}n/a{/if}</td>
			<td>${m.numberOfComments}</td>
			{if !clogOnGateway}<td><a target="_blank" href="/direct/clog-rss/user/${m.userId}.xml?siteId=${startupArgs.siteId}" title="${clog_user_rss_tooltip}"><img src="/library/image/silk/feed.png"/></a></td>{/if}
		</tr>
		{/for}
		</tbody>
	</table>
-->
</div>

<div id="clog_post_page_content_template" style="display:none"><!--
	<a id="clog_user_posts_link" href="javascript:;">${clog_author_posts} ${creatorDisplayName}</a>
	<div id="post_${id}" style="width:100%"></div>
-->
</div>

<div id="clog_post_template" style="display:none"><!--
	<div class="postOuterContainer">
		{if 'RECYCLED' === visibility && !clogCurrentUserPermissions.postDeleteAny}
			<span>POST DELETED</span>
		{else}
		<div class="postInnerContainer">
			<span class="postOptionsPanel">
			{if 'RECYCLED' != visibility}
			{if clogCurrentUserPermissions.commentCreate && commentable}
				<a href="#" title="${clog_comment_on_post_tooltip}" onClick="switchState('createComment',{postId: '${id}'});"><img src="/library/image/silk/comment_add.png" width="16" height="16" alt="Add Comment"/></a>&nbsp;
			{/if}
			{if startupArgs.userId == '!admin' || clogCurrentUserPermissions.postDeleteAny || (clogCurrentUserPermissions.postDeleteOwn && creatorId == startupArgs.userId)}
				<a href="#" title="${clog_delete_post_tooltip}" onclick="return ClogUtils.recyclePost('${id}');"><img src="/library/image/silk/cross.png" width="16" height="16" alt="Delete"/></a>&nbsp;
			{/if}
			{if clogCurrentUserPermissions.postUpdateAny || (clogCurrentUserPermissions.postUpdateOwn && creatorId == startupArgs.userId)}
				<a href="#" onClick="switchState('createPost',{postId:'${id}'});" title="${clog_edit_post_tooltip}"><img src="/library/image/silk/pencil.png" width="16" height="16" alt="Edit"/></a>&nbsp;
			{/if}
			{/if}
			</span>
			<div class="author">
				<div class="clog_author_image_div"><img src="/direct/profile/${creatorId}/image" width="50"/></div>
				<div style="float: right;">
				<a class="profile" rel="/direct/profile/${creatorId}/formatted">${creatorDisplayName}</a><br /><span style="font-size: smaller;font-style: italic;">${clog_created_label}</span><span style="font-size: smaller;font-style: italic;">${formattedCreatedDate}</span>{if modifiedDate > createdDate}<br/><span style="font-size: smaller;font-style: italic;">${clog_modified_label}</span><span style="font-size: smaller;font-style: italic;">${formattedModifiedDate}</span>{/if}
				</div>
			</div>
			<br/>
			<br/>
			<div class="title">
				<span>
					<a href="#" onclick="switchState('post',{'postId':'${id}'});" title="${clog_view_post_tooltip}"><span>${title}</span></a>
				</span>
				{if private}
				<img src="/library/image/silk/key.png" title="${clog_private_post_tooltip}" width="16" height="16"/>
				{/if}
			</div>

            <div class="clog_body">

			    <div class="content">${content}</div>

			    <br />

			    <span id="${id}_comments" class="comments">
					{for comment in comments}
					<div class="clog_comment">
						<div style="overflow:auto; margin-bottom: 10px;">
							<div class="clog_author_image_div"><img src="/direct/profile/${D}{escape(comment.creatorId)}/image" width="50"/></div>
							<div style="float:left;"><a class="profile" rel="/direct/profile/${comment.creatorId}/formatted">${comment.creatorDisplayName}</a><br/><span class="clog_comment_date">${clog_created_label} ${comment.formattedCreatedDate}</span>{if comment.modifiedDate > comment.createdDate}<br/><span class="clog_comment_date">${clog_modified_label} ${comment.formattedModifiedDate}</span>{/if}</div>
						</div>
						<div class="clog_comment_content">${comment.content}</div>
						{if clogCurrentUserPermissions.commentDeleteAny || (clogCurrentUserPermissions.commentDeleteOwn && comment.creatorId == startupArgs.userId)}
							<a href="#" onclick="return ClogUtils.deleteComment('${comment.id}');" title="${clog_delete_comment_tooltip}"><img src="/library/image/silk/cross.png" width="16" height="16" alt="Delete Comment"/></a>&nbsp;
						{/if}
						{if clogCurrentUserPermissions.commentUpdateAny || (clogCurrentUserPermissions.commentUpdateOwn && comment.creatorId == startupArgs.userId)}
							<a href="#" onClick="switchState('createComment',{postId:'${id}',commentId:'${comment.id}'});" title="${clog_edit_comment_tooltip}"><img src="/library/image/silk/pencil.png" width="16" height="16" alt="Edit Comment"/></a>
						{/if}
					</div>
					{/for}
			    </span>
		    </div>
		</div>
		{/if}
	</div>
-->
</div>

<div id="clog_all_posts_template" style="display:none"><!--
	{if ! clogOnGateway}
	<h2>${clog_worksite_posts_label}&nbsp;<a target="_blank" href="/direct/clog-rss/site/${startupArgs.siteId}.xml" title="${clog_user_rss_tooltip}"><img src="/library/image/silk/feed.png"/></a></h2>
	{/if}
	{if posts.length <= 0}
	<div id="noPostsLabel"><span>${noPosts}</span></div>
	{else}
	<div style="clear: left; margin-top: 10px;" id="viewOptions">
   		<input type="checkbox"
                 {if !clogSettings.showBody} checked {/if}
                 onclick="toggleFullContent(this);"/>
		<span>${clog_show_title_only_label}</span>
   	</div>
	{/if}
    <div id="postsWrapper">
    	<br />
  	 	<div id="posts">
	  	    <table class="tableHeader">
				<tbody>
					{for post in posts}
					<tr>
						<td>
							<div id="post_${post.id}"></div>
						</td>
					</tr>
					{/for}
				</tbody>
			</table>
		</div>
	</div>
-->
</div>

<div id="clog_user_posts_template" style="display:none"><!--
	<div id="clog_author_profile"></div>
	{if posts.length <= 0}
	<div id="noPostsLabel"><span>${noPosts}</span></div>
	{else}
	<div style="clear: left; margin-top: 10px;" id="viewOptions">
   		<input type="checkbox"
                 {if !clogSettings.showBody} checked {/if}
                 onclick="toggleFullContent(this);"/>
		<span>${clog_show_title_only_label}</span>{if creatorId != startupArgs.userId && !clogOnGateway}&nbsp;<a target="_blank" href="/direct/clog-rss/user/${creatorId}.xml{if !clogOnGateway}?siteId=${startupArgs.siteId}{/if}" title="${clog_user_rss_tooltip}"><img src="/library/image/silk/feed.png"/></a>{/if}
   	</div>
	{/if}
    <div id="postsWrapper">
    	<br />
  	 	<div id="posts">
	  	    <table class="tableHeader">
				<tbody>
					{for post in posts}
					<tr>
						<td>
							<div id="post_${post.id}"></div>
						</td>
					</tr>
					{/for}
				</tbody>
			</table>
		</div>
	</div>
-->
</div>

<div id="clog_recycled_posts_template" style="display:none"><!--
	<h2>${clog_recycle_bin_label}</h2>
        {if posts.length <= 0}
        <div id="noPostsLabel"><span>${noPosts}</span></div>
        {/if}
    <div id="postsWrapper">
    
    	<input type="button" id="clog_really_delete_button" value="${del}"/>
    	<input type="button" id="clog_restore_button" value="${restore}"/>
  	    
  	 	<div id="posts">
  	    <table class="tableHeader">
		<tbody>
			{for post in posts}
			<tr>
				<td valign="top" width="20px"><input type="checkbox" id="${post.id}" class="clog_recycled_post_checkbox"/></td>
				<td>
					<div id="post_${post.id}"></div>
				</td>
			</tr>
			{/for}
		</tbody>
		</table>
		</div>
	</div>
	</div>
-->
</div>

<div id="clog_posts_print_template" style="display:none"><!--
    <div id="postsWrapper">
    	<br />
  	 	<div id="posts">
	  	    <table class="tableHeader">
				<tbody>
					{for post in posts}
					<tr>
						<td>
							<div id="post_${post.id}" style="width:100%"></div>
						</td>
					</tr>
					{/for}
				</tbody>
			</table>
		</div>
	</div>
-->
</div>

<div id="clog_search_results_template" style="display:none;"><!--
<h2>${searchResults}</h2>
{for result in results}
        <b>${result.tool}:</b><a href="${result.url}">${result.title}</a>
        <br />
        <div class="clog_search_result">${result.searchResult}</div>
        <br />
        <br />
{/for}
-->
</div>

<!--  Templates End -->

</div> <!-- /portletBody-->

    <script type="text/javascript" src="/clog-tool/js/clog.js"></script>

<!--link rel="stylesheet" type="text/css" href="/clog-tool/css/jquery.cluetip.css" /-->


</body>
</HTML>
