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
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.bridgeiot.lib.Consumer;
import org.eclipse.bridgeiot.lib.examples.types.Weather;
import org.eclipse.bridgeiot.lib.exceptions.AccessToNonActivatedOfferingException;
import org.eclipse.bridgeiot.lib.exceptions.AccessToNonSubscribedOfferingException;
import org.eclipse.bridgeiot.lib.exceptions.IncompleteOfferingQueryException;
import org.eclipse.bridgeiot.lib.feed.AccessFeed;
import org.eclipse.bridgeiot.lib.misc.BridgeIotProperties;
import org.eclipse.bridgeiot.lib.misc.Helper;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.LicenseType;
import org.eclipse.bridgeiot.lib.model.Price.Euros;
import org.eclipse.bridgeiot.lib.offering.AccessResponse;
import org.eclipse.bridgeiot.lib.offering.Offering;
import org.eclipse.bridgeiot.lib.offering.OfferingSelector;
import org.eclipse.bridgeiot.lib.offering.SubscribableOfferingDescription;
import org.eclipse.bridgeiot.lib.offering.parameters.AccessParameters;
import org.eclipse.bridgeiot.lib.query.OfferingQuery;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example for using Bridge.IoT API as a consumer.
 * 
 * An offering is consumed for the weather service of Open Weather Map.
 */

public class OwmWeatherConsumer {

    private static final Logger logger = LoggerFactory.getLogger(OwmWeatherConsumer.class);

    public static void main(String[] args)
            throws InterruptedException, ExecutionException, IncompleteOfferingQueryException, IOException,
            AccessToNonSubscribedOfferingException, AccessToNonActivatedOfferingException {

        // Load example properties file
        BridgeIotProperties prop = BridgeIotProperties.load("example.properties");

        // Initialize Consumer with Consumer ID and marketplace URL
        Consumer consumer = new Consumer(prop.CONSUMER_ID, prop.MARKETPLACE_URI);

        // consumer.setProxy("127.0.0.1", 3128); //Enable this line if you are behind a proxy
        // consumer.addProxyBypass("172.17.17.100"); //Enable this line and the addresses for internal hosts

        // Authenticate consumer on the marketplace
        consumer.authenticate(prop.CONSUMER_SECRET);

        // Construct Offering Query incrementally
        OfferingQuery query = OfferingQuery.create().withName("Star Wars Registry")
                .withCategory("urn:proposed:Miscellaneous").withPricingModel(BridgeIotTypes.PricingModel.PER_ACCESS)
                .withMaxPrice(Euros.amount(0.002)).withLicenseType(LicenseType.OPEN_DATA_LICENSE);

        // Discover available offerings based on Offering Query
        CompletableFuture<SubscribableOfferingDescription> offeringDescriptionFuture = consumer.discover(query)
                .thenApply(SubscribableOfferingDescription::showOfferingDescriptions)
                .thenApply(l -> OfferingSelector.create().onlyLocalhost().cheapest().mostPermissive().select(l));

        // Complete Future and get Offering Description
        SubscribableOfferingDescription offeringDescription = offeringDescriptionFuture.get();

        if (offeringDescription != null) {

            // Subscribe to a selected OfferingDescription (if successful, returns accessible Offering instance)
            CompletableFuture<Offering> offeringFuture = offeringDescription.subscribe();
            Offering offering = offeringFuture.get();

            // Prepare Access Parameters
            AccessParameters accessParameters = AccessParameters.create().addRdfTypeValue("schema:latitude", 48.0904)
                    .addRdfTypeValue("schema:longitude", 11.6469);

            CompletableFuture<AccessResponse> responseFuture = offering.accessOneTime(accessParameters);

            AccessResponse response = responseFuture.get();

            if (response.getBody().contains("error")) {
                throw new RuntimeException(response.getBody());
            } else {
                logger.error("One time Offering access: {} elements received. ", response.asJsonNode().size());
            }

            if (logger.isInfoEnabled()) {
                logger.info("Got this JSON response");
                logger.info(Helper.jsonPrettyPrint(response.getBody()));
            }

            List<Weather> weatherResult = responseFuture.get().map(Weather.class);

            for (Iterator<?> iterator = weatherResult.iterator(); iterator.hasNext();) {
                Weather weather = (Weather) iterator.next();
                logger.info("\nMapped PoJo is follows: {}\n", weather);
            }

            // Create an Access Feed with callbacks for the received results
            Duration feedDuration = Duration.standardHours(1);
            Duration feedInterval = Duration.standardSeconds(10);
            AccessFeed accessFeed = offering.accessContinuous(accessParameters, feedDuration.getMillis(),
                    feedInterval.getMillis(),
                    (f, r) -> logger.info("Incoming feed data: " + r.asJsonNode().size() + " elements received. "),
                    (f, r) -> logger.info("Feed operation failed"));

            // Run until user presses the ENTER key
            System.out.println(">>>>>>  Terminate ExampleConsumer by pressing ENTER  <<<<<<");
            Scanner keyboard = new Scanner(System.in);
            keyboard.nextLine();
            keyboard.close();

            // Stop Access Feed
            accessFeed.stop();

            // Unsubscribe the Offering
            offering.unsubscribe();

        } else {
            // No active Offerings could be discovered
            logger.error("Couldn't find any offering. Are sure that one is registered? It could be expired meanwhile");
        }

        // Terminate consumer instance
        consumer.terminate();

    }

}
