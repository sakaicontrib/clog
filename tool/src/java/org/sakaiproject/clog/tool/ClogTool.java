package org.sakaiproject.clog.tool;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.sakaiproject.clog.api.SakaiProxy;
import org.sakaiproject.component.api.ComponentManager;
import org.sakaiproject.search.api.InvalidSearchQueryException;
import org.sakaiproject.search.api.SearchResult;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.Tool;
import org.sakaiproject.util.RequestFilter;
import org.sakaiproject.util.ResourceLoader;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

/**
 * @author Adrian Fish (a.fish@lancaster.ac.uk)
 */
public class ClogTool extends HttpServlet {

	private Logger logger = Logger.getLogger(getClass());

	private SakaiProxy sakaiProxy;
	
	private Template bootstrapTemplate = null;

	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		if (logger.isDebugEnabled()) {
			logger.debug("init");
		}

		
		try {
			ComponentManager componentManager = org.sakaiproject.component.cover.ComponentManager.getInstance();
			sakaiProxy = (SakaiProxy) componentManager.get(SakaiProxy.class);
			VelocityEngine ve = new VelocityEngine();
            Properties props = new Properties();
            props.setProperty("file.resource.loader.path",config.getServletContext().getRealPath("/WEB-INF"));
            ve.init(props);
            bootstrapTemplate = ve.getTemplate("bootstrap.vm");
		} catch (Throwable t) {
			throw new ServletException("Failed to initialise ClogTool servlet.", t);
		}
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		if (logger.isDebugEnabled()) {
			logger.debug("doGet()");
		}

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
		
		String placementId = (String) request.getAttribute(Tool.PLACEMENT_ID);
		
		String sakaiHtmlHead = (String) request.getAttribute("sakai.html.head");

		String state = request.getParameter("state");
		String postId = request.getParameter("postId");

		if (state == null) {
			// If we're on the gateway show the authors view by default
			if ("!gateway".equals(siteId)) {
				state = "viewMembers";
			} else {
				state = "viewAllPosts";
			}
		}
		
		Locale locale = (new ResourceLoader(userId)).getLocale();
		String isoLanguage = locale.getLanguage();
		String country = locale.getCountry();
		
        if(country != null && !country.equals("")) {
            isoLanguage += "_" + country;
        }
        
        System.out.println("ISO LANGUAGE:" + isoLanguage);
		
		VelocityContext ctx = new VelocityContext();
		
		// This is needed so certain trimpath variables don't get parsed.
		ctx.put("D", "$");
       
		ctx.put("sakaiHtmlHead",sakaiHtmlHead);
		
	    ctx.put("userId",userId);
	    ctx.put("siteId",siteId);
	    ctx.put("state",state);
	    ctx.put("placementId",placementId);
	    ctx.put("editor",sakaiProxy.getWysiwygEditor());
	    ctx.put("isolanguage",isoLanguage);
	    ctx.put("publicAllowed",sakaiProxy.isPublicAllowed() ? "true":"false");

		if (postId != null) {
			ctx.put("postId",postId);
		}

		String pathInfo = request.getPathInfo();

		if (pathInfo == null || pathInfo.length() < 1) {
			String uri = request.getRequestURI();

			// There's no path info, so this is the initial state
			if (uri.contains("/portal/pda/")) {
				ctx.put("onPDAPortal","true");
			}
			
	        response.setStatus(HttpServletResponse.SC_OK);
	        response.setContentType("text/html");
	        Writer writer = new BufferedWriter(response.getWriter());
	        try {
	        	bootstrapTemplate.merge(ctx,writer);
			} catch (Exception e) {
				logger.error("Failed to merge template. Returning 500.",e);
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
	        writer.close();
		} else {
			String[] parts = pathInfo.substring(1).split("/");

			if (parts.length >= 1) {
				String part1 = parts[0];

				if ("perms.json".equals(part1)) {
					doPermsGet(response);
				}

				else if ("userPerms.json".equals(part1)) {
					doUserPermsGet(response);
				}
			}
		}
	}

	private void doUserPermsGet(HttpServletResponse response) throws ServletException, IOException {
		Set<String> perms = sakaiProxy.getPermissionsForCurrentUserAndSite();
		JSONArray data = JSONArray.fromObject(perms);
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType("application/json");
		response.getWriter().write(data.toString());
		response.getWriter().close();
		return;

	}

	private void doPermsGet(HttpServletResponse response) throws ServletException, IOException {
		Map<String, Set<String>> perms = sakaiProxy.getPermsForCurrentSite();
		JSONObject data = JSONObject.fromObject(perms);
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType("application/json");
		response.getWriter().write(data.toString());
		response.getWriter().close();
		return;
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		logger.info("doPost()");

		String pathInfo = request.getPathInfo();

		String[] parts = new String[] {};

		if (pathInfo != null)
			parts = pathInfo.substring(1).split("/");

		if (parts.length >= 1) {
			String part1 = parts[0];

			if ("search".equals(part1))
				doSearchPost(request, response);
			else if ("setPerms".equals(part1))
				doPermsPost(request, response);
		}
	}

	private void doPermsPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (sakaiProxy.setPermsForCurrentSite(request.getParameterMap())) {
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType("text/plain");
			response.getWriter().write("success");
			response.getWriter().close();
			return;
		} else {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
	}

	private void doSearchPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String searchTerms = request.getParameter("searchTerms");

		if (searchTerms == null || searchTerms.length() == 0)
			throw new ServletException("No search terms supplied.");

		try {
			List<SearchResult> results = sakaiProxy.searchInCurrentSite(searchTerms);

			JSONArray data = JSONArray.fromObject(results);
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType("application/json");
			response.getWriter().write(data.toString());
			response.getWriter().close();
			return;
		} catch (InvalidSearchQueryException isqe) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().write("Your search terms were invalid");
			response.getWriter().close();
			return;
		}
	}
}
