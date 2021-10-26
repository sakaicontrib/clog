package org.sakaiproject.clog.tool;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.sakaiproject.clog.api.ClogManager;
import org.sakaiproject.clog.api.SakaiProxy;
import org.sakaiproject.component.api.ComponentManager;
import org.sakaiproject.search.api.InvalidSearchQueryException;
import org.sakaiproject.search.api.SearchResult;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.Tool;
import org.sakaiproject.util.RequestFilter;
import org.sakaiproject.util.ResourceLoader;

/**
 * @author Adrian Fish (a.fish@lancaster.ac.uk)
 */
public class ClogTool extends HttpServlet {

    private Logger logger = Logger.getLogger(getClass());

    private SakaiProxy sakaiProxy;
    private ClogManager clogManager;
    
    public void init(ServletConfig config) throws ServletException {

        super.init(config);

        logger.debug("init");
        
        try {
            ComponentManager componentManager = org.sakaiproject.component.cover.ComponentManager.getInstance();
            sakaiProxy = (SakaiProxy) componentManager.get(SakaiProxy.class);
            clogManager = (ClogManager) componentManager.get(ClogManager.class);
        } catch (Throwable t) {
            throw new ServletException("Failed to initialise ClogTool servlet.", t);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        
        logger.debug("doGet()");

        if (sakaiProxy == null) {
            throw new ServletException("sakaiProxy MUST be initialised.");
        }
        
        String siteId = sakaiProxy.getCurrentSiteId();
        
        String userId = null;
        Session session = (Session) request.getAttribute(RequestFilter.ATTR_SESSION);
        if(session != null) {
            userId = session.getUserId();
        } else {
            if (!"!gateway".equals(siteId)) {
                // We are not logged in
                throw new ServletException("Not logged in.");
            }
        }

        // If we're on the gateway show the authors view by default
        String state = ("!gateway".equals(siteId)) ? "viewMembers" : "viewAllPosts";

        String siteLanguage = sakaiProxy.getCurrentSiteLocale();

        Locale locale = null;
        ResourceLoader rl = null;

        if (siteLanguage != null) {
            String[] parts = siteLanguage.split("_");
            if (parts.length == 1) {
                locale = new Locale(parts[0]);
            } else if (parts.length == 2) {
                locale = new Locale(parts[0], parts[1]);
            } else if (parts.length == 3) {
                locale = new Locale(parts[0], parts[1], parts[2]);
            }
            rl = new ResourceLoader("org.sakaiproject.clog");
            rl.setContextLocale(locale);
        } else {
            rl = new ResourceLoader(userId, "org.sakaiproject.clog");
            locale = rl.getLocale();
        }

        if (locale == null || rl == null) {
            logger.error("Failed to load the site or user i18n bundle");
        }

        String language = locale.getLanguage();
        String country = locale.getCountry();

        if (country != null && !country.equals("")) {
            language += "_" + country;
        }

        String displayName = sakaiProxy.getDisplayNameForTheUser(userId);

        request.setAttribute("sakaiHtmlHead", (String) request.getAttribute("sakai.html.head"));
        
        request.setAttribute("userId", userId);
        request.setAttribute("userDisplayName", StringEscapeUtils.escapeJavaScript(displayName));
        request.setAttribute("siteId", siteId);
        request.setAttribute("state", state);

        String placementId = (String) request.getAttribute(Tool.PLACEMENT_ID);
        request.setAttribute("placementId", placementId);
        request.setAttribute("editor", sakaiProxy.getWysiwygEditor());
        request.setAttribute("isolanguage", language);
        request.setAttribute("groups", clogManager.getSiteGroupsForCurrentUser(siteId));
        request.setAttribute("publicAllowed", sakaiProxy.isPublicAllowed() ? "true":"false");

        String postId = request.getParameter("postId");
        if (postId != null) {
            request.setAttribute("state", "post");
            request.setAttribute("postId", postId);
        }

        response.setContentType("text/html");
        request.getRequestDispatcher("/WEB-INF/bootstrap.jsp").include(request, response);  
    }
}
