<%@ page contentType="text/html; charset=UTF-8" %>
<%--
  -
  - Copyright (C) 2004-2008 Jive Software. All rights reserved.
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
--%>

<%@ page import="org.jivesoftware.util.*,
                 org.jivesoftware.openfire.*,
                 org.igniterealtime.openfire.plugin.xep0418.XEP0418Plugin"
    errorPage="error.jsp"
%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<jsp:useBean id="webManager" class="org.jivesoftware.util.WebManager"  />
<% webManager.init(request, response, session, application, out ); %>

<html>
<head>
<title><fmt:message key="xep0418.settings.title"/></title>
<meta name="pageID" content="server-dnsoverxmpp-id"/>
</head>
<body>

<%  // Get parameters:
    boolean update = request.getParameter("update") != null;

    boolean pluginEnabled = ParamUtils.getParameter(request,"pluginEnabled")!=null&&ParamUtils.getParameter(request,"pluginEnabled").equals("on")?true:false;
    boolean filterEnabled = ParamUtils.getParameter(request,"filterEnabled")!=null&&ParamUtils.getParameter(request,"filterEnabled").equals("on")?true:false;
    boolean customDNSEnabled = ParamUtils.getParameter(request,"customDNSEnabled")!=null&&ParamUtils.getParameter(request,"customDNSEnabled").equals("on")?true:false;

    String hostv4= ParamUtils.getParameter(request,"hostv4")!=null&&ParamUtils.getParameter(request,"hostv4").trim().length()>0?ParamUtils.getParameter(request,"hostv4").trim():"";
    String hostv6= ParamUtils.getParameter(request,"hostv6")!=null&&ParamUtils.getParameter(request,"hostv6").trim().length()>0?ParamUtils.getParameter(request,"hostv6").trim():"";

    Cookie csrfCookie = CookieUtils.getCookie(request, "csrf");
    String csrfParam = ParamUtils.getParameter(request, "csrf");

    if (update) {
        if (csrfCookie == null || csrfParam == null || !csrfCookie.getValue().equals(csrfParam)) {
            update = false;
        }
    }
    csrfParam = StringUtils.randomString(15);
    CookieUtils.setCookie(request, response, "csrf", csrfParam, -1);
    pageContext.setAttribute("csrf", csrfParam);

    if (update) {
        XEP0418Plugin.XMPP_DNSOVERXMPP_ENABLED.setValue(pluginEnabled);
        XEP0418Plugin.XMPP_DNSOVERXMPP_SYSTEM_DNS_ENABLED.setValue(customDNSEnabled);
        XEP0418Plugin.XMPP_DNSOVERXMPP_FILTER_ENABLED.setValue(filterEnabled);
        XEP0418Plugin.XMPP_DNSOVERXMPP_CUSTOM_DNS4.setValue(hostv4);
        XEP0418Plugin.XMPP_DNSOVERXMPP_CUSTOM_DNS6.setValue(hostv6);
        // Log the event
        webManager.logEvent((pluginEnabled ? "enabled" : "disabled")+" xep0418", null);
    %>
    <div class="jive-success">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr><td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0" alt=""></td>
        <td class="jive-icon-label">
        <fmt:message key="xep0418.settings.update" />
        </td></tr>
    </tbody>
    </table>
    </div><br>
    <%
    
    }

    // Set page vars
    pluginEnabled = XEP0418Plugin.XMPP_DNSOVERXMPP_ENABLED.getValue();
    filterEnabled = XEP0418Plugin.XMPP_DNSOVERXMPP_FILTER_ENABLED.getValue();
    customDNSEnabled = XEP0418Plugin.XMPP_DNSOVERXMPP_SYSTEM_DNS_ENABLED.getValue();

    hostv4= XEP0418Plugin.XMPP_DNSOVERXMPP_CUSTOM_DNS4.getValue();
    hostv6= XEP0418Plugin.XMPP_DNSOVERXMPP_CUSTOM_DNS6.getValue();

%>

<!-- BEGIN 'Set Avatarconversion Policy' -->
<form action="dnsoverxmpp.jsp">
    <input type="hidden" name="csrf" value="${csrf}">
    <div class="jive-contentBoxHeader">
        <fmt:message key="xep0418.settings.title" />
    </div>
    <div class="jive-contentBox">
        <p>
            DNS over HTTP(S) endpoint: <b>http(s)://domain:port/doh/dns-query?dns=<base64url>[&format=text]</b><br> 
            port depends on Openfire's http-binding preferences<br>
            format = optional (text=readable text)
        </p>
        <br><br>
        <table cellpadding="3" cellspacing="0" border="0">
        <tbody>
            <tr valign="top">
                
                <td >
                    <label for="pluginEnabled">
                     <b><fmt:message key="xep0418.settings.enable" /></b> -
                     <fmt:message key="xep0418.settings.enable_info" />
                    </label>
                </td>
                <td  nowrap>
                    <input type="checkbox" name="pluginEnabled" id="pluginEnabled"  <%=(pluginEnabled?"checked" : "")%>>
                </td>
            </tr>
            <tr valign="top">
                <td>
                    <label for="filterEnabled">
                     <b><fmt:message key="system_property.xmpp.xep0418.filter.enabled" /></b>
                    </label>
                </td>
                <td  nowrap>
                    <input type="checkbox" name="filterEnabled" id="filterEnabled"  <%=(filterEnabled?"checked" : "")%>>
                </td>
            </tr>
            <tr valign="top">
                <td>
                    <label for="customDNSEnabled">
                     <b><fmt:message key="system_property.xmpp.xep0418.usesytemdns.enabled" /></b>
                    </label>
                </td>
                <td  nowrap>
                    <input type="checkbox" name="customDNSEnabled" id="customDNSEnabled"  <%=(customDNSEnabled?"checked" : "")%>>
                </td>
            </tr>
            <tr valign="top">
                <td width="99%">
                    <label for="occupantIdentifiersEnabled">
                     <b><fmt:message key="system_property.xmpp.xep0418.dns4server" /></b>
                    </label>
                </td>
                <td width="1%" nowrap>
                    <input type="text" name="hostv4" id="hostv4"  value="<%=hostv4 %>">
                </td>
            </tr>
            <tr valign="top">
                <td width="99%">
                    <label for="hostv6">
                     <b><fmt:message key="system_property.xmpp.xep0418.dns6server" /></b>
                    </label>
                </td>
                <td width="1%" nowrap>
                    <input type="text" name="hostv6" id="hostv6"  value="<%=hostv6 %>">
                </td>
            </tr>
        </tbody>
        </table>
    </div>
    <input type="submit" name="update" value="<fmt:message key="global.save_settings" />">
</form>
<!-- END 'Set Private Data Policy' -->

</body>
</html>
