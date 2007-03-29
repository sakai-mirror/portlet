/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2005, 2006, 2007 The Sakai Foundation.
 * 
 * Licensed under the Educational Community License, Version 1.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 *      http://www.opensource.org/licenses/ecl1.php
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.portlets;

import java.lang.Integer;

import javax.portlet.GenericPortlet;
import javax.portlet.RenderRequest;
import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.RenderResponse;
import javax.portlet.PortletRequest;
import javax.portlet.PortletException;
import javax.portlet.PortletURL;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletContext;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.PortletConfig;
import javax.portlet.WindowState;
import javax.portlet.PortletMode;
import javax.portlet.ValidatorException;
import javax.portlet.PortletSession;

import java.io.PrintWriter;
import java.io.IOException;

import java.util.Map;
import java.util.Properties;

import org.sakaiproject.tool.cover.ToolManager;
import org.sakaiproject.tool.cover.SessionManager;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.Placement;
import org.sakaiproject.tool.api.ToolSession;

// Hacking

import org.sakaiproject.thread_local.cover.ThreadLocalManager;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * a simple SakaiApiTest Portlet
 */
public class SakaiApiTest extends GenericPortlet {

    private PortletContext pContext;

    public void init(PortletConfig config) throws PortletException {
        super.init(config);

        pContext = config.getPortletContext();
    }

        /** Key in the ThreadLocalManager for binding our remoteUser preference. */
        protected final static String CURRENT_REMOTE_USER = "org.sakaiproject.util.RequestFilter.remote_user";

        /** Key in the ThreadLocalManager for binding our http session preference. */
        protected final static String CURRENT_HTTP_SESSION = "org.sakaiproject.util.RequestFilter.http_session";

        /** Key in the ThreadLocalManager for binding our context id. */
        protected final static String CURRENT_CONTEXT = "org.sakaiproject.util.RequestFilter.context";

        /** Key in the ThreadLocalManager for access to the current http request object. */
        public final static String CURRENT_HTTP_REQUEST = "org.sakaiproject.util.RequestFilter.http_request";

        /** Key in the ThreadLocalManager for access to the current http response object. */
        public final static String CURRENT_HTTP_RESPONSE = "org.sakaiproject.util.RequestFilter.http_response";

        /** Key in the ThreadLocalManager for access to the current servlet context. */
        public final static String CURRENT_SERVLET_CONTEXT = "org.sakaiproject.util.RequestFilter.servlet_context";

    private void hack_local()
    {
                Boolean curRemoteUser = (Boolean) ThreadLocalManager.get(CURRENT_REMOTE_USER);
                Integer curHttpSession = (Integer) ThreadLocalManager.get(CURRENT_HTTP_SESSION);
                String curContext = (String) ThreadLocalManager.get(CURRENT_CONTEXT);
                ServletRequest curRequest = (ServletRequest) ThreadLocalManager.get(CURRENT_HTTP_REQUEST);
                ServletResponse curResponse = (ServletResponse) ThreadLocalManager.get(CURRENT_HTTP_RESPONSE);
		System.out.println("ThreadLocal Stuff");
		System.out.println("curRemoteUser="+curRemoteUser);
		System.out.println("curHttpSession="+curHttpSession);
		System.out.println("curContext="+curContext);
		System.out.println("curRequest="+curRequest);
		System.out.println("curResponse="+curResponse);
     }

    public void doView(RenderRequest request, RenderResponse response)
            throws PortletException, IOException {
        response.setContentType("text/html");

	System.out.println("==== doView called ====");

	// Dump out some stuff directly (cheating) to see what is set
	hack_local();

	// Call some APIs the stock way
	Session session = SessionManager.getCurrentSession();
	System.out.println("Session = "+session);
	if ( session != null ) {
		System.out.println("UserId="+session.getUserId()+" UserEID="+session.getUserEid());
	}

	ToolSession ts = SessionManager.getCurrentToolSession();
	
	System.out.println("Tool Session = "+ts);

	Placement placement = ToolManager.getCurrentPlacement();
	System.out.println("Placement = "+placement);
	if ( placement != null ) {
		String placementContext = placement.getContext();
		System.out.println("Context = "+placementContext);
	}
	System.out.println("==== doView complete ====");
    }

    public void doEdit(RenderRequest request, RenderResponse response)
            throws PortletException, IOException {

	System.out.println("==== doEdit called ====");
        sendToJSP(request, response, "/edit.jsp");
    }

    public void doHelp(RenderRequest request, RenderResponse response)
            throws PortletException, IOException {
	System.out.println("==== doHelp called ====");
        sendToJSP(request, response, "/help.jsp");
    }

    // Process action is called for action URLs / form posts, etc
    // Process action is called once for each click - doView may be called many times
    // Hence an obsession in process action with putting things in session to 
    // Send to the render process.
    public void processAction(ActionRequest request, ActionResponse response)
            throws PortletException, IOException {

	System.out.println("==== processAction called ====");

	System.out.println("==== End of ProcessAction view ====");
    }

    private void sendToJSP(RenderRequest request, RenderResponse response,
            String jspPage) throws PortletException {
        response.setContentType(request.getResponseContentType());
        if (jspPage != null && jspPage.length() != 0) {
            try {
                PortletRequestDispatcher dispatcher = pContext
                        .getRequestDispatcher(jspPage);
                dispatcher.include(request, response);
            } catch (IOException e) {
                throw new PortletException("Sakai Dispatch unabble to use "
                        + jspPage, e);
            }
        }
    }

    private String snoopPortlet(PortletRequest request)
    {
	String retval = "==== Portlet Request Snoop:\n";
	
        String remoteUser = request.getRemoteUser();
	retval += "getRemoteUser()="+remoteUser+"\n";

        PortletSession pSession = request.getPortletSession(true);
        PortletPreferences prefs = request.getPreferences();

        Map userInfo = (Map) request.getAttribute(PortletRequest.USER_INFO);
	retval += "UserInfo (needs Pluto 1.1.1 or later)\n"+userInfo+"\n";

	retval += "isUserInRole(admin)="+request.isUserInRole("admin")+"\n";
	retval += "isUserInRole(access)="+request.isUserInRole("access")+"\n";
	retval += "isUserInRole(maintain)="+request.isUserInRole("maintain")+"\n";
	retval += "isUserInRole(student)="+request.isUserInRole("student")+"\n";
	retval += "isUserInRole(instructor)="+request.isUserInRole("instructor")+"\n";
	retval += "isUserInRole(site.upd)="+request.isUserInRole("site.upd")+"\n";
	retval += "isUserInRole(content.read)="+request.isUserInRole("content.read")+"\n";

	return retval;
    }

}
