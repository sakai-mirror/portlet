/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2005, 2006 The Sakai Foundation.
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

import org.sakaiproject.portlet.util.PortalUser;
import org.sakaiproject.portlet.util.SakaiPortletUtil;

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

import java.util.List;
import java.util.Vector;
import java.util.Map;
import java.util.Properties;

import java.net.URLEncoder;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.AxisFault;

import javax.xml.namespace.QName;

import org.sakaiproject.util.Xml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * a simple SakaiTest Portlet
 */
public class SakaiTest extends GenericPortlet {

    private PortletContext pContext;

    public void init(PortletConfig config) throws PortletException {
        super.init(config);

        pContext = config.getPortletContext();
    }


    // Render the portlet - this is not supposed to change the state of the portlet
    // Render may be called many times so if it changes the state - that is tacky
    // Render will be called when someone presses "refresh" or when another portlet
    // onthe same page is handed an Action.
    public void doView(RenderRequest request, RenderResponse response)
            throws PortletException, IOException {
        response.setContentType("text/html");

	System.out.println("==== doView called ====");

        PortletSession pSession = request.getPortletSession(true);

	// Silly test of session - just to make sure
	System.out.println("doView has been called "+
               sessionTest("sakai.view.calls",pSession)+" times.");

	String view = (String) pSession.getAttribute("sakai.view");
	System.out.println("sakai.view="+view);

	// We name our views and actions the same - that is not necessary
	if ( "css.test".equalsIgnoreCase(view) ) {
        	sendToJSP(request, response, "/css.jsp");
	} else if ( "param.test".equalsIgnoreCase(view) ) {
		doParamView(request, response);
	} else {
		doMainView(request, response);
	}
    }

    public void doMainView(RenderRequest request, RenderResponse response)
            throws PortletException, IOException {

        PrintWriter out = response.getWriter();
	out.println("<p>Welcome to the Sakai JSR-168 test portlet.");
	out.println("It is pretty talkative to logs, so it is not a good thing to run in production.</p>");
	out.println("<p>Here are some choices.</p>");

        PortletURL url = response.createActionURL();

        url.setParameter("sakai.url.action","param.test");
	out.println("<a  href=\"" + url.toString() + "\">Test Form and URL Parameters</a><br>");

        url.setParameter("sakai.url.action","css.test");
	out.println("<a  href=\"" + url.toString() + "\">Test JSR-168 CSS</a><br>");

	// Put out the snoop information in a comment
	out.println("\n<p/>\nView Source to see PortletRequest snoop information<!--");
	out.println(snoopPortlet(request));
	out.println("\n-->\n");
    }

    public void doParamView(RenderRequest request, RenderResponse response)
            throws PortletException, IOException {

        PortletURL url = response.createActionURL();

        PrintWriter out = response.getWriter();
	// Set up the return link (actually a test of the form style)
	out.println("<form method=post action=\"" + url.toString() +"\">");
	out.println("<input type=\"hidden\" name=\"sakai.form.action\" value=\"main\">");
	out.println("<input type=\"submit\" value=\"Return To Main\">");
	out.println("</form><br/>");

	// Test the Form Parameter
	out.println("<Testing sending form parameters including action.form.a.form<br/>");
	out.println("<form method=post action=\"" + url.toString() +"\">");
	out.println("<input type=\"hidden\" name=\"sakai.form.action\" value=\"action.from.a.form\">");
	out.println("<input type=\"submit\" value=\"Test Form Parameter\">");
	out.println("</form><br/>");

	// Test the URL parameter
        url.setParameter("sakai.url.action","action.from.a.url");
	out.println("<a  href=\"" + url.toString() + "\">Test URL Parameter</a><br>");

        PortletSession pSession = request.getPortletSession(true);
	String actionResults = (String) pSession.getAttribute("sakai.action.debug");
	out.println("<p/>Action results = "+actionResults);

	// Put out the snoop information in a comment
	out.println("<p/>\nView Source to see PortletRequest snoop information<!--");
	out.println(snoopPortlet(request));
	out.println("\n-->\n");
    }

