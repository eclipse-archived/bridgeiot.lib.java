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
import org.eclipse.bridgeiot.lib.examples.types.MyParkingResultPojo;
import org.eclipse.bridgeiot.lib.examples.types.MyParkingResultPojoAnnotated;
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
 */
public class BasicConsumer {

    private static final String MARKETPLACE_URI = "https://market.big-iot.org";
    private static final String CONSUMER_ID = "TestOrganization-TestConsumer";
    private static final String CONSUMER_SECRET = "UDiR00ysTbqcOLRMn6dTTQ==";

    private static final Logger logger = LoggerFactory.getLogger(BasicConsumer.class);

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

        // Initialize Consumer with Consumer ID and marketplace URL
        Consumer consumer = new Consumer(CONSUMER_ID, MARKETPLACE_URI);

        // consumer.setProxy("127.0.0.1", 3128); //Enable this line if you are behind a proxy
        // consumer.addProxyBypass("172.17.17.100"); //Enable this line and the addresses for internal hosts

        // Authenticate provider on the marketplace
        consumer.authenticate(CONSUMER_SECRET);

        // Construct Offering search query incrementally
        OfferingQuery query = OfferingQuery.create("BasicDemoParkingSpot_Query").withName("Basic Demo Parking Query")
                .withCategory("urn:big-iot:ParkingSpaceCategory")
                .inRegion(BoundingBox.create(Location.create(42.2, 9.1), Location.create(43.0, 9.8)))
                // .inRegion("Germany")
                .withTimePeriod(new DateTime(2017, 1, 1, 0, 0, 0), new DateTime().plusDays(1))
                .addInputData("schema:longitude", ValueType.NUMBER).addInputData("schema:latitude", ValueType.NUMBER)
                .addOutputData("schema:longitude", ValueType.NUMBER).addOutputData("schema:latitude", ValueType.NUMBER)
                .addOutputData("datex:parkingSpaceStatus", ValueType.TEXT).withMaxPrice(Euros.amount(0.5))
                .withPricingModel(BridgeIotTypes.PricingModel.PER_ACCESS)
                .withLicenseType(LicenseType.OPEN_DATA_LICENSE);

        CompletableFuture<SubscribableOfferingDescription> offeringDescriptionFuture = consumer.discover(query)
                .thenApply(l -> OfferingSelector.create().onlyLocalhost().cheapest().mostPermissive().select(l));

        // Alternatively you can use discover with callbacks
        // consumer.discoverContinous(query, (q, list)-> {
        // log("Discovery with callback");
        // SubscribableOfferingDescription.showOfferingDescriptions(list);
        // }, null, 60 /*secs*/);

        SubscribableOfferingDescription offeringDescription = offeringDescriptionFuture.get();
        if (offeringDescription == null) {
            logger.error("Couldn't find any offering. Are sure that one is registered? It could be expired meanwhile");
            System.exit(1);
        }

        // Instantiation of Offering Access objects via subscribe
        Offering offering = offeringDescription.subscribe().get();

        // Prepare access parameters
        AccessParameters accessParameters = AccessParameters.create().addRdfTypeValue("schema:latitude", 42.0)
                .addRdfTypeValue("schema:longitude", 9.0).addRdfTypeValue("schema:geoRadius", 777);

        CompletableFuture<AccessResponse> response = offering.accessOneTime(accessParameters);

        if (response.get().getBody().contains("error")) {
            throw new RuntimeException(response.get().getBody());
        } else {
            log("One time Offering access: " + response.get().asJsonNode().size() + " elements received. ");
        }

        // Mapping the response automatically to your pojo
        List<MyParkingResultPojoAnnotated> parkingResult = response.get().map(MyParkingResultPojoAnnotated.class);
        parkingResult.forEach(t -> log("Record: " + t.toString()));

        // Alternatively you can manually map your response
        List parkingResult2 = response.get().map(MyParkingResultPojo.class,
                OutputMapping.create().addTypeMapping("schema:longitude", "longitude")
                        .addTypeMapping("schema:latitude", "latitude")
                        .addTypeMapping("datex:distanceFromParkingSpace", "distance")
                        .addTypeMapping("datex:parkingSpaceStatus", "status"));

        Duration feedDuration = Duration.standardHours(1);
        Duration feedInterval = Duration.standardSeconds(10);

        // Create a data feed using callbacks for the received results
        AccessFeed accessFeed = offering.accessContinuous(accessParameters, feedDuration.getMillis(),
                feedInterval.getMillis(),
                (f, r) -> log("Incoming feed data: " + r.asJsonNode().size() + " elements received. "),
                (f, r) -> log("Feed operation failed"));

        Thread.sleep(200L * Helper.Second);

        // Pausing Feed
        accessFeed.stop();

        // Unsubscribe Offering
        offering.unsubscribe();

        // Terminate consumer session (unsubscribe from marketplace)
        consumer.terminate();

    }
}
