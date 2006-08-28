#! /bin/csh

# csh makeweb.csh pluto-1.0
# csh makeweb.csh pluto-1.1

echo "<!-- Start of auto-generated  servlet entries -->"


if ( $1 == "pluto-1.1" ) then
  echo "<!--  csh makeweb.csh pluto-1.1 -->"
else if ( $1 == "pluto-1.0" ) then
  echo "<!--  csh makeweb.csh pluto-1.0 -->"
else
  echo "csh makeweb.csh pluto-1.1 or csh makeweb pluto-1.0"
  echo
endif
echo " "

# servlet entries
foreach i ( `grep portlet-name portlet.xml |sed 's/.*<portlet-name>//' | sed 's/<.portlet.*//'` )
  if ( $1 == "pluto-1.1" ) then
    echo "  <servlet>"
    echo "    <servlet-name>$i</servlet-name>"
    echo "    <servlet-class>org.apache.pluto.core.PortletServlet</servlet-class>"
    echo "    <init-param>"
    echo "    <param-name>portlet-name</param-name>"
    echo "    <param-value>$i</param-value>"
    echo "    </init-param>"
    echo "    <load-on-startup>1</load-on-startup>"
    echo "  </servlet>"
    echo " "
  else if ( $1 == "pluto-1.0" ) then
    echo "  <servlet>"
    echo "    <servlet-name>$i</servlet-name>"
    echo "    <display-name>$i (Pluto Invoker)</display-name>"
    echo "    <description>Auto Generated Portlet Invoker Servlet</description>"
    echo "    <servlet-class>org.apache.pluto.core.PortletServlet</servlet-class>"
    echo "    <init-param>"
    echo "      <param-name>portlet-class</param-name>"
    echo "      <param-value>org.sakaiproject.portlets.SakaiLaunch</param-value>"
    echo "    </init-param>"
    echo "    <init-param>"
    echo "      <param-name>portlet-guid</param-name>"
    echo "      <param-value>sakai-portlets.$i</param-value>"
    echo "    </init-param>"
    echo "  </servlet>"
    echo " "
  else if ( $1 == "portal-1.1" ) then
    echo "      <portlet name="\"$i\""/>"
  else 
    echo $i
  endif 
end

#servlet-mapping entries
foreach i ( `grep portlet-name portlet.xml |sed 's/.*<portlet-name>//' | sed 's/<.portlet.*//'` )
  if ( $1 == "pluto-1.1" ) then
    echo "  <servlet-mapping>"
    echo "      <servlet-name>$i</servlet-name>"
    echo "      <url-pattern>/PlutoInvoker/$i</url-pattern>"
    echo "  </servlet-mapping>"
  else if ( $1 == "pluto-1.0" ) then
    echo "  <servlet-mapping>"
    echo "      <servlet-name>$i</servlet-name>"
    echo "      <url-pattern>/$i/*</url-pattern>"
    echo "  </servlet-mapping>"
  else if ( $1 == "portal-1.1" ) then
    echo "    <page name="\"$i\" uri=\"/WEB-INF/themes/pluto-default-theme.jsp\"">"
    echo "      <portlet context="\"/sakai-portlets\" name=\"$i\""/>"
    echo "    </page>"
  endif
end

echo "<!-- End of auto-generated  servlet entries -->"

