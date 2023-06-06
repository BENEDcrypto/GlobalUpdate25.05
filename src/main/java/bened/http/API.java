/******************************************************************************
 * Copyright © 2013-2016 The Nxt Core Developers.                             *
 *                                                                            *
 * See the AUTHORS.txt, DEVELOPER-AGREEMENT.txt and LICENSE.txt files at      *
 * the top-level directory of this distribution for the individual copyright  *
 * holder information and the developer policies on copyright and licensing.  *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * Nxt software, including this file, may be copied, modified, propagated,    *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

package bened.http;

import bened.Constants;
import bened.Bened;
import bened.crypto.SSLCert;
import bened.util.Convert;
import bened.util.Logger;
import bened.util.ThreadPool;
import bened.util.UPnP;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static bened.http.JSONResponses.MISSING_ADMIN_PASSWORD;
import static bened.http.JSONResponses.INCORRECT_ADMIN_PASSWORD;
import static bened.http.JSONResponses.LOCKED_ADMIN_PASSWORD;
import static bened.http.JSONResponses.NO_PASSWORD_IN_CONFIG;

public final class API {

    public static final int TESTNET_API_PORT = 6576;
    public static final int TESTNET_API_SSLPORT = 6577;
    private static final String[] DISABLED_HTTP_METHODS = {"TRACE", "OPTIONS", "HEAD"};

    public static final int openAPIPort;
    public static final int openAPISSLPort;
    public static final boolean isOpenAPI;

    public static final List<String> disabledAPIs;
    public static final List<APITag> disabledAPITags;

    private static final Set<String> allowedBotHosts;
    private static final List<NetworkAddress> allowedBotNets;
    private static final Map<String, PasswordCount> incorrectPasswords = new HashMap<>();
    public static final String adminPassword = Bened.getStringProperty("bened.adminPassword", "", true);
    static final boolean disableAdminPassword;
    static final int maxRecords = Bened.getIntProperty("bened.maxAPIRecords");
    static final boolean enableAPIUPnP = Bened.getBooleanProperty("bened.enableAPIUPnP");
    public static final int apiServerIdleTimeout = Bened.getIntProperty("bened.apiServerIdleTimeout");
    public static final boolean apiServerCORS = Bened.getBooleanProperty("bened.apiServerCORS");
    private static final String forwardedForHeader = Bened.getStringProperty("bened.forwardedForHeader");

    private static final Server apiServer;
    private static URI welcomePageUri;
    private static URI serverRootUri;

    static {
       System.setProperty( "org.eclipse.jetty.server.Request.maxFormContentSize", Integer.toString(Constants.MAX_HASH_TRX_SIZE) );
        
        List<String> disabled = new ArrayList<>(Bened.getStringListProperty("bened.disabledAPIs"));
        Collections.sort(disabled);
        disabledAPIs = Collections.unmodifiableList(disabled);
        disabled = Bened.getStringListProperty("bened.disabledAPITags");
        Collections.sort(disabled);
        List<APITag> apiTags = new ArrayList<>(disabled.size());
        disabled.forEach(tagName -> apiTags.add(APITag.fromDisplayName(tagName)));
        disabledAPITags = Collections.unmodifiableList(apiTags);
        List<String> allowedBotHostsList = Bened.getStringListProperty("bened.allowedBotHosts");
        if (! allowedBotHostsList.contains("*")) {
            Set<String> hosts = new HashSet<>();
            List<NetworkAddress> nets = new ArrayList<>();
            for (String host : allowedBotHostsList) {
                if (host.contains("/")) {
                    try {
                        nets.add(new NetworkAddress(host));
                    } catch (UnknownHostException e) {
                        Logger.logErrorMessage("Unknown network " + host, e);
                        throw new RuntimeException(e.toString(), e);
                    }
                } else {
                    hosts.add(host);
                }
            }
            allowedBotHosts = Collections.unmodifiableSet(hosts);
            allowedBotNets = Collections.unmodifiableList(nets);
        } else {
            allowedBotHosts = null;
            allowedBotNets = null;
        }

        boolean enableAPIServer = Bened.getBooleanProperty("bened.enableAPIServer");
        if (enableAPIServer) {
            final int port = Constants.isTestnet ? TESTNET_API_PORT : Bened.getIntProperty("bened.apiServerPort");
            final int sslPort = Constants.isTestnet ? TESTNET_API_SSLPORT : Bened.getIntProperty("bened.apiServerSSLPort");
            final String host = Bened.getStringProperty("bened.apiServerHost");
            disableAdminPassword = Bened.getBooleanProperty("bened.disableAdminPassword") || ("127.0.0.1".equals(host) && adminPassword.isEmpty());

            apiServer = new Server();
            ServerConnector connector;
           
            final boolean enableSSL;
            String keyStoreType = Bened.getStringProperty("bened.keyStoreType");
            String keyStorePassword = Bened.getStringProperty("bened.keyStorePassword", null, true);
            boolean enableSSLProperty = Bened.getBooleanProperty("bened.apiSSL");
            boolean enforceSSL = Bened.getBooleanProperty("bened.apiServerEnforceSSL", true);
            boolean listeningLocally = host.equals("127.0.0.1");
            String keyStorePathProperty = Bened.getStringProperty("bened.keyStorePath");

            //
            // Enforce SSL if API is not for local usage
            //
            if (enforceSSL && !enableSSLProperty && !listeningLocally) {
                keyStorePassword = API.adminPassword.length()==0?"DefaultBenedSSLPassword":API.adminPassword;
                SSLCert cert = new SSLCert(keyStorePassword);
                String keyStorePath = Paths.get(Bened.getUserHomeDir()).resolve(Paths.get("bened.default.ks")).toString();
                if (cert.isValid(keyStorePath)) {
                    Logger.logInfoMessage("SSLEnforce: Default keystore already exists");
                } else {
                    Logger.logInfoMessage("SSLEnforce: Creating keystore: " + keyStorePath);
                    final String fingerprint = cert.write(keyStorePath);
                    Logger.logWarningMessage("SSLEnforce: We generated SSL certificate for you. Please remember it's SHA-1 fingerprint: \n ----------- SSL CERTIFICATE SHA1 FINGERPRINT -----------\n" + fingerprint + "\n ----------- END OF FINGERPRINT -----------");
                }
                keyStoreType="JKS";
                keyStorePathProperty = "bened.default.ks";
                enableSSL = true;
            } else {
                enableSSL = enableSSLProperty;
            }

            //
            // Create the HTTP connector
            //
            if (!enableSSL || port != sslPort) {
                HttpConfiguration configuration = new HttpConfiguration();
                configuration.setSendDateHeader(false);
                configuration.setSendServerVersion(false);

                connector = new ServerConnector(apiServer, new HttpConnectionFactory(configuration));
                connector.setPort(port);
                connector.setHost(host);
                connector.setIdleTimeout(apiServerIdleTimeout);
                connector.setReuseAddress(true);
                apiServer.addConnector(connector);
                Logger.logMessage("API server using HTTP port " + port);
            }
            //
            // Create the HTTPS connector
            //
            final SslContextFactory sslContextFactory;
            if (enableSSL) {
                HttpConfiguration https_config = new HttpConfiguration();
                https_config.setSendDateHeader(false);
                https_config.setSendServerVersion(false);
                https_config.setSecureScheme("https");
                https_config.setSecurePort(sslPort);
                https_config.addCustomizer(new SecureRequestCustomizer());
                sslContextFactory = new SslContextFactory();
                String keyStorePath = Paths.get(Bened.getUserHomeDir()).resolve(Paths.get(keyStorePathProperty)).toString();
                Logger.logInfoMessage("Using keystore: " + keyStorePath);
                sslContextFactory.setKeyStorePath(keyStorePath);
                sslContextFactory.setKeyStorePassword(keyStorePassword);
                sslContextFactory.addExcludeCipherSuites("SSL_RSA_WITH_DES_CBC_SHA", "SSL_DHE_RSA_WITH_DES_CBC_SHA",
                        "SSL_DHE_DSS_WITH_DES_CBC_SHA", "SSL_RSA_EXPORT_WITH_RC4_40_MD5", "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
                        "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA", "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA");
                sslContextFactory.addExcludeProtocols("SSLv3");
                sslContextFactory.setKeyStoreType(keyStoreType);
                List<String> ciphers = Bened.getStringListProperty("bened.apiSSLCiphers");
                if (!ciphers.isEmpty()) {
                    sslContextFactory.setIncludeCipherSuites(ciphers.toArray(new String[ciphers.size()]));
                }
                connector = new ServerConnector(apiServer, new SslConnectionFactory( sslContextFactory, "http/1.1"), new HttpConnectionFactory(https_config));

                connector.setPort(sslPort);
                connector.setHost(host);
                connector.setIdleTimeout(apiServerIdleTimeout);
                connector.setReuseAddress(true);
                apiServer.addConnector(connector);
                Logger.logMessage("API server using HTTPS port " + sslPort);
            } else {
                sslContextFactory = null;
            }
            String localhost = "0.0.0.0".equals(host) || "127.0.0.1".equals(host) ? "localhost" : host;
            try {
                welcomePageUri = new URI(enableSSL ? "https" : "http", null, localhost, enableSSL ? sslPort : port, "/index.html", null, null);
                serverRootUri = new URI(enableSSL ? "https" : "http", null, localhost, enableSSL ? sslPort : port, "", null, null);
            } catch (URISyntaxException e) {
                Logger.logInfoMessage("Cannot resolve browser URI", e);
            }
            openAPIPort = !Constants.isLightClient && "0.0.0.0".equals(host) && allowedBotHosts == null && (!enableSSL || port != sslPort) ? port : 0;
            openAPISSLPort = !Constants.isLightClient && "0.0.0.0".equals(host) && allowedBotHosts == null && enableSSL ? sslPort : 0;
            isOpenAPI = openAPIPort > 0 || openAPISSLPort > 0;

            HandlerList apiHandlers = new HandlerList();

            ServletContextHandler apiHandler = new ServletContextHandler();
            String apiResourceBase = Bened.getStringProperty("bened.apiResourceBase");
           if (apiResourceBase != null) {
                ServletHolder defaultServletHolder = new ServletHolder(new DefaultServlet());
                defaultServletHolder.setInitParameter("dirAllowed", "false");
                defaultServletHolder.setInitParameter("resourceBase", apiResourceBase);
                defaultServletHolder.setInitParameter("welcomeServlets", "true");
                defaultServletHolder.setInitParameter("redirectWelcome", "true");
                defaultServletHolder.setInitParameter("gzip", "true");
                defaultServletHolder.setInitParameter("etags", "true");
                apiHandler.addServlet(defaultServletHolder, "/*");
                apiHandler.setWelcomeFiles(new String[]{Bened.getStringProperty("bened.apiWelcomeFile")});                
            }

            String javadocResourceBase = Bened.getStringProperty("bened.javadocResourceBase");
            ContextHandler contextHandler = new ContextHandler("/doc");
            if (javadocResourceBase != null) {
                ResourceHandler docFileHandler = new ResourceHandler();
                docFileHandler.setDirectoriesListed(false);
                docFileHandler.setWelcomeFiles(new String[]{"index.html"});
                docFileHandler.setResourceBase(javadocResourceBase);
                contextHandler.setHandler(docFileHandler);
                apiHandlers.addHandler(contextHandler);
            }

            ServletHolder servletHolder = apiHandler.addServlet(APIServlet.class, "/xsrv");
            servletHolder.getRegistration().setMultipartConfig(new MultipartConfigElement( null, Constants.MAX_HASH_TRX_SIZE, Constants.MAX_HASH_TRX_SIZE, Constants.MAX_HASH_TRX_SIZE));

            servletHolder = apiHandler.addServlet(APIProxyServlet.class, "/xsrv-proxy");
            servletHolder.setInitParameters(Collections.singletonMap("idleTimeout",
                    "" + Math.max(apiServerIdleTimeout - APIProxyServlet.PROXY_IDLE_TIMEOUT_DELTA, 0)));
            servletHolder.getRegistration().setMultipartConfig(new MultipartConfigElement(
                    null, 0, -1L, 0));

            GzipHandler gzipHandler = new GzipHandler();
            if (!Bened.getBooleanProperty("bened.enableAPIServerGZIPFilter", isOpenAPI)) {
                gzipHandler.setExcludedPaths("/xsrv", "/xsrv-proxy");
            }
            gzipHandler.setIncludedMethods("GET", "POST");
            gzipHandler.setMinGzipSize(bened.peer.Peers.MIN_COMPRESS_SIZE);
            apiHandler.setGzipHandler(gzipHandler);

            apiHandler.addServlet(APITestServlet.class, "/test");
        //    apiHandler.addServlet(APITestServlet.class, "/test-proxy");

        //    apiHandler.addServlet(DbShellServlet.class, "/dbshell");
            
        apiHandler.addServlet(ApizCirculationSupply.class, "/CirculationSupply");
        apiHandler.addServlet(ApizCirculationSupply.class, "/CirculationSupplyjson");
        apiHandler.addServlet(ApizMaxSupply.class, "/MaxSupply");
        apiHandler.addServlet(ApizMaxSupply.class, "/MaxSupplyjson");
        apiHandler.addServlet(ApizMiningSupply.class, "/MiningSupply");
        apiHandler.addServlet(ApizMiningSupply.class, "/MiningSupplyjson");
        apiHandler.addServlet(ApizShow.class, "/Show"); // /Show?Trx=17355369217368057888
        apiHandler.addServlet(ApizShow.class, "/Showjson"); // /Show?Trx=17355369217368057888
        apiHandler.addServlet(ApizSupply.class, "/Supply");
        apiHandler.addServlet(ApizSupply.class, "/Supplyjson");
        apiHandler.addServlet(ApizTop100accounts.class, "/Top100accounts"); 
        apiHandler.addServlet(ApizTop100accounts.class, "/Top100accountsjson");
        apiHandler.addServlet(ApizHeight.class, "/Height");
        apiHandler.addServlet(ApizHeight.class, "/Heightjson");

            if (apiServerCORS) {
                FilterHolder filterHolder = apiHandler.addFilter(CrossOriginFilter.class, "/*", null);
                filterHolder.setInitParameter("allowedHeaders", "*");
                filterHolder.setAsyncSupported(true);
            }

            if (Bened.getBooleanProperty("bened.apiFrameOptionsSameOrigin")) {
                FilterHolder filterHolder = apiHandler.addFilter(XFrameOptionsFilter.class, "/*", null);
                filterHolder.setAsyncSupported(true);
            }
            disableHttpMethods(apiHandler);

            apiHandlers.addHandler(apiHandler);
            apiHandlers.addHandler(new DefaultHandler());

            apiServer.setHandler(apiHandlers);
            apiServer.setStopAtShutdown(true);

//apiServer.setAttribute("org.eclipse.jetty.server.Request.maxFormContentSize", Constants.MAX_HASH_TRX_SIZE);

        
            ThreadPool.runBeforeStart(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (enableAPIUPnP) {
                            Connector[] apiConnectors = apiServer.getConnectors();
                            for (Connector apiConnector : apiConnectors) {
                                if (apiConnector instanceof ServerConnector serverConnector)
                                UPnP.addPort(serverConnector.getPort());
                        }
                        }
                        APIServlet.initClass();
                        APIProxyServlet.initClass();
                        APITestServlet.initClass();
//apiServer.setAttribute("org.eclipse.jetty.server.Request.maxFormContentSize", Constants.MAX_HASH_TRX_SIZE);
                        apiServer.start();
                        if (sslContextFactory != null) {
                            Logger.logDebugMessage("API SSL Protocols: " + Arrays.toString(sslContextFactory.getSelectedProtocols()));
                            Logger.logDebugMessage("API SSL Ciphers: " + Arrays.toString(sslContextFactory.getSelectedCipherSuites()));
                        }
                        Logger.logMessage("Started API server at " + host + ":" + port + (enableSSL && port != sslPort ? ", " + host + ":" + sslPort : ""));
                    } catch (Exception e) {
                        Logger.logErrorMessage("Failed to start API server", e);
                        throw new RuntimeException(e.toString(), e);
                    }
                }
            }, true);

        } else {
            apiServer = null;
            disableAdminPassword = false;
            openAPIPort = 0;
            openAPISSLPort = 0;
            isOpenAPI = false;
            Logger.logMessage("API server not enabled");
        }

    }

    public static void init() {}

    public static void shutdown() {
        if (apiServer != null) {
            try {
                apiServer.stop();
                if (enableAPIUPnP) {
                    Connector[] apiConnectors = apiServer.getConnectors();
                    for (Connector apiConnector : apiConnectors) {
                        if (apiConnector instanceof ServerConnector)
                            UPnP.deletePort(((ServerConnector)apiConnector).getPort());
                    }
                }
            } catch (Exception e) {
                Logger.logShutdownMessage("Failed to stop API server", e);
            }
        }
    }

    public static void verifyPassword(HttpServletRequest req) throws ParameterException {
        if (API.disableAdminPassword) {
            return;
        }
        if (API.adminPassword.isEmpty()) {
            throw new ParameterException(NO_PASSWORD_IN_CONFIG);
        }
        checkOrLockPassword(req);
    }

    public static boolean checkPassword(HttpServletRequest req) {
        if (API.disableAdminPassword) {
            return true;
        }
        if (API.adminPassword.isEmpty()) {
            return false;
        }
        if (Convert.emptyToNull(req.getParameter("adminPassword")) == null) {
            return false;
        }
        try {
            checkOrLockPassword(req);
            return true;
        } catch (ParameterException e) {
            return false;
        }
    }


    private static class PasswordCount {
        private int count;
        private int time;
    }

    private static void checkOrLockPassword(HttpServletRequest req) throws ParameterException {
        int now = Bened.getEpochTime();
        String remoteHost = null;
        if (forwardedForHeader != null) {
            remoteHost = req.getHeader(forwardedForHeader);
        }
        if (remoteHost == null) {
            remoteHost = req.getRemoteHost();
        }
        synchronized(incorrectPasswords) {
            PasswordCount passwordCount = incorrectPasswords.get(remoteHost);
            if (passwordCount != null && passwordCount.count >= 25 && now - passwordCount.time < 60*60) {
                Logger.logWarningMessage("Too many incorrect admin password attempts from " + remoteHost);
                throw new ParameterException(LOCKED_ADMIN_PASSWORD);
            }
            String adminPassword = Convert.nullToEmpty(req.getParameter("adminPassword"));
            if (!API.adminPassword.equals(adminPassword)) {
                if (adminPassword.length() > 0) {
                    if (passwordCount == null) {
                        passwordCount = new PasswordCount();
                        incorrectPasswords.put(remoteHost, passwordCount);
                        if (incorrectPasswords.size() > 1000) {
                            // Remove one of the locked hosts at random to prevent unlimited growth of the map
                            List<String> remoteHosts = new ArrayList<>(incorrectPasswords.keySet());
                            Random r = new Random();
                            incorrectPasswords.remove(remoteHosts.get(r.nextInt(remoteHosts.size())));
                        }
                    }
                    passwordCount.count++;
                    passwordCount.time = now;
                    Logger.logWarningMessage("Incorrect adminPassword from " + remoteHost);
                    throw new ParameterException(INCORRECT_ADMIN_PASSWORD);
                } else {
                    throw new ParameterException(MISSING_ADMIN_PASSWORD);
                }
            }
            if (passwordCount != null) {
                incorrectPasswords.remove(remoteHost);
            }
        }
    }

    static boolean isAllowed(String remoteHost) {
        if (API.allowedBotHosts == null || API.allowedBotHosts.contains(remoteHost)) {
            return true;
        }
        try {
            BigInteger hostAddressToCheck = new BigInteger(InetAddress.getByName(remoteHost).getAddress());
            for (NetworkAddress network : API.allowedBotNets) {
                if (network.contains(hostAddressToCheck)) {
                    return true;
                }
            }
        } catch (UnknownHostException e) {
            // can't resolve, disallow
            Logger.logMessage("Unknown remote host " + remoteHost);
        }
        return false;

    }

    private static void disableHttpMethods(ServletContextHandler servletContext) {
        SecurityHandler securityHandler = servletContext.getSecurityHandler();
        if (securityHandler == null) {
            securityHandler = new ConstraintSecurityHandler();
            servletContext.setSecurityHandler(securityHandler);
        }
        disableHttpMethods(securityHandler);
    }

    private static void disableHttpMethods(SecurityHandler securityHandler) {
        if (securityHandler instanceof ConstraintSecurityHandler) {
            ConstraintSecurityHandler constraintSecurityHandler = (ConstraintSecurityHandler) securityHandler;
            for (String method : DISABLED_HTTP_METHODS) {
                disableHttpMethod(constraintSecurityHandler, method);
            }
            ConstraintMapping enableEverythingButTraceMapping = new ConstraintMapping();
            Constraint enableEverythingButTraceConstraint = new Constraint();
            enableEverythingButTraceConstraint.setName("Enable everything but TRACE");
            enableEverythingButTraceMapping.setConstraint(enableEverythingButTraceConstraint);
            enableEverythingButTraceMapping.setMethodOmissions(DISABLED_HTTP_METHODS);
            enableEverythingButTraceMapping.setPathSpec("/");
            constraintSecurityHandler.addConstraintMapping(enableEverythingButTraceMapping);
        }
    }

    private static void disableHttpMethod(ConstraintSecurityHandler securityHandler, String httpMethod) {
        ConstraintMapping mapping = new ConstraintMapping();
        Constraint constraint = new Constraint();
        constraint.setName("Disable " + httpMethod);
        constraint.setAuthenticate(true);
        mapping.setConstraint(constraint);
        mapping.setPathSpec("/");
        mapping.setMethod(httpMethod);
        securityHandler.addConstraintMapping(mapping);
    }

    private static class NetworkAddress {

        private BigInteger netAddress;
        private BigInteger netMask;

        private NetworkAddress(String address) throws UnknownHostException {
            String[] addressParts = address.split("/");
            if (addressParts.length == 2) {
                InetAddress targetHostAddress = InetAddress.getByName(addressParts[0]);
                byte[] srcBytes = targetHostAddress.getAddress();
                netAddress = new BigInteger(1, srcBytes);
                int maskBitLength = Integer.valueOf(addressParts[1]);
                int addressBitLength = (targetHostAddress instanceof Inet4Address) ? 32 : 128;
                netMask = BigInteger.ZERO
                        .setBit(addressBitLength)
                        .subtract(BigInteger.ONE)
                        .subtract(BigInteger.ZERO.setBit(addressBitLength - maskBitLength).subtract(BigInteger.ONE));
            } else {
                throw new IllegalArgumentException("Invalid address: " + address);
            }
        }

        private boolean contains(BigInteger hostAddressToCheck) {
            return hostAddressToCheck.and(netMask).equals(netAddress);
        }

    }

    public static final class XFrameOptionsFilter implements Filter {

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
            ((HttpServletResponse) response).setHeader("X-FRAME-OPTIONS", "SAMEORIGIN");
            chain.doFilter(request, response);
        }

        @Override
        public void destroy() {
        }

    }

    public static URI getWelcomePageUri() {
        return welcomePageUri;
    }

    public static URI getServerRootUri() {
        return serverRootUri;
    }

    private API() {} // never

}
