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
                onPDAPortal:'${onPDAPortal}',
                i18n: {
                    <c:forEach items="${i18n}" var="i">${i.key}: "${i.value}",</c:forEach>
                    months: [<c:forEach items="${months}" var="m" varStatus="ms">'${m}'<c:if test="${not ms.last}">,</c:if></c:forEach>]
                },
                groups: [
                            <c:forEach items="${groups}" var="i" varStatus="is">{id: '${i.id}', title: '${i.title}', totalPosts: ${i.totalPosts}, lastPostDate: ${i.lastPostDate}}<c:if test="${not is.last}">,</c:if></c:forEach>
                ]
            };
        
        </script>
        ${sakaiHtmlHead}
        <link rel="stylesheet" type="text/css" href="/library/js/jquery/jquery-ui/css/smoothness/jquery-ui.css" media="all"/>
        <link href="/profile2-tool/css/profile2-profile-entity.css" type="text/css" rel="stylesheet" media="all" />
        <link rel="stylesheet" type="text/css" href="/clog-tool/css/clog.css"  media="all"/>
        <script type="text/javascript" src="/library/js/jquery/jquery-1.9.1.min.js"></script>
        <script type="text/javascript" src="/library/js/jquery/ui/1.10.3/jquery-ui.1.10.3.full.min.js"></script>
        <script type="text/javascript" src="/library/js/jquery/hoverIntent/r7/jquery.hoverIntent.minified.js"></script>
        <script type="text/javascript" src="/library/js/jquery/cluetip/1.2.10/jquery.cluetip.min.js"></script>
        <script type="text/javascript" src="/clog-tool/lib/jquery.tablesorter.min.js"></script>
        <script type="text/javascript" src="/clog-tool/lib/jquery.tablesorter.pager.min.js"></script>
        <script type="text/javascript" src="/clog-tool/lib/handlebars.runtime-v2.0.0.js"></script>
        <script type="text/javascript" src="/clog-tool/templates/templates.js"></script>
        <script type="text/javascript" src="/clog-tool/lib/sakai_utils.js"></script>
        <script type="text/javascript" src="/clog-tool/js/clog_utils.js"></script>
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

        <script type="text/javascript" src="/clog-tool/js/clog.js"></script>

    </body>
</html>
