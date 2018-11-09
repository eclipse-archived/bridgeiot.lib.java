/**
 * Copyright (c) 2016-2017 in alphabetical order:
 * Bosch Software Innovations GmbH, Robert Bosch GmbH, Siemens AG
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Denis Kramer     (Bosch Software Innovations GmbH)
 *    Stefan Schmid    (Robert Bosch GmbH)
 *    Andreas Ziller   (Siemens AG)
 */
package org.eclipse.bridgeiot.lib.embeddedspark;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.bridgeiot.lib.configuration.LibConfiguration;
import org.eclipse.bridgeiot.lib.handlers.AccessRequestHandler;
import org.eclipse.bridgeiot.lib.handlers.ProxyAccessRequestHandler;
import org.eclipse.bridgeiot.lib.misc.Constants;
import org.eclipse.bridgeiot.lib.offering.OfferingDescription;
import org.eclipse.bridgeiot.lib.offering.RegistrableOfferingDescription;
import org.eclipse.bridgeiot.lib.security.AccessToken;
import org.eclipse.bridgeiot.lib.serverwrapper.BridgeIotHttpResponse;
import org.eclipse.bridgeiot.lib.serverwrapper.EmbededdedRouteBasedServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spark.Spark;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class EmbeddedSpark extends EmbededdedRouteBasedServer {

    private static boolean sparkInitialized = false;

    Map<String, OfferingDescription> routes = new HashMap<>();
    ServerOptionsSpark serverOptions;
    String baseDomain;
    int basePort;
    boolean sslServer = false;

    private static final Logger logger = LoggerFactory.getLogger(EmbeddedSpark.class);

    public EmbeddedSpark(String domain, int port) {
        this(domain, port, "", ServerOptionsSpark.defaultOptions);
    }

    public EmbeddedSpark(String domain, int port, String baseRoute, ServerOptionsSpark serverOptions) {
        super(domain, port);
        this.serverOptions = serverOptions;
        this.baseRoute = baseRoute;
        this.baseDomain = domain;
        this.basePort = port;
    }

    @Override
    public void start() {
        // Start the SparkServer for the first instance only
        if (!sparkInitialized) {
            sparkInitialized = true;
            // TODO: This is a temporary solution until SparkJava supports the stream-based secure method
            File keystoreFile = writeTempFile(this.getResourceKeystoreInputStream());
            start(true, keystoreFile.getPath(), this.getResourceKeystorePassword());
        }
    }

    public void startHttp() {
        if (!sparkInitialized) {
            sparkInitialized = true;
            start(false, null, null);
        }
    }

    @Override
    public void start(String keyStoreFile, String keyStorePassword) {
        if ((keyStoreFile == null) || (keyStoreFile.isEmpty())) {
            keyStoreFile = this.getDefaultKeystoreFile();
        }
        if ((keyStorePassword == null) || (keyStorePassword.isEmpty())) {
            keyStorePassword = this.getDefaultKeystorePassword();
        }
        start(true, keyStoreFile, keyStorePassword);
    }

    private void start(boolean sslServer, String keyStoreLocation, String keyStorePassword) {

        String baseUrl = "https://" + this.baseDomain + ":" + this.basePort + "/" + this.baseRoute;

        int maxThreads = 50;
        int minThreads = 2;
        int timeOutMillis = 30000;
        Spark.threadPool(maxThreads, minThreads, timeOutMillis);

        Spark.port(port);

        if (sslServer) {
            this.sslServer = true;
            Spark.secure(keyStoreLocation, keyStorePassword, null, null);
        }

        logger.info("Start web service for A1 interface at: {}", baseUrl);

        prepareSwaggerSupport();

        Spark.get(baseRoute, "application/json", (req, res) -> {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode rootNode = mapper.createObjectNode();
            ArrayNode arrayNode = rootNode.putArray("routes");

            logger.info("Request received: {}, {}, {}", req.pathInfo(), req.ip(), req.queryParams());

            for (Map.Entry<String, OfferingDescription> entry : routes.entrySet()) {
                ObjectNode childNode = mapper.createObjectNode();
                childNode.put("offering id", entry.getValue().getId());
                if (entry.getKey().startsWith(Constants.SAMPLEDATA_ROUTE)) {
                    childNode.put("sampledata route", baseUrl + "/" + entry.getKey());
                } else if (entry.getKey().startsWith(Constants.METADATA_ROUTE)) {
                    childNode.put("metadata route", baseUrl + "/" + entry.getKey());
                } else {
                    for (int i = 0; i < entry.getValue().getEndpoints().size(); i++) {
                        childNode.put("access route" + i, entry.getValue().getEndpoints().get(0).getUri());
                    }
                }
                arrayNode.add(childNode);
            }

            res.type("application/json");
            return rootNode.toString();
        });

        Spark.awaitInitialization();

    }

    private void prepareSwaggerSupport() {

        Spark.staticFileLocation("/" + Constants.wwwFolder);
        String folderName = Constants.wwwFolder + "/" + Constants.wwwBigIot;
        File f = new File(folderName);
        if (!f.exists() && !f.isDirectory()) {
            new File(folderName).mkdirs();
        }
        Spark.externalStaticFileLocation(Constants.wwwFolder);

    }

    @Override
    public void stop() {
        if (sparkInitialized) {
            Spark.stop();
            sparkInitialized = false;
        }
    }

    @Override
    public void addRoute(final String route, final AccessRequestHandler accessCallback,
            final RegistrableOfferingDescription offeringDescription, final boolean authorizationRequired) {

        String fullRoute = baseRoute + "/" + route;

        if (routes.containsKey(route)) {
            logger.info("Adding a route a second Time: Check code!");
            routes.remove(route);
        }

        Spark.get(fullRoute, "application/json", (req, res) -> {

            logger.debug("Session info: id {}, creationTime {}, isNew {}, lastAccessedTime {}, maxInactiveInt {}",
                    req.session().id(), req.session().creationTime(), req.session().isNew(),
                    req.session().lastAccessedTime(), req.session().maxInactiveInterval());

            logger.info("Access Request received: {}, {}, {}", req.pathInfo(), req.ip(), req.queryParams());

            final String authHeader = req.headers("Authorization");
            if (!checkAccessToken(authorizationRequired, authHeader, offeringDescription)) {
                Spark.halt(401, "OfferingAccessToken missing, invalid or expired!!!");
            }

            Map<String, Object> inputData = extractInputDataMap(req.queryMap().toMap());
            String subscriptionId = getSubscriptionId(authHeader);
            String accessSessionId = createAccessSessionId(req.headers("AccessSessionId"), getSubscriberId(authHeader),
                    req.ip(), req.session().id(), inputData);

            BridgeIotHttpResponse response = accessCallback.processRequestHandler(offeringDescription, inputData,
                    subscriptionId, accessSessionId);

            res.type("application/json");
            res.status(new Integer(response.getStatus()));
            for (Entry<String, String> entry : response.getHeaders().entrySet()) {
                res.header(entry.getKey(), entry.getValue());
            }

            return response.getBody();

        });

        routes.put(route, offeringDescription);

    }

    public void activateProxy(ProxyAccessRequestHandler accessCallback) {

        String fullRoute = "/bigiot/proxy/access/*";

        Spark.get(fullRoute, "application/json", (req, res) -> {
            logger.info("Request received: {}, {}, {}", req.pathInfo(), req.ip(), req.queryParams());

            Map<String, Object> inputData = extractInputDataMap(req.queryMap().toMap());

            res.type("application/json");

            int beginIndex = req.pathInfo().lastIndexOf("/");
            String offeringId = req.pathInfo().substring(beginIndex + 1);

            return accessCallback.processRequestHandler(offeringId, inputData);

        });

    }

    @Override
    protected String getProtocolName() {
        return "HTTP";
    }

    @Override
    public void removeRoute(String route) {

        if (!routes.containsKey(route)) {
            logger.error("ERROR: Cannot remove route ({}) for an undeployed Offering", route);
        }

        Spark.get(baseRoute + "/" + route, "application/json", (req, res) -> {
            logger.info("Request received for Offering that has been removed: {}, {}, {}", req.pathInfo(), req.ip(),
                    req.queryParams());

            return "This Offering has been removed!";
        });

        routes.remove(route);
    }

    @Override
    public List<String> getRoutes() {
        return new ArrayList<>(routes.keySet());
    }

    public static void enableCorsAll() {
        enableCors("*", "GET,POST,HEAD", "X-Requested-With,Content-Type,Accept,Origin");
    }

    public static void enableCors(final String origin, final String methods, final String headers) {

        Spark.options("/*", (request, response) -> {

            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }

            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }

            return "OK";
        });

        Spark.before((request, response) -> {
            response.header("Access-Control-Allow-Origin", origin);
            response.header("Access-Control-Request-Method", methods);
            response.header("Access-Control-Allow-Headers", headers);
            // Note: this may or may not be necessary in your particular application
            response.type("application/json");
        });

    }

    private boolean checkAccessToken(final boolean authorizationRequired, final String authHeader,
            final RegistrableOfferingDescription offeringDescription) {

        if ((authHeader != null) && (authHeader.startsWith("Bearer "))) {
            final String token = authHeader.substring(authHeader.indexOf(" ") + 1);
            // Check if token is valid, if not, halt
            if (!AccessToken.validateOfferingAccessToken(token,
                    offeringDescription.getMarketplaceClient().getClientSecret(), offeringDescription.getId())) {
                return false;
            }
        } else {
            if (authorizationRequired) {
                if (LibConfiguration.ACCESS_TOKEN_VALIDATION_REQUIRED) {
                    return false;
                }
                logger.info("Warning: Debug-Mode without access token!!!");
            }
        }

        return true;
    }

    private String getSubscriptionId(final String authHeader) {

        String subscriptionId = Constants.UNKNOWN_SUBSCRIPTION_ID;
        if ((authHeader != null) && (authHeader.startsWith("Bearer "))) {
            subscriptionId = AccessToken.getSubscriptionId(authHeader.substring(authHeader.indexOf(" ") + 1));
        }

        return subscriptionId;
    }

    private String getSubscriberId(final String authHeader) {

        String subscriberId = "";
        if ((authHeader != null) && (authHeader.startsWith("Bearer "))) {
            subscriberId = AccessToken.getOfferingAccessTokenInfo(authHeader.substring(authHeader.indexOf(" ") + 1));
        }

        return subscriberId;
    }

    private String createAccessSessionId(String accessSessionId, final String subscriberId, final String reqIpAddress,
            final String reqSessionId, final Map<String, Object> inputData) {

        if (accessSessionId == null) {
            // Get accessSessionId from Input Data. If not available, auto-generate a unique ID
            // from IP address, subscriberId, inputData Hash, etc.
            if (inputData != null) {
                if ((!inputData.isEmpty()) && inputData.containsKey("accessSessionId")) {
                    return String.valueOf(inputData.get("accessSessionId"));
                } else {
                    if (!subscriberId.isEmpty()) {
                        return reqIpAddress + "_" + subscriberId + "_" + inputData.toString().hashCode();
                    } else {
                        return reqIpAddress + "_" + reqSessionId + "_" + inputData.toString().hashCode();
                    }
                }
            } else {
                if (!subscriberId.isEmpty()) {
                    return reqIpAddress + "_" + subscriberId;
                } else {
                    return reqIpAddress + "_" + reqSessionId;
                }
            }
        }

        return accessSessionId;
    }

    private File writeTempFile(InputStream is) {

        final int BUFFER_SIZE = 500;
        File tempFile = null;

        try {
            // Create temp file.
            tempFile = File.createTempFile("temp", ".dat");

            // Delete temp file when program exits.
            tempFile.deleteOnExit();

            // Write to temp file
            OutputStream os = new FileOutputStream(tempFile);

            int num = BUFFER_SIZE;
            byte[] buffer = new byte[BUFFER_SIZE];
            while (num == BUFFER_SIZE) {
                num = is.read(buffer, 0, BUFFER_SIZE);
                os.write(buffer, 0, num);
            }

            os.close();
            is.close();

        } catch (IOException e) {
            logger.error("IOException during creating temporary server certificate file!");
            throw new RuntimeException("IOException during creating temporary server certificate file");
        }

        return tempFile;
    }

}
