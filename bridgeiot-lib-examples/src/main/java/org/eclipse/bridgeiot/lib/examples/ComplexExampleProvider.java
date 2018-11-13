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
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import org.eclipse.bridgeiot.lib.ProviderSpark;
import org.eclipse.bridgeiot.lib.exceptions.IncompleteOfferingDescriptionException;
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
import org.eclipse.bridgeiot.lib.offering.parameters.NumberParameter;
import org.eclipse.bridgeiot.lib.offering.parameters.ObjectParameter;
import org.eclipse.bridgeiot.lib.serverwrapper.BridgeIotHttpResponse;
import org.joda.time.DateTime;

/**
 * Example for using Bridge.IoT API as a provider.
 */
public class ComplexExampleProvider {

    private static AccessRequestHandler accessCallbackDummy = new AccessRequestHandler() {
        @Override
        public BridgeIotHttpResponse processRequestHandler(OfferingDescription offeringDescription,
                Map<String, Object> inputData, String subscriptionId, String consumerInfo) {

            double longitude = 0, latitude = 0, radius = 0;

            BridgeIotHttpResponse errorResponse = BridgeIotHttpResponse.error().withBody("{\"status\":\"error\"}")
                    .withStatus(422).asJsonType();
            if (!inputData.containsKey("center"))
                return errorResponse;
            Map<String, String> center = (Map<String, String>) inputData.get("center");

            if (!center.containsKey("longitude"))
                return errorResponse;
            longitude = new Double((String) center.get("longitude"));
            if (!center.containsKey("latitude"))
                return errorResponse;
            latitude = new Double((String) center.get("latitude"));

            if (!inputData.containsKey("radius"))
                return errorResponse;
            radius = new Double((String) inputData.get("radius"));

            Random r = new Random();
            int n = Math.round(r.nextFloat() * 10 + 10);
            String s = "[";

            for (int i = 0; i < n; i++) {
                if (i > 0)
                    s += ",\n";
                s += String.format(Locale.US,
                        "{\"geoCoordinates\":{\n\"latitude\": %.4f,\n\"longitude\": %.4f},\n\"distance\": %.2f,\n\"status\":\"available\"\n}",
                        r.nextFloat() * 0.01 + latitude, r.nextFloat() * 0.01 + longitude, r.nextFloat() * radius);
            }
            s += "]";

            return BridgeIotHttpResponse.okay().withBody(s).asJsonType();

        }
    };

    public static void main(String[] args)
            throws IncompleteOfferingDescriptionException, IOException, NotRegisteredException {

        // Initialize provider per configuration file
        ProviderSpark provider = ProviderSpark.create("example.properties").authenticate();

        // Construct Offering Description of your Offering incrementally
        RegisteredOffering offering = provider.createOfferingDescription("ComplexParkingSpotProvider")
                .withName("Complex Demo Parking Offering")
                .withTimePeriod(new DateTime("2018-01-01T0:00Z"), DateTime.now())
                .inRegion(BoundingBox.create(Location.create(48.05, 11.6), Location.create(48.15, 11.7)))
                .withCategory("urn:proposed:Miscellaneous").addInputData("radius", "schema:geoRadius", ValueType.NUMBER)
                .addInputData("center", "schema:geoMidpoint", ObjectParameter.create()
                        .addMember("latitude", "schema:latitude", NumberParameter.create(-90.0, 90.0), true)
                        .addMember("longitude", "schema:longitude", NumberParameter.create(-180.0, 180.0), true), true)
                .addOutputData("center", "schema:geoMidpoint", ObjectParameter.create()
                        .addMember("latitude", "schema:latitude", NumberParameter.create(-90.0, 90.0), true)
                        .addMember("longitude", "schema:longitude", NumberParameter.create(-180.0, 180.0), true), true)
                .addOutputData("distance", "datex:distanceFromParkingSpace", ValueType.NUMBER)
                .addOutputData("status", "datex:parkingSpaceStatus", ValueType.TEXT).withPrice(Euros.amount(0.001))
                .withPricingModel(PricingModel.PER_ACCESS).withLicenseType(LicenseType.CREATIVE_COMMONS)
                .withAccessRequestHandler(accessCallbackDummy).register();

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
