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
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

import org.eclipse.bridgeiot.lib.Consumer;
import org.eclipse.bridgeiot.lib.examples.types.MyParkingResultPojoAnnotated;
import org.eclipse.bridgeiot.lib.exceptions.AccessToNonActivatedOfferingException;
import org.eclipse.bridgeiot.lib.exceptions.AccessToNonSubscribedOfferingException;
import org.eclipse.bridgeiot.lib.exceptions.IncompleteOfferingQueryException;
import org.eclipse.bridgeiot.lib.handlers.DiscoverFailureException;
import org.eclipse.bridgeiot.lib.handlers.DiscoverResponseErrorHandler;
import org.eclipse.bridgeiot.lib.handlers.DiscoverResponseHandler;
import org.eclipse.bridgeiot.lib.misc.BridgeIotProperties;
import org.eclipse.bridgeiot.lib.model.BoundingBox;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.LicenseType;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.ValueType;
import org.eclipse.bridgeiot.lib.model.Location;
import org.eclipse.bridgeiot.lib.model.Price.Euros;
import org.eclipse.bridgeiot.lib.model.TimePeriod;
import org.eclipse.bridgeiot.lib.offering.AccessResponse;
import org.eclipse.bridgeiot.lib.offering.Offering;
import org.eclipse.bridgeiot.lib.offering.SubscribableOfferingDescription;
import org.eclipse.bridgeiot.lib.offering.parameters.AccessParameters;
import org.eclipse.bridgeiot.lib.query.IOfferingQuery;
import org.eclipse.bridgeiot.lib.query.OfferingQuery;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example for using Bridge.IoT API as a consumer.
 * 
 * 
 */
public class ExampleConsumerDiscoverContinuous {

    private static final Logger logger = LoggerFactory.getLogger(ExampleConsumerDiscoverContinuous.class);

    private static DiscoverResponseHandler discoverResponseHandler = new DiscoverResponseHandler() {
        @Override
        public void processResponse(IOfferingQuery reference, List offeringDescriptions) {

            logger.info("Discovered {} offerings", offeringDescriptions.size());

            if (offeringDescriptions.size() == 0)
                return;

            SubscribableOfferingDescription offeringDescription = (SubscribableOfferingDescription) offeringDescriptions
                    .get(0);

            try {
                Offering offering = offeringDescription.subscribe().get();

                AccessParameters accessParameters = AccessParameters.create().addRdfTypeValue("schema:latitude", 42.0)
                        .addRdfTypeValue("schema:longitude", 9.0).addRdfTypeValue("schema:geoRadius", 777);

                AccessResponse response = offering.accessOneTime(accessParameters).get();

                logger.info("One time Offering access: " + response.asJsonNode().size() + " elements received.");

                // Mapping the response automatically to your pojo
                List<MyParkingResultPojoAnnotated> parkingResult = response.map(MyParkingResultPojoAnnotated.class);
                // parkingResult.forEach(t -> logger.info("Record: " + t.toString()));

                offering.unsubscribe();

            } catch (InterruptedException | ExecutionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    };

    private static DiscoverResponseErrorHandler discoverResponseErrorHandler = new DiscoverResponseErrorHandler() {
        @Override
        public void processResponse(IOfferingQuery reference, DiscoverFailureException failure) {
            logger.error("Discovery error");
        }
    };

    public static void main(String[] args)
            throws InterruptedException, ExecutionException, IncompleteOfferingQueryException, IOException,
            AccessToNonSubscribedOfferingException, AccessToNonActivatedOfferingException {

        // Load example properties file
        BridgeIotProperties prop = BridgeIotProperties.load("example.properties");

        // Initialize Consumer with Consumer ID and marketplace URL
        Consumer consumer = new Consumer(prop.CONSUMER_ID, prop.MARKETPLACE_URI).authenticate(prop.CONSUMER_SECRET);

        // Construct Offering search query incrementally

        OfferingQuery query = OfferingQuery.create("DemoParkingQueryContinuous")
                .withName("Demo Parking Query Continuous").withCategory("urn:big-iot:ParkingSpaceCategory")
                .withTimePeriod(TimePeriod.create(new DateTime(1999, 1, 1, 0, 0, 0), new DateTime()))
                // .inCity("Barcelona")
                .inRegion(BoundingBox.create(Location.create(40.0, 8.0), Location.create(45.0, 12.0)))
                .addInputData("schema:longitude", ValueType.NUMBER).addInputData("schema:latitude", ValueType.NUMBER)
                // .addInputData("schema:geoRadius", ValueType.NUMBER)
                .addOutputData("schema:longitude", ValueType.NUMBER).addOutputData("schema:latitude", ValueType.NUMBER)
                // .addOutputData("datex:distanceFromParkingSpace", ValueType.NUMBER)
                .addOutputData("datex:parkingSpaceStatus", ValueType.TEXT)
                .withPricingModel(BridgeIotTypes.PricingModel.PER_ACCESS).withMaxPrice(Euros.amount(0.5))
                // .withPricingModel(BridgeIotTypes.PricingModel.FREE)
                .withLicenseType(LicenseType.CREATIVE_COMMONS);

        consumer.discoverContinous(query, discoverResponseHandler, discoverResponseErrorHandler, 10);

        // Run until user input is obtained
        System.out.println(">>>>>>  Terminate ExampleConsumer by pressing ENTER  <<<<<<");
        Scanner keyboard = new Scanner(System.in);
        keyboard.nextLine();
        keyboard.close();

        // Stop continuous discovery
        consumer.stopDiscoverContinuous(query);

        // Terminate consumer session (unsubscribe from marketplace)
        consumer.terminate();

    }
}
