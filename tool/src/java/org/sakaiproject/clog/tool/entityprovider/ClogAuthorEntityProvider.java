package org.sakaiproject.clog.tool.entityprovider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.sakaiproject.clog.api.ClogManager;
import org.sakaiproject.clog.api.ClogMember;
import org.sakaiproject.clog.api.datamodel.AuthorsData;
import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.entitybroker.EntityView;
import org.sakaiproject.entitybroker.entityprovider.CoreEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.annotations.EntityCustomAction;
import org.sakaiproject.entitybroker.entityprovider.capabilities.ActionsExecutable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.AutoRegisterEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Describeable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Outputable;
import org.sakaiproject.entitybroker.entityprovider.extension.Formats;
import org.sakaiproject.entitybroker.entityprovider.search.Restriction;
import org.sakaiproject.entitybroker.entityprovider.search.Search;
import org.sakaiproject.entitybroker.exception.EntityException;
import org.sakaiproject.entitybroker.util.AbstractEntityProvider;
import org.sakaiproject.user.api.UserDirectoryService;

@Slf4j
@Setter
public class ClogAuthorEntityProvider extends AbstractEntityProvider implements CoreEntityProvider, AutoRegisterEntityProvider, Outputable, Describeable, ActionsExecutable {
    
    public final static String ENTITY_PREFIX = "clog-author";

    private ClogManager clogManager;
    private UserDirectoryService userDirectoryService;

    public boolean entityExists(String id) {
        log.debug("entityExists({})", id);

        if (id == null) {
            return false;
        }

        if ("".equals(id)) {
            return false;
        }

        try {
            userDirectoryService.getUser(id);
            return true;
        } catch (Exception e) {
            log.error("Caught exception whilst getting user.", e);
            return false;
        }
    }

    /**
     * No intention of implementing this. Forced to due to the fact that
     * CollectionsResolvable extends Resolvable
     */
    public Object getEntity(EntityReference ref) {
        log.debug("getEntity({})", ref.getId());
        log.warn("getEntity is unimplemented. Returning null ...");

        return null;
    }

    public Object getSampleEntity() {
        return new ClogMember();
    }

    public String getEntityPrefix() {
        return ENTITY_PREFIX;
    }

    public String[] getHandledOutputFormats() {
        return new String[] { Formats.JSON };
    }

    @EntityCustomAction(action = "authors", viewKey = EntityView.VIEW_LIST)
    public AuthorsData handleAuthors(EntityReference ref, Map<String, Object> params) {

        String siteId = (String) params.get("siteId");

        if (siteId == null) {
            throw new EntityException("No site id supplied.", "", HttpServletResponse.SC_BAD_REQUEST);
        }

        int page = 0;
        String pageString = (String) params.get("page");
        if (pageString != null) {
            try {
                page = Integer.parseInt(pageString);
            } catch (NumberFormatException nfe) {
                log.error("Invalid page number {} supplied. The first page will be returned ...", pageString);
                throw new EntityException("Invalid page value. Needs to be an integer.", ""
                                                                    , HttpServletResponse.SC_BAD_REQUEST);
            }
        }

        String sort = (String) params.get("sort");

        try {
            List<ClogMember> authors = clogManager.getAuthors(siteId, sort);
            int pageSize = 50;
            int start  = page * pageSize;
            int authorsTotal = authors.size();

            if (start >= authorsTotal) {
                AuthorsData data = new AuthorsData();
                data.status = "END";
                data.siteId = siteId;
                return data;
            } else {
                int end = start + pageSize;

                log.debug("end: {}", end);

                AuthorsData data = new AuthorsData();
                data.authorsTotal = authorsTotal;

                if (end >= authorsTotal) {
                    end = authorsTotal;
                    data.status = "END";
                }

                data.authors = authors.subList(start, end);
                data.siteId = siteId;
                return data;
            }

        } catch (Exception e) {
            log.error("Caught exception whilst getting authors.", e);
            return null;
        }
    }
}
