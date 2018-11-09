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
package org.eclipse.bridgeiot.lib.examples;

import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import org.eclipse.bridgeiot.lib.ProviderSpark;
import org.eclipse.bridgeiot.lib.exceptions.BridgeIoTException;
import org.eclipse.bridgeiot.lib.exceptions.IncompleteOfferingDescriptionException;
import org.eclipse.bridgeiot.lib.exceptions.InvalidOfferingException;
import org.eclipse.bridgeiot.lib.exceptions.NotRegisteredException;
import org.eclipse.bridgeiot.lib.handlers.AccessRequestHandler;
import org.eclipse.bridgeiot.lib.model.BoundingBox;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.LicenseType;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.PricingModel;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.ValueType;
import org.eclipse.bridgeiot.lib.model.Location;
import org.eclipse.bridgeiot.lib.model.Price.Euros;
import org.eclipse.bridgeiot.lib.offering.OfferingDescription;
import org.eclipse.bridgeiot.lib.offering.RegisteredOffering;
import org.eclipse.bridgeiot.lib.serverwrapper.BridgeIotHttpResponse;
import org.joda.time.DateTime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Example for using Bridge.IoT API as a provider.
 */
public class ExampleProvider {

    static ObjectMapper mapper = new ObjectMapper();

    private static AccessRequestHandler accessCallback = new AccessRequestHandler() {
        @Override
        public BridgeIotHttpResponse processRequestHandler(OfferingDescription offeringDescription,
                Map<String, Object> inputData, String subscriptionId, String consumerInfo) {

            BridgeIotHttpResponse errorResponse = BridgeIotHttpResponse.error().withBody("{\"status\":\"error\"}")
                    .withStatus(422).asJsonType();

            double longitude = 41.0;
            if (inputData.containsKey("longitude"))
                longitude = Double.parseDouble((String) inputData.get("longitude"));

            double latitude = 9.0;
            if (inputData.containsKey("latitude"))
                latitude = Double.parseDouble((String) inputData.get("latitude"));

            Random r = new Random();
            ArrayNode arrayNode = mapper.getNodeFactory().arrayNode();
            int n = Math.round(r.nextFloat() * 10 + 10);
            for (int i = 0; i < n; i++) {
                ObjectNode jsonObject = mapper.createObjectNode().put("lat", latitude + r.nextFloat() * 0.01)
                        .put("lon", longitude + r.nextFloat() * 0.01)
                        .put("status", r.nextBoolean() ? "available" : "occupied");
                arrayNode.add(jsonObject);
            }

            String body;
            try {
                body = mapper.writeValueAsString(arrayNode);
            } catch (JsonProcessingException e) {
                throw new BridgeIoTException("Processing Json body failed!", e);
            }

            return BridgeIotHttpResponse.okay().withJsonArrayBody(body);
        }
    };

    public static void main(String[] args) throws IncompleteOfferingDescriptionException, IOException,
            NotRegisteredException, InvalidOfferingException {

        // Initialize provider per configuration file
        ProviderSpark provider = ProviderSpark.create("example.properties").authenticate();

        // Construct Offering Description of your Offering incrementally
        RegisteredOffering offering = provider.createOfferingDescription("DemoParkingOffering")
                .withName("Demo Parking Offering").withCategory("urn:big-iot:ParkingSpaceCategory")
                .withTimePeriod(new DateTime("2018-01-01T0:00Z"), DateTime.now())
                .inRegion(BoundingBox.create(Location.create(42.1, 9.0), Location.create(43.2, 10.0)))
                .addInputData("longitude", "schema:longitude", ValueType.NUMBER)
                .addInputData("latitude", "schema:latitude", ValueType.NUMBER)
                .addInputData("radius", "schema:geoRadius", ValueType.NUMBER)
                .addOutputData("lon", "schema:longitude", ValueType.NUMBER)
                .addOutputData("lat", "schema:latitude", ValueType.NUMBER)
                .addOutputData("dist", "datex:distanceFromParkingSpace", ValueType.NUMBER)
                .addOutputData("status", "datex:parkingSpaceStatus", ValueType.TEXT).withPrice(Euros.amount(0.02))
                .withPricingModel(PricingModel.PER_ACCESS).withLicenseType(LicenseType.CREATIVE_COMMONS)
                .withAccessRequestHandler(accessCallback).register();

        // Run until user input is obtained
        System.out.println(">>>>>>  Terminate ExampleProvider by pressing ENTER  <<<<<<");
        Scanner keyboard = new Scanner(System.in);
        keyboard.nextLine();
        keyboard.close();

        // Deregister your offering form Marketplace
        offering.deregister();

        // Terminate provider instance
        provider.terminate();

    }

}
