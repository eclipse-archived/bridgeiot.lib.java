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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.eclipse.bridgeiot.lib.BridgeIotClientId;
import org.eclipse.bridgeiot.lib.Consumer;
import org.eclipse.bridgeiot.lib.Provider;
import org.eclipse.bridgeiot.lib.ProviderSpark;
import org.eclipse.bridgeiot.lib.exceptions.AccessToNonSubscribedOfferingException;
import org.eclipse.bridgeiot.lib.exceptions.FailedDiscoveryException;
import org.eclipse.bridgeiot.lib.exceptions.IllegalEndpointException;
import org.eclipse.bridgeiot.lib.exceptions.IncompleteOfferingDescriptionException;
import org.eclipse.bridgeiot.lib.exceptions.IncompleteOfferingQueryException;
import org.eclipse.bridgeiot.lib.exceptions.InvalidOfferingException;
import org.eclipse.bridgeiot.lib.exceptions.NotRegisteredException;
import org.eclipse.bridgeiot.lib.handlers.AccessRequestHandler;
import org.eclipse.bridgeiot.lib.handlers.DiscoverFailureException;
import org.eclipse.bridgeiot.lib.handlers.DiscoverResponseErrorHandler;
import org.eclipse.bridgeiot.lib.handlers.DiscoverResponseHandler;
import org.eclipse.bridgeiot.lib.misc.BridgeIotProperties;
import org.eclipse.bridgeiot.lib.misc.MarketplaceClient;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.LicenseType;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.PricingModel;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.ValueType;
import org.eclipse.bridgeiot.lib.model.Price.Euros;
import org.eclipse.bridgeiot.lib.offering.Offering;
import org.eclipse.bridgeiot.lib.offering.OfferingByLib;
import org.eclipse.bridgeiot.lib.offering.OfferingDescription;
import org.eclipse.bridgeiot.lib.offering.RegistrableOfferingDescription;
import org.eclipse.bridgeiot.lib.offering.SubscribableOfferingDescription;
import org.eclipse.bridgeiot.lib.query.IOfferingQuery;
import org.eclipse.bridgeiot.lib.query.OfferingQuery;
import org.eclipse.bridgeiot.lib.serverwrapper.BridgeIotHttpResponse;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsumerTests {

    private static Provider provider = null;
    private static Consumer consumer = null;
    private static OfferingQuery query = null;
    private static Offering offering = null;
    private static RegistrableOfferingDescription offeringDescription = null;

    private final static String TEST_OFFERING_LOCALID = "TestOffering_ES_CT";
    private static String TEST_OFFERING_ID;
    private final static String FIND_OFF = "{\"query\": \"query { allOfferings { allOfferings { id } } }\"}";

    private static BridgeIotProperties testProperties;

    final static Logger logger = LoggerFactory.getLogger(ConsumerTests.class);

    private static Throwable testMarketplaceConnections() {
        Throwable x = null;
        MarketplaceClient helperClient = MarketplaceClient.create(testProperties.MARKETPLACE_URI,
                new BridgeIotClientId(testProperties.PROVIDER_ID), testProperties.PROVIDER_SECRET);
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

            return BridgeIotHttpResponse.okay().withBody("0123456789");
        }
    };

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

        testProperties = BridgeIotProperties.load("src/test/resources/test.properties");

        TEST_OFFERING_ID = testProperties.PROVIDER_ID + "-" + TEST_OFFERING_LOCALID;

        org.junit.Assume.assumeNoException(testMarketplaceConnections());

        initializeProvider();

    }

    public static void initializeConsumerAndQuery()
            throws IOException, IncompleteOfferingQueryException, InterruptedException {

        consumer = new Consumer(testProperties.CONSUMER_ID, testProperties.MARKETPLACE_URI);
        consumer.authenticate(testProperties.CONSUMER_SECRET);

        query = OfferingQuery.create(TEST_OFFERING_LOCALID).withName(TEST_OFFERING_LOCALID)
                .withCategory("urn:big-iot:ParkingSpaceCategory").inRegion("TestCity")
                .withPricingModel(PricingModel.PER_ACCESS).withMaxPrice(Euros.amount(0.005))
                .withLicenseType(LicenseType.OPEN_DATA_LICENSE);

    }

    public static void initializeProvider()
            throws IOException, IncompleteOfferingDescriptionException, NotRegisteredException, InterruptedException {

        // Create provider instance and offering
        provider = new ProviderSpark(testProperties.PROVIDER_ID, testProperties.MARKETPLACE_URI, "localhost", 9876);
        provider.authenticate(testProperties.PROVIDER_SECRET);

        // Create simple OfferingDescription with an access endpoint that serves
        // a specific data item

        offeringDescription = provider.createOfferingDescription(TEST_OFFERING_LOCALID).withName(TEST_OFFERING_LOCALID)
                .withCategory("urn:big-iot:ParkingSpaceCategory")
                .addInputData("number", "http://schema.org/parkingNumberOfVacantSpaces", ValueType.NUMBER)
                .addOutputData("number", "http://schema.org/parkingNumberOfVacantSpaces", ValueType.NUMBER)
                .inRegion("TestCity").withPrice(Euros.amount(0.005)).withPricingModel(PricingModel.PER_ACCESS)
                .withLicenseType(LicenseType.OPEN_DATA_LICENSE).withAccessRequestHandler(accessCallbackDummy);

        offeringDescription.register();

    }

    @Before
    public void setUp() throws Exception {
        // initializeProvider();
        initializeConsumerAndQuery();
    }

    /*
     * @Test(expected = IOException.class) public void testMarketplaceNotAvailable() throws IOException {
     * 
     * String MARKETPLACE_URIFAIL = "http://localhost:9999"; MarketplaceClient helperClientFail =
     * MarketplaceClient.create(MARKETPLACE_URIFAIL, new BridgeIotClientId(PROVIDER_ID), PROVIDER_SECRET);
     * 
     * helperClientFail.request(CREATE_ORGA);
     * 
     * }
     */

    @Test
    public void testDiscover() throws IOException, NotRegisteredException, FailedDiscoveryException,
            InterruptedException, ExecutionException {
        logger.info("start discover");
        List<SubscribableOfferingDescription> offeringDescriptionList = consumer.discover(query).get();
        assertEquals(1, offeringDescriptionList.size());
    }

    @Test
    public void testDiscoverWithInputOuputData() throws IOException, NotRegisteredException,
            IncompleteOfferingQueryException, FailedDiscoveryException, InterruptedException, ExecutionException {
        logger.info("start discover with input/output data query");

        OfferingQuery q = OfferingQuery.create(TEST_OFFERING_LOCALID + "2").withName(TEST_OFFERING_LOCALID + "2")
                .withCategory("urn:big-iot:ParkingSpaceCategory").inRegion("TestCity")
                .addInputData("http://schema.org/parkingNumberOfVacantSpaces", ValueType.NUMBER)
                .addOutputData("http://schema.org/parkingNumberOfVacantSpaces", ValueType.NUMBER)
                .withMaxPrice(Euros.amount(0.005)).withPricingModel(PricingModel.PER_ACCESS)
                .withLicenseType(LicenseType.OPEN_DATA_LICENSE);

        List<SubscribableOfferingDescription> offeringDescriptionList = consumer.discover(q).get();
        assertEquals(1, offeringDescriptionList.size());
    }

    @Test
    public void testDiscoverWithWrongInputOuputData() throws IOException, NotRegisteredException,
            IncompleteOfferingQueryException, FailedDiscoveryException, InterruptedException, ExecutionException {
        logger.info("start discover with non-matching input/output data query");

        OfferingQuery q = OfferingQuery.create(TEST_OFFERING_LOCALID + "2").withName(TEST_OFFERING_LOCALID + "2")
                .withCategory("urn:big-iot:ParkingSpaceCategory").inRegion("TestCity")
                .withPricingModel(PricingModel.PER_ACCESS)
                .addInputData("http://schema.org/89470297589275825235235", ValueType.NUMBER)
                .addOutputData("http://schema.org/2098572987589273832495", ValueType.NUMBER)
                .withMaxPrice(Euros.amount(0.005)).withLicenseType(LicenseType.OPEN_DATA_LICENSE);

        List<SubscribableOfferingDescription> offeringDescriptionList = consumer.discover(q).get();
        assertEquals(0, offeringDescriptionList.size());
    }

    @Test
    public void testDiscoverCallbackDual() throws IncompleteOfferingQueryException, InterruptedException {
        logger.info("start discover");
        final CountDownLatch lock = new CountDownLatch(1);
        consumer.discover(query, new DiscoverResponseHandler() {
            @Override
            public void processResponse(IOfferingQuery reference, List offeringDescriptions) {
                assertNotNull(offeringDescriptions.size());
                assertTrue(offeringDescriptions.size() > 0);
                assertEquals(SubscribableOfferingDescription.class, offeringDescriptions.get(0).getClass());
                assertTrue(offeringFoundById(offeringDescriptions, TEST_OFFERING_ID));
                lock.countDown();
            }
        }, new DiscoverResponseErrorHandler() {
            @Override
            public void processResponse(IOfferingQuery reference, DiscoverFailureException failure) {
                logger.debug("x");
                lock.countDown();
            }
        });
        assertEquals(true, lock.await(10000, TimeUnit.MILLISECONDS));
        logger.info("OfferingID: " + offeringDescription.getId().toString());
    }

    @Test
    public void testDiscoverCallbackOneTime() throws IncompleteOfferingQueryException, InterruptedException {
        final CountDownLatch lock = new CountDownLatch(1);
        consumer.discover(query, new DiscoverResponseHandler() {
            @Override
            public void processResponse(IOfferingQuery reference, List offeringDescriptions) {
                assertNotNull(offeringDescriptions.size());
                assertTrue(offeringDescriptions.size() > 0);
                assertEquals(SubscribableOfferingDescription.class, offeringDescriptions.get(0).getClass());
                assertTrue(offeringFoundById(offeringDescriptions, TEST_OFFERING_ID));
                lock.countDown();
            }
        });
        // Thread.sleep(2000);
        assertEquals(true, lock.await(10000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testDiscoverContinuous() throws IncompleteOfferingQueryException, InterruptedException {
        final CountDownLatch lock = new CountDownLatch(2);

        consumer.discoverContinous(query, new DiscoverResponseHandler() {

            @Override
            public void processResponse(IOfferingQuery reference, List offeringDescriptions) {
                logger.info("Continued Discovery (countDown: {}) - Discovered {} offerings", lock.getCount(),
                        offeringDescriptions.size());
                assertNotNull(offeringDescriptions.size());
                assertTrue(offeringDescriptions.size() > 0);
                assertEquals(SubscribableOfferingDescription.class, offeringDescriptions.get(0).getClass());
                assertTrue(offeringFoundById(offeringDescriptions, TEST_OFFERING_ID));
                lock.countDown();
            }
        }, new DiscoverResponseErrorHandler() {

            @Override
            public void processResponse(IOfferingQuery reference, DiscoverFailureException failure) {
                logger.error("Discovery error");

            }
        }, 5);
        assertEquals(true, lock.await(20000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testDiscoverContinuousStop() throws IncompleteOfferingQueryException, InterruptedException {
        final CountDownLatch lock = new CountDownLatch(2);

        consumer.discoverContinous(query, new DiscoverResponseHandler() {
            @Override
            public void processResponse(IOfferingQuery reference, List offeringDescriptions) {
                logger.info("Discovered {} offerings", offeringDescriptions.size());
                consumer.stopDiscoverContinuous(reference);
                lock.countDown();
            }
        }, new DiscoverResponseErrorHandler() {
            @Override
            public void processResponse(IOfferingQuery reference, DiscoverFailureException failure) {
                logger.error("Discovery error");

            }
        }, 2);

        Thread.sleep(4000);
        assertEquals(1, lock.getCount());
    }

    @Test(expected = IncompleteOfferingQueryException.class)
    public void testQueryFail() throws IncompleteOfferingQueryException {
        OfferingQuery queryFail = OfferingQuery.create(null).withName("Parking Information Query")
                .withCategory("urn:big-iot:ParkingSpaceCategory").inRegion("Barcelona")
                .withPricingModel(BridgeIotTypes.PricingModel.PER_ACCESS).withMaxPrice(Euros.amount(0.002))
                .withLicenseType(LicenseType.OPEN_DATA_LICENSE);

        consumer.discover(queryFail, null);
    }

    // Change in OfferingCoreByLib-constructor to http
    @Test
    public void testSubscription() throws IncompleteOfferingQueryException, InterruptedException, IOException,
            ExecutionException, IncompleteOfferingDescriptionException {

        List<SubscribableOfferingDescription> offeringDescriptions = consumer.discover(query).get();

        assertTrue(offeringDescriptions.size() > 0);
        assertTrue(offeringFoundById(offeringDescriptions, TEST_OFFERING_ID));

        SubscribableOfferingDescription selectedOfferingDescription = getOfferingDescriptionWithId(offeringDescriptions,
                TEST_OFFERING_ID);
        offering = selectedOfferingDescription.subscribe().get();

        assertEquals(OfferingByLib.class, offering.getClass());
        assertEquals(TEST_OFFERING_ID, offering.getOfferingDescription().getId());
    }

    @Test
    public void testSubscribeToOffering()
            throws IOException, InterruptedException, ExecutionException, IncompleteOfferingDescriptionException {

        List<SubscribableOfferingDescription> offeringDescriptions = consumer.discover(query).get();

        assertTrue(offeringDescriptions.size() > 0);
        assertTrue(offeringFoundById(offeringDescriptions, TEST_OFFERING_ID));

        SubscribableOfferingDescription selectedOfferingDescription = getOfferingDescriptionWithId(offeringDescriptions,
                TEST_OFFERING_ID);
        offering = selectedOfferingDescription.subscribe().get();

        logger.info("Subscription - Offering Access Token: {}", offering.getOfferingToken());

        assertEquals(TEST_OFFERING_ID, offering.getOfferingDescription().getId());
        assertTrue(offering.getOfferingToken().contains("."));
    }

    @Test
    public void testSubscribeByOfferingId() throws IllegalEndpointException, IncompleteOfferingDescriptionException,
            AccessToNonSubscribedOfferingException, InterruptedException, ExecutionException, InvalidOfferingException,
            IOException {

        offering = consumer.subscribeByOfferingId(TEST_OFFERING_ID).get();

        logger.info("Subscription - Offering Access Token: {}", offering.getOfferingToken());

        assertEquals(TEST_OFFERING_ID, offering.getOfferingDescription().getId());
        assertTrue(offering.getOfferingToken().contains("."));
    }

    @Test
    public void testDiscoverAndSubscribeByOfferingId()
            throws IllegalEndpointException, IncompleteOfferingDescriptionException,
            AccessToNonSubscribedOfferingException, InterruptedException, ExecutionException, InvalidOfferingException,
            IOException, NotRegisteredException, FailedDiscoveryException {

        // setup query on marketplace
        consumer.discover(query).get();

        // discover by query id
        List<SubscribableOfferingDescription> list = consumer.discoverById(query.getId()).get();

        // Instantiation of Offering Access objects via subscribe
        SubscribableOfferingDescription offeringDescription = list.get(0);
        offering = offeringDescription.subscribe().get();

        logger.info("Subscription - Offering Access Token: {}", offering.getOfferingToken());

        assertEquals(TEST_OFFERING_ID, offering.getOfferingDescription().getId());
        assertTrue(offering.getOfferingToken().contains("."));
    }

    @After
    public void cleanup() {

        if (offering != null) {
            offering.unsubscribe();
        }
        offering = null;

        if (consumer != null) {
            consumer.terminate();
        }
        consumer = null;

    }

    @AfterClass
    public static void cleanupAfterClass() {
        if (provider != null) {
            provider.terminate();
        }
        provider = null;
    }

    private boolean offeringFoundById(List<SubscribableOfferingDescription> lD, String offeringId) {
        for (int i = 0; i < lD.size(); i++) {
            if (lD.get(i).getId().equals(offeringId))
                return true;
        }
        return false;
    }

    private SubscribableOfferingDescription getOfferingDescriptionWithId(List<SubscribableOfferingDescription> lD,
            String offeringId) {
        for (int i = 0; i < lD.size(); i++) {
            if (lD.get(i).getId().equals(offeringId))
                return lD.get(i);
        }
        return null;
    }

}