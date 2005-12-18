package org.sakaiproject.portlets;

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
import java.io.File;

import java.util.List;
import java.util.Vector;
import java.util.Map;
import java.util.Properties;

import java.net.URLEncoder;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.AxisFault;

import javax.xml.namespace.QName;

import org.sakaiproject.util.xml.Xml;
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

    // The designated host and secret that we have override capabilities for
    private String initHost = null;

    private String initSecret = null;

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
                java.io.InputStream is = getConfigStream("sakaiportlet.properties",this.getClass());

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
	}

	// System.out.println("SakaiLaunch.init() from properties host="+initHost+" secret="+initSecret+" type="+portalType);

	// Retrieve overrides from init-parms 
        pContext = config.getPortletContext();
        String parmsHost = config.getInitParameter("sakai.host");
	if ( parmsHost != null ) initHost = parmsHost;
        String parmsSecret = config.getInitParameter("sakai.secret");
	if ( parmsSecret != null ) initSecret = parmsSecret;
        String parmsType = config.getInitParameter("portal.type");
	if ( parmsType != null ) portalType = parmsType;

	// System.out.println("SakaiLaunch.init() after parms host="+initHost+" secret="+initSecret+" type="+portalType);

	// Produce defaults in cases where none was found - only for host - not for secret

	// if initSecret is null, autologin will not be tried.  For testing, this is always 
	// set in the default properties file.  But if the user uses a different properties
	// file, they can not set this property to tunr off any attempt to 
	// auto-login

	if ( initHost == null ) initHost = "http://localhost:8080";

	// Figure out how we are supposed to display ourselves :)

	initTool = config.getInitParameter("sakai.tool");
	if ( initTool != null ) {
		if ( initTool.equalsIgnoreCase("gallery") ) {
			initTool = null;  // not really a tool
			initType = "gallery";
		} else if ( initTool.equalsIgnoreCase("tree") ) {
			initTool = null;
			initType = "tree";
		}
	}

        // Figure out what type of portal we are and how to get user information
        if (portalType == null) {
            pUser = new PortalUser(PortalUser.UNKNOWN);
	} else if (portalType.equalsIgnoreCase("gridsphere")) {
            pUser = new PortalUser(PortalUser.GRIDSPHERE);
        } else if (portalType.equalsIgnoreCase("uportal")) {
            pUser = new PortalUser(PortalUser.UPORTAL);
	} else {
            pUser = new PortalUser(PortalUser.UNKNOWN);
        }

        autoLoginPossible = (initHost != null) && (initSecret != null);

        System.out.println("SakaiLaunch.init() complete host = " + initHost 
                + " auto.login=" + autoLoginPossible+" initType="+initType+" initTool="+initTool);
        // System.out.println("secret=" + initSecret);

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

        public String toStringFull() {
            return "title=" + title + " url=" + getUrl() + " toolTitle=" + toolTitle + " toolUrl=" + getToolUrl();
        }

        // TODO: UrlEncode
        public String getUrl() {
	    if ( id == null ) return "null";
            String retval = host + "/portal/worksite/" + URLEncoder.encode(id);
            if (session != null)
                retval = retval + "?sakai.session="
                        + URLEncoder.encode(session);
            return retval;
        }

        // TODO: UrlEncode
        public String getToolUrl() {
            if ( toolId == null ) return "null";
            String retval = host + "/portal/page/" + URLEncoder.encode(toolId);
            if (session != null)
                retval = retval + "?sakai.session="
                        + URLEncoder.encode(session);
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
        if (autoLogin == null) autoLogin = "x"; // Don't care
        String remoteUser = pUser.getUsername(request);

        // System.out.println("autoLogin = " + autoLogin + " autoDone=" + autoDone
        //         + " remote=" + remoteUser+" session="+session);

        if (siteList == null && autoDone == null && remoteUser != null
                && autoLoginPossible && !autoLogin.equals("0")) {
            // Only do this once, success or failure
            pSession.setAttribute("auto.done", "true");
            siteList = loadSiteList(request, initHost, remoteUser, initSecret,
                    true);
            if (siteList != null)
                pSession.setAttribute("site.list", siteList);
        }

        if (siteList == null) {
            pSession.setAttribute(LAUNCH_STATE, LAUNCH_STATE_LOGIN);
            String sakaiHost = prefs.getValue("sakai.host",initHost);
            request.setAttribute("sakai.host", sakaiHost);

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

	// System.out.println("sakai.tool = "+initTool+" Placement = " + sakaiPlacement);

	// Handle single tool placement display

	// If we are in tool mode and have no placement from preferences, we must
	// get a placement - switch into Select mode
	if ( initTool != null && sakaiPlacement == null ) {
		// Add the list of sites which contain the tools to the request
		int numTools = addToolList(request,siteList);
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
        	sakaiPlacement  = (String) pSession.getAttribute("sakai.placement");
	}

        pSession.setAttribute(LAUNCH_STATE, LAUNCH_STATE_MAIN);
	
	// We are ready to produce a response
        PrintWriter out = response.getWriter();

	// Single tool mode
	if ( sakaiPlacement != null ) {
	    String thisUrl = host + "/portal/page/" + URLEncoder.encode(sakaiPlacement)
                + "?panel=Main&sakai.session=" + URLEncoder.encode(session);
            out
                    .println("<iframe frameborder=\"0\" marginwidth=\"0\" marginheight=\"0\" scrolling=\"auto\"");
            out.println("width=\"100%\" height=\"" + theHeight + "px\"");
            out.println("src=\"" + thisUrl + "\"></iframe>");
	    return;
        }

	// Gallery Mode
	// TODO: Some type of logout possibility (or perhaps not - just use edit mode)
	if ( initType.equals("gallery") ) {

	    theUrl = host + "/portal/gallery";
	    if ( session != null ) theUrl = theUrl + "?sakai.session=" + URLEncoder.encode(session);

            out.println("<iframe frameborder=\"0\" marginwidth=\"0\" marginheight=\"0\" scrolling=\"auto\"");
            out.println("width=\"100%\" height=\"" + theHeight + "px\"");
            out.println("src=\"" + theUrl + "\"></iframe>");
	    return;
	}
	
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

		// Sites do not look good because there are two columns of buttons
	        // out.print("  ['"+getTag(site ,"title")+"','"+getTag(site ,"url")+"'");
	        out.print("  ['"+getTag(site ,"title")+"',0");
		NodeList pages = site .getElementsByTagName("page");
		if ( pages.getLength() > 0 ) out.print(",\n");

		for (int j=0; j < pages.getLength(); j++ ) {
			if ( j > 0 ) out.print(",\n");
			Element page = (Element) pages.item(j);
			theUrl = getTag(page ,"url");

	    		if ( session != null ) theUrl = theUrl + "?sakai.session=" + URLEncoder.encode(session);

			if ( firstUrl == null ) firstUrl = theUrl;

	        	out.print("    ['"+getTag(page ,"title")+"','"+theUrl+"'");
			NodeList tools = page.getElementsByTagName("tool");
			if ( tools.getLength() > 1 ) {
				out.print(",\n");

				for (int k=0; k < tools.getLength();k++) {
					if ( k > 0 ) out.print(",\n");
					Element tool = (Element) tools.item(k);
					theUrl = getTag(tool ,"url");
	    				if ( session != null ) theUrl = theUrl + "?sakai.session=" + URLEncoder.encode(session);
	        			out.print("      ['"+getTag(tool ,"title")+"','"+theUrl+"']");
				}
			}
			out.println("\n    ]");
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

/*
	// Keep this as an example, because it can do Logout within the window
        PortletURL url = response.createActionURL();
        out
                .println("<table border=0 cellspacing=0 cellpadding=2 width=\"100%\"><tr>");
        out
                .println("<td bgcolor=\"#666699\"><font face=\"sans-serif\" color=\"#FFFFFF\" size=\"+1\">");
        out.println("<FORM METHOD=POST ACTION=\"" + url.toString() + "\">");
        out
                .println("<select name=site.index onchange=\"JavaScript:submit()\">");
        out.println("<option value=select>Select Sakai Site</option>");
*/
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
            if (sakaiAuto != null && sakaiAuto.equals("1")) {
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
        pSession.removeAttribute(LAUNCH_STATE);

        // System.out.println("processAction mode=" + request.getPortletMode());
        // System.out.println("state = " + launchState);

        if (request.getPortletMode().equals(PortletMode.VIEW)) {
            if (launchState != null && launchState.equals(LAUNCH_STATE_LOGIN)) {
                processActionLogin(request, response);
                return;
            }
            if (launchState != null && launchState.equals(LAUNCH_STATE_MAIN)) {
                processActionMain(request, response);
                return;
            }
            if (launchState != null && launchState.equals(LAUNCH_STATE_SELECT)) {
                processActionSelect(request, response);
                return;
            }
        } else if (request.getPortletMode().equals(PortletMode.EDIT)) {
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

	// After login in clear out placement
	pSession.removeAttribute("sakai.placement");
	pSession.removeAttribute("sakai.host");

        List allSites = loadSiteList(request, sakaiHost, sakaiId, sakaiPw,
                false);
        String errorStr = (String) pSession.getAttribute("error");

        // System.out.println("Error from load=" + errorStr);

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
        if (strIndex.equals("logout")) {
            pSession.removeAttribute("site.list");
            pSession.removeAttribute("site.index");
            pSession.removeAttribute("sakai.placement"); 
        } else {
            pSession.setAttribute("site.index", strIndex);
        }
        System.out.println("PAM index=" + strIndex);
    }

    public List loadSiteList(PortletRequest request, String sakaiHost,
            String sakaiId, String sakaiPw, boolean doAuto) {
        // System.out.println("loadSiteList host=" + sakaiHost + " id=" + sakaiId + " pw=" + sakaiPw + " auto=" + doAuto);

        PortletSession pSession = request.getPortletSession();
        String endpoint;
        String session = null;
        String siteList = null;

        try {
            pSession.removeAttribute("site.list");
            pSession.removeAttribute("sakai.session");
            pSession.removeAttribute("error");
            pSession.removeAttribute("sakai.host");
	    // Do not remove this so we don't keep forcing re-choosing
            // pSession.removeAttribute("sakai.placement"); 

            String axisPoint = sakaiHost + "/sakai-axis/";

            Service service = new Service();
            Call call = (Call) service.createCall();

            // Get user's information
            String firstName = pUser.getFirstName(request);
            String lastName = pUser.getLastName(request);
            String email = pUser.getEmail(request);

	    //HACK FOR DEMO
	    if ( sakaiId.equals("tomcat") && firstName == null ) {
		firstName = "Tom";
		lastName = "Cat";
		email = "tom@cat.com";
	    }

            // System.out.println("firstname=" + firstName + ", lastName=" + lastName + ", email=" + email);

            if (doAuto && email != null && firstName != null
                    && lastName != null) {
                endpoint = axisPoint + "SakaiPortalLogin.jws";
                System.out.println("Portal Login and Create " + endpoint);
                call.setOperationName("loginAndCreate");
                call.setTargetEndpointAddress(new java.net.URL(endpoint));
                session = (String) call.invoke(new Object[] { sakaiId, sakaiPw,
                        firstName, lastName, email });
            } else if ( doAuto ) {
                    endpoint = axisPoint + "SakaiPortalLogin.jws";
                    System.out.println("Portal Login " + endpoint);
                    call.setOperationName("login");
                    call.setTargetEndpointAddress(new java.net.URL(endpoint));
                    session = (String) call.invoke(new Object[] { sakaiId, sakaiPw });
            } else {
                endpoint = axisPoint + "SakaiLogin.jws";
                call.setOperationName("login");
                call.setTargetEndpointAddress(new java.net.URL(endpoint));
                session = (String) call
                        .invoke(new Object[] { sakaiId, sakaiPw });
            }

            // System.out.println("Login successful...");

            if (session == null || session.length() < 2) {
                System.out.println("Unable to establish session to "
                        + sakaiHost);
                pSession.setAttribute("error",
                        "Unable to establish session to " + sakaiHost);
                return null;
            }

            service = new Service();
            call = (Call) service.createCall();

            call.setTargetEndpointAddress(new java.net.URL(axisPoint
                    + "SakaiSite.jws"));
            call.setOperationName("getToolsDom");

            siteList = (String) call.invoke(new Object[] { session, "",
                    new Integer(1), new Integer(9999) });

            Document doc = Xml.readDocumentFromString(siteList);

            pSession.setAttribute("site.doc", doc);

            NodeList children = doc.getElementsByTagName("site");
            // System.out.println("There are " + children.getLength() + " child elements.\n");

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

		// System.out.println(theSite.toStringFull());
            }

            pSession.setAttribute("site.list", allSites);
            pSession.setAttribute("sakai.session", session);
            pSession.setAttribute("sakai.host", sakaiHost);
            return allSites;

        } catch (Exception e) {
            pSession.setAttribute("error", "Error retrieving site list: "
                    + e.toString());
            System.out.println("Exception:" + e.toString());
            e.printStackTrace();
            return null;
        }
    }


        /**
         * Get an InputStream for a particular file name - first check the sakai.home area and then 
         * revert to the classpath.
         *
         * This is a utility method used several places.
         */
        public static java.io.InputStream getConfigStream(String fileName, Class curClass)
        {
		// Within Sakai default path is usually tomcat/sakai/file.properties
		// Sakai deployers can move this.

		// When we area not in Sakai's JVM, this may be several places
		// depending on the JVM/OS, etc
		//  - the directory where we started Tomcat
		//  - the user's hojme directory
		//  - the root directory of the system
		// Also the user can start the portal JVN with -Dsakai.home= to force this path
		
                String sakaiHome = System.getProperty("sakai.home");
                String filePath = sakaiHome + fileName;
		// System.out.println("filePath="+filePath);

                try
                {
                        java.io.File f = new java.io.File(filePath);
                        if (f.exists())
                        {
                                return new java.io.FileInputStream(f);
                        }
                }
                catch (Throwable t)
                {
                        // Not found in the sakai.home area
                }

		// See if we can find this property file relative to a  class loader
                if ( curClass == null ) return null;

                java.io.InputStream istream = null;

		// TODO: Figure out *where* the file really needs to go to 
		// trigger this first section of code. It would be cool
		// to have this be shared/lib or somewhere - I just cannot
		// figure this out at this point - Chuck

                // Load from the class loader
                istream = curClass.getClassLoader().getResourceAsStream(fileName);
                if ( istream != null ) return istream;

                // Load from the webapp class relative
		// tomcat/webapps/sakai-webapp/WEB-INF/classes/org/sakaiproject/this/class/file.properties
                istream = curClass.getResourceAsStream(fileName);
                if ( istream != null ) return istream;

                // Loading from the webapp class at the root
		// tomcat/webapps/sakai-webapp/WEB-INF/classes/file.properties
                istream = curClass.getResourceAsStream("/"+fileName);
                return istream;
        }

}
