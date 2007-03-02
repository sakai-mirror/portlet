<%@ page import="javax.portlet.RenderRequest" %>
<%@ page import="javax.portlet.RenderResponse" %>
<%@ taglib uri="http://java.sun.com/portlet" prefix="portlet" %>
<%@ page session="false" %>

<portlet:defineObjects/>

<%
        String error = renderRequest.getParameter("error");
        if (error!=null) {
%>
        <font color="red"><b>ERROR: </b><%=error%>
        </font>
<%
        }
%>

<form method="post" action="<portlet:actionURL/>">
<table border=0 cellspacing=0 cellpadding=2 width="100%">
  <tr>                
    <td bgcolor="#666699"><font face="sans-serif" color="#FFFFFF" size="+1"><b>Sakai Launch Preferences</b></font></td>
  </tr>
  <tr>
    <td>&nbsp;</td>
  </tr>
</table>

<center>
<table border=0 cellspacing=1 cellpadding=0 width="40%" align="center">
  <tr>
    <td align="right" nowrap><font face="sans-serif" size=+0><b><LABEL FOR="sakai.host">Sakai Host: </LABEL></b></font></td>
    <td><font face="sans-serif" size=+0><input type="text" name="sakai.host" value="<%=renderRequest.getAttribute("sakai.host")%>" ID="sakai.host"></font></td>
  </tr>
  <tr>
    <td align="right" nowrap><font face="sans-serif" size=+0><b><LABEL FOR="sakai.id">Account: </LABEL></b></font></td>
    <td><font face="sans-serif" size=+0><input type="text" name="sakai.id" value="<%=renderRequest.getAttribute("sakai.id")%>" ID="sakai.id"></font></td>
  </tr>
<% if ( renderRequest.getAttribute("do.auto") != null ) { %>
  <tr>
    <td align="right" nowrap><font face="sans-serif" size=+0><b><LABEL FOR="sakai.auto">Auto Login:</LABEL></b></font></td>
    <td><font face="sans-serif" size=+0><input type="checkbox" name="sakai.auto" 
<% if ( renderRequest.getAttribute("sakai.auto") != null ){ %>
          CHECKED
<% } // end sakai.auto %>
          ID="sakai.auto"></font></td>
  </tr>
<% } // end do.auto %>
  <tr>
    <td align="right" nowrap><font face="sans-serif" size=+0><b><LABEL FOR="sakai.height">Frame Height (px): </LABEL></b></font></td>
    <td><font face="sans-serif" size=+0><input type="text" name="sakai.height" value="<%=renderRequest.getAttribute("sakai.height")%>" ID="sakai.height"></font></td>
  </tr>
</table>
</center>
<br>

<center>
    <input type="submit" name="Submit" onClick="finish.value='true';" value="Finished"> 
    <input type="reset" onClick="reset.value='true';"  value="Reset">
<br>
<p>

</form>
