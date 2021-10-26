<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@taglib prefix="util" uri="http://example.com/functions" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html  
      xmlns="http://www.w3.org/1999/xhtml"  
      xml:lang="${isolanguage}"
      lang="${isolanguage}">
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <script>

            var clog = {
                userId:'${userId}',
                userDisplayName:'${userDisplayName}',
                siteId:'${siteId}',
                placementId:'${placementId}',
                state:'${state}',
                editor:'${editor}',
                publicAllowed:'${publicAllowed}',
                postId:'${postId}',
                onPDAPortal:'${onPDAPortal}',
                i18n: {},
                groups: [
                            <c:forEach items="${groups}" var="i" varStatus="is">{id: '${i.id}', title: '${util:escapeJS(i.title)}', totalPosts: ${i.totalPosts}, lastPostDate: ${i.lastPostDate}}<c:if test="${not is.last}">,</c:if></c:forEach>
                ]
            };
        
        </script>
        ${sakaiHtmlHead}
        <link rel="stylesheet" type="text/css" href="/library/webjars/jquery-ui/1.12.1/jquery-ui.css" media="all"/>
        <link rel="stylesheet" type="text/css" href="/library/webjars/fontawesome/4.7.0/css/font-awesome.min.css" media="all"/>
        <link href="/profile2-tool/css/profile2-profile-entity.css" type="text/css" rel="stylesheet" media="all" />
        <link rel="stylesheet" type="text/css" href="/clog-tool/css/clog.css"  media="all"/>
        <script type="text/javascript" src="/library/webjars/jquery/1.12.4/jquery.min.js"></script>
        <script type="text/javascript" src="/clog-tool/lib/jquery.tablesorter.min.js"></script>
        <script type="text/javascript" src="/clog-tool/lib/jquery.tablesorter.pager.min.js"></script>
        <script type="text/javascript" src="/library/webjars/handlebars/4.0.6/handlebars.runtime.min.js"></script>
        <script type="text/javascript" src="/clog-tool/templates/templates.js"></script>
        <script type="text/javascript" src="/clog-tool/js/clog_utils.js"></script>
        <script type="text/javascript" src="/clog-tool/lib/sakai_utils.js"></script>
        <script type="text/javascript" src="/clog-tool/js/clog_permissions.js"></script>
        <script type="text/javascript" src="/profile2-tool/javascript/profile2-eb.js"></script>
    </head>
    <body>

        <div class="portletBody">

            <ul id="clog_toolbar" class="navIntraTool actionToolBar" role="menu"></ul>

            <div id="clogMainContainer">
                <div id="clog_content"></div>
            </div>

        </div> <!-- /portletBody-->

        <script type="module">
            import {loadClog} from "/clog-tool/js/clog.js${portalCDNQuery}";
            loadClog();
        </script>

    </body>
</html>
