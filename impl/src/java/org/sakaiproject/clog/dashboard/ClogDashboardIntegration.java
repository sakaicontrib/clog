/** * Copyright 2009 The Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.opensource.org/licenses/ecl1.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.clog.dashboard;

import org.apache.log4j.Logger;

import org.sakaiproject.clog.api.ClogManager;
import org.sakaiproject.dash.logic.DashboardLogic;
import org.sakaiproject.user.api.UserDirectoryService;

import lombok.Setter;

@Setter
public class ClogDashboardIntegration {

    private final Logger logger = Logger.getLogger(ClogDashboardIntegration.class);

    private ClogManager clogManager;
    private DashboardLogic dashboardLogic;
    private UserDirectoryService userDirectoryService;

    /**
     * Register all the CLOG event processors with the Dashboard 
     */
    public void init() {

        logger.debug("init()");
        
        ClogPostEntityType pet = new ClogPostEntityType();
        pet.setClogManager(clogManager);
        pet.setUserDirectoryService(userDirectoryService);
        dashboardLogic.registerEntityType(pet);

        ClogPostCreatedEventProcessor pceProc = new ClogPostCreatedEventProcessor();
        pceProc.setClogManager(clogManager);
        pceProc.setDashboardLogic(dashboardLogic);
        pceProc.init();
        dashboardLogic.registerEventProcessor(pceProc);
        
        ClogPostRecycledEventProcessor pdeProc = new ClogPostRecycledEventProcessor();
        pdeProc.setClogManager(clogManager);
        pdeProc.setDashboardLogic(dashboardLogic);
        dashboardLogic.registerEventProcessor(pdeProc);
        
        ClogPostRestoredEventProcessor preProc = new ClogPostRestoredEventProcessor();
        preProc.setClogManager(clogManager);
        preProc.setDashboardLogic(dashboardLogic);
        preProc.init();
        dashboardLogic.registerEventProcessor(preProc);
        
        ClogPostWithdrawnEventProcessor pweProc = new ClogPostWithdrawnEventProcessor();
        pweProc.setClogManager(clogManager);
        pweProc.setDashboardLogic(dashboardLogic);
        dashboardLogic.registerEventProcessor(pweProc);
    }
}
