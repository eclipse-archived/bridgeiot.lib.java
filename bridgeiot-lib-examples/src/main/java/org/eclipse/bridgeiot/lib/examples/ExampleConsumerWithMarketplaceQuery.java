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
import org.eclipse.bridgeiot.lib.examples.types.MyParkingResultPojoAnnotated;
import org.eclipse.bridgeiot.lib.exceptions.AccessToNonActivatedOfferingException;
import org.eclipse.bridgeiot.lib.exceptions.AccessToNonSubscribedOfferingException;
import org.eclipse.bridgeiot.lib.exceptions.IncompleteOfferingQueryException;
import org.eclipse.bridgeiot.lib.misc.BridgeIotProperties;
import org.eclipse.bridgeiot.lib.offering.AccessResponse;
import org.eclipse.bridgeiot.lib.offering.Offering;
import org.eclipse.bridgeiot.lib.offering.SubscribableOfferingDescription;
import org.eclipse.bridgeiot.lib.offering.parameters.AccessParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example for using Bridge.IoT API as a consumer.
 * 
 * 
 */
public class ExampleConsumerWithMarketplaceQuery {

    private static final Logger logger = LoggerFactory.getLogger(ExampleConsumerWithMarketplaceQuery.class);

    /*
     * Main Routine
     */

    public static void main(String[] args)
            throws InterruptedException, ExecutionException, IncompleteOfferingQueryException, IOException,
            AccessToNonSubscribedOfferingException, AccessToNonActivatedOfferingException {

        // Load example properties file
        BridgeIotProperties prop = BridgeIotProperties.load("example.properties");

        // Initialize Consumer with Consumer ID and marketplace URL
        Consumer consumer = new Consumer(prop.CONSUMER_ID, prop.MARKETPLACE_URI).authenticate(prop.CONSUMER_SECRET);

        // Discover matching offerings (by Query ID) based on a pre-defined Query on the Marketplace
        List<SubscribableOfferingDescription> offeringDescriptionList = consumer
                .discoverById("TestOrganization-TestConsumer-DemoParkingQuery").get();

        if (offeringDescriptionList.isEmpty()) {
            logger.error("Couldn't find any offering. Are sure that one is registered? It could be expired meanwhile");
            System.exit(1);
        }

        // Select 1st offering in list
        SubscribableOfferingDescription offeringDescription = offeringDescriptionList.get(0);

        // Instantiation of Offering Access objects via subscribe
        Offering offering = offeringDescription.subscribe().get();

        // Prepare access parameters
        AccessParameters accessParameters = AccessParameters.create().addRdfTypeValue("schema:latitude", 42.0)
                .addRdfTypeValue("schema:longitude", 9.0);

        CompletableFuture<AccessResponse> response = offering.accessOneTime(accessParameters);

        if (response.get().getBody().contains("error")) {
            throw new RuntimeException(response.get().getBody());
        } else {
            logger.info("One time Offering access: " + response.get().asJsonNode().size() + " elements received. ");
        }

        // Mapping the response automatically to your pojo
        List<MyParkingResultPojoAnnotated> parkingResult = response.get().map(MyParkingResultPojoAnnotated.class);
        parkingResult.forEach(t -> logger.info("Record: " + t.toString()));

        // Unsubscribe Offering
        offering.unsubscribe();

        // Terminate consumer session (unsubscribe from marketplace)
        consumer.terminate();

    }
}
