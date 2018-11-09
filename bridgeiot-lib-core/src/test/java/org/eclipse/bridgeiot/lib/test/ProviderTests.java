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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.eclipse.bridgeiot.lib.BridgeIotClientId;
import org.eclipse.bridgeiot.lib.ConsumerCore;
import org.eclipse.bridgeiot.lib.Provider;
import org.eclipse.bridgeiot.lib.exceptions.FailedDiscoveryException;
import org.eclipse.bridgeiot.lib.exceptions.IncompleteOfferingDescriptionException;
import org.eclipse.bridgeiot.lib.exceptions.IncompleteOfferingQueryException;
import org.eclipse.bridgeiot.lib.exceptions.NotRegisteredException;
import org.eclipse.bridgeiot.lib.handlers.AccessRequestHandler;
import org.eclipse.bridgeiot.lib.misc.BridgeIotProperties;
import org.eclipse.bridgeiot.lib.misc.MarketplaceClient;
import org.eclipse.bridgeiot.lib.model.BoundingBox;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.LicenseType;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.PricingModel;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.ValueType;
import org.eclipse.bridgeiot.lib.model.Location;
import org.eclipse.bridgeiot.lib.model.Price.Euros;
import org.eclipse.bridgeiot.lib.model.TimePeriod;
import org.eclipse.bridgeiot.lib.offering.OfferingDescription;
import org.eclipse.bridgeiot.lib.offering.RegisteredOffering;
import org.eclipse.bridgeiot.lib.offering.RegistrableOfferingDescription;
import org.eclipse.bridgeiot.lib.offering.RegistrableOfferingDescriptionChain;
import org.eclipse.bridgeiot.lib.offering.SubscribableOfferingDescriptionCore;
import org.eclipse.bridgeiot.lib.query.OfferingQuery;
import org.eclipse.bridgeiot.lib.serverwrapper.BridgeIotHttpResponse;
import org.eclipse.bridgeiot.lib.serverwrapper.EmbededdedRouteBasedServer;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static ConsumerCore consumer;
    private static OfferingQuery query;
    private static RegistrableOfferingDescription offeringDescription;
    // private static RegistrableOfferingDescription secondOfferingDescription;

    private final static String TEST_OFFERING_ID = "Test_Offering";
    private final static String FIND_OFF = "{\"query\": \"query { allOfferings { allOfferings { id } } }\"}";

    final static Logger logger = LoggerFactory.getLogger(ProviderTests.class);

    AccessRequestHandler accessCallbackDummy;

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

    private static EmbededdedRouteBasedServer embeddedServer = new EmbededdedRouteBasedServer("localhost", 8000) {

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
                final RegistrableOfferingDescription offeringDescription, final boolean authorizationRequired) {
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
    };

    private static AccessRequestHandler accessCallback = new AccessRequestHandler() {

        @Override
        public BridgeIotHttpResponse processRequestHandler(OfferingDescription offeringDescription,
                Map<String, Object> inputData, String subscriptionId, String consumerInfo) {
            // TODO Auto-generated method stub
            return null;
        }
    };

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

        testProperties = BridgeIotProperties.load("src/test/resources/test.properties");

        org.junit.Assume.assumeNoException(testMarketplaceConnections());

        initializeConsumerAndQuery();

    }

    public static void initializeConsumerAndQuery() throws IOException, IncompleteOfferingQueryException {

        consumer = new ConsumerCore(testProperties.CONSUMER_ID, testProperties.MARKETPLACE_URI);
        consumer.authenticate(testProperties.CONSUMER_SECRET);

        query = OfferingQuery.create("TestOfferingQuery").withName("Parking Information Query")
                .withCategory("urn:big-iot:ParkingSpaceCategory").inRegion("Barcelona")
                .withPricingModel(PricingModel.PER_ACCESS)
                .withTimePeriod(TimePeriod.create(new DateTime(1999, 1, 1, 0, 0, 0), new DateTime().plusDays(1)))
                .withMaxPrice(Euros.amount(0.005)).withLicenseType(LicenseType.OPEN_DATA_LICENSE);

    }

    @Before
    public void setUp() throws Exception {

        // Create provider instance and offering
        provider = new Provider(testProperties.PROVIDER_ID, testProperties.MARKETPLACE_URI);
        provider.authenticate(testProperties.PROVIDER_SECRET);

        // Create simple OfferingDescription with an access endpoint that serves
        // a specific data item

        offeringDescription = provider.createOfferingDescription(TEST_OFFERING_ID).withName(TEST_OFFERING_ID)
                .withCategory("urn:big-iot:ParkingSpaceCategory")
                .withTimePeriod(TimePeriod.create(new DateTime(2017, 1, 1, 0, 0, 0), new DateTime()))
                .addInputData("number", "http://schema.org/parkingNumberOfVacantSpaces", ValueType.NUMBER)
                .addOutputData("number", "http://schema.org/parkingNumberOfVacantSpaces", ValueType.NUMBER)
                .inRegion("Barcelona").withPrice(Euros.amount(0.001)).withPricingModel(PricingModel.PER_ACCESS)
                .withLicenseType(LicenseType.OPEN_DATA_LICENSE).withRoute("test").deployOn(embeddedServer)
                .withAccessRequestHandler(accessCallback);
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
    public void testOfferingRegistration() throws IncompleteOfferingDescriptionException,
            IncompleteOfferingQueryException, IOException, NotRegisteredException, FailedDiscoveryException {

        RegisteredOffering registeredOffering = offeringDescription.register();
        assertNotNull(registeredOffering);
        assertNotNull(registeredOffering.getOfferingDescription().getActivation());
        assertTrue(registeredOffering.getOfferingDescription().getActivation().getStatus());

        List<SubscribableOfferingDescriptionCore> lD = consumer.discoverBlocking(query);

        boolean match = false;
        for (int i = 0; i < lD.size(); i++) {
            if (((SubscribableOfferingDescriptionCore) lD.get(i)).getName().equals(TEST_OFFERING_ID))
                match = true;
        }
        assertTrue(match);
    }

    @Test
    public void testProviderRegister() throws IncompleteOfferingDescriptionException, IncompleteOfferingQueryException,
            IOException, NotRegisteredException, FailedDiscoveryException {

        // OfferingId id = new OfferingId("TestOrga-TestProvider-TestOffering");
        provider.register(offeringDescription);

        List<SubscribableOfferingDescriptionCore> lD = consumer.discoverBlocking(query);

        boolean match = false;
        for (int i = 0; i < lD.size(); i++) {
            if (((SubscribableOfferingDescriptionCore) lD.get(i)).getName().equals(TEST_OFFERING_ID))
                match = true;
        }
        assertTrue(match);
    }

    @Test
    public void testDiscoveryAfterDeregistration() throws IncompleteOfferingDescriptionException,
            IncompleteOfferingQueryException, IOException, NotRegisteredException, FailedDiscoveryException {

        offeringDescription.register();
        offeringDescription.deregister();

        List<SubscribableOfferingDescriptionCore> lD = consumer.discoverBlocking(query);

        boolean match = false;
        for (int i = 0; i < lD.size(); i++) {
            if (((SubscribableOfferingDescriptionCore) lD.get(i)).getName().equals(TEST_OFFERING_ID))
                match = true;
        }
        assertFalse(match);
    }

    @Test
    public void testDiscoveryWithNonMatchingTimePeriod() throws IncompleteOfferingDescriptionException,
            IncompleteOfferingQueryException, IOException, NotRegisteredException, FailedDiscoveryException {

        offeringDescription.register();

        OfferingQuery nonMatchingTimePeriodQuery = OfferingQuery.create("TestOfferingQuery")
                .withName("Parking Information Query").withCategory("urn:big-iot:ParkingSpaceCategory")
                .withTimePeriod(
                        TimePeriod.create(new DateTime(1999, 1, 1, 0, 0, 0), new DateTime(2016, 12, 31, 23, 59, 59)))
                .withPricingModel(PricingModel.PER_ACCESS).withMaxPrice(Euros.amount(0.5))
                .withLicenseType(LicenseType.OPEN_DATA_LICENSE);

        List<SubscribableOfferingDescriptionCore> lD = consumer.discoverBlocking(nonMatchingTimePeriodQuery);

        boolean match = false;
        for (int i = 0; i < lD.size(); i++) {
            if (((SubscribableOfferingDescriptionCore) lD.get(i)).getName().equals(TEST_OFFERING_ID))
                match = true;
        }
        assertFalse(match);
    }

    @Test(expected = IncompleteOfferingDescriptionException.class)
    public void testOfferingRegistrationFail() throws IncompleteOfferingDescriptionException, NotRegisteredException {

        // Test if corrupt registration fails
        // incomplete offeringDescription Exception -> should be thrown
        // since we did not provide any endpoint
        RegistrableOfferingDescription incompleteOfferingDescription = provider
                .createOfferingDescription("IncompleteOffering").withName("IncompleteOffering")
                .withCategory("urn:big-iot:ParkingSpaceCategory")
                .addInputData("longitude", "schema:longitude", ValueType.NUMBER)
                .addInputData("latitude", "schema:latitude", ValueType.NUMBER)
                .addInputData("radius", "schema:geoRadius", ValueType.NUMBER)
                .addOutputData("longitude", "schema:longitude", ValueType.NUMBER)
                .addOutputData("latitude", "schema:latitude", ValueType.NUMBER)
                .addOutputData("distance", "datex:distanceFromParkingSpace", ValueType.NUMBER)
                .addOutputData("status", "datex:parkingSpaceStatus", ValueType.TEXT).inRegion("Barcelona")
                .withPrice(Euros.amount(0.005)).withPricingModel(PricingModel.PER_ACCESS)
                .withLicenseType(LicenseType.OPEN_DATA_LICENSE);

        incompleteOfferingDescription.register();
    }

    @Test(expected = IncompleteOfferingDescriptionException.class)
    public void testOfferingRegistrationFailNoServer()
            throws IncompleteOfferingDescriptionException, NotRegisteredException {

        // Test if corrupt registration fails
        // incomplete offeringDescription Exception -> should be thrown
        // since we did not provide any endpoint
        RegistrableOfferingDescription incompleteOfferingDescription = provider
                .createOfferingDescription("IncompleteOffering").withName("IncompleteOffering")
                .withCategory("urn:big-iot:ParkingSpaceCategory")
                .addInputData("longitude", "schema:longitude", ValueType.NUMBER)
                .addInputData("latidue", "schema:latitude", ValueType.NUMBER)
                .addInputData("radius", "schema:geoRadius", ValueType.NUMBER)
                .addOutputData("longitude", "schema:longitude", ValueType.NUMBER)
                .addOutputData("latitude", "schema:latitude", ValueType.NUMBER)
                .addOutputData("distance", "datex:distanceFromParkingSpace", ValueType.NUMBER)
                .addOutputData("status", "datex:parkingSpaceStatus", ValueType.TEXT).inRegion("Barcelona")
                .withPrice(Euros.amount(0.004)).withPricingModel(PricingModel.PER_ACCESS)
                .withLicenseType(LicenseType.OPEN_DATA_LICENSE);

        incompleteOfferingDescription.register();
    }

    @Test
    public void testDiscoveryWithWrongCity() throws IncompleteOfferingDescriptionException,
            IncompleteOfferingQueryException, IOException, NotRegisteredException, FailedDiscoveryException {

        offeringDescription.deregister();

        RegistrableOfferingDescription wrongCityOfferingDescription = provider
                .createOfferingDescription("WrongCityOffering").withName("WrongCityOffering")
                .withCategory("urn:big-iot:ParkingSpaceCategory").inCity("asdlfkjaf")
                .addInputData("longitude", "schema:longitude", ValueType.NUMBER)
                .addInputData("latidue", "schema:latitude", ValueType.NUMBER)
                .addInputData("radius", "schema:geoRadius", ValueType.NUMBER)
                .addOutputData("longitude", "schema:longitude", ValueType.NUMBER)
                .addOutputData("latitude", "schema:latitude", ValueType.NUMBER)
                .addOutputData("distance", "datex:distanceFromParkingSpace", ValueType.NUMBER)
                .addOutputData("status", "datex:parkingSpaceStatus", ValueType.TEXT).withPrice(Euros.amount(0.004))
                .withPricingModel(PricingModel.PER_ACCESS).withLicenseType(LicenseType.OPEN_DATA_LICENSE)
                .deployOn(embeddedServer).withAccessRequestHandler(accessCallback);

        wrongCityOfferingDescription.register();

        List<SubscribableOfferingDescriptionCore> lD = consumer.discoverBlocking(query);

        boolean match = false;
        for (int i = 0; i < lD.size(); i++) {
            if (((SubscribableOfferingDescriptionCore) lD.get(i)).getId().equals("WrongCityOffering"))
                match = true;
        }
        assertFalse(match);
    }

    @Test
    public void testDiscoveryWithBoundaryBox() throws IncompleteOfferingDescriptionException,
            IncompleteOfferingQueryException, IOException, NotRegisteredException, FailedDiscoveryException {

        RegistrableOfferingDescription boundingBoxOfferingDescription = provider
                .createOfferingDescription("BoundingBoxOffering").withName("BoundingBoxOffering")
                .withCategory("urn:big-iot:ParkingSpaceCategory")
                .inRegion(BoundingBox.create(Location.create(50.0, 50.0), Location.create(60.0, 60.0)))
                .addInputData("longitude", "schema:longitude", ValueType.NUMBER)
                .addInputData("latidue", "schema:latitude", ValueType.NUMBER)
                .addInputData("radius", "schema:geoRadius", ValueType.NUMBER)
                .addOutputData("longitude", "schema:longitude", ValueType.NUMBER)
                .addOutputData("latitude", "schema:latitude", ValueType.NUMBER)
                .addOutputData("distance", "datex:distanceFromParkingSpace", ValueType.NUMBER)
                .addOutputData("status", "datex:parkingSpaceStatus", ValueType.TEXT).withPrice(Euros.amount(0.01))
                .withPricingModel(PricingModel.PER_ACCESS).withLicenseType(LicenseType.CREATIVE_COMMONS)
                .deployOn(embeddedServer).withAccessRequestHandler(accessCallback);

        boundingBoxOfferingDescription.register();

        OfferingQuery boundingBoxQuery = OfferingQuery.create("BoundingBoxQuery").withName("BoundingBoxQuery")
                .withCategory("urn:big-iot:ParkingSpaceCategory")
                .inRegion(BoundingBox.create(Location.create(55.0, 55.0), Location.create(56.0, 56.0)))
                .withPricingModel(PricingModel.PER_ACCESS).withMaxPrice(Euros.amount(0.05))
                .withLicenseType(LicenseType.CREATIVE_COMMONS);

        List<SubscribableOfferingDescriptionCore> lD = consumer.discoverBlocking(boundingBoxQuery);

        boolean match = false;
        for (int i = 0; i < lD.size(); i++) {
            if (((SubscribableOfferingDescriptionCore) lD.get(i)).getName().equals("BoundingBoxOffering"))
                match = true;
        }
        assertTrue(match);
    }

    @Test
    public void testDiscoveryWithNonMatchingBoundaryBox() throws IncompleteOfferingDescriptionException,
            IncompleteOfferingQueryException, IOException, NotRegisteredException, FailedDiscoveryException {

        RegistrableOfferingDescription boundingBoxOfferingDescription = provider
                .createOfferingDescription("BoundingBoxOffering").withName("BoundingBoxOffering")
                .withCategory("urn:big-iot:ParkingSpaceCategory")
                .inRegion(BoundingBox.create(Location.create(50.0, 50.0), Location.create(60.0, 60.0)))
                .addInputData("longitude", "schema:longitude", ValueType.NUMBER)
                .addInputData("latidue", "schema:latitude", ValueType.NUMBER)
                .addInputData("radius", "schema:geoRadius", ValueType.NUMBER)
                .addOutputData("longitude", "schema:longitude", ValueType.NUMBER)
                .addOutputData("latitude", "schema:latitude", ValueType.NUMBER)
                .addOutputData("distance", "datex:distanceFromParkingSpace", ValueType.NUMBER)
                .addOutputData("status", "datex:parkingSpaceStatus", ValueType.TEXT).withPrice(Euros.amount(0.004))
                .withPricingModel(PricingModel.PER_ACCESS).withLicenseType(LicenseType.PROJECT_INTERNAL_USE_ONLY)
                .deployOn(embeddedServer).withAccessRequestHandler(accessCallback);

        boundingBoxOfferingDescription.register();

        OfferingQuery nonMatchingBoundingBoxQuery = OfferingQuery.create("NonMatchingBoundingBoxQuery")
                .withName("NonMatchingBoundingBoxQuery").withCategory("urn:big-iot:ParkingSpaceCategory")
                .inRegion(BoundingBox.create(Location.create(45.0, 45.0), Location.create(46.0, 46.0)))
                .withPricingModel(PricingModel.PER_ACCESS).withMaxPrice(Euros.amount(0.005))
                .withLicenseType(LicenseType.PROJECT_INTERNAL_USE_ONLY);

        List<SubscribableOfferingDescriptionCore> lD = consumer.discoverBlocking(nonMatchingBoundingBoxQuery);

        boolean match = false;
        for (int i = 0; i < lD.size(); i++) {
            if (((SubscribableOfferingDescriptionCore) lD.get(i)).getId().equals("BoundingBoxOffering"))
                match = true;
        }
        assertFalse(match);
    }

    @Test
    public void testAccessList() throws IncompleteOfferingDescriptionException, IncompleteOfferingQueryException,
            IOException, NotRegisteredException, FailedDiscoveryException {

        offeringDescription = ((RegistrableOfferingDescriptionChain) offeringDescription)
                .restrictedToOrganizations("TestOrganization");

        RegisteredOffering registeredOffering = offeringDescription.register();
        assertNotNull(registeredOffering);
        assertNotNull(registeredOffering.getOfferingDescription().getActivation());
        assertTrue(registeredOffering.getOfferingDescription().getActivation().getStatus());

        List<SubscribableOfferingDescriptionCore> lD = consumer.discoverBlocking(query);

        boolean match = false;
        for (int i = 0; i < lD.size(); i++) {
            if (((SubscribableOfferingDescriptionCore) lD.get(i)).getName().equals(TEST_OFFERING_ID))
                match = true;
        }
        assertTrue(match);
    }

    @Test
    public void testAccessListWithoutMatch() throws IncompleteOfferingDescriptionException,
            IncompleteOfferingQueryException, IOException, NotRegisteredException, FailedDiscoveryException {

        offeringDescription = ((RegistrableOfferingDescriptionChain) offeringDescription)
                .restrictedToOrganizations("Bosch_CR");

        RegisteredOffering registeredOffering = offeringDescription.register();
        assertNotNull(registeredOffering);
        assertNotNull(registeredOffering.getOfferingDescription().getActivation());
        assertTrue(registeredOffering.getOfferingDescription().getActivation().getStatus());

        List<SubscribableOfferingDescriptionCore> lD = consumer.discoverBlocking(query);

        boolean match = false;
        for (int i = 0; i < lD.size(); i++) {
            if (((SubscribableOfferingDescriptionCore) lD.get(i)).getName().equals(TEST_OFFERING_ID))
                match = true;
        }

        // TODO: This test needs to be extended as a provider can always discover its own offerings,
        // independent from access restrictions.
        // assertFalse(match);
    }
}
