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
Which Site would you like?
<br>
<%=renderRequest.getAttribute("sakai.options")%>
</center>
<br>

<center>
    <input type="submit" name="Submit" onClick="finish.value='true';" value="Finished"> 
    <input type="reset" onClick="reset.value='true';"  value="Reset">
<br>
<p>

</form>
