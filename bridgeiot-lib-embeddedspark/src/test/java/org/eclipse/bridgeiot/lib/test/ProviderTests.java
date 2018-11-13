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
package org.eclipse.bridgeiot.lib.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import org.eclipse.bridgeiot.lib.BridgeIotClientId;
import org.eclipse.bridgeiot.lib.Consumer;
import org.eclipse.bridgeiot.lib.Provider;
import org.eclipse.bridgeiot.lib.ProviderSpark;
import org.eclipse.bridgeiot.lib.exceptions.BridgeIoTException;
import org.eclipse.bridgeiot.lib.exceptions.FailedDiscoveryException;
import org.eclipse.bridgeiot.lib.exceptions.IncompleteOfferingDescriptionException;
import org.eclipse.bridgeiot.lib.exceptions.IncompleteOfferingQueryException;
import org.eclipse.bridgeiot.lib.exceptions.NotRegisteredException;
import org.eclipse.bridgeiot.lib.handlers.AccessRequestHandler;
import org.eclipse.bridgeiot.lib.misc.BridgeIotProperties;
import org.eclipse.bridgeiot.lib.misc.MarketplaceClient;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.LicenseType;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.PricingModel;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.ValueType;
import org.eclipse.bridgeiot.lib.model.Price.Euros;
import org.eclipse.bridgeiot.lib.offering.OfferingDescription;
import org.eclipse.bridgeiot.lib.offering.RegisteredOffering;
import org.eclipse.bridgeiot.lib.offering.RegistrableOfferingDescription;
import org.eclipse.bridgeiot.lib.offering.SubscribableOfferingDescription;
import org.eclipse.bridgeiot.lib.offering.SubscribableOfferingDescriptionCore;
import org.eclipse.bridgeiot.lib.query.OfferingQuery;
import org.eclipse.bridgeiot.lib.serverwrapper.BridgeIotHttpResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

//import Exceptions.IncompleteofferingDescritionException;

public class ProviderTests {
    /**
     * Test the following things: Is it possible to register offering Is it possible to register offering by ID Is it
     * possible to discover a registered offering Is a deployed Offering accessible via consumer lib (1time, continuous)
     * Is a registered Offering discoverable via specific search queries (range, attributes, values, etc.) Is error
     * handling correct if offering registering fails
     * 
     * @throws Exception
     */

    private static BridgeIotProperties testProperties;

    private static Provider provider;
    private static Consumer consumer;
    private static OfferingQuery query;
    private static RegistrableOfferingDescription offeringDescription;
    // private static RegistrableOfferingDescription secondOfferingDescription;

    private final static String TEST_OFFERING_LOCALID = "TestOffering_EmbeddedSpark";
    private static String TEST_OFFERING_ID;

    private final static String FIND_OFF = "{\"query\": \"query { allOfferings { allOfferings { id } } }\"}";

    static ObjectMapper mapper = new ObjectMapper();

    final static Logger logger = LoggerFactory.getLogger(ProviderTests.class);

    private static Throwable testMarketplaceConnections() {

        MarketplaceClient helperClient = MarketplaceClient.create(testProperties.MARKETPLACE_URI,
                new BridgeIotClientId(testProperties.PROVIDER_ID), "");

        Throwable x = null;
        try {
            helperClient.request(FIND_OFF);
        } catch (IOException e) {
            x = new RuntimeException("Marketplace unavailable");
        }
        return x;
    }

    private static AccessRequestHandler accessCallbackDummy = new AccessRequestHandler() {
        @Override
        public BridgeIotHttpResponse processRequestHandler(OfferingDescription offeringDescription,
                Map<String, Object> inputData, String subscriptionId, String consumerInfo) {

            Random r = new Random();

            ObjectNode jsonObject = mapper.createObjectNode().put("number", Math.round(r.nextFloat() * 9));
            String body;
            try {
                body = mapper.writeValueAsString(jsonObject);
            } catch (JsonProcessingException e) {
                throw new BridgeIoTException("Processing Json body failed!", e);
            }

            return BridgeIotHttpResponse.okay().withJsonArrayBody(body);
        }
    };

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

        testProperties = BridgeIotProperties.load("src/test/resources/test.properties");

        org.junit.Assume.assumeNoException(testMarketplaceConnections());

        initializeConsumerAndQuery();

    }

    public static void initializeConsumerAndQuery() throws IOException, IncompleteOfferingQueryException {

        consumer = new Consumer(testProperties.CONSUMER_ID, testProperties.MARKETPLACE_URI);
        consumer.authenticate(testProperties.CONSUMER_SECRET);

        query = OfferingQuery.create("TestOfferingQuery_EmbeddedSpark").withName("TestOfferingQuery_EmbeddedSpark")
                .withCategory("urn:big-iot:ParkingSpaceCategory").inRegion("Barcelona")
                .withPricingModel(PricingModel.PER_MONTH).withMaxPrice(Euros.amount(5000))
                .withLicenseType(LicenseType.CREATIVE_COMMONS);

    }

    @Before
    public void setUp() throws Exception {

        // Create provider instance and offering
        provider = ProviderSpark.create(testProperties.PROVIDER_ID, testProperties.MARKETPLACE_URI, "localhost", 9876);
        provider.authenticate(testProperties.PROVIDER_SECRET);

        // Create simple OfferingDescription with an access endpoint that serves
        // a specific data item

        offeringDescription = provider.createOfferingDescription(TEST_OFFERING_LOCALID).withName(TEST_OFFERING_LOCALID)
                .withCategory("urn:big-iot:ParkingSpaceCategory")
                .addInputData("number", "http://schema.org/parkingNumberOfVacantSpaces", ValueType.NUMBER)
                .addOutputData("number", "http://schema.org/parkingNumberOfVacantSpaces", ValueType.NUMBER)
                .inRegion("Barcelona").withPrice(Euros.amount(4999)).withPricingModel(PricingModel.PER_MONTH)
                .withLicenseType(LicenseType.CREATIVE_COMMONS).withAccessRequestHandler(accessCallbackDummy);

        offeringDescription.register();

    }

    @After
    public void cleanup() {
        if (provider != null) {
            provider.terminate();
        }
        provider = null;
    }

    /*
     * @Test(expected = IOException.class) public void testMarketplaceNotAvailable() throws IOException { String
     * MARKETPLACE_URIFAIL = "http://localhost:9999"; MarketplaceClient helperClientFail =
     * MarketplaceClient.create(MARKETPLACE_URIFAIL, new BridgeIotClientId(PROVIDER_ID), "");
     * helperClientFail.request(CREATE_ORGA); }
     */

    @Test
    public void testOfferingRegistration()
            throws IncompleteOfferingDescriptionException, IncompleteOfferingQueryException, IOException,
            NotRegisteredException, FailedDiscoveryException, InterruptedException, ExecutionException {

        RegisteredOffering registeredOffering = offeringDescription.register();
        assertNotNull(registeredOffering);

        List<SubscribableOfferingDescription> offeringDescriptions = consumer.discover(query).get();
        assertEquals(1, offeringDescriptions.size());
        assertEquals(TEST_OFFERING_LOCALID,
                ((SubscribableOfferingDescriptionCore) offeringDescriptions.get(0)).getName());

        // deregister
        offeringDescription.deregister();

        offeringDescriptions = consumer.discover(query).get();

        assertTrue(offeringDescriptions.isEmpty());
        assertFalse(offeringDescription.getActivation().getStatus());
        assertFalse(offeringDescriptions.contains(offeringDescription));

    }

}
