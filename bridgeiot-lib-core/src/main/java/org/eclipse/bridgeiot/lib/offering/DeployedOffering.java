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
package org.eclipse.bridgeiot.lib.offering;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.bridgeiot.lib.exceptions.BridgeIoTException;
import org.eclipse.bridgeiot.lib.handlers.AccessRequestHandler;
import org.eclipse.bridgeiot.lib.misc.Constants;
import org.eclipse.bridgeiot.lib.offering.internal.Accounting;
import org.eclipse.bridgeiot.lib.offering.internal.Accounting.AccountingReport;
import org.eclipse.bridgeiot.lib.serverwrapper.BridgeIotHttpResponse;
import org.eclipse.bridgeiot.lib.serverwrapper.EmbededdedRouteBasedServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

//In the future shall provide the only lifecycle operation on an offering. Comparable to an Offering Object for a Consumer 
public class DeployedOffering extends RegisteredOffering {

    private static final Logger logger = LoggerFactory.getLogger(RegistrableOfferingDescription.class);

    static ObjectMapper mapper = new ObjectMapper();

    protected EmbededdedRouteBasedServer server;
    protected AccessRequestHandler accessRequestHandler;
    protected Accounting accounting;

    public DeployedOffering(RegistrableOfferingDescription offeringDescription, EmbededdedRouteBasedServer server) {
        super(offeringDescription);
        this.server = server;
        this.accessRequestHandler = offeringDescription.getAccessRequestHandler();
        this.deploy();
        accounting = Accounting.create(offeringDescription.getId());
    }

    @Override
    public void deregister() {
        if (registrableOfferingDescription.getProvider() != null) {
            registrableOfferingDescription.getProvider().addAccountingReports(this.getAccountingReports());
        }
        super.deregister();
        this.undeploy();
    }

    public void deploy() {
        // NOTE: Offerings on Provider Lib currently just have one Endpoint
        if (server != null) {
            if (accessRequestHandler != null) {
                server.addRoute(registrableOfferingDescription.getRoute(), internalAccessRequestHandler,
                        registrableOfferingDescription, true);
            } else {
                server.addRoute(registrableOfferingDescription.getRoute(), internalAccessStreamRequestHandler,
                        registrableOfferingDescription, true);
            }
            if (registrableOfferingDescription.getSampleDataAccessRequestHandler() != null) {
                server.addRoute(Constants.SAMPLEDATA_ROUTE + registrableOfferingDescription.getRoute(),
                        registrableOfferingDescription.getSampleDataAccessRequestHandler(),
                        registrableOfferingDescription, false);
            }
            if (registrableOfferingDescription.getMetaDataAccessRequestHandler() != null) {
                server.addRoute(Constants.METADATA_ROUTE + registrableOfferingDescription.getRoute(),
                        registrableOfferingDescription.getMetaDataAccessRequestHandler(),
                        registrableOfferingDescription, false);
            }
        } else {
            logger.info("Deployment server not defined!");
            throw new BridgeIoTException("ERROR: Deployment server not defined!");
        }
    }

    public void undeploy() {
        // Offerings on Libs currently just have one endpoint
        if (server != null) {

            server.removeRoute(registrableOfferingDescription.getRoute());

            if (registrableOfferingDescription.getSampleDataAccessRequestHandler() != null) {
                server.removeRoute(Constants.SAMPLEDATA_ROUTE + registrableOfferingDescription.getLocalId());
            }
            if (registrableOfferingDescription.getMetaDataAccessRequestHandler() != null) {
                server.removeRoute(Constants.METADATA_ROUTE + registrableOfferingDescription.getLocalId());
            }

        } else {
            logger.info("Deployment server not defined!");
        }
    }

    public List<AccountingReport> getAccountingReports() {
        return accounting.getCurrentReports();
    }

    protected AccessRequestHandler internalAccessRequestHandler = new AccessRequestHandler() {

        @Override
        public BridgeIotHttpResponse processRequestHandler(OfferingDescription offeringDescription,
                Map<String, Object> inputData, String subscriptionId, String sessionId) {

            BridgeIotHttpResponse response = accessRequestHandler.processRequestHandler(offeringDescription, inputData,
                    subscriptionId, sessionId);

            if (response.getStatus() == BridgeIotHttpResponse.OK_STATUS) {
                accounting.addEvent(subscriptionId, sessionId, response);
            }

            return response;
        }

    };

    protected AccessRequestHandler internalAccessStreamRequestHandler = new AccessRequestHandler() {

        @Override
        public BridgeIotHttpResponse processRequestHandler(OfferingDescription offeringDescription,
                Map<String, Object> inputData, String subscriptionId, String sessionId) {

            // BridgeIotHttpResponse errorResponse =
            // BridgeIotHttpResponse.error().withBody("{\"status\":\"error\"}").withStatus(422).asJsonType();

            if (offeringStream == null) {
                offeringStream = AccessStream.create();
            }

            String accessStreamId = subscriptionId + "_" + sessionId;

            if (!mapAccessStreams.containsKey(accessStreamId)) {
                // new Consumer session --> create new access stream based on overall offering stream
                mapAccessStreams.put(accessStreamId, offeringStream.clone());
                logger.info("New Access Stream started: {}", accessStreamId);
            } else {
                if (mapAccessStreams.get(accessStreamId)
                        .hasExpired(registrableOfferingDescription.accessStreamSessionTimeout)) {
                    // Consumer session has expired --> create new access stream based on overall offering stream
                    mapAccessStreams.put(accessStreamId, offeringStream.clone());
                    logger.info("Access Stream expired --> new Access Stream started: {}", accessStreamId);
                }
            }

            List<JsonObject> jsonObjList = new ArrayList<>();
            JsonObject jsonObj;
            while ((jsonObj = mapAccessStreams.get(accessStreamId).poll()) != null) {
                if ((inputData == null) || inputData.isEmpty()
                        || (registrableOfferingDescription.accessStreamFilterHandler == null) ||
                // check which elements should be sent based on input parameters
                registrableOfferingDescription.accessStreamFilterHandler.processRequestHandler(offeringDescription,
                        jsonObj, inputData, subscriptionId, sessionId)) {
                    jsonObjList.add(jsonObj);
                }
            }

            BridgeIotHttpResponse response = BridgeIotHttpResponse.okay()
                    .withBody(JsonObject.createJsonArray(jsonObjList));
            accounting.addEvent(subscriptionId, sessionId, response);
            return response;
        }

    };

}
