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
import org.eclipse.bridgeiot.lib.exceptions.NotRegisteredException;
import org.eclipse.bridgeiot.lib.handlers.AccessRequestHandler;
import org.eclipse.bridgeiot.lib.misc.BridgeIotProperties;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.LicenseType;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.PricingModel;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.ValueType;
import org.eclipse.bridgeiot.lib.model.Price.Euros;
import org.eclipse.bridgeiot.lib.offering.OfferingDescription;
import org.eclipse.bridgeiot.lib.offering.RegistrableOfferingDescription;
import org.eclipse.bridgeiot.lib.serverwrapper.BridgeIotHttpResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Example for using Bridge.IoT API as a provider.
 */
public class ExampleProviderWithSampleAndMetaData {

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

    private static AccessRequestHandler metaDataCallback = new AccessRequestHandler() {
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
                ObjectNode jsonObject = mapper.createObjectNode().put("lat", latitude + r.nextFloat() * 0.01).put("lon",
                        longitude + r.nextFloat() * 0.01);
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

    public static void main(String[] args)
            throws IncompleteOfferingDescriptionException, IOException, NotRegisteredException {

        // Load example properties file
        BridgeIotProperties prop = BridgeIotProperties.load("example.properties");

        // Initialize provider with provider id and Marketplace URI
        ProviderSpark provider = ProviderSpark.create(prop.PROVIDER_ID, prop.MARKETPLACE_URI, prop.PROVIDER_DNS_NAME,
                prop.PROVIDER_PORT);

        // provider.setProxy("127.0.0.1", 3128); //Enable this line if you are behind a proxy
        // provider.addProxyBypass("172.17.17.100"); //Enable this line and the addresses for internal hosts

        // Authenticate provider on the marketplace
        provider.authenticate(prop.PROVIDER_SECRET);

        // Construct Offering Description of your Offering incrementally
        RegistrableOfferingDescription offeringDescription =
                // provider.createOfferingDescriptionFromOfferingId("TestOrganization-TestProvider-Manual_Offering_Test")
                provider.createOfferingDescription("ParkingSpotProvider").withName("Demo Parking Offering")
                        .withCategory("urn:big-iot:ParkingSpaceCategory")
                        .addInputData("longitude", "schema:longitude", ValueType.NUMBER)
                        .addInputData("latitude", "schema:latitude", ValueType.NUMBER)
                        .addInputData("radius", "schema:geoRadius", ValueType.NUMBER)
                        .addOutputData("lon", "schema:longitude", ValueType.NUMBER)
                        .addOutputData("lat", "schema:latitude", ValueType.NUMBER)
                        .addOutputData("dist", "datex:distanceFromParkingSpace", ValueType.NUMBER)
                        .addOutputData("status", "datex:parkingSpaceStatus", ValueType.TEXT)
                        // .inCity("Barcelona")
                        .withPrice(Euros.amount(0.001)).withPricingModel(PricingModel.PER_ACCESS)
                        .withLicenseType(LicenseType.OPEN_DATA_LICENSE)
                        // Below is actually Offering specific
                        .withSampleDataRequestHandler(accessCallback).withMetaDataRequestHandler(metaDataCallback)
                        .withAccessRequestHandler(accessCallback);

        provider.register(offeringDescription);

        // Run until user input is obtained
        System.out.println(">>>>>>  Terminate ExampleProvider by pressing ENTER  <<<<<<");
        Scanner keyboard = new Scanner(System.in);
        keyboard.nextLine();
        keyboard.close();

        // Deregister your offering form Marketplace
        provider.deregister(offeringDescription);

        // Terminate provider instance
        provider.terminate();

    }

}
