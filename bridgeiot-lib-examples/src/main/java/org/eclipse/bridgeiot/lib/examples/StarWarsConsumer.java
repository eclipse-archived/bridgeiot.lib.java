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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.bridgeiot.lib.Consumer;
import org.eclipse.bridgeiot.lib.exceptions.IncompleteOfferingQueryException;
import org.eclipse.bridgeiot.lib.misc.BridgeIotProperties;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.LicenseType;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.PricingModel;
import org.eclipse.bridgeiot.lib.model.Price.Euros;
import org.eclipse.bridgeiot.lib.offering.AccessResponse;
import org.eclipse.bridgeiot.lib.offering.Offering;
import org.eclipse.bridgeiot.lib.offering.OfferingSelector;
import org.eclipse.bridgeiot.lib.offering.SubscribableOfferingDescription;
import org.eclipse.bridgeiot.lib.offering.parameters.AccessParameters;
import org.eclipse.bridgeiot.lib.query.OfferingQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StarWarsConsumer {

    private static final Logger logger = LoggerFactory.getLogger(StarWarsConsumer.class);

    public static void main(String[] args)
            throws IOException, IncompleteOfferingQueryException, InterruptedException, ExecutionException {

        // Load example properties file
        BridgeIotProperties prop = BridgeIotProperties.load("example.properties");

        // Initialize Consumer with Consumer ID and marketplace URL
        Consumer consumer = new Consumer(prop.CONSUMER_ID, prop.MARKETPLACE_URI);

        // consumer.setProxy("127.0.0.1", 3128); //Enable this line if you are behind a proxy
        // consumer.addProxyBypass("172.17.17.100"); //Enable this line and the addresses for internal hosts

        // Authenticate provider on the marketplace (not yet supported)
        consumer.authenticate(prop.CONSUMER_SECRET);

        // Construct Offering search query incrementally
        OfferingQuery query = OfferingQuery.create("starWarsQuery").withName("Star Wars Registry")
                .withCategory("urn:proposed:Miscellaneous").withMaxPrice(Euros.amount(0.1))
                .withPricingModel(PricingModel.PER_ACCESS).withLicenseType(LicenseType.OPEN_DATA_LICENSE);

        CompletableFuture<SubscribableOfferingDescription> offeringDescriptionFuture = consumer.discover(query)
                .thenApply(SubscribableOfferingDescription::showOfferingDescriptions)
                .thenApply(l -> OfferingSelector.create().cheapest().mostPermissive().select(l));

        SubscribableOfferingDescription offeringDescription = offeringDescriptionFuture.get();

        if (offeringDescription == null) {
            logger.error(
                    "Couldn't find any matching offering description. Are sure that one is registered? It could be also expired meanwhile!");
            System.exit(1);
        }
        Offering offering = offeringDescription.subscribe().get();

        // Select your favorite Star Wars information
        //
        // resourceType =
        // people
        // planets
        // films
        // species
        // vehicles
        // starships
        //
        // resourceId is an INTEGER. null shows all

        AccessParameters accessParameters = AccessParameters.create().addNameValue("resourceType", "species")
                .addNameValue("resourceId", 3);

        AccessResponse response = offering.accessOneTime(accessParameters).get();

        System.out.println("Receiving:\n\n" + response.getPrettyPrint());
    }

}
