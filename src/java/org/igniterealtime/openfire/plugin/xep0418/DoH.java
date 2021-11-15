package org.igniterealtime.openfire.plugin.xep0418;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Message;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.ResolverConfig;
import org.xbill.DNS.SimpleResolver;

/**
 * Generates a JSON object that contains configuration for the web application.
 *
 * 
 */
public class DoH extends HttpServlet {
    private static final long serialVersionUID = 8631700283494910231L;
    @SuppressWarnings("unused")
    private static final Logger Log = LoggerFactory.getLogger(DoH.class);



    class MyResponse {
        public Message data=null;
        public long ttl = 300;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Log.info("Processing DNS over HTTP(S) Query - Post");
        try {
            BufferedInputStream bin = new BufferedInputStream(request.getInputStream());
            int nRead;
            byte[] data = new byte[16384];
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            while ((nRead = bin.read(data, 0, data.length)) != -1) {
              buffer.write(data, 0, nRead);
            }

            response.setContentType("application/dns-message");

            byte[] brequest =  buffer.toByteArray();
            MyResponse res = getDNSResponse(brequest);
            if (res!=null)
            {
                Log.info("Send response");
                response.addHeader("cache-control","max-age="+String.valueOf(res.ttl));
                response.addHeader("content-length",String.valueOf(res.data.toWire().length));
                BufferedOutputStream bout = new BufferedOutputStream(response.getOutputStream());
                bout.write(res.data.toWire());
                bout.flush();
                bout.close();
            }
            else {
                response.sendError(500, "Error while processing dns query");
            }
        }
        catch (Exception e) {
            Log.error(e.getMessage(),e);
            response.sendError(500, e.getMessage());
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        Log.debug("Processing DNS over HTTP(S) Query - GET");
        String dns = request.getParameter("dns");
        String format = request.getParameter("format");
        if (dns==null||dns.trim().length()==0)
        {
            Log.error("dns parameter = null or length = 0");
            response.sendError(400,"Bad Request");
            return;
        }

        response.setContentType("application/dns-message");

        try {
            Log.debug("Convert base64 to byte[]");
            byte[] brequest = convertToByteArray(dns);
            MyResponse res = getDNSResponse(brequest);
            if (res!=null)
            {
                Log.debug("Send response");
                response.addHeader("cache-control","max-age="+String.valueOf(res.ttl));
               
                BufferedOutputStream bout = new BufferedOutputStream(response.getOutputStream());
                if (format!=null&&format.equalsIgnoreCase("text"))
                {
                    response.addHeader("content-length",String.valueOf(res.data.toString().getBytes().length));
                    bout.write(res.data.toString().getBytes());
                }
                else {
                    response.addHeader("content-length",String.valueOf(res.data.toWire().length));
                    bout.write(res.data.toWire());
                }
                bout.flush();
                bout.close();
            }
            else {
                response.sendError(500, "Error while processing dns query");
            }
        }
        catch (Exception e) {
            Log.error(e.getMessage(),e);
            response.sendError(500, e.getMessage());
        }
    }

    private byte[] convertToByteArray(String data)
    {
        try {
            Log.debug("try decoding with urldecoder");
            return java.util.Base64.getUrlDecoder().decode(data.trim());
        }
        catch (Exception e) {
            Log.debug("try decoding with decoder");
            return java.util.Base64.getDecoder().decode(data.trim());
        }
    }

    private MyResponse getDNSResponse(byte[] brequest) {
        try 
        {
            Log.debug("Parse dns query from byte[]");
            Message msgrequest = new Message(brequest);
            if (msgrequest.getQuestion()==null)
            { 
                Log.error(msgrequest.toString());
                return null;
            }
            Resolver r = null; 
            String hostv4 = null;
            String hostv6 = null;
            Log.debug("Loading systems dns servers");
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

            Log.debug("System: hostv4="+hostv4+" | hostv6="+hostv6);

            if (msgrequest.getQuestion().getType()==org.xbill.DNS.Type.AAAA) {
                Log.debug("Request AAAA Record");
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
                Log.debug("Using hostv6="+hostv6);
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
                Log.debug("Using hostv4="+hostv4);
                r = new SimpleResolver(hostv4);
            }

            Log.debug("Do DNS lookup now...");
            Message msg = r.send(msgrequest);
            MyResponse result = new MyResponse();
            Log.debug("Response: "+msg.toString());
            result.data=msg;
            result.ttl=msg.getSection(1).size()>0?msg.getSection(1).get(0).getTTL():300;
            return result;
        }
        catch (Exception e)
        {
            Log.error(e.getMessage(),e);
            return null;
        }
    }
}
