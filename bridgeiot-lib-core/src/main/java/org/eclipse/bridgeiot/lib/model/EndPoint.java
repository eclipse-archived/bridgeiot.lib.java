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
package org.eclipse.bridgeiot.lib.model;

import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.AccessInterfaceType;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.EndpointType;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.MimeType;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Endpoint Information of Offering
 * 
 *
 */
public class EndPoint {

    private String uri;
    private EndpointType endpointType = null;
    private AccessInterfaceType accessInterfaceType = null;
    private MimeType acceptType;
    private MimeType contentType;

    public EndPoint() {
        this.setUri("");
    }

    public EndPoint(EndpointType endpointType, AccessInterfaceType accessInterfaceType, String uri) {
        this(uri, endpointType, accessInterfaceType, MimeType.APPLICATION_JSON, MimeType.APPLICATION_JSON);

    }

    public EndPoint(String endpointType, String accessInterfaceType, String uri) {
        this(uri, EndpointType.valueOf(endpointType), AccessInterfaceType.valueOf(accessInterfaceType),
                MimeType.APPLICATION_JSON, MimeType.APPLICATION_JSON);
    }

    public EndPoint(String uri, String endpointType, String accessInterfaceType, String acceptType,
            String contentType) {
        this(uri, EndpointType.valueOf(endpointType), AccessInterfaceType.valueOf(accessInterfaceType),
                MimeType.valueOf(acceptType), MimeType.valueOf(contentType));
    }

    public EndPoint(String uri, EndpointType endpointType, AccessInterfaceType accessInterfaceType, MimeType acceptType,
            MimeType contentType) {
        super();
        this.uri = uri;
        this.endpointType = endpointType;
        this.accessInterfaceType = accessInterfaceType;
        this.acceptType = acceptType;
        this.contentType = contentType;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public EndpointType getEndpointType() {
        return endpointType;
    }

    public void setEndpointType(EndpointType endpointType) {
        this.endpointType = endpointType;
    }

    public AccessInterfaceType getAccessInterfaceType() {
        return accessInterfaceType;
    }

    public void setAccessInterfaceType(AccessInterfaceType accessInterfaceType) {
        this.accessInterfaceType = accessInterfaceType;
    }

    @Override
    public String toString() {
        return "endpoint[uri=" + uri + "]";
    }

    public MimeType getAcceptType() {
        return acceptType;
    }

    public void setAcceptType(MimeType acceptType) {
        this.acceptType = acceptType;
    }

    public MimeType getContentType() {
        return contentType;
    }

    public void setContentType(MimeType contentType) {
        this.contentType = contentType;
    }

    @JsonIgnore
    public boolean isSecured() {
        return uri.startsWith("https:") || uri.startsWith("coaps:") || uri.startsWith("tls:") || uri.startsWith("ssl:")
                || uri.startsWith("wss:");
    }

}
