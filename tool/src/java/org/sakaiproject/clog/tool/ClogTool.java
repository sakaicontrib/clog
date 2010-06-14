package org.sakaiproject.clog.tool;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.sakaiproject.blog.api.SakaiProxy;
import org.sakaiproject.component.api.ComponentManager;
import org.sakaiproject.util.ResourceLoader;

/**
 * @author Adrian Fish (a.fish@lancaster.ac.uk)
 */
public class ClogTool extends HttpServlet
{
	private Logger logger = Logger.getLogger(getClass());

	private SakaiProxy sakaiProxy;
	
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);

		if (logger.isDebugEnabled()) logger.debug("init");
		
        ComponentManager componentManager = org.sakaiproject.component.cover.ComponentManager.getInstance();
		sakaiProxy = (SakaiProxy) componentManager.get(SakaiProxy.class);
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		if (logger.isDebugEnabled()) logger.debug("doGet()");
		
		if(sakaiProxy == null)
			throw new ServletException("sakaiProxy MUST be initialised.");
		
		String state = request.getParameter("state");
		String postId = request.getParameter("postId");
		
		if(state == null) state = "viewAllPosts";
		
		if(postId == null) postId = "none";
		
		String userId = sakaiProxy.getCurrentUserId();
		
		if(userId == null)
		{
			// We are not logged in
			throw new ServletException("getCurrentUser returned null.");
		}
		
		String siteId = sakaiProxy.getCurrentSiteId();
		
		// We need to pass the language code to the JQuery code in the pages.
		Locale locale = (new ResourceLoader(userId)).getLocale();
		String languageCode = locale.getLanguage();

		response.sendRedirect("/blog-tool/blog.html?state=" + state + "&siteId=" + siteId + "&postId=" + postId + "&language=" + languageCode);
	}
}
