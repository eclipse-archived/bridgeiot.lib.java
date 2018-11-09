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

import org.eclipse.bridgeiot.lib.ProviderSpark;
import org.eclipse.bridgeiot.lib.exceptions.BridgeIoTException;
import org.eclipse.bridgeiot.lib.exceptions.IncompleteOfferingDescriptionException;
import org.eclipse.bridgeiot.lib.exceptions.NotRegisteredException;
import org.eclipse.bridgeiot.lib.handlers.AccessStreamFilterHandler;
import org.eclipse.bridgeiot.lib.misc.BridgeIotProperties;
import org.eclipse.bridgeiot.lib.model.BoundingBox;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.LicenseType;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.PricingModel;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.ValueType;
import org.eclipse.bridgeiot.lib.model.Location;
import org.eclipse.bridgeiot.lib.model.Price.Euros;
import org.eclipse.bridgeiot.lib.offering.Endpoints;
import org.eclipse.bridgeiot.lib.offering.JsonObject;
import org.eclipse.bridgeiot.lib.offering.OfferingDescription;
import org.eclipse.bridgeiot.lib.offering.RegisteredOffering;
import org.eclipse.bridgeiot.lib.offering.RegistrableOfferingDescription;
import org.joda.time.DateTime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Example for using Bridge.IoT API as a provider.
 */
public class ExampleProviderAccessStream {

    static ObjectMapper mapper = new ObjectMapper();

    private static AccessStreamFilterHandler accessStreamFilterCallback = new AccessStreamFilterHandler() {
        @Override
        public boolean processRequestHandler(OfferingDescription offeringDescription, JsonObject json,
                Map<String, Object> inputData, String subscriptionId, String consumerInfo) {

            double longitude = 9.0;
            if (inputData.containsKey("longitude")) {
                longitude = new Double((String) inputData.get("longitude"));
            }

            double latitude = 42.0;
            if (inputData.containsKey("latitude")) {
                latitude = new Double((String) inputData.get("latitude"));
            }

            JsonNode jsonNode;
            try {
                jsonNode = mapper.reader().readTree(json.write());
            } catch (IOException e) {
                throw new BridgeIoTException("Processing Json body failed!", e);
            }

            double lon = jsonNode.get("lon").asDouble();
            double lat = jsonNode.get("lat").asDouble();

            return ((Math.abs(latitude - lat) < 0.005) || (Math.abs(longitude - lon) < 0.005));
        }
    };

    public static void main(String[] args)
            throws InterruptedException, IncompleteOfferingDescriptionException, IOException, NotRegisteredException {

        // Load example properties file
        BridgeIotProperties prop = BridgeIotProperties.load("example.properties");

        // Initialize provider with provider id and Marketplace URI
        ProviderSpark provider = new ProviderSpark(prop.PROVIDER_ID, prop.MARKETPLACE_URI, prop.PROVIDER_DNS_NAME,
                prop.PROVIDER_PORT);

        // provider.setProxy("127.0.0.1", 3128); //Enable this line if you are behind a proxy
        // provider.addProxyBypass("172.17.17.100"); //Enable this line and the addresses for internal hosts

        // Authenticate provider on the marketplace
        provider.authenticate(prop.PROVIDER_SECRET);

        // Construct Offering Description of your Offering incrementally
        RegistrableOfferingDescription offeringDescription =
                // provider.createOfferingDescriptionFromOfferingId("TestOrganization-TestProvider-Manual_Offering_Test")
                OfferingDescription.createOfferingDescription("DemoParkingOffering_WithAccessStream")
                        .withName("Demo Parking Offering with Access Stream")
                        .withCategory("urn:big-iot:ParkingSpaceCategory")
                        .withTimePeriod(new DateTime(2017, 1, 1, 0, 0, 0), new DateTime())
                        .inRegion(BoundingBox.create(Location.create(42.1, 9.0), Location.create(43.2, 10.0)))
                        // .inCity("Barcelona")
                        .addInputData("longitude", "schema:longitude", ValueType.NUMBER)
                        .addInputData("latitude", "schema:latitude", ValueType.NUMBER)
                        .addInputData("radius", "schema:geoRadius", ValueType.NUMBER)
                        .addOutputData("lon", "schema:longitude", ValueType.NUMBER)
                        .addOutputData("lat", "schema:latitude", ValueType.NUMBER)
                        .addOutputData("status", "datex:parkingSpaceStatus", ValueType.TEXT)
                        .withPrice(Euros.amount(0.02)).withPricingModel(PricingModel.PER_ACCESS)
                        .withLicenseType(LicenseType.CREATIVE_COMMONS);

        Endpoints endpoints = Endpoints.create(offeringDescription)
                .withAccessStreamFilterHandler(accessStreamFilterCallback); // Optional only if filtering needed

        RegisteredOffering offering = provider.register(offeringDescription, endpoints);

        // Run until user input is obtained
        System.out.println(">>>>>>  Terminate ExampleProvider by pressing ENTER  <<<<<<");

        int i = 0;
        Random r = new Random();
        while (System.in.available() == 0) {

            ObjectNode jsonObject = mapper.createObjectNode().put("lat", 42.0 + r.nextFloat() * 0.01)
                    .put("lon", 9.0 + r.nextFloat() * 0.01).put("status", r.nextBoolean() ? "available" : "occupied");

            String jsonString;
            try {
                jsonString = mapper.writeValueAsString(jsonObject);
            } catch (JsonProcessingException e) {
                throw new BridgeIoTException("Processing Json body failed!", e);
            }

            // add new Output Data element to the Offering Access Stream=
            offering.queue(new JsonObject(jsonString));

            System.out.println("Add output data element: " + jsonObject.toString());

            Thread.sleep(4000);

            // Optional
            if (i++ % 100 == 0) {
                offering.flush(); // To flush old elements in the Offering Access Stream
                System.out.println("Flushed offering access stream");
            }

        }

        // Deregister your offering form Marketplace
        offering.deregister();

        // Terminate provider instance
        provider.terminate();

    }

}
