package org.igniterealtime.openfire.plugin.xep0418;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.SimpleInstanceManager;
import org.eclipse.jetty.apache.jsp.JettyJasperInitializer;
import org.eclipse.jetty.plus.annotation.ContainerInitializer;
import org.eclipse.jetty.webapp.WebAppContext;
import org.jivesoftware.admin.AuthCheckFilter;
import org.jivesoftware.openfire.IQRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.http.HttpBindManager;
import org.jivesoftware.util.SystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An Openfire plugin that integrates XEP-0418 and DNS over HTTPS.
 *
 * @author 
 */
public class XEP0418Plugin implements Plugin
{
    private static final Logger Log = LoggerFactory.getLogger( XEP0418Plugin.class );

    private XEP0418IQHandler xep0418Handler = null;
    private IQRouter iqRouter;

    public static final SystemProperty<Boolean> XMPP_DNSOVERXMPP_ENABLED = SystemProperty.Builder.ofType(Boolean.class)
            .setKey("xmpp.xep0418.enabled")
            .setPlugin( "xep0418" )
            .setDefaultValue(false)
            .setDynamic(true)
            .build();

    public static final SystemProperty<Boolean> XMPP_DNSOVERXMPP_SYSTEM_DNS_ENABLED = SystemProperty.Builder.ofType(Boolean.class)
            .setKey("xep0418.usesytemdns.enabled")
            .setPlugin( "xep0418" )
            .setDefaultValue(true)
            .setDynamic(true)
            .build();

    public static final SystemProperty<Boolean> XMPP_DNSOVERXMPP_FILTER_ENABLED = SystemProperty.Builder.ofType(Boolean.class)
            .setKey("xep0418.filter.enabled")
            .setPlugin( "xep0418" )
            .setDefaultValue(true)
            .setDynamic(true)
            .build();

    public static final SystemProperty<String> XMPP_DNSOVERXMPP_CUSTOM_DNS6 = SystemProperty.Builder.ofType(String.class)
            .setKey("xep0418.dns6server")
            .setPlugin( "xep0418" )
            .setDefaultValue("")
            .setDynamic(true)
            .build();

    public static final SystemProperty<String> XMPP_DNSOVERXMPP_CUSTOM_DNS4 = SystemProperty.Builder.ofType(String.class)
            .setKey("xep0418.dns4server")
            .setPlugin( "xep0418" )
            .setDefaultValue("")
            .setDynamic(true)
            .build();

    public final static String SERVICE_NAME = "doh";

    public static final String FALLBACKHOSTV6 = "2620:119:53::53";
    public static final String FALLBACKHOSTV4 = "8.8.4.4";

    private WebAppContext context = null;
    private String[] publicResources;
    private void setRessources(String path) {
        publicResources = new String[] { path + "/dns-query" };
    }

    @Override
    public void initializePlugin( PluginManager manager, File pluginDirectory )
    {
        SystemProperty.removePropertiesForPlugin("xep0418");
        Log.info("Initialize XEP-0418 Plugin enabled:"+XMPP_DNSOVERXMPP_ENABLED.getDisplayValue());
        this.iqRouter = XMPPServer.getInstance().getIQRouter();
        this.xep0418Handler = new XEP0418IQHandler();
        this.xep0418Handler.initialize( XMPPServer.getInstance());
        this.xep0418Handler.start();    
        this.iqRouter.addHandler(this.xep0418Handler);
        XMPPServer.getInstance().getIQDiscoInfoHandler().addServerFeature(XEP0418IQHandler.NAMESPACE_XEP0418);

        setRessources(SERVICE_NAME);
        for (final String publicResource : publicResources) {
            AuthCheckFilter.addExclude(publicResource);
        }

        // Add the Webchat sources to the same context as the one that's providing the
        // BOSH interface.
        context = new WebAppContext(null, pluginDirectory.getPath() + File.separator + "classes",
                "/" + SERVICE_NAME);
        context.setClassLoader(this.getClass().getClassLoader());

        // Ensure the JSP engine is initialized correctly (in order to be able to cope
        // with Tomcat/Jasper precompiled JSPs).
        final List<ContainerInitializer> initializers = new ArrayList<>();
        initializers.add(new ContainerInitializer(new JettyJasperInitializer(), null));
        context.setAttribute("org.eclipse.jetty.containerInitializers", initializers);
        context.setAttribute(InstanceManager.class.getName(), new SimpleInstanceManager());

        HttpBindManager.getInstance().addJettyHandler(context);
    }

    @Override
    public void destroyPlugin()
    {
        Log.info("Destroy XEP-0418 Plugin");
        if (context != null) {
            HttpBindManager.getInstance().removeJettyHandler(context);
            context.destroy();
            context = null;
        }

        setRessources(SERVICE_NAME);

        for (final String publicResource : publicResources) {
            AuthCheckFilter.removeExclude(publicResource);
        }

        this.iqRouter.removeHandler(this.xep0418Handler);
        this.xep0418Handler.stop();
        XMPPServer.getInstance().getIQDiscoInfoHandler().removeServerFeature(XEP0418IQHandler.NAMESPACE_XEP0418);
        this.xep0418Handler = null;
    }
}
