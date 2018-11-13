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
import java.util.HashMap;
import java.util.Map;

import org.eclipse.bridgeiot.lib.exceptions.BridgeIoTException;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.MimeType;
import org.eclipse.bridgeiot.lib.offering.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Encapsulation for Offering Response in Bridge.IoT Message Format
 */
public class BridgeIotHttpResponse {

    private static final Logger logger = LoggerFactory.getLogger(BridgeIotHttpResponse.class);

    Map<String, String> headers = new HashMap<>();
    String status;
    String body;
    Boolean mimeTypeDefined = false;

    static ObjectMapper mapper = new ObjectMapper();

    public static final String OK_STATUS = "200";
    public static final String BAD_REQUEST_STATUS = "400";
    public static final String ERROR_STATUS = "500";

    /**
     * Construct a Bridge.IoT Message for encapsulation offering responses
     * 
     * @param headers
     * @param status
     * @param body
     */
    public BridgeIotHttpResponse(Map<String, String> headers, String status, String body) {
        super();
        this.headers = headers;
        this.status = status;
        this.body = body;
    }

    /**
     * Construct an empty Bridge.IoT Message for encapsulation offering responses
     * 
     * @param status
     */
    public BridgeIotHttpResponse(String status) {
        this.status = status;
    }

    /**
     * Construct an empty Bridge.IoT Message for encapsulation offering responses with HTTP status code 'okay'
     * 
     * @return
     */
    public static BridgeIotHttpResponse okay() {

        return new BridgeIotHttpResponse(OK_STATUS);
    }

    /**
     * Construct an empty Bridge.IoT Message for encapsulation offering responses with HTTP status code 'Bad request'
     * 
     * @return
     */
    public static BridgeIotHttpResponse badRequest() {
        return new BridgeIotHttpResponse(BAD_REQUEST_STATUS);
    }

    /**
     * Constructing an empty Bridge.IoT Message for encapsulation offering responses with HTTP status code 'Internal
     * server error'
     * 
     * @return
     */
    public static BridgeIotHttpResponse error() {
        return new BridgeIotHttpResponse(ERROR_STATUS);
    }

    /**
     * Set the body as a String
     * 
     * @param body
     * @return
     */
    public BridgeIotHttpResponse withBody(String body) {
        this.body = body;
        return this;
    }

    /**
     * Set the body from a json-array string representation
     * 
     * @param rawJsonArray
     * @return
     */
    public BridgeIotHttpResponse withJsonArrayBody(String rawJsonArray) {
        try {
            JsonNode jsonObject = mapper.reader().readTree(rawJsonArray);
            if (jsonObject.isArray()) {
                // jsonResult = mapper.writeValueAsString(jsonObject);
                this.body = rawJsonArray;
            } else {
                ArrayNode arrayNode = mapper.getNodeFactory().arrayNode();
                arrayNode.add(jsonObject);
                this.body = mapper.writeValueAsString(arrayNode);
            }
        } catch (IOException e) {
            String errorMsg = "Processing Json body failed!";
            logger.error(errorMsg);
            throw new BridgeIoTException(errorMsg, e);
        }
        return this;
    }

    /**
     * Set the body as a String mapped from a JsonObject
     * 
     * @param jsonObject
     * @return
     */
    public BridgeIotHttpResponse withBody(JsonObject jsonObject) {
        this.body = jsonObject.write();
        return this;
    }

    /**
     * Set content type to application/json
     * 
     * @return
     */
    public BridgeIotHttpResponse asJsonType() {
        return asType(MimeType.APPLICATION_JSON.toString());
    }

    /**
     * Set content type to application/xml
     * 
     * @return
     */
    public BridgeIotHttpResponse asXmlType() {
        return asType(MimeType.APPLICATION_XML.toString());
    }

    /**
     * Set content type to text/plain
     * 
     * @return
     */
    public BridgeIotHttpResponse asTextPlain() {
        return asType(MimeType.TEXT_PLAIN.toString());
    }

    /**
     * Set content type
     * 
     * @param type
     * @return
     */
    public BridgeIotHttpResponse asType(String type) {
        headers.put("content-type", type);
        mimeTypeDefined = true;
        return this;
    }

    /**
     * Add a HTTP header to an existing response
     * 
     * @param key
     * @param value
     * @return
     */
    public BridgeIotHttpResponse addHeader(String key, String value) {
        headers.put(key, value);
        return this;
    }

    /**
     * Add a Bridge.IoT response status to an existing response.
     * 
     * @param status
     * @return
     */

    public BridgeIotHttpResponse withStatus(String status) {
        this.status = status;
        return this;
    }

    /**
     * Add a Bridge.IoT response status as an integer to an existing response.
     * 
     * @param status
     * @return
     */
    public BridgeIotHttpResponse withStatus(int status) {

        return withStatus(String.valueOf(status));
    }

    /**
     * Get map of response headers.
     * 
     * @return
     */
    public Map<String, String> getHeaders() {
        if (!mimeTypeDefined) {
            asJsonType(); // set JSON Type as default, if no Type is set explicitly
        }
        return headers;
    }

    /**
     * Get reponse status
     * 
     * @return
     */
    public String getStatus() {
        return status;
    }

    /**
     * Get response body as string.
     * 
     * @return
     */
    public String getBody() {
        return body;
    }

}
