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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.bridgeiot.lib.BridgeIotClientId;
import org.eclipse.bridgeiot.lib.ConsumerCore;
import org.eclipse.bridgeiot.lib.Provider;
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
import org.eclipse.bridgeiot.lib.offering.OfferingCore;
import org.eclipse.bridgeiot.lib.offering.OfferingCoreByLib;
import org.eclipse.bridgeiot.lib.offering.OfferingDescription;
import org.eclipse.bridgeiot.lib.offering.RegistrableOfferingDescription;
import org.eclipse.bridgeiot.lib.offering.SubscribableOfferingDescriptionCore;
import org.eclipse.bridgeiot.lib.query.IOfferingQuery;
import org.eclipse.bridgeiot.lib.query.OfferingQuery;
import org.eclipse.bridgeiot.lib.serverwrapper.BridgeIotHttpResponse;
import org.eclipse.bridgeiot.lib.serverwrapper.EmbededdedRouteBasedServer;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsumerTests {

    private static Provider provider = null;
    private static ConsumerCore consumer = null;
    private static OfferingQuery query = null;
    private static OfferingCore offering = null;
    private static RegistrableOfferingDescription offeringDescription = null;

    private final static String TEST_OFFERING_LOCALID = "TestOffering_CT";
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

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

        testProperties = BridgeIotProperties.load("src/test/resources/test.properties");

        TEST_OFFERING_ID = testProperties.PROVIDER_ID + "-" + TEST_OFFERING_LOCALID;

        org.junit.Assume.assumeNoException(testMarketplaceConnections());

    }

    public static void initializeConsumerAndQuery() throws IOException, IncompleteOfferingQueryException {

        consumer = new ConsumerCore(testProperties.CONSUMER_ID, testProperties.MARKETPLACE_URI);
        consumer.authenticate(testProperties.CONSUMER_SECRET);

        query = OfferingQuery.create(TEST_OFFERING_LOCALID).withName(TEST_OFFERING_LOCALID)
                .withCategory("urn:big-iot:ParkingSpaceCategory").inRegion("Test-City")
                .withPricingModel(PricingModel.PER_ACCESS).withMaxPrice(Euros.amount(500))
                .withLicenseType(LicenseType.OPEN_DATA_LICENSE);

        offering = null;
    }

    public static void initializeProvider()
            throws IOException, IncompleteOfferingDescriptionException, NotRegisteredException, InterruptedException {

        // Create provider instance and offering
        provider = new Provider(testProperties.PROVIDER_ID, testProperties.MARKETPLACE_URI);
        provider.authenticate(testProperties.PROVIDER_SECRET);

        // Create simple OfferingDescription with an access endpoint that serves
        // a specific data item

        offeringDescription = provider.createOfferingDescription(TEST_OFFERING_LOCALID).withName(TEST_OFFERING_LOCALID)
                .withCategory("urn:big-iot:ParkingSpaceCategory")
                .addInputData("number", "http://schema.org/parkingNumberOfVacantSpaces", ValueType.NUMBER)
                .addOutputData("number", "http://schema.org/parkingNumberOfVacantSpaces", ValueType.NUMBER)
                .inRegion("Test-City").withPrice(Euros.amount(499)).withPricingModel(PricingModel.PER_ACCESS)
                .withLicenseType(LicenseType.OPEN_DATA_LICENSE)
                // .withRoute("test")
                .deployOn(
                        new EmbededdedRouteBasedServer(testProperties.PROVIDER_DNS_NAME, testProperties.PROVIDER_PORT) {

                            @Override
                            public void removeRoute(String route) {
                                // TODO Auto-generated method stub

                            }

                            @Override
                            public List<String> getRoutes() {
                                // TODO Auto-generated method stub
                                return null;
                            }

                            @Override
                            public void addRoute(final String route, final AccessRequestHandler accessCallback,
                                    final RegistrableOfferingDescription offeringDescription,
                                    final boolean authorizationRequired) {
                                // TODO Auto-generated method stub
                            }

                            @Override
                            public void stop() {
                                // TODO Auto-generated method stub

                            }

                            @Override
                            public void start() {
                                // TODO Auto-generated method stub

                            }

                            @Override
                            protected String getProtocolName() {
                                // TODO Auto-generated method stub
                                return "https";
                            }

                            @Override
                            public void start(String keyStoreLocation, String keyStorePassword) {
                                // TODO Auto-generated method stub

                            }
                        })
                .withAccessRequestHandler(new AccessRequestHandler() {

                    @Override
                    public BridgeIotHttpResponse processRequestHandler(OfferingDescription offeringDescription,
                            Map<String, Object> inputData, String subscriptionId, String consumerInfo) {
                        // TODO Auto-generated method stub
                        return BridgeIotHttpResponse.okay().withBody("TEST STRING");
                    }
                });

        offeringDescription.register();

        // Thread.sleep(500);
    }

    @Before
    public void setUp() throws Exception {

        initializeProvider();
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
    public void testDiscoverBlocking() throws IOException, NotRegisteredException, FailedDiscoveryException {
        logger.info("start discover");
        List<SubscribableOfferingDescriptionCore> lD = consumer.discoverBlocking(query);

        boolean match = false;
        for (int i = 0; i < lD.size(); i++) {
            if (((SubscribableOfferingDescriptionCore) lD.get(i)).getId().equals(TEST_OFFERING_ID))
                match = true;
        }
        assertTrue(match);
    }

    @Test
    public void testDiscoverBlockingWithInputOuputData()
            throws IOException, NotRegisteredException, IncompleteOfferingQueryException, FailedDiscoveryException {
        logger.info("start discover with input/output data query");

        OfferingQuery q = OfferingQuery.create(TEST_OFFERING_LOCALID + "2").withName(TEST_OFFERING_LOCALID + "2")
                .withCategory("urn:big-iot:ParkingSpaceCategory").inRegion("Test-City")
                .withPricingModel(PricingModel.PER_ACCESS)
                .addInputData("http://schema.org/parkingNumberOfVacantSpaces", ValueType.NUMBER)
                .addOutputData("http://schema.org/parkingNumberOfVacantSpaces", ValueType.NUMBER)
                .withMaxPrice(Euros.amount(500)).withLicenseType(LicenseType.OPEN_DATA_LICENSE);

        List<SubscribableOfferingDescriptionCore> lD = consumer.discoverBlocking(q);

        boolean match = false;
        for (int i = 0; i < lD.size(); i++) {
            if (((SubscribableOfferingDescriptionCore) lD.get(i)).getId().equals(TEST_OFFERING_ID))
                match = true;
        }
        assertTrue(match);
    }

    @Test
    public void testDiscoverBlockingWithWrongInputOuputData()
            throws IOException, NotRegisteredException, IncompleteOfferingQueryException, FailedDiscoveryException {
        logger.info("start discover with non-matching input/output data query");

        OfferingQuery q = OfferingQuery.create(TEST_OFFERING_LOCALID + "2").withName(TEST_OFFERING_LOCALID + "2")
                .withCategory("urn:big-iot:ParkingSpaceCategory").inRegion("Test-City")
                .withPricingModel(PricingModel.PER_ACCESS)
                .addInputData("http://schema.org/89470297589275825235235", ValueType.NUMBER)
                .addOutputData("http://schema.org/2098572987589273832495", ValueType.NUMBER)
                .withMaxPrice(Euros.amount(500)).withLicenseType(LicenseType.OPEN_DATA_LICENSE);

        List<SubscribableOfferingDescriptionCore> lD = consumer.discoverBlocking(q);

        boolean match = false;
        for (int i = 0; i < lD.size(); i++) {
            if (((SubscribableOfferingDescriptionCore) lD.get(i)).getId().equals(TEST_OFFERING_ID))
                match = true;
        }
        assertFalse(match);
    }

    @Test
    public void testDiscoverWithFuture() throws IOException, InterruptedException, ExecutionException {

        Future<List<SubscribableOfferingDescriptionCore>> listFuture = consumer.discoverFuture(query);
        List<SubscribableOfferingDescriptionCore> lD = listFuture.get();
        boolean match = false;
        for (int i = 0; i < lD.size(); i++) {
            if (((SubscribableOfferingDescriptionCore) lD.get(i)).getId().equals(TEST_OFFERING_ID))
                match = true;
        }
        assertTrue(match);

        listFuture = consumer.discoverByIdFuture(query.getId());
        lD = listFuture.get();
        match = false;
        for (int i = 0; i < lD.size(); i++) {
            if (((SubscribableOfferingDescriptionCore) lD.get(i)).getId().equals(TEST_OFFERING_ID))
                match = true;
        }
        assertTrue(match);
    }

    @Test
    public void testDiscoverCallbackDual() throws IncompleteOfferingQueryException, InterruptedException {
        logger.info("start discover");
        final CountDownLatch lock = new CountDownLatch(1);
        consumer.discover(query, new DiscoverResponseHandler() {
            @Override
            public void processResponse(IOfferingQuery reference, List offeringDescriptions) {
                assertNotNull(offeringDescriptions.size());
                assertEquals(SubscribableOfferingDescriptionCore.class, offeringDescriptions.get(0).getClass());
                assertEquals(TEST_OFFERING_ID,
                        ((SubscribableOfferingDescriptionCore) offeringDescriptions.get(0)).getId());
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
    public void testDiscoverCallbackSingle() throws IncompleteOfferingQueryException, InterruptedException {
        final CountDownLatch lock = new CountDownLatch(1);
        consumer.discover(query, new DiscoverResponseHandler() {
            @Override
            public void processResponse(IOfferingQuery reference, List offeringDescriptions) {
                assertNotNull(offeringDescriptions.size());
                assertEquals(SubscribableOfferingDescriptionCore.class, offeringDescriptions.get(0).getClass());
                assertEquals(TEST_OFFERING_ID,
                        ((SubscribableOfferingDescriptionCore) offeringDescriptions.get(0)).getId());
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
                assertEquals(SubscribableOfferingDescriptionCore.class, offeringDescriptions.get(0).getClass());
                assertEquals(TEST_OFFERING_ID,
                        ((SubscribableOfferingDescriptionCore) offeringDescriptions.get(0)).getId());
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

        OfferingQuery queryFail = OfferingQuery.create(null).withName(TEST_OFFERING_LOCALID + "3")
                .withCategory("bigiotParking").inRegion("Test-City")
                .withPricingModel(BridgeIotTypes.PricingModel.PER_ACCESS).withMaxPrice(Euros.amount(0.002))
                .withLicenseType(LicenseType.OPEN_DATA_LICENSE);

        consumer.discover(queryFail, null);

    }

    // Change in OfferingCoreByLib-constructor to http
    @Test
    public void testSubscription() throws IncompleteOfferingQueryException, InterruptedException, IOException,
            ExecutionException, IncompleteOfferingDescriptionException {

        Future<List<SubscribableOfferingDescriptionCore>> listFuture = consumer.discoverFuture(query);
        final List<SubscribableOfferingDescriptionCore> offeringDescriptionList = listFuture.get();
        SubscribableOfferingDescriptionCore selectedOfferingDescription = offeringDescriptionList.get(0);
        offering = selectedOfferingDescription.subscribeFuture().get();

        assertEquals(OfferingCoreByLib.class, offering.getClass());
        assertEquals(TEST_OFFERING_ID, offering.getOfferingDescription().getId());
    }

    @Test
    public void testSubscribeToOffering()
            throws IOException, InterruptedException, ExecutionException, IncompleteOfferingDescriptionException {

        Future<List<SubscribableOfferingDescriptionCore>> listFuture = consumer.discoverFuture(query);
        final List<SubscribableOfferingDescriptionCore> offeringDescriptionList = listFuture.get();
        SubscribableOfferingDescriptionCore selectedOfferingDescription = offeringDescriptionList.get(0);

        offering = selectedOfferingDescription.subscribeFuture().get();

        logger.info("Subscription - Offering Access Token: {}", offering.getOfferingToken());

        assertEquals(TEST_OFFERING_ID, offering.getOfferingDescription().getId());
        assertTrue(offering.getOfferingToken().contains("."));
    }

    // Change in OfferingCoreByLib-constructor to https
    @Test(expected = RuntimeException.class)
    public void testDeactivatedOfferingIsNotSubscribeable()
            throws IOException, InterruptedException, ExecutionException, IncompleteOfferingDescriptionException {

        Future<List<SubscribableOfferingDescriptionCore>> listFuture = consumer.discoverFuture(query);
        final List<SubscribableOfferingDescriptionCore> offeringDescriptionList = listFuture.get();
        SubscribableOfferingDescriptionCore selectedOfferingDescription = offeringDescriptionList.get(0);

        offeringDescription.deregister();

        Thread.sleep(500);

        OfferingCore offeringCore = selectedOfferingDescription.subscribeFuture().get();
        throw new RuntimeException(); // TODO: FIX until subscribe/unsubscribe feature is completed on eXchange!
    }

    @Test
    public void testAfterDeregister() throws IOException, InterruptedException, ExecutionException,
            IncompleteOfferingDescriptionException, NotRegisteredException, FailedDiscoveryException {

        offeringDescription.deregister();
        Thread.sleep(500);
        List<SubscribableOfferingDescriptionCore> list = consumer.discoverBlocking(query);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testSubscribeByOfferingIdBlocking() throws IllegalEndpointException,
            IncompleteOfferingDescriptionException, AccessToNonSubscribedOfferingException, InterruptedException,
            ExecutionException, InvalidOfferingException, IOException {

        offering = consumer.subscribeByOfferingIdBlocking(TEST_OFFERING_ID);

        logger.info("Subscription - Offering Access Token: {}", offering.getOfferingToken());

        assertEquals(TEST_OFFERING_ID, offering.getOfferingDescription().getId());
        assertTrue(offering.getOfferingToken().contains("."));
    }

    @Test
    public void testSubscribeByOfferingIdFuture() throws IllegalEndpointException,
            IncompleteOfferingDescriptionException, AccessToNonSubscribedOfferingException, InterruptedException,
            ExecutionException, InvalidOfferingException, IOException {

        offering = consumer.subscribeByOfferingIdFuture(TEST_OFFERING_ID).get();

        logger.info("Subscription - Offering Access Token: {}", offering.getOfferingToken());

        assertEquals(TEST_OFFERING_ID, offering.getOfferingDescription().getId());
        assertTrue(offering.getOfferingToken().contains("."));
    }

    @Test
    public void testDiscoverAndSubscribeByOfferingIdFuture()
            throws IllegalEndpointException, IncompleteOfferingDescriptionException,
            AccessToNonSubscribedOfferingException, InterruptedException, ExecutionException, InvalidOfferingException,
            IOException, NotRegisteredException, FailedDiscoveryException {

        // setup query on marketplace
        consumer.discoverBlocking(query);

        // discover by query id
        List<SubscribableOfferingDescriptionCore> list = consumer.discoverByIdFuture(query.getId()).get();

        // Instantiation of Offering Access objects via subscribe
        SubscribableOfferingDescriptionCore offeringDescription = list.get(0);
        offering = offeringDescription.subscribeFuture().get();

        logger.info("Subscription - Offering Access Token: {}", offering.getOfferingToken());

        assertEquals(TEST_OFFERING_ID, offering.getOfferingDescription().getId());
        assertTrue(offering.getOfferingToken().contains("."));
    }

    @After
    public void cleanup() {

        if (offering != null) {
            offering.unsubscribeBlocking();
        }
        offering = null;

        if (provider != null) {
            provider.terminate();
        }
        provider = null;

        if (consumer != null) {
            consumer.terminate();
        }
        consumer = null;

    }

}
