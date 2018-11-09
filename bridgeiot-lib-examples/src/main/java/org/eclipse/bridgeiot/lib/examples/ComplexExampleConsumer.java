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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.bridgeiot.lib.Consumer;
import org.eclipse.bridgeiot.lib.examples.types.AlternativeParkingPojo;
import org.eclipse.bridgeiot.lib.examples.types.MyComplexParkingResultPojo;
import org.eclipse.bridgeiot.lib.examples.types.MyComplexParkingResultPojoAnnotated;
import org.eclipse.bridgeiot.lib.exceptions.AccessToNonActivatedOfferingException;
import org.eclipse.bridgeiot.lib.exceptions.AccessToNonSubscribedOfferingException;
import org.eclipse.bridgeiot.lib.exceptions.IncompleteOfferingQueryException;
import org.eclipse.bridgeiot.lib.feed.AccessFeed;
import org.eclipse.bridgeiot.lib.misc.Helper;
import org.eclipse.bridgeiot.lib.model.BoundingBox;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.LicenseType;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.ValueType;
import org.eclipse.bridgeiot.lib.model.Location;
import org.eclipse.bridgeiot.lib.model.Price.Euros;
import org.eclipse.bridgeiot.lib.model.TimePeriod;
import org.eclipse.bridgeiot.lib.offering.AccessResponse;
import org.eclipse.bridgeiot.lib.offering.Offering;
import org.eclipse.bridgeiot.lib.offering.OfferingSelector;
import org.eclipse.bridgeiot.lib.offering.SubscribableOfferingDescription;
import org.eclipse.bridgeiot.lib.offering.mapping.OutputMapping;
import org.eclipse.bridgeiot.lib.offering.parameters.AccessParameters;
import org.eclipse.bridgeiot.lib.query.OfferingQuery;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example for using Bridge.IoT API as a consumer.
 * 
 * 
 */
public class ComplexExampleConsumer {

    private static final Logger logger = LoggerFactory.getLogger(ComplexExampleConsumer.class);

    /*
     * Main Routine
     */
    public static void log(Object s) {
        if (logger.isInfoEnabled()) {
            logger.info(s.toString());
        }
    }

    public static void main(String[] args)
            throws InterruptedException, ExecutionException, IncompleteOfferingQueryException, IOException,
            AccessToNonSubscribedOfferingException, AccessToNonActivatedOfferingException {

        // Initialize per configuration file
        Consumer consumer = Consumer.create("example.properties").authenticate();

        // Construct Offering search query incrementally
        OfferingQuery query = OfferingQuery.create("ParkingQuery").withName("Parking Query")
                .withCategory("urn:proposed:Miscellaneous")
                .withTimePeriod(TimePeriod.create(new DateTime("2018-01-01T0:00Z"), DateTime.now()))
                .inRegion(BoundingBox.create(Location.create(48.07, 11.65), Location.create(48.13, 11.67)))
                .addInputData("schema:geoMidpoint").addOutputData("datex:parkingSpaceStatus", ValueType.TEXT)
                .addOutputData("datex:distanceFromParkingSpace", ValueType.UNDEFINED)
                .withPricingModel(BridgeIotTypes.PricingModel.PER_ACCESS).withMaxPrice(Euros.amount(0.5))
                .withLicenseType(LicenseType.CREATIVE_COMMONS);

        CompletableFuture<SubscribableOfferingDescription> offeringDescriptionFuture = consumer.discover(query)
                .thenApply(SubscribableOfferingDescription::showOfferingDescriptions)
                .thenApply(l -> OfferingSelector.create().onlyLocalhost().cheapest().mostPermissive().select(l));

        SubscribableOfferingDescription offeringDescription = offeringDescriptionFuture.get();
        if (offeringDescription == null) {
            logger.error("Couldn't find any offering. Are sure that one is registered? It could be expired meanwhile");
            System.exit(1);
        }

        // Instantiation of Offering Access objects via subscribe
        CompletableFuture<Offering> offeringFuture = offeringDescription.subscribe();
        Offering offering = offeringFuture.get();

        // // Prepare access parameters
        AccessParameters accessParameters = AccessParameters.create().addNameValue("radius", 500).addNameValue("center",
                AccessParameters.create().addNameValue("latitude", 48.10).addNameValue("longitude", 11.23));

        CompletableFuture<AccessResponse> response = offering.accessOneTime(accessParameters);

        if (response.get().getBody().contains("error")) {
            throw new RuntimeException(response.get().getBody());
        } else {
            log("One time Offering access: " + response.get().asJsonNode().size() + " elements received. ");
        }

        // Mapping the response automatically to your pojo
        List<MyComplexParkingResultPojoAnnotated> parkingResult = response.get()
                .map(MyComplexParkingResultPojoAnnotated.class);

        // Alternatively you can manually map your response
        List parkingResult2 = response.get().map(MyComplexParkingResultPojo.class,
                OutputMapping.create().addTypeMapping("schema:geoCoordinates", "myCoordinate")
                        .addTypeMapping("datex:distanceFromParkingSpace", "myDistance")
                        .addTypeMapping("datex:parkingSpaceStatus", "myStatus"));

        // Or you can do your own mapping cherry-picking your favorite fields
        List parkingResult3 = response.get().map(AlternativeParkingPojo.class,
                OutputMapping.create().addNameMapping("geoCoordinates.latitude", "coordinates.latitude")
                        .addNameMapping("geoCoordinates.longitude", "coordinates.longitude")
                        .addNameMapping("distance", "meters"));

        Thread.sleep(5L * Helper.Second);

        Duration feedDuration = Duration.standardHours(1);

        // Create a data feed using callbacks for the received results
        AccessFeed accessFeed = offering.accessContinuous(accessParameters, feedDuration.getMillis(),
                (f, r) -> log("Incoming feed data: " + r.asJsonNode().size() + " elements received. "),
                (f, r) -> log("Feed operation failed"));

        Thread.sleep(23L * Helper.Second);

        // Pausing Feed
        accessFeed.stop();

        // Printing feed status
        log(accessFeed.getStatus());

        Thread.sleep(10L * Helper.Second);

        // Resuming Feed
        accessFeed.resume();

        Thread.sleep(10L * Helper.Second);

        // Setting a new lifetime for the feed
        accessFeed.setLifetimeSeconds(5000);

        Thread.sleep(10L * Helper.Second);

        accessFeed.stop();

        // Unsubscribe Offering
        offering.unsubscribe();

        // Terminate consumer session (unsubscribe from marketplace)
        consumer.terminate();

    }
}
