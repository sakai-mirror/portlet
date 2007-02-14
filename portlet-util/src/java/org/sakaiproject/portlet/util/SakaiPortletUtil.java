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

package org.sakaiproject.portlet.util;

import java.util.List;
import java.util.Vector;

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

import javax.portlet.PortletRequest;

public class SakaiPortletUtil
{

    private static boolean debugPrintFlag = true;

    public static String getTag(Element theElement, String elementName) {
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

    public static String establishSession(String session, String sakaiHost,
            String sakaiId, String sakaiPw, String firstName, String lastName, String email,  boolean doAuto) {

        String endpoint;
        String siteList = null;
	Service service = null;
	Call call = null;
        String axisPoint = sakaiHost + "/sakai-axis/";

        debugPrint("loadSiteList host=" + sakaiHost + " id=" + sakaiId + " pw=" + sakaiPw + " auto=" + doAuto);

        try {

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

	    // Must establish or have a session - Someday might want to validate session
	    // each time over web services before simply accepting it.
	    if ( session == null ) {
	    
              service = new Service();
              call = (Call) service.createCall();

	      //HACK FOR DEMO
	      if ( "tomcat".equals(sakaiId) && firstName == null ) {
		  firstName = "Tom";
		  lastName = "Cat";
		  email = "tom@cat.com";
	      }

              System.out.println("firstname=" + firstName + ", lastName=" + lastName + ", email=" + email);

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

	    return session;

        } catch (Exception e) {
            System.out.println("Exception:" + e.toString());
            return null;
        }
    }

    public static Document loadSiteList2(String session, String sakaiHost) {

        String endpoint;
        String siteList = null;
	Service service = null;
	Call call = null;
        String axisPoint = sakaiHost + "/sakai-axis/";

        try {

            debugPrint("Retrieving Sites="+session+" host="+sakaiHost);

	    // Retrieve Site List
            service = new Service();
            call = (Call) service.createCall();

	    endpoint = axisPoint + "SakaiSite.jws";

	    debugPrint("Site Retrieval " + endpoint+ " session="+session);

            call.setTargetEndpointAddress(new java.net.URL(endpoint));
            call.setOperationName("getToolsDom");

            siteList = (String) call.invoke(new Object[] { session, "",
                    new Integer(1), new Integer(9999) });

	    debugPrint(siteList);
            Document doc = Xml.readDocumentFromString(siteList);

            NodeList children = doc.getElementsByTagName("site");
            debugPrint("There are " + children.getLength() + " child elements.\n");

            if (children.getLength() < 1) {
                // pSession.setAttribute("error", "No sites available to you on " + sakaiHost);
                return null;
            }
	    return doc;

        } catch (Exception e) {
            System.out.println("Exception:" + e.toString());
            return null;
        }
    }

    public static List parseSiteList(Document doc, String initTool, String sakaiHost, String session) {

            NodeList children = doc.getElementsByTagName("site");
            debugPrint("There are " + children.getLength() + " child elements.\n");

            if (children.getLength() < 1) {
                // pSession.setAttribute("error", "No sites available to you on " + sakaiHost);
                return null;
            }

            List allSites = new Vector();
            for (int i = 0; i < children.getLength(); i++) {
                Element elem = (Element) children.item(i);

                // System.out.println("ID = " + SakaiPortletUtil.getTag(elem, "id"));
                // System.out.println("title = " + SakaiPortletUtil.getTag(elem, "title"));
                SakaiSite theSite = new SakaiSite();
                theSite.host = sakaiHost;
                theSite.session = session;
                theSite.id = SakaiPortletUtil.getTag(elem, "id");
                theSite.title = SakaiPortletUtil.getTag(elem, "title");
		theSite.toolId = null;
		theSite.toolTitle = null;
		// descend into pages -> page -> tools -> tool -> toolid
		boolean found = false;
		if ( initTool == null ) found = true;  // Nothing to find
		NodeList pages = elem.getElementsByTagName("page");
		for (int j=0; ! found && j < pages.getLength(); j++ ) {
			Element page = (Element) pages.item(j);
			NodeList tools = page.getElementsByTagName("tool");
			String pageTitle = (String) SakaiPortletUtil.getTag(page,"title");
			// We prefer the page ID because pages look better and have nice title bars
			// TODO: We could switch to tool IDs and replicate the Charon Page code here
			// That would allow for dynamic resize
			String pageId = (String) SakaiPortletUtil.getTag(page, "id");
			for (int k=0; ! found && k<tools.getLength();k++) {
				Element tool = (Element) tools.item(k);
				String toolId = (String) SakaiPortletUtil.getTag(tool, "toolid");
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

	    debugPrint("Returning="+allSites);
            return allSites;
    }

    public static  boolean isSakaiPortal(PortletRequest request)
    {
        String portalInfo = request.getPortalContext().getPortalInfo();
        return portalInfo.toLowerCase().startsWith("sakai-charon") ;
    }

    private static void debugPrint(String str)
    {
        if ( debugPrintFlag )
        {
                System.out.println(str);
        } else {
                // Should do Log.debug here
        }
    }
}
