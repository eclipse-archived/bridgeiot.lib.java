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
package org.eclipse.bridgeiot.lib.misc;

import org.eclipse.bridgeiot.lib.model.BridgeIotTypes;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Jan Seeger on 24.08.2017.
 */
public class CoapClient {

    private static final Logger logger = LoggerFactory.getLogger(CoapClient.class);
    public static final int MEDIA_TYPE_JSON = MediaTypeRegistry.APPLICATION_JSON;
    public static final int MEDIA_TYPE_XML = MediaTypeRegistry.APPLICATION_XML;

    private final org.eclipse.californium.core.CoapClient internalClient;

    public CoapClient() {
        internalClient = new org.eclipse.californium.core.CoapClient();
    }

    org.eclipse.californium.core.CoapClient getInternalClient() {
        return internalClient;
    }

    private void setUpRequest(Request req, String url, BridgeIotTypes.MimeType accept,
            BridgeIotTypes.MimeType content) {
        req.setURI(url);
        switch (accept) {
        case APPLICATION_JSON:
            req.getOptions().setAccept(MEDIA_TYPE_JSON);
            break;
        case APPLICATION_XML:
            req.getOptions().setAccept(MEDIA_TYPE_XML);
            break;
        case APPLICATION_UNDEFINED:
            req.getOptions().setAccept(MediaTypeRegistry.UNDEFINED);
            break;
        case TEXT_PLAIN:
            String errorMsg = BridgeIotTypes.MimeType.TEXT_PLAIN.name();
            errorMsg += " is not supported as Accept-Type to setup the request.";
            logger.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        switch (content) {
        case APPLICATION_JSON:
            req.getOptions().setContentFormat(MEDIA_TYPE_JSON);
            break;
        case APPLICATION_XML:
            req.getOptions().setContentFormat(MEDIA_TYPE_XML);
            break;
        case APPLICATION_UNDEFINED:
            req.getOptions().setContentFormat(MediaTypeRegistry.UNDEFINED);
            break;
        case TEXT_PLAIN:
            String errorMsg = BridgeIotTypes.MimeType.TEXT_PLAIN.name();
            errorMsg += " is not supported as ContentFormat to setup the request.";
            logger.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
    }

    public CoapResponse doGet(String url, BridgeIotTypes.MimeType accept) {
        final Request req = Request.newGet();
        setUpRequest(req, url, accept, BridgeIotTypes.MimeType.APPLICATION_UNDEFINED);
        // TODO: This returns null on timeout.
        return getInternalClient().advanced(req);
    }

    public CoapResponse doPost(String url, BridgeIotTypes.MimeType accept, BridgeIotTypes.MimeType content,
            String body) {
        return doAdvanced(url, accept, content, body, Request.newPost());
    }

    public CoapResponse doPut(String url, BridgeIotTypes.MimeType accept, BridgeIotTypes.MimeType content,
            String body) {
        return doAdvanced(url, accept, content, body, Request.newPut());
    }

    CoapResponse doAdvanced(String url, BridgeIotTypes.MimeType accept, BridgeIotTypes.MimeType content, String body,
            Request req) {
        setUpRequest(req, url, accept, content);
        req.setPayload(body);
        return getInternalClient().advanced(req);
    }

}
