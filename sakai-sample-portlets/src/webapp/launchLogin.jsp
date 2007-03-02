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
    <td bgcolor="#666699"><font face="sans-serif" color="#FFFFFF" size="+1"><b>Sakai Login</b></font></td>
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
    <td align="right" nowrap><font face="sans-serif" size=+0><b><LABEL FOR="sakai.id">Login Account: </LABEL></b></font></td>
    <td><font face="sans-serif" size=+0><input type="text" name="sakai.id" value="<%=renderRequest.getAttribute("sakai.id")%>" ID="sakai.id"></font></td>
  </tr>
  <tr>
    <td align="right" nowrap><font face="sans-serif" size=+0><b><LABEL FOR="sakai.pw">Password: </LABEL></b></font></td>
    <td><font face="sans-serif" size=+0><input type="text" name="sakai.pw" value="<%=renderRequest.getAttribute("sakai.pw")%>" ID="sakai.host"></font></td>
  </tr>
</table>
</center>
<br>

<center>
    <input type="submit" name="Submit" onClick="finish.value='true';" value="Log In"> 
    <input type="reset" onClick="reset.value='true';"  value="Reset">
<br>
<p>

</form>
