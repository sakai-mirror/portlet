<%@ page import="javax.portlet.RenderRequest" %>
<%@ page import="javax.portlet.RenderResponse" %>
<%@ taglib uri="http://java.sun.com/portlet" prefix="portlet" %>
<%@ page import="javax.portlet.PortletURL" %>
<%@ page session="false" %>

<%
RenderResponse rRes = (RenderResponse)request.getAttribute("javax.portlet.response");
PortletURL actionURL = rRes.createActionURL();
%>

<portlet:defineObjects/>

This is a sample of the use of JSR-168 CSS See page 115-117 (Section PLT.C) in the 
<a href=http://jcp.org/en/jsr/detail?id=168 target=_new>JSR-168</a> documentation.
<p/>
<FORM NAME=NOTEPAD METHOD="POST" ACTION="<%=actionURL.toString()%>">
        <input type=hidden name=sakai.form.action value=main>
        <input type=submit value="Go Back"> 
</FORM>
<p/>
<p class="portlet-font">This is regular text with in the portlet body - uses style "portlet-font"</p>
<p class="portlet-font-dim">This is regular text with in the portlet body - uses style "portlet-font-dim"</p>
<p class="portlet-msg-status">This is regular text with in the portlet body - uses style "portlet-msg-status"</p>
<p class="portlet-msg-info">This is regular text with in the portlet body - uses style "portlet-msg-info"</p>
<p class="portlet-msg-error">This is regular text with in the portlet body - uses style "portlet-msg-error"</p>
<p class="portlet-message-alert">This is regular text with in the portlet body - uses style "portlet-message-alert"</p>
<p class="portlet-msg-success">This is regular text with in the portlet body - uses style "portlet-msg-success"</p>
<p class="portlet-section-header">This is a portlet section header - uses style "portlet-section-header"</p>
<p class= "portlet-section-subheader">This is a portlet section sub-header - uses style "portlet-section-subheader"</p>
<p class= "portlet-section-alternate">This is a portlet section body - uses style "portlet-section-alternate"</p>
<p class= "portlet-section-selected">This is a portlet section body - uses style "portlet-section-selected"</p>
<p class= "portlet-section-subheader">This is a portlet section body - uses style "portlet-section-subheader"</p>
<p class= "portlet-section-footer">This is a portlet section body - uses style "portlet-section-footer"</p>
<p class= "portlet-section-text">This is a portlet section body - uses style "portlet-section-text"</p>
<center>
<table width="90%" border="1">  
<tr class="portlet-table-header">    
<td>This is a table heading - uses style"portlet-table-header"</td>    
<td colspan="2">Heading 1</td>    <td>Heading 2</td>    <td>Heading 3</td>
</tr>
<tr class="portlet-table-subheader">    
<td>This is a table sub-heading - uses style "portlet-table-subheader"</td>    
<td>Sub-heading 1</td>    <td>Sub-heading 2</td>    <td>&nbsp;</td>    <td>&nbsp;</td>
</tr>
<tr class="portlet-table-body">    
<td>This is table body text - uses style "portlet-table-body"</td>    
<td>Table body text</td>    <td>Table body text</td>    
<td>Table body text</td>    <td>Table body text</td>
</tr>
</table>
</center>
<form><p align="center" class="portlet-section-header">A Sample Form</p>
<center><table>
<tr><td class="portlet-form-label" colspan="2">This is a form label - uses style "portlet-form-label"</td></tr>
<tr><td class="portlet-form-field-label">This is text for a form field - uses style "portlet-form-field-label"</td><td><input name="textfield" type="text" class="portlet-form-input-field"value='This is user input in a text field - uses style"portlet-form-input-field"'/></td></tr>
<tr><td class="portlet-form-label" colspan="2" align="center"><input name="Submit" type="button" class="portlet-form-button" value='Text on a form button - uses style "portlet-form-button"'/></td></tr>
<!-- I am not sure if I used this class properly /Chuck -->
<tr><td class="portlet-form-label" colspan="2" align="center"><input name="Check" type="checkbox"/><span class="portlet-form-field">Text
for a field (not input field, e.g. checkboxes, etc.) - "portlet-form-field"</span></td></tr>
</table>
<form><p align="center" class="portlet-icon-label">Text that appears beside a context dependent action icon - "portlet-icon-label"</p>
<form><p align="center" class="portlet-dlg-icon-label">Text that appears beside a "standard" icon (e.g. Ok or Cancel) - "portlet-dlg-icon-label"</p>
</center></form>
