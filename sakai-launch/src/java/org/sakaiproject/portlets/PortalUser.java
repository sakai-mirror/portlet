package org.sakaiproject.portlets;

import java.util.Map;

import javax.portlet.PortletRequest;

/**
 * 
 * @author machrist
 * 
 * The gridsphere attribute information is available from the following:
 * http://www.gridsphere.org/gridsphere/docs/FAQ/FAQ.html question #5
 * 
 * The uPortal attribute information is available from
 * http://www.uportal.org/implementors/portlets/workingWithPortlets.html#User_Information
 * Note that with uPortal you need to configure it to export user information to
 * portlets, so the user attribute names used is somewhat arbitrary but here I
 * am trying to stick to the suggestions in the JSR 168 Portlet Standard (PLT.D).
 */
public class PortalUser {

    // If we 
    public static final int UNKNOWN = 0;

    public static final int GRIDSPHERE = 1;

    public static final int UPORTAL = 2;

    private int portalType;

    public PortalUser(int portalType) {
        this.portalType = portalType;
    }

    public String getUsername(PortletRequest request) {
        String username = null;
        Map userInfo = (Map) request.getAttribute(PortletRequest.USER_INFO);

        switch (portalType) {
        case GRIDSPHERE:
            if (userInfo != null) {
                username = (String) userInfo.get("user.name");
            }
            break;
        case UNKNOWN:  
        case UPORTAL:
            username = request.getRemoteUser();
            break;
        }
        // System.out.println("Remote User=" + username);
        return username;
    }

    // for backwards compatibility
    public String getPortalUsername(PortletRequest request) {
        return getUsername(request);
    }

    public String getFirstName(PortletRequest request) {
        String firstName = null;
        Map userInfo = (Map) request.getAttribute(PortletRequest.USER_INFO);

        switch (portalType) {
        case GRIDSPHERE:
            String fullName = getGridsphereFullName(request);
            firstName = fullName.trim().substring(0, fullName.indexOf(" "));
            break;
	case UNKNOWN:
        case UPORTAL:
            if (userInfo != null) {
                firstName = (String) userInfo.get("user.name.given");
            }
            break;
        }
        return firstName;
    }

    public String getLastName(PortletRequest request) {
        String lastName = null;
        Map userInfo = (Map) request.getAttribute(PortletRequest.USER_INFO);

        switch (portalType) {
        case GRIDSPHERE:
            String fullName = getGridsphereFullName(request);
            lastName = fullName.substring(fullName.trim().lastIndexOf(" ") + 1);
            break;
	case UNKNOWN:
        case UPORTAL:
            if (userInfo != null) { 
                lastName =  (String) userInfo.get("user.name.family");
            }
            break;
        }
        return lastName;
    }

    public String getEmail(PortletRequest request) {
        String email = null;
        Map userInfo = (Map) request.getAttribute(PortletRequest.USER_INFO);

        switch (portalType) {
        case GRIDSPHERE:
            if (userInfo != null) {
                email = (String) userInfo.get("user.email");
            }
            break;
	case UNKNOWN:
        case UPORTAL:
            if (userInfo != null) {
                email = (String) userInfo.get("user.home-info.online.email");
            }
        }

        return email;
    }

    private String getGridsphereFullName(PortletRequest request) {
        String fullName = null;
        Map userInfo = (Map) request.getAttribute(PortletRequest.USER_INFO);
        if (userInfo != null) {
            fullName = (String) userInfo.get("user.name.full");
        }
        return fullName;
    }
}
