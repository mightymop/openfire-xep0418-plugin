package org.igniterealtime.openfire.plugin.xep0418;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;

import org.dom4j.Element;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Message;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.ResolverConfig;
import org.xbill.DNS.SimpleResolver;
import org.xmpp.packet.IQ;
import org.xmpp.packet.IQ.Type;
import org.xmpp.packet.JID;

public class XEP0418IQHandler extends IQHandler
{
    private static final Logger Log = LoggerFactory.getLogger(XEP0418IQHandler.class);

    //XEP-0418
    public static String NAMESPACE_XEP0418="urn:xmpp:dox:0";

    public static String NAMESPACE_DISCO_INFO = "http://jabber.org/protocol/disco#info";

    private IQHandlerInfo info;

    //Constructors
    public XEP0418IQHandler()
    {
        super(NAMESPACE_XEP0418);
        this.info = new IQHandlerInfo("query", NAMESPACE_XEP0418);
    }

    @Override
    public IQHandlerInfo getInfo() {
        return info;
    }

    @Override
    public IQ handleIQ(IQ iq) throws UnauthorizedException 
    {
        IQ result=null;
        if (XEP0418Plugin.XMPP_DNSOVERXMPP_ENABLED.getValue())
        {
           if (iq.getType()==Type.get&&iq.getChildElement()!=null&&iq.getChildElement().getNamespaceURI().equalsIgnoreCase(NAMESPACE_XEP0418))
           {
               Element dns = null;
               if ((dns=iq.getElement().element("dns"))!=null&&dns.hasContent())
               {
                   if (!iq.getTo().toBareJID().equalsIgnoreCase(XMPPServer.getInstance().getServerInfo().getXMPPDomain())&&
                       XEP0418Plugin.XMPP_DNSOVERXMPP_FILTER_ENABLED.getValue())
                   {
                       return getError(iq.getFrom(),NAMESPACE_XEP0418,"cancel","503","service-unavailable","urn:ietf:params:xml:ns:xmpp-stanzas");
                   }

                   Log.debug("Processing incoming dns query, we have content and resolving the query (XEP-0418)");
                   String data = dns.getText();
                   try 
                   {
                       byte[] brequest = java.util.Base64.getDecoder().decode(data);
                       Message msgrequest = new Message(brequest);
                       if (msgrequest.getQuestion()==null)
                       { 
                           Log.error(msgrequest.toString());
                           return getError(iq.getFrom(),NAMESPACE_XEP0418,"modify","400","bad-request","urn:ietf:params:xml:ns:xmpp-stanzas");
                       }
                       Resolver r = null; 
                       String hostv4 = null;
                       String hostv6 = null;
                       List <InetSocketAddress> dnsServers = ResolverConfig.getCurrentConfig().servers();

                       for (InetSocketAddress server : dnsServers)
                       {
                           if (server.getAddress() instanceof Inet6Address && hostv6==null && server.getAddress().getHostAddress().contains(":"))
                           {
                               hostv6 = server.getAddress().getHostAddress();
                               continue;
                           }
                           if (server.getAddress() instanceof InetAddress && hostv4==null && server.getAddress().getHostAddress().contains("."))
                           {
                               hostv4 = server.getAddress().getHostAddress();
                               continue;
                           }
                       }

                       if (msgrequest.getQuestion().getType()==org.xbill.DNS.Type.AAAA) {
                           if (!XEP0418Plugin.XMPP_DNSOVERXMPP_SYSTEM_DNS_ENABLED.getValue())
                           {
                               hostv6=XEP0418Plugin.XMPP_DNSOVERXMPP_CUSTOM_DNS6.getValue()!=null&&
                                      XEP0418Plugin.XMPP_DNSOVERXMPP_CUSTOM_DNS6.getValue().trim().length()>0?
                                      XEP0418Plugin.XMPP_DNSOVERXMPP_CUSTOM_DNS6.getValue():hostv6;
                           }
                           if (hostv6==null)
                           {
                               hostv6=XEP0418Plugin.FALLBACKHOSTV6;
                           }
                           r = new SimpleResolver(hostv6);

                       }
                       else 
                       {
                           if (!XEP0418Plugin.XMPP_DNSOVERXMPP_SYSTEM_DNS_ENABLED.getValue())
                           {
                               hostv4=XEP0418Plugin.XMPP_DNSOVERXMPP_CUSTOM_DNS4.getValue()!=null&&
                                      XEP0418Plugin.XMPP_DNSOVERXMPP_CUSTOM_DNS4.getValue().trim().length()>0?
                                      XEP0418Plugin.XMPP_DNSOVERXMPP_CUSTOM_DNS4.getValue():hostv4;
                           }
                           if (hostv4==null)
                           {
                               hostv4=XEP0418Plugin.FALLBACKHOSTV4;
                           }
                           r = new SimpleResolver(hostv4);
                       }

                       Message msg = r.send(msgrequest);
                       byte[] bresponse = msg.toWire();
                       String responsedata = java.util.Base64.getEncoder().withoutPadding().encodeToString(bresponse);

                       dns.setText(responsedata);
                       result = IQ.createResultIQ(iq);
                   }
                   catch (Exception e)
                   {
                       Log.error(e.getMessage(),e);
                       return getError(iq.getFrom(),NAMESPACE_XEP0418,"modify","400","bad-request","urn:ietf:params:xml:ns:xmpp-stanzas");
                   }
               }
               else {
                   return getError(iq.getFrom(),NAMESPACE_XEP0418,"modify","400","bad-request","urn:ietf:params:xml:ns:xmpp-stanzas");
               }
           }
           else {
               return getError(iq.getFrom(),NAMESPACE_XEP0418,"modify","400","bad-request","urn:ietf:params:xml:ns:xmpp-stanzas");
           }
       }
       else {
           return getError(iq.getFrom(),NAMESPACE_XEP0418,"cancel","503","service-unavailable","urn:ietf:params:xml:ns:xmpp-stanzas");
       }
       return result;
    }

    private static IQ getError(JID to, String namespace, String type, String code, String errorelement, String errornamespace) {
        IQ result= new IQ(Type.error);
        result.setTo(to);
        result.setFrom(new JID(XMPPServer.getInstance().getServerInfo().getXMPPDomain()));
        Element error = result.setChildElement("error",namespace);
        error.addAttribute("type", type);
        error.addAttribute("code", code);
        error.addElement(errorelement,errornamespace);
        return result;
    }
}
