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
package org.eclipse.bridgeiot.lib.serverwrapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.bridgeiot.lib.configuration.LibConfiguration;
import org.eclipse.bridgeiot.lib.exceptions.BridgeIoTException;
import org.eclipse.bridgeiot.lib.misc.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

public abstract class EmbededdedRouteBasedServer implements IServerWrapper, IRouter {

    private static final Logger logger = LoggerFactory.getLogger(EmbededdedRouteBasedServer.class);

    private static final String DEFAULT_KEYSTORE_FILE = "keystore/keystore.jks";
    private static final String DEFAULT_KEYSTORE_PASSWORD = "12345678";

    protected String domain;
    protected int port;
    protected String baseRoute;

    public EmbededdedRouteBasedServer(String domain, int port) {
        this(domain, "", port);
    }

    public EmbededdedRouteBasedServer(String domain, String baseRoute, int port) {
        this.domain = domain;
        this.port = port;
        this.baseRoute = baseRoute;
    }

    protected abstract String getProtocolName();

    public String getBaseUrl() {
        return getProtocolName() + "://" + domain + ":" + port + "/" + baseRoute;
    }

    /*
     * DOES NOT WORK WITH GRADLE IMPORTED JARS - UNCLEAR WHY protected String getResourceKeystoreFile() { ClassLoader
     * classLoader = EmbededdedRouteBasedServer.class.getClassLoader(); URL filename =
     * classLoader.getResource(DEFAULT_KEYSTORE_FILE); return filename.getFile(); }
     */
    protected InputStream getResourceKeystoreInputStream() {
        ClassLoader classLoader = EmbededdedRouteBasedServer.class.getClassLoader();
        return classLoader.getResourceAsStream(DEFAULT_KEYSTORE_FILE);
    }

    protected String getResourceKeystorePassword() {
        return DEFAULT_KEYSTORE_PASSWORD;
    }

    protected String getDefaultKeystoreFile() {
        return DEFAULT_KEYSTORE_FILE;
    }

    protected String getDefaultKeystorePassword() {
        return DEFAULT_KEYSTORE_PASSWORD;
    }

    protected Map<String, Object> extractInputDataMap(Map<String, String[]> queryMap) {
        Map<String, Object> inputData = new HashMap<>();
        if (queryMap.containsKey(Constants.COMPLEX_PARAMETER_KEY)) {
            if (queryMap.size() != 1) {
                throw new BridgeIoTException("Request has complex parameters. However encoding is wrong");
            }
            try {
                JsonNode complexParameterTree = new ObjectMapper()
                        .readTree(queryMap.get(Constants.COMPLEX_PARAMETER_KEY)[0]);

                inputData = traverseJsonNode(complexParameterTree, LibConfiguration.JSON_MAPPING_DEPTH);

            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }

        } else {

            for (Map.Entry<String, String[]> entry : queryMap.entrySet()) {
                inputData.put(entry.getKey(), entry.getValue()[0]);
            }
        }
        return inputData;
    }

    protected Map<String, Object> traverseJsonNode(JsonNode node, int depth) {
        if (depth <= 0)
            throw new BridgeIoTException("Nested access parameter strcture too deep. Maybe it has a loop");

        Map<String, Object> map = new HashMap<>();

        Iterator<Entry<String, JsonNode>> itr = node.fields();
        while (itr.hasNext()) {
            Map.Entry<java.lang.String, com.fasterxml.jackson.databind.JsonNode> entry = itr.next();
            if (entry.getValue().isContainerNode()) {
                if (entry.getValue().isObject()) {
                    map.put(entry.getKey(), traverseJsonNode(entry.getValue(), depth - 1));
                } else if (entry.getValue().isArray()) {
                    map.put(entry.getKey(), traverseJsonNodeArray((ArrayNode) entry.getValue(), depth));

                }

            } else {
                map.put(entry.getKey(), entry.getValue().asText());
            }

        }
        return map;
    }

    private Object traverseJsonNodeArray(ArrayNode arrayNode, int depth) {
        if (depth <= 0)
            throw new BridgeIoTException("Nested access parameter strcture too deep. Maybe it has a loop");

        LinkedList<Object> list = new LinkedList<>();

        Iterator<JsonNode> itr = arrayNode.elements();
        while (itr.hasNext()) {
            JsonNode node = itr.next();
            if (node.isContainerNode()) {
                if (node.isObject()) {
                    list.add(traverseJsonNode(node, depth - 1));
                } else if (node.isArray()) {
                    list.add(traverseJsonNodeArray((ArrayNode) node, depth));
                }

            } else {
                list.add(node.asText());
            }
        }
        return list;
    }
}