    public void doEdit(RenderRequest request, RenderResponse response)
            throws PortletException, IOException {

        sendToJSP(request, response, "/edit.jsp");
    }

    public void doHelp(RenderRequest request, RenderResponse response)
            throws PortletException, IOException {
        sendToJSP(request, response, "/help.jsp");
    }

    // Process action is called for action URLs / form posts, etc
    // Process action is called once for each click - doView may be called many times
    // Hence an obsession in process action with putting things in session to 
    // Send to the render process.
    public void processAction(ActionRequest request, ActionResponse response)
            throws PortletException, IOException {

	System.out.println("==== processAction called ====");

        PortletSession pSession = request.getPortletSession(true);

	// Silly test of session - just to make sure
	System.out.println("processAction has been called "+
               sessionTest("sakai.action.calls",pSession)+" times.");

	// Our first challenge is to figure out which action we want to take
	// The view selects the "next action" either as a URL parameter
	// or as a hidden field in the POST data - we check both

        String urlAction = request.getParameter("sakai.url.action");
	String action = urlAction;
        String formAction = request.getParameter("sakai.form.action");
	if ( action == null ) action = formAction;
	String actionValues = "action=" + action + " sakai.url.action=" + urlAction + " sakai.form.action="+formAction;

	// Send to doParamView() for debug printout
        pSession.setAttribute("sakai.action.debug", actionValues);
	System.out.println(actionValues);

	// Retrieve the most recent view (may be null) from session
	// This is different from things like "Edit mode" - this is simply
	// Our own convention to record what our last and/next view
	// is to be.

	String view = (String) pSession.getAttribute("sakai.view");
	System.out.println("action=" + action + " sakai.view=" + view);
        // pSession.setAttribute("sakai.action", urlAction);

	// Our next challenge is to pick which action the previous view
	// has told us to do.  Note that the view may place several actions
	// on the screen and the user may have an option to pick between
	// them.  Make sure we handle the "no action" fall-through.

	if ( "main".equalsIgnoreCase(action) ) {
		pSession.removeAttribute("sakai.view");
        } else if ( "param.test".equalsIgnoreCase(action) || 
                    "action.from.a.form".equalsIgnoreCase(action) || 
                    "action.from.a.url".equalsIgnoreCase(action)) {
        	pSession.setAttribute("sakai.view", "param.test");
        } else if ( "css.test".equalsIgnoreCase(action) ) {
        	pSession.setAttribute("sakai.view", "css.test");
	} else { // Fall through - go to the main view
		pSession.removeAttribute("sakai.view");
	}

	// Print out our new view - off to the Render :)
	String newView = (String) pSession.getAttribute("sakai.view");
	System.out.println("==== End of ProcessAction view="+newView+" ====");
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

    // Increment a meaningless number to make sure session is working
    // and test session reset when we get session reset working 
    private String sessionTest(String key, PortletSession pSession)
    {
        String count  = (String) pSession.getAttribute(key);
	String retval = count;
        if ( count == null ) {
                count = "0";
		retval = "0";
        }
	try {
		count = (Integer.parseInt(count)+1) + "";
	}  catch (Exception e) {
		count = "0";
	}
	pSession.setAttribute(key, count);
        return retval;
    }

    private String snoopPortlet(PortletRequest request)
    {
	String retval = "==== Portlet Request Snoop:\n";
	
        String remoteUser = request.getRemoteUser();
	retval += "getRemoteUser()="+remoteUser+"\n";

        PortletSession pSession = request.getPortletSession(true);
        PortletPreferences prefs = request.getPreferences();

	retval += "Are we running in sakai="+SakaiPortletUtil.isSakaiPortal(request)+"\n";


        Map userInfo = (Map) request.getAttribute(PortletRequest.USER_INFO);
	retval += "UserInfo\n"+userInfo+"\n";

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
