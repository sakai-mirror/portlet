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
 * a simple SakaiLaunch Portlet
 */
public class SakaiLaunch extends GenericPortlet {

    private final String LAUNCH_STATE = "launch.session";

    private final String LAUNCH_STATE_LOGIN = "login";

    private final String LAUNCH_STATE_MAIN = "main";

    private final String LAUNCH_STATE_SELECT = "select";

    private final String LAUNCH_STATE_LAUNCH = "launch";

    private final String LAUNCH_STATE_SITE = "site";

    private final String LAUNCH_STATE_TOOL = "tool";

    private final String CURRENT_SITE = "current.site";
    private final String CURRENT_SITE_TITLE = "current.site.title";

    private final String CURRENT_TOOL = "current.tool";
    private final String CURRENT_TOOL_TITLE = "current.tool.title";
    private final String CURRENT_TOOL_URL = "current.tool.url";

    // The designated host and secret that we have override capabilities for
    private String initHost = null;

    private String initSecret = null;

    private boolean debugPrintFlag = false;

    // Valid values: tool, gallery, tree
    private String initType = "tool";

    // In tool mode, this must be non-null and hold the Sakai tool ID
    // such as sakai.schedule
    private String initTool = null;

    // What type of Portal 
    private String portalType = null;

    private boolean autoLoginPossible = false;

    private PortalUser pUser = null;

    private PortletContext pContext;

    // Get the host for this placement
    public void init(PortletConfig config) throws PortletException {
        super.init(config);

	// Retrieve initialization parameters from properties file
	// The properties file is placed in the war by default
	// in this class path.

	// Users can override these properties by using -Dsakai.home=/some/path when
	// the portal JVM is started - if this is specified, any properties found
	// at this locaation take precedence of the default properties in the webapp.

	// <init-parms> in portlet.xml override the properties file
	// The default distribution does not specify init-parms
	// for sakai.host or sakai.secret in portlet.xml
	// Some portals may have ways of "faking' init parms as part of 
	// publish/placement - this approach allows those portal admins to 
	// nicely override parameters on a portlet by portlet or placement
	// by placement basis (depending on the capabilities of the particular
	// portal).

	Properties properties = null;
        try {
                java.io.InputStream is = SakaiPortletUtil.getConfigStream("sakaiportlet.properties",this.getClass());

                if (null != is) {
                    properties = new java.util.Properties();
                    properties.load(is);
 		    // properties.list(System.out);
                }
        } catch (Throwable ex) {
        }

	// Retrieve parameters from properties
	if ( properties != null ) {
		initHost = properties.getProperty("sakai.host");
		initSecret = properties.getProperty("sakai.secret");
		portalType = properties.getProperty("portal.type");
		debugPrintFlag = "true".equals(properties.getProperty("debug.print"));
	}

	debugPrint ("SakaiLaunch.init() from properties host="+initHost+" secret="+initSecret+" type="+portalType);

	// Retrieve overrides from init-parms 
        pContext = config.getPortletContext();
        String parmsHost = config.getInitParameter("sakai.host");
	if ( parmsHost != null ) initHost = parmsHost;
        String parmsSecret = config.getInitParameter("sakai.secret");
	if ( parmsSecret != null ) initSecret = parmsSecret;
        String parmsType = config.getInitParameter("portal.type");
	if ( parmsType != null ) portalType = parmsType;

	// Produce defaults in cases where none was found - only for host - not for secret

	// if initSecret is null, autologin will not be tried.  For testing, this is always 
	// set in the default properties file.  But if the user uses a different properties
	// file, they can not set this property to tunr off any attempt to 
	// auto-login

	if ( initHost == null ) initHost = "http://localhost:8080";

	// Figure out how we are supposed to display ourselves :)

	initTool = config.getInitParameter("sakai.tool");
	if ( "gallery".equalsIgnoreCase(initTool) ) {
		initTool = null;  // not really a tool
		initType = "gallery";
	} else if ( "tree".equalsIgnoreCase(initTool) ) {
		initTool = null;
		initType = "tree";
	} else if ( "launch".equalsIgnoreCase(initTool) ) {
		initTool = null;
		initType = "launch";
	}

        // Figure out what type of portal we are and how to get user information
        if (portalType == null) {
            pUser = new PortalUser(PortalUser.UNKNOWN);
	} else if (portalType.equalsIgnoreCase("gridsphere")) {
            pUser = new PortalUser(PortalUser.GRIDSPHERE);
        } else if (portalType.equalsIgnoreCase("uportal")) {
            pUser = new PortalUser(PortalUser.UPORTAL);
	} else if (portalType.equalsIgnoreCase("oracle")) {
	    pUser = new PortalUser(PortalUser.ORACLEPORTAL);
	} else {
            pUser = new PortalUser(PortalUser.UNKNOWN);
        }

        autoLoginPossible = (initHost != null) && (initSecret != null);

        System.out.println("SakaiLaunch.init() complete host = " + initHost 
                + " auto.login=" + autoLoginPossible+" initType="+initType+" initTool="+initTool);
        // System.out.println("secret=" + initSecret);

    }

