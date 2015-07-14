package org.sakaiproject.clog.dashboard;

import org.sakaiproject.clog.api.ClogManager;
import org.sakaiproject.dash.listener.EventProcessor;
import org.sakaiproject.dash.logic.DashboardLogic;

import lombok.Setter;

/**
 * Base class so we can get the dependencies in
 * 
 * @author Adrian Fish (adrian.r.fish@gmail.com)
 */
@Setter
public abstract class ClogDashboardEventProcessor implements EventProcessor{
    
    protected ClogManager clogManager;
    protected DashboardLogic dashboardLogic;
}