    public void debugPrint(String str)
    {
	if ( debugPrintFlag ) 
	{
		System.out.println(str);
	} else {
		// Should do Log.debug here
	}
    }

    private class SakaiSite {
        public String id = null;

        public String title = null;

        public String host = null;

        public String session = null; // Session is optional
	
        public String toolId  = null; 

        public String toolTitle  = null; 

        public String toString() {
            return title;
        }

        // TODO: UrlEncode
        public String getUrl(PortletRequest request) {
	    if ( id == null ) return "null";
            String retval = host + "/portal/worksite/" + URLEncoder.encode(id);
            if (session != null)
                retval = retval + addSakaiSession(request, session, host, "?");

            return retval;
        }

        // TODO: UrlEncode
        public String getToolUrl(PortletRequest request) {
            if ( toolId == null ) return "null";
            String retval = host + "/portal/page/" + URLEncoder.encode(toolId);
            if (session != null)
                retval = retval + addSakaiSession(request, session, host, "?");
            return retval;
        }
    }

    private String getTag(Element theElement, String elementName) {
        try {
            Node node = theElement.getElementsByTagName(elementName).item(0);

            if (node.getNodeType() == node.TEXT_NODE) {
                return node.getNodeValue();
            } else if (node.getNodeType() == node.ELEMENT_NODE) {
                return node.getFirstChild().getNodeValue();
            }
            return null;
        } catch (Exception e) {
            return null;
        }

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

    public void doView(RenderRequest request, RenderResponse response)
            throws PortletException, IOException {
        response.setContentType("text/html");

	String theUrl;

        PortletSession pSession = request.getPortletSession(true);
        PortletPreferences prefs = request.getPreferences();

        List siteList = (List) pSession.getAttribute("site.list");
        String theHeight = prefs.getValue("sakai.height", "2000");
	String session = (String) pSession.getAttribute("sakai.session");
	String host = (String) pSession.getAttribute("sakai.host");
        String autoDone = (String) pSession.getAttribute("auto.done");
        String sakaiPlacement  = (String) pSession.getAttribute("sakai.placement");

        String autoLogin = prefs.getValue("sakai.auto", null);
        String remoteUser = pUser.getUsername(request);

	debugPrint("doView site.list = "+siteList+" sakai.host="+host);
        debugPrint("autoLogin = " + autoLogin + " autoDone=" + autoDone + " remote=" + remoteUser+" session="+session);

        if (siteList == null && autoDone == null && remoteUser != null
                && autoLoginPossible && !"0".equals(autoLogin)) {
            // Only do this once, success or failure
            pSession.setAttribute("auto.done", "true");
            siteList = loadSiteList(request, initHost, remoteUser, initSecret,
                    true);
	    debugPrint("loadSiteList  site.list="+siteList);
	    // Things are looking good
            if (siteList != null) {
                pSession.setAttribute("site.list", siteList);
		session = (String) pSession.getAttribute("sakai.session");
		host = (String) pSession.getAttribute("sakai.host");
		debugPrint("From session host="+host+" session="+session);
	    }
        }

	// If for some reason we did not get (or do not have ) a list of sites, simply 
	// send the userto login 

        if (siteList == null) {
            pSession.setAttribute(LAUNCH_STATE, LAUNCH_STATE_LOGIN);
            String sakaiHost = prefs.getValue("sakai.host",initHost);
            request.setAttribute("sakai.host", sakaiHost);

	    debugPrint("Sending user to login sakai.host=" + sakaiHost + " initHost=" + initHost);

            String theUser = prefs.getValue("sakai.id", null);
            if (theUser == null || theUser.length() < 1)
                theUser = pUser.getUsername(request);
            if (theUser == null)
                theUser = "";
            request.setAttribute("sakai.id", theUser);

            request.setAttribute("sakai.pw", "");
            sendToJSP(request, response, "/launchLogin.jsp");
            return;
        }

	// We now have a list of sites, properly retrieved

	debugPrint("sakai.tool = "+initTool+" Placement = " + sakaiPlacement);

	// Handle single tool placement display

	// If we are in tool mode and have no placement from preferences, we must
	// get a placement - switch into Select mode
	if ( initTool != null && sakaiPlacement == null ) {
		debugPrint("Loading placements for "+initTool);
		// Add the list of sites which contain the tools to the request
		int numTools = addToolList(request,siteList);
		debugPrint("Found placements="+numTools);

		// With two tools or more do the selection
		if ( numTools > 1 ) {
        		pSession.setAttribute(LAUNCH_STATE, LAUNCH_STATE_SELECT);
            		sendToJSP(request, response, "/launchSelect.jsp");
            		return;
		}
		if ( numTools < 1 ) {
        		PrintWriter myOut = response.getWriter();
        		myOut.println("<font color=\"red\">");
			myOut.println("You do not have permission for any tools of type "+initTool);
			myOut.println(" in the Sakai server at "+host+". You may need to join some Sakai sites");
			myOut.println("using the Membership tool, or perhaps the tool simply is not present in");
			myOut.println("any of the Sakai sites for which you have access</font>");;
			return;
		}
		// Fall through - we have a single placement
		// TODO: This fall through does not make sense
		debugPrint("Hmmm.  Getting placement from session");
        	sakaiPlacement  = (String) pSession.getAttribute("sakai.placement");
	}

        pSession.setAttribute(LAUNCH_STATE, LAUNCH_STATE_MAIN);
	
	// We are ready to produce a response
        PrintWriter out = response.getWriter();

	// Single tool mode
	if ( sakaiPlacement != null ) {
	    String thisUrl = host + "/portal/page/" + URLEncoder.encode(sakaiPlacement)
                + "?panel=Main" + addSakaiSession(request, session, host, "&");
	    debugPrint("Single tool url="+thisUrl);
            out
                    .println("<iframe frameborder=\"0\" marginwidth=\"0\" marginheight=\"0\" scrolling=\"auto\"");
            out.println("width=\"100%\" height=\"" + theHeight + "px\"");
            out.println("src=\"" + thisUrl + "\"></iframe>");
	    return;
        }

	// Gallery Mode
	// TODO: Some type of logout possibility (or perhaps not - just use edit mode)
	if ( "gallery".equals(initType) ) {

	    theUrl = host + "/portal/gallery";
	    if ( session != null ) theUrl = theUrl + addSakaiSession(request, session, host, "?");
	    debugPrint("Galery tool url="+theUrl);

            out.println("<iframe frameborder=\"0\" marginwidth=\"0\" marginheight=\"0\" scrolling=\"auto\"");
            out.println("width=\"100%\" height=\"" + theHeight + "px\"");
            out.println("src=\"" + theUrl + "\"></iframe>");
	    return;
	}

	// Tree Mode
	if ( "tree".equals(initType) ) {
	
	  // Tree Mode - If we fall through - be a tree!
	  String cPath = request.getContextPath();
          Document doc = (Document) pSession.getAttribute("site.doc");

	  out.println("<script language=\"JavaScript\" src=\""+cPath+"/tree.js\"></script>");
	  out.println("<script language=\"JavaScript\" src=\""+cPath+"/tree_tpl_local.js\"></script>");

	  out.println("<script language=\"JavaScript\">");
	  out.println("<!--//");
	  out.println("var TREE_ITEMS = [");

          NodeList children = doc.getElementsByTagName("site");

	  String firstUrl = null;
          for (int i = 0; i < children.getLength(); i++) {
		if ( i > 0 ) out.print(",\n");
                Element site  = (Element) children.item(i);

	        out.print("  ['"+getTag(site ,"title")+"',0");

		NodeList pages = site .getElementsByTagName("page");
		if ( pages.getLength() > 0 ) out.print(",\n");

		for (int j=0; j < pages.getLength(); j++ ) {
			if ( j > 0 ) out.print(",\n");
			Element page = (Element) pages.item(j);

			NodeList tools = page.getElementsByTagName("tool");
	        	if ( tools.getLength() > 1 ) out.print("    ['"+getTag(page ,"title")+"',0,\n");

			for (int k=0; k < tools.getLength();k++) {
				if ( k > 0 ) out.print(",\n");
				Element tool = (Element) tools.item(k);
				theUrl = getTag(tool ,"url");
	    			if ( session != null ) theUrl = theUrl + 
                                        "?sakai.state.reset=true" + 
                                        addSakaiSession(request, session, host, "&");
				debugPrint("Tree url="+theUrl);
				if ( firstUrl == null ) firstUrl = theUrl;
	        		out.print("      ['"+getTag(tool ,"title")+"','"+theUrl+"']");
			}
			if ( tools.getLength() > 1 ) out.println("\n    ]");
		}
		out.println("\n  ]");

            }

	  out.println("\n];");
          out.println("//-->");
	  out.println("</script>");

	  out.println("<table cellpadding=\"5\" cellspacing=\"0\" cellpadding=\"10\" border=\"0\" width=\"100%\"><tr><td valign=top width=200>");

	  out.println("<font size=-2>");
	  out.println("<script language=\"JavaScript\">");
	  out.println("<!--//");
          out.println("     new tree (TREE_ITEMS, TREE_TPL);");
          out.println("//-->");
	  out.println("</script>");
	  out.println("</font>");
	  out.println("</td><td>");
	
	  // TODO: Better error checking if there is nothing to show
	  if ( firstUrl == null ) firstUrl = cPath + "/blank.htm";
	  out.println("<iframe src=\""+firstUrl+"\" name=frameset width=100% height=2400 align=top>");

	  out.println("</td></tr></table>");
	  return;
	}

	// Fell through - be the Launch Portlet
	pSession.setAttribute(LAUNCH_STATE, LAUNCH_STATE_LAUNCH);

        String launchState = (String) pSession.getAttribute(LAUNCH_STATE);
	String currentSite = (String) pSession.getAttribute(CURRENT_SITE);
	String currentSiteTitle = (String) pSession.getAttribute(CURRENT_SITE_TITLE);
	String currentTool = (String) pSession.getAttribute(CURRENT_TOOL);
	String currentToolTitle = (String) pSession.getAttribute(CURRENT_TOOL_TITLE);
	String currentToolUrl = (String) pSession.getAttribute(CURRENT_TOOL_URL);
        Document doc = (Document) pSession.getAttribute("site.doc");

        debugPrint("state=" + launchState + " currentSite="+currentSite+" currentTool="+currentTool);
        debugPrint("currentSiteTitle="+currentSiteTitle+" currentToolTitle="+currentToolTitle+" currentToolUrl="+currentToolUrl);

        PortletURL url = response.createActionURL();
	url.setParameter("sakai.action","all.sites");
	String className = "portlet-menu-item";
	if ( currentSite == null ) {
		className = "portlet-menu-item-selected";
	}

	out.println("<a class=\""+className+"\" href=\"" + url.toString() + "\">Sites</a>");

	// If we are in a site
	if ( currentSite != null ) {
		url.setParameter("sakai.action","site");
		if ( currentSiteTitle == null ) currentSiteTitle = currentSite;   //  WIll look Ugly!
		if ( currentTool == null ) {
			className = "portlet-menu-item-selected";
		}
		out.println(" > <a class=\""+className+"\" href=\"" + url.toString() + "\">"+currentSiteTitle+"</a>");

	// If we are not in a site - print out the list of sites
        } else {
	  out.println("<br>");
          NodeList children = doc.getElementsByTagName("site");

          if ( children.getLength() <= 0 ) {
		out.println("<br>&nbsp;<br>No Sites Found...<br>");
		return;
	  }

	  out.println("<ul>");
          for (int i = 0; i < children.getLength(); i++) {
                Element site  = (Element) children.item(i);
                url.setParameter("sakai.action","switch.site");
		url.setParameter("new.site",getTag(site,"id"));
	        out.print("<li><a  href=\"" + url.toString() + "\">"+getTag(site ,"title")+"</a>");
            }
	    out.println("</ul>");
	    return;
	}

	// If we are in a tool
	if ( currentTool != null ) {
		// url.setParameter("sakai.action","tool");
                		url.setParameter("sakai.action","select.tool");
				url.setParameter("new.tool",currentTool);
		if ( currentToolTitle == null ) currentToolTitle = currentTool;   //  Will look Ugly!
		out.println(" > <a class=\"portlet-menu-item-selected\" href=\"" + url.toString() + "\">"+currentToolTitle+"</a>");
		// TODO - Add Help Here someday - need to add it to the DOM :)
		// TODO - Emit iFrame here
		out.println("<br>");

		theUrl = currentToolUrl;
	    	if ( session != null ) theUrl = theUrl + 
			"?sakai.state.reset=true" +
	     	        addSakaiSession(request, session, host, "&");

		String windowID = "Main" + currentTool.replace("-","x");

		out.println("<iframe name=\""+windowID+"\" id=\""+windowID+"\"");
		out.println("  title=\""+currentToolTitle+"\" class =\"portletMainIframe\" height=\"500px\" width=\"100%\"");
		out.println("  frameborder=\"0\" marginwidth=\"0\" marginheight=\"0\" scrolling=\"auto\" ");
		out.println("  src=\""+theUrl+"\">");
		// out.println("  src=\""+theUrl+"\">");
		// out.println("  src=\"/library/image/sakai_powered.gif\">");
		out.println("</iframe>");
		return;

	// If we are not in a tool, list all the tools in the current site
	} else {
          NodeList children = doc.getElementsByTagName("site");

          if ( children.getLength() <= 0 ) {
		out.println("<br>&nbsp;<br>No Sites Found...<br>");
		return;
	  }

          for (int i = 0; i < children.getLength(); i++) {
                Element site  = (Element) children.item(i);
		if ( ! currentSite.equals(getTag(site,"id")) ) continue;

		NodeList pages = site .getElementsByTagName("page");

		if ( pages.getLength() > 0 ) out.println("<ul>");

		for (int j=0; j < pages.getLength(); j++ ) {
			Element page = (Element) pages.item(j);

			NodeList tools = page.getElementsByTagName("tool");
	        	if ( tools.getLength() > 1 ) {
				out.println("<li>"+getTag(page ,"title"));
				out.println("<ul>");
			}

			for (int k=0; k < tools.getLength();k++) {
				Element tool = (Element) tools.item(k);
                		url.setParameter("sakai.action","select.tool");
				url.setParameter("new.tool",getTag(tool,"id"));
				out.println("<li><a href=\""+url.toString()+"\">"+getTag(tool,"title")+"</a>");
			} // Tools Loop

			if ( tools.getLength() > 1 ) out.println("</ul>");

		} // Pages Loop

	    	if ( pages.getLength() > 0 ) out.println("</ul>");
		break;  // We found our site
            } // Sites loop

	    return;
        }

    }

    public int addToolList(RenderRequest request, List siteList)
            throws PortletException, IOException 
    {
        PortletPreferences prefs = request.getPreferences();
        PortletSession pSession = request.getPortletSession(true);
        String sakaiPlacement = prefs.getValue("sakai.placement",null);

	String optList = "";

	// We skip the select step if we have a pre-existing placement 
	// or if there is only one placement t

	int count = 0;
	if ( sakaiPlacement == null ) {
		count = 0;
        	for (int i = 0; i < siteList.size(); i++) {
            		SakaiSite theSite = (SakaiSite) siteList.get(i);
	    		if ( theSite.toolId == null || theSite.toolTitle == null ) continue;
	    		optList = optList + "<INPUT TYPE=RADIO NAME=sakai.placement VALUE="+theSite.toolId+">"+theSite.title+"<BR>\n";
	    		count++;
	    		sakaiPlacement = theSite.toolId;
        	}
	} else {
		count = 1;
	}

	// If there is only one placement, set it and move on
	// there is no need to select...
	if ( count == 1 && sakaiPlacement != null )  {
        	pSession.setAttribute("sakai.placement", sakaiPlacement);
	}

        request.setAttribute("sakai.options", optList);
	return count;
    }

    public void doEdit(RenderRequest request, RenderResponse response)
            throws PortletException, IOException {

        PortletPreferences prefs = request.getPreferences();
        String sakaiHost = prefs.getValue("sakai.host",initHost);
        request.setAttribute("sakai.host", sakaiHost);
        String sakaiId = prefs.getValue("sakai.id", pUser.getUsername(request));
        request.setAttribute("sakai.id", sakaiId);
        String sakaiHeight = prefs.getValue("sakai.height", "800");
        request.setAttribute("sakai.height", sakaiHeight);
        if (initHost != null && initSecret != null) {
            request.setAttribute("do.auto", "yes");
            String sakaiAuto = prefs.getValue("sakai.auto", "0");
            if (sakaiAuto != null && "1".equals(sakaiAuto)) {
                request.setAttribute("sakai.auto", "yes");
            }
        }
        sendToJSP(request, response, "/launchEdit.jsp");
    }

    public void doHelp(RenderRequest request, RenderResponse response)
            throws PortletException, IOException {
        sendToJSP(request, response, "/launchHelp.jsp");
    }

    public void processAction(ActionRequest request, ActionResponse response)
            throws PortletException, IOException {
        PortletPreferences prefs = request.getPreferences();
        PortletSession pSession = request.getPortletSession(true);

        String launchState = (String) pSession.getAttribute(LAUNCH_STATE);
	if ( launchState == null ) {
		launchState = LAUNCH_STATE_LAUNCH;
System.out.println("WHAT THE HELL!");
	}

        debugPrint("processAction mode=" + request.getPortletMode()+" state=" + launchState);

        if (PortletMode.VIEW.equals(request.getPortletMode())) {
            if (LAUNCH_STATE_LOGIN.equals(launchState)) {
                processActionLogin(request, response);
                return;
            }
            if (LAUNCH_STATE_MAIN.equals(launchState)) {
                processActionMain(request, response);
                return;
            }
            if (LAUNCH_STATE_SELECT.equals(launchState)) {
                processActionSelect(request, response);
                return;
            }
            if (LAUNCH_STATE_LAUNCH.equals(launchState)) {
                processActionLaunch(request, response);
                return;
            }
System.out.println("LKJASKLJASKLASLKJASJLKSLJKS");
        } else if (PortletMode.EDIT.equals(request.getPortletMode())) {
            boolean editOK;
            pSession.removeAttribute("site.list");
            pSession.removeAttribute("sakai.session");
            String errorMsg = null;
            String newHost = request.getParameter("sakai.host");
            prefs.setValue("sakai.host", newHost);
            String newId = request.getParameter("sakai.id");
            prefs.setValue("sakai.id", newId);
            String newHeight = request.getParameter("sakai.height");
            prefs.setValue("sakai.height", newHeight);

            String newAuto = request.getParameter("sakai.auto");
            // System.out.println("Sakai.auto = " + newAuto);
            if (newAuto == null) {
                prefs.setValue("sakai.auto", "0");
            } else {
                prefs.setValue("sakai.auto", "1");
            }

            try {
                prefs.store();
                editOK = true;
            } catch (ValidatorException ex) {
                editOK = false;
                errorMsg = ex.getMessage();
            }
            if (editOK) {
                response.setPortletMode(PortletMode.VIEW);
            } else {
                response.setRenderParameter("error", errorMsg);
            }
        }
    }

    public void processActionLogin(ActionRequest request,
            ActionResponse response) throws PortletException, IOException {

        PortletSession pSession = request.getPortletSession(true);
        String sakaiHost = request.getParameter("sakai.host");
        String sakaiId = request.getParameter("sakai.id");
        String sakaiPw = request.getParameter("sakai.pw");
System.out.println("sakai.host="+sakaiHost+" sakaiId="+sakaiId+" sakaiPw="+sakaiPw);

	// After login in clear out placement
	pSession.removeAttribute("sakai.placement");
	pSession.removeAttribute("sakai.host");

        List allSites = loadSiteList(request, sakaiHost, sakaiId, sakaiPw,
                false);

        String errorStr = (String) pSession.getAttribute("error");
        if (errorStr != null) {
            response.setRenderParameter("error", errorStr);
        }

	pSession.setAttribute("sakai.host", sakaiHost);

        if (allSites != null) {
            try {
                PortletPreferences prefs = request.getPreferences();
                String newHost = request.getParameter("sakai.host");
                sakaiId = request.getParameter("sakai.id");
                prefs.setValue("sakai.host", newHost);
                prefs.setValue("sakai.id", sakaiId);
                prefs.store();
            } catch (ValidatorException ex) {
                // No harm - this is a nice side effect if it happens
            }
        }
    }

    public void processActionSelect(ActionRequest request,
            ActionResponse response) throws PortletException, IOException {

	// TODO: Need to add some type of Cancel processing
        PortletSession pSession = request.getPortletSession(true);

        String sakaiPlacement = request.getParameter("sakai.placement");
	// System.out.println("sakaiPlacement = " + sakaiPlacement);

	// TODO: Should check to see if this is a valid placement in the site list
	// But for now, since we set it up, it is likely correct
        pSession.setAttribute("sakai.placement", sakaiPlacement);

        try {
        	PortletPreferences prefs = request.getPreferences();
        	prefs.setValue("sakai.placement", sakaiPlacement);
        	prefs.store();
        } catch (ValidatorException ex) {
        	// No harm - this is a nice side effect if it happens
        }
    }

    public void processActionMain(ActionRequest request, ActionResponse response)
            throws PortletException, IOException {
        PortletSession pSession = request.getPortletSession(true);
        String strIndex = request.getParameter("site.index");
        if ("logout".equals(strIndex)) {
            pSession.removeAttribute("site.list");
            pSession.removeAttribute("site.index");
            pSession.removeAttribute("sakai.placement"); 
        } else {
            pSession.setAttribute("site.index", strIndex);
        }
        System.out.println("PAM index=" + strIndex);
    }

    // ProcessActions for the Launch Portlet

    // Coming from the Site list  display
    public void processActionLaunch(ActionRequest request, ActionResponse response)
            throws PortletException, IOException {

        PortletSession pSession = request.getPortletSession();
        pSession.setAttribute(LAUNCH_STATE, LAUNCH_STATE_LAUNCH);

        String action = request.getParameter("sakai.action");
        String newSite = request.getParameter("new.site");
        String nt = request.getParameter("new.tool");
	debugPrint("sakai.action="+action+" newSite="+newSite+" nt="+nt);

	if ( "tool".equals(action) ) {
		System.out.println("Need to handle Reset Tool!");
		return;
	}

        Document doc = (Document) pSession.getAttribute("site.doc");

	if ( doc == null ) {
		debugPrint("processActionLaunch switch.site - Sites dom tree missing");
		return;	
	}

        NodeList children = doc.getElementsByTagName("site");

        if ( children.getLength() <= 0 ) {
	 	debugPrint("<br>&nbsp;<br>No Sites Found...<br>");
	 	return;
         }

	// From here we need no tool, or a need to find new tool
        pSession.removeAttribute(CURRENT_TOOL);
        pSession.removeAttribute(CURRENT_TOOL_TITLE);
        pSession.removeAttribute(CURRENT_TOOL_URL);

	// Handle the "leave a tool and go back to a site"
	if ( "site".equals(action) ) return;  // Tool has been cleared

	// Handle the Tool Case
	if ( "select.tool".equalsIgnoreCase(action) && nt != null ) {

	  // Loop through pages and sites and tools, looking for "the one"
	  boolean found = false;
          for (int i = 0; i < children.getLength(); i++) {
                Element site  = (Element) children.item(i);
		NodeList pages = site .getElementsByTagName("page");
		for (int j=0; j < pages.getLength(); j++ ) {
			Element page = (Element) pages.item(j);
			NodeList tools = page.getElementsByTagName("tool");

			for (int k=0; k < tools.getLength();k++) {
				Element tool = (Element) tools.item(k);
				if ( nt.equals(getTag(tool,"id")) ) {
					found = true;
					debugPrint("Tool Found! nt="+nt);
        				pSession.setAttribute(CURRENT_TOOL, nt);
        				pSession.setAttribute(CURRENT_TOOL_URL, getTag(tool,"url"));
        				pSession.setAttribute(CURRENT_TOOL_TITLE, getTag(tool,"title"));
					break;
				}
			} // Tools Loop
			if ( found ) break;
		} // Pages Loop
		if ( found ) break;
            } // Sites loop
	    return;
	}

	// From here on, we need no existing site - we either clear it or reset it
        pSession.removeAttribute(CURRENT_SITE);
        pSession.removeAttribute(CURRENT_SITE_TITLE);

	// Force the reloading of the sitet list
	if ( "all.sites".equalsIgnoreCase(action) ) {
		pSession.removeAttribute("site.list");
		pSession.removeAttribute("auto.done");
		// Leave session alone - no need to relogin
		// pSession.removeAttribute("sakai.session");
		debugPrint("Cleared Attributes");
		return;  // All clear - to the top we go
        }

	// Search through and find the new site
	if ( "switch.site".equalsIgnoreCase(action) ) {

	  if ( newSite == null ) {
		debugPrint("processActionLaunch switch.site - new.site not specified");
		return;
	  }

	  boolean found = false;
          for (int i = 0; i < children.getLength(); i++) {
                Element site  = (Element) children.item(i);
		String siteId = getTag(site,"id");
		debugPrint("newSite="+newSite+" id="+siteId);
		if ( siteId == null ) continue;
		if ( siteId.equals(newSite) ) {
			found = true;
			debugPrint("found it newSite="+newSite);
        		pSession.setAttribute(CURRENT_SITE,newSite);
        		pSession.setAttribute(CURRENT_SITE_TITLE,getTag(site ,"title"));
			break;
		}
            }
	    if ( !found ) debugPrint("processActionLaunch switch.site - site not found id="+newSite);
	    return;
        }


    }
   
    public List loadSiteList(PortletRequest request, String sakaiHost,
            String sakaiId, String sakaiPw, boolean doAuto) {

        PortletSession pSession = request.getPortletSession();
        String endpoint;
        String session = null;
        String siteList = null;
	Service service = null;
	Call call = null;
        String axisPoint = sakaiHost + "/sakai-axis/";

        debugPrint("loadSiteList host=" + sakaiHost + " id=" + sakaiId + " pw=" + sakaiPw + " auto=" + doAuto);
	debugPrint("  session="+pSession.getAttribute("sakai.session")+
                    " host="+pSession.getAttribute("sakai.host")+
                    " user="+pSession.getAttribute("sakai.user")+
                    " placement="+pSession.getAttribute("sakai.placement") );

        try {

            pSession.removeAttribute("error");
	    
            session = (String) pSession.getAttribute("sakai.session");

	    // Make sure this session is still active and alive - if for any reason we cannot validate
	    // Including a pre-revision 14063 SakaiSession.jws, we just whack the session.

	    try {
	      	if ( session != null ) {
                	service = new Service();
                	call = (Call) service.createCall();
  
                	endpoint = axisPoint + "SakaiSession.jws";
                	debugPrint("Checking session" + endpoint);
                	call.setOperationName("getSessionUser");
                	call.setTargetEndpointAddress(new java.net.URL(endpoint));
                	String sessionUser = (String) call.invoke(new Object[] { session });
	        	if ( ! sakaiId.equals(sessionUser) ) {
		   		debugPrint("Session user="+sessionUser+" does not match id="+sakaiId);
		   		session = null;
	        	}
	      	}
	    } catch (Exception e) {
		   // We really, literally, absolutely do not care why this failed
		   session = null;
	    }

	    // If we have changed hosts or users or have no session or have no site list - wipe it all out 
	    // Also since the site.list is *not* serializable, it is a cheating way to work
            // around insanity in Tomcat maintaining sessions over restart.  If there is a site.list
	    // We put it there in this session :)
            if ( ( ! sakaiId.equals(pSession.getAttribute("sakai.user")) )  ||
		 ( ! sakaiHost.equals(pSession.getAttribute("sakai.host")) ) ||
		 ( pSession.getAttribute("site.list") == null ) ||
		 ( session  == null ) ) {
            	pSession.removeAttribute("sakai.session");
            	pSession.removeAttribute("sakai.host");
            	pSession.removeAttribute("sakai.user");
		// May not want to remove this to force reChoosing
                // pSession.removeAttribute("sakai.placement"); 
           }

	    // Remove the old one in case something fails
            pSession.removeAttribute("site.list");

	    // Must establish or have a session - Someday might want to validate session
	    // each time over web services before simply accepting it.
	    if ( session == null ) {
	    
              service = new Service();
              call = (Call) service.createCall();

              // Get user's information
              String firstName = pUser.getFirstName(request);
              String lastName = pUser.getLastName(request);
              String email = pUser.getEmail(request);

	      //HACK FOR DEMO
	      if ( "tomcat".equals(sakaiId) && firstName == null ) {
		  firstName = "Tom";
		  lastName = "Cat";
		  email = "tom@cat.com";
	      }

              // System.out.println("firstname=" + firstName + ", lastName=" + lastName + ", email=" + email);

              if (doAuto && email != null && firstName != null
                      && lastName != null) {
                  endpoint = axisPoint + "SakaiPortalLogin.jws";
                  debugPrint("Portal Login and Create " + endpoint + " id="+sakaiId+" pw="+sakaiPw);
                  call.setOperationName("loginAndCreate");
                  call.setTargetEndpointAddress(new java.net.URL(endpoint));
                  session = (String) call.invoke(new Object[] { sakaiId, sakaiPw,
                          firstName, lastName, email });
              } else if ( doAuto ) {
                      endpoint = axisPoint + "SakaiPortalLogin.jws";
                      debugPrint("Portal Login " + endpoint+ " id="+sakaiId+" pw="+sakaiPw);
                      call.setOperationName("login");
                      call.setTargetEndpointAddress(new java.net.URL(endpoint));
                      session = (String) call.invoke(new Object[] { sakaiId, sakaiPw });
              } else {
                  endpoint = axisPoint + "SakaiLogin.jws";
                  debugPrint("Web Services Login " + endpoint + " id="+sakaiId+" pw="+sakaiPw);
                  call.setOperationName("login");
                  call.setTargetEndpointAddress(new java.net.URL(endpoint));
                  session = (String) call
                          .invoke(new Object[] { sakaiId, sakaiPw });
              }
	    }  // End establishing Session
  
            if (session == null || session.length() < 2) {
                System.out.println("Unable to establish session to "
                        + sakaiHost);
                pSession.setAttribute("error",
                        "Unable to establish session to " + sakaiHost);
                return null;
            }

            debugPrint("Login successful session="+session+" id="+sakaiId+" host="+sakaiHost);
            pSession.setAttribute("sakai.session", session);
	    pSession.setAttribute("sakai.user", sakaiId);
            pSession.setAttribute("sakai.host", sakaiHost);

	    // Retrieve Site List
            service = new Service();
            call = (Call) service.createCall();

	    endpoint = axisPoint + "SakaiSite.jws";

	    debugPrint("Site Retrieval " + endpoint+ " session="+session);

            call.setTargetEndpointAddress(new java.net.URL(endpoint));
            call.setOperationName("getToolsDom");

            siteList = (String) call.invoke(new Object[] { session, "",
                    new Integer(1), new Integer(9999) });

	    // debugPrint(siteList);
            Document doc = Xml.readDocumentFromString(siteList);

            pSession.setAttribute("site.doc", doc);

            NodeList children = doc.getElementsByTagName("site");
            // debugPrint("There are " + children.getLength() + " child elements.\n");

            if (children.getLength() < 1) {
                pSession.setAttribute("error", "No sites available to you on "
                        + sakaiHost);
                return null;
            }

            List allSites = new Vector();
            for (int i = 0; i < children.getLength(); i++) {
                Element elem = (Element) children.item(i);

                // System.out.println("ID = " + getTag(elem, "id"));
                // System.out.println("title = " + getTag(elem, "title"));
                SakaiSite theSite = new SakaiSite();
                theSite.host = sakaiHost;
                theSite.session = session;
                theSite.id = getTag(elem, "id");
                theSite.title = getTag(elem, "title");
		theSite.toolId = null;
		theSite.toolTitle = null;
		// descend into pages -> page -> tools -> tool -> toolid
		boolean found = false;
		if ( initTool == null ) found = true;  // Nothing to find
		NodeList pages = elem.getElementsByTagName("page");
		for (int j=0; ! found && j < pages.getLength(); j++ ) {
			Element page = (Element) pages.item(j);
			NodeList tools = page.getElementsByTagName("tool");
			String pageTitle = (String) getTag(page,"title");
			// We prefer the page ID because pages look better and have nice title bars
			// TODO: We could switch to tool IDs and replicate the Charon Page code here
			// That would allow for dynamic resize
			String pageId = (String) getTag(page, "id");
			for (int k=0; ! found && k<tools.getLength();k++) {
				Element tool = (Element) tools.item(k);
				String toolId = (String) getTag(tool, "toolid");
                // System.out.println("ToolID = " + toolId);
				if ( initTool.equals(toolId) ) {
					theSite.toolId = pageId;
					theSite.toolTitle = pageTitle;
					found = true;
				}
			}
		}

                allSites.add(theSite);

            }

            pSession.setAttribute("site.list", allSites);
	    debugPrint("Adding to session site.list="+allSites+" sakai.session="+session+" sakai.host="+sakaiHost);
            return allSites;

        } catch (Exception e) {
            pSession.setAttribute("error", "Error retrieving site list: "
                    + e.toString());
            System.out.println("Exception:" + e.toString());
            e.printStackTrace();
            return null;
        }
    }

    // This is a way to test the portlet *within* Sakai - we do not want to add the 
    // sakai.session parameter to the URLs or the cookies keep getting reset to a new 
    // session each time you navigate to a tool
    private String addSakaiSession(PortletRequest request, String session, String host, 
	String connect)
    {
	if ( SakaiPortletUtil.isSakaiPortal(request) && host.startsWith("http://localhost:") ) return "";

	return connect + "sakai.session=" + URLEncoder.encode(session);
    }
}
