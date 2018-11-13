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

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import okhttp3.Response;

import org.eclipse.bridgeiot.lib.BridgeIotClientId;
import org.eclipse.bridgeiot.lib.ConsumerCore;
import org.eclipse.bridgeiot.lib.Provider;
import org.eclipse.bridgeiot.lib.embeddedspark.EmbeddedSpark;
import org.eclipse.bridgeiot.lib.embeddedspark.ServerOptionsSpark;
import org.eclipse.bridgeiot.lib.exceptions.AccessToNonActivatedOfferingException;
import org.eclipse.bridgeiot.lib.exceptions.AccessToNonSubscribedOfferingException;
import org.eclipse.bridgeiot.lib.exceptions.FailedDiscoveryException;
import org.eclipse.bridgeiot.lib.exceptions.IllegalAccessParameterException;
import org.eclipse.bridgeiot.lib.exceptions.IllegalEndpointException;
import org.eclipse.bridgeiot.lib.exceptions.IncompleteOfferingDescriptionException;
import org.eclipse.bridgeiot.lib.exceptions.NotRegisteredException;
import org.eclipse.bridgeiot.lib.feed.AccessFeed;
import org.eclipse.bridgeiot.lib.feed.IAccessFeed;
import org.eclipse.bridgeiot.lib.handlers.AccessRequestHandler;
import org.eclipse.bridgeiot.lib.handlers.AccessResponseSuccessHandler;
import org.eclipse.bridgeiot.lib.handlers.FeedFailureException;
import org.eclipse.bridgeiot.lib.handlers.FeedNotificationFailureHandler;
import org.eclipse.bridgeiot.lib.handlers.FeedNotificationSuccessHandler;
import org.eclipse.bridgeiot.lib.misc.MarketplaceClient;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.EndpointType;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.LicenseType;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.PricingModel;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.ValueType;
import org.eclipse.bridgeiot.lib.model.Price.Euros;
import org.eclipse.bridgeiot.lib.offering.AccessResponse;
import org.eclipse.bridgeiot.lib.offering.IOfferingCore;
import org.eclipse.bridgeiot.lib.offering.OfferingCore;
import org.eclipse.bridgeiot.lib.offering.OfferingDescription;
import org.eclipse.bridgeiot.lib.offering.RegistrableOfferingDescription;
import org.eclipse.bridgeiot.lib.offering.SubscribableOfferingDescriptionCore;
import org.eclipse.bridgeiot.lib.offering.parameters.AccessParameters;
import org.eclipse.bridgeiot.lib.query.OfferingQuery;
import org.eclipse.bridgeiot.lib.serverwrapper.BridgeIotHttpResponse;
import org.eclipse.bridgeiot.lib.serverwrapper.EmbededdedRouteBasedServer;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestAccess {

    private static Provider provider;
    private static final String DEFAULT_KEYSTORE_LOCATION = "keystore/keystore.jks";
    private static final String DEFAULT_KEYSTORE_PASSWORD = "12345678";
    private static final String DEFAULT_PEM_CERTIFICATE_FILE = "keystore/bigiot-lib-cert.pem";
    private static final String MARKETPLACE_URI = "http://172.17.17.11:8082";
    // private static String MARKETPLACE_URI = "http://localhost:8080";
    private static String PROVIDER_ID = "";
    private static String CONSUMER_ID = "";
    private static RegistrableOfferingDescription offeringDescription;
    final static String FIND_OFF = "{\"query\":  \"query {findAllOfferings{allOfferings {offerings{id}}}}\"}";
    // private static RegistrableOfferingDescription secondOfferingDescription;
    final static String CREATE_ORGA = "{\"query\":  \"mutation {createOrganization(input:{name:\\\"TestOrga\\\" localId:\\\"TestOrga\\\"}){organisation {id}}}\"}";
    final static String CREATE_PROV = "{\"query\":  \"mutation {createProvider(input:{ organizationId: \\\"TestOrga\\\" name:\\\"TestProvider\\\"}){provider {id}}}\"}";
    final static String CREATE_CON = "{\"query\":  \"mutation {createConsumer(input:{ organizationId: \\\"TestOrga\\\" name:\\\"TestConsumer\\\"}){consumer {id}}}\"}";
    private static MarketplaceClient helperClient = MarketplaceClient.create(MARKETPLACE_URI,
            new BridgeIotClientId(PROVIDER_ID), "");
    final static Logger logger = LoggerFactory.getLogger(TestAccess.class);
    public static OfferingQuery query;
    public static ConsumerCore consumer;

    static ObjectMapper mapper = new ObjectMapper();

    private static Throwable testMarketplaceConnections() {
        Throwable x = null;
        try {
            helperClient.request(FIND_OFF);
        } catch (IOException e) {
            x = new RuntimeException("Marketplace unavailable");
        }
        return x;
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

        org.junit.Assume.assumeNoException(testMarketplaceConnections());
        boolean run = true;

        Response r = helperClient.request(CREATE_ORGA);
        JsonNode jsonObject = mapper.reader().readTree(r.body().string());
        r.close();

        String rootKey = jsonObject.fieldNames().next();
        String id = jsonObject.get(rootKey).get("createOrganization").get("organisation").get("id").asText();

        r = helperClient.request(CREATE_PROV);
        jsonObject = mapper.reader().readTree(r.body().string());
        r.close();

        rootKey = jsonObject.fieldNames().next();
        PROVIDER_ID = jsonObject.get(rootKey).get("createProvider").get("provider").get("id").asText();

        r = helperClient.request(CREATE_CON);
        jsonObject = mapper.reader().readTree(r.body().string());
        r.close();

        rootKey = jsonObject.fieldNames().next();
        CONSUMER_ID = jsonObject.get(rootKey).get("createConsumer").get("consumer").get("id").asText();
        logger.info(CONSUMER_ID);

        // Setup provider
        EmbededdedRouteBasedServer myServer = new EmbeddedSpark("localhost", 9002, "bigiot/access",
                ServerOptionsSpark.defaultOptions);
        myServer.start();

        // Initialize provider with provider id and marketpalce URL
        Provider provider = Provider.create(PROVIDER_ID, MARKETPLACE_URI);
        provider.authenticate("12345678");

        // Construct Offering Description of your Offering incrementally
        offeringDescription = provider.createOfferingDescription("available_parking_info_offering")
                .withName("Parking Information Query").withCategory("urn:big-iot:ParkingSpaceCategory")
                .addInputData("longitude", "schema:longitude", ValueType.NUMBER)
                .addInputData("latitude", "schema:latitude", ValueType.NUMBER)
                .addInputData("radius", "schema:geoRadius", ValueType.NUMBER)
                .addOutputData("longitude", "schema:longitude", ValueType.NUMBER)
                .addOutputData("latitude", "schema:latitude", ValueType.NUMBER)
                .addOutputData("distance", "datex:distanceFromParkingSpace", ValueType.NUMBER)
                .addOutputData("status", "datex:parkingSpaceStatus", ValueType.TEXT).inRegion("Barcelona")
                .withPrice(Euros.amount(0.001)).withPricingModel(PricingModel.PER_ACCESS)
                .withLicenseType(LicenseType.OPEN_DATA_LICENSE).withProtocol(EndpointType.HTTP_GET)
                .withRoute("availableparkinginfo").withAccessRequestHandler(accessCallbackDummy).deployOn(myServer);

        offeringDescription.register();

        // Create query
        query = OfferingQuery.create("TestOfferingQuery").withName("Parking Information Query")
                .withCategory("urn:big-iot:ParkingSpaceCategory").inRegion("Barcelona")
                .withPricingModel(PricingModel.PER_ACCESS).withMaxPrice(Euros.amount(0.001))
                .withLicenseType(LicenseType.OPEN_DATA_LICENSE);

        consumer = new ConsumerCore("TestOrga-TestConsumer", MARKETPLACE_URI);
        consumer.authenticate("12345678");

    }

    private static AccessRequestHandler accessCallbackDummy = new AccessRequestHandler() {
        @Override
        public BridgeIotHttpResponse processRequestHandler(OfferingDescription offeringDescription,
                Map<String, Object> inputData, String subscriptionId, String consumerInfo) {

            double longitude = 0, latitude = 0, radius = 0;

            BridgeIotHttpResponse errorResponse = BridgeIotHttpResponse.error().withBody("{\"status\":\"error\"}")
                    .withStatus(422).asJsonType();
            if (!inputData.containsKey("areaSpecification"))
                return errorResponse;
            Map areaSpecification = (Map) inputData.get("areaSpecification");

            if (!areaSpecification.containsKey("geoCoordinates"))
                return errorResponse;
            Map geoCoordinates = (Map) areaSpecification.get("geoCoordinates");

            if (!geoCoordinates.containsKey("longitude"))
                return errorResponse;
            longitude = new Double((String) geoCoordinates.get("longitude"));

            if (!geoCoordinates.containsKey("latitude"))
                return errorResponse;
            latitude = new Double((String) geoCoordinates.get("latitude"));

            if (!areaSpecification.containsKey("radius"))
                return errorResponse;
            radius = new Double((String) areaSpecification.get("radius"));

            // java.util.Random r = new java.util.Random();
            double r = 1;
            int n = 1;
            String s = "{\n\"_comment\": \"dummy data\",\n\"results\": [\n";

            for (int i = 0; i < n; i++) {
                if (i > 0)
                    s += ",\n";
                s += String.format(Locale.US,
                        "{\"distance\": %.2f,\n\"latitude\": %.4f,\n\"longitude\": %.4f,\n\"status\":\"available\"\n}",
                        r, r * 0.01 + latitude, r * 0.01 + longitude);
            }
            s += "]}";
            return BridgeIotHttpResponse.okay().withBody(s).asJsonType();
        };
    };

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void consumerAccessRegisteredOffering() throws IOException, InterruptedException, ExecutionException,
            AccessToNonSubscribedOfferingException, NotRegisteredException, IllegalEndpointException,
            IncompleteOfferingDescriptionException, FailedDiscoveryException {
        List<SubscribableOfferingDescriptionCore> offeringDescriptions = consumer.discoverBlocking(query);
        assertEquals(1, offeringDescriptions.size());
        assertEquals("PI", ((SubscribableOfferingDescriptionCore) offeringDescriptions.get(0)).getName());
        // Just select the first Offering in the list
        SubscribableOfferingDescriptionCore selectedOfferingDescription = offeringDescriptions.get(0);

        // Instantiation of Offering Access objects via subscribe
        OfferingCore offering = selectedOfferingDescription.subscribeBlocking();

        // Prepare access parameters
        AccessParameters accessParameters = AccessParameters.create().addNameValue("areaSpecification",
                AccessParameters
                        .create().addNameValue("geoCoordinates", AccessParameters.create()
                                .addNameValue("latitude", 41.3879).addNameValue("longitude", 2.1342))
                        .addNameValue("radius", 777));

        Future<AccessResponse> response = offering.accessOneTime(accessParameters);

        AccessResponse accessResponse = response.get();
        // assertFalse(offering.getMyAccessFeeds().isEmpty());
        assertEquals("[2.1442]", accessResponse.asJsonNode().get("results").findValuesAsText("longitude").toString());
        assertEquals("[41.3979]", accessResponse.asJsonNode().get("results").findValuesAsText("latitude").toString());

    }

    @Test
    public void testAccessRegisteredOfferingCallback()
            throws IOException, InterruptedException, ExecutionException, AccessToNonSubscribedOfferingException,
            IllegalAccessParameterException, AccessToNonActivatedOfferingException, NotRegisteredException,
            IllegalEndpointException, IncompleteOfferingDescriptionException, FailedDiscoveryException {
        List<SubscribableOfferingDescriptionCore> offeringDescriptions = consumer.discoverBlocking(query);
        assertEquals("PI", ((SubscribableOfferingDescriptionCore) offeringDescriptions.get(0)).getName());
        // Just select the first Offering in the list
        SubscribableOfferingDescriptionCore selectedOfferingDescription = offeringDescriptions.get(0);

        // Instantiation of Offering Access objects via subscribe
        OfferingCore offering = selectedOfferingDescription.subscribeBlocking();

        // Prepare access parameters
        // AccessParameters accessParameters = AccessParameters.create()
        // .addRdfTypeValue("schema:longitude"), 2.1342)
        // .addRdfTypeValue("schema:latitude"), 41.3879)
        // .addRdfTypeValue("schema:geoRadius"), 0.5);

        AccessParameters accessParameters = AccessParameters.create().addNameValue("areaSpecification",
                AccessParameters
                        .create().addNameValue("geoCoordinates", AccessParameters.create()
                                .addNameValue("latitude", 41.3879).addNameValue("longitude", 2.1342))
                        .addNameValue("radius", 0.5));

        final CountDownLatch lock = new CountDownLatch(1);
        offering.accessOneTime(accessParameters, new AccessResponseSuccessHandler() {

            @Override
            public void processResponseOnSuccess(IOfferingCore reference, AccessResponse response) {

                assertEquals("[2.1442]", response.asJsonNode().get("results").findValuesAsText("longitude").toString());
                assertEquals("[41.3979]", response.asJsonNode().get("results").findValuesAsText("latitude").toString());

                lock.countDown();

            }
        });
        assertEquals(true, lock.await(10000, TimeUnit.MILLISECONDS));

    }

    // @Test (expected = AccessToNonActivatedOfferingException.class) //Does not work as offeringDescription object's
    // active flag in OfferingCore is not updated when provider deregisters his offering
    public void testNoAccessDeregisteredOffering()
            throws IOException, AccessToNonSubscribedOfferingException, InterruptedException, ExecutionException,
            IllegalAccessParameterException, AccessToNonActivatedOfferingException, NotRegisteredException,
            IllegalEndpointException, IncompleteOfferingDescriptionException, FailedDiscoveryException {
        List<SubscribableOfferingDescriptionCore> offeringDescriptions = consumer.discoverBlocking(query);

        // Just select the first Offering in the list
        SubscribableOfferingDescriptionCore selectedOfferingDescription = offeringDescriptions.get(0);
        offeringDescription.deregister();

        // Instantiation of Offering Access objects via subscribe
        OfferingCore offering = selectedOfferingDescription.subscribeBlocking();
        // Prepare access parameters
        AccessParameters accessParameters = AccessParameters.create().addRdfTypeValue("schema:longitude", 2.1342)
                .addRdfTypeValue("schema:latitude", 41.3879).addRdfTypeValue("schema:geoRadius", 0.5);

        final CountDownLatch lock = new CountDownLatch(1);
        offering.accessOneTime(accessParameters, new AccessResponseSuccessHandler() {

            @Override
            public void processResponseOnSuccess(IOfferingCore reference, AccessResponse response) {
                // assertFalse(offeringDescription.getActive());

                lock.countDown();

            }
        });
        assertEquals(true, lock.await(10000, TimeUnit.MILLISECONDS));

    }

    @Test(expected = AccessToNonSubscribedOfferingException.class) // RuntimeException.class)
    public void testNoAccessUnsubscribedOffering()
            throws IOException, InterruptedException, ExecutionException, IllegalAccessParameterException,
            AccessToNonSubscribedOfferingException, AccessToNonActivatedOfferingException, NotRegisteredException,
            IllegalEndpointException, IncompleteOfferingDescriptionException, FailedDiscoveryException {
        List<SubscribableOfferingDescriptionCore> offeringDescriptions = consumer.discoverBlocking(query);

        // Just select the first Offering in the list
        SubscribableOfferingDescriptionCore selectedOfferingDescription = offeringDescriptions.get(0);

        // Instantiation of Offering Access objects via subscribe
        OfferingCore offering = selectedOfferingDescription.subscribeBlocking();
        offering.unsubscribeBlocking();

        // Prepare access parameters
        AccessParameters accessParameters = AccessParameters.create().addRdfTypeValue("schema:longitude", 2.1342)
                .addRdfTypeValue("schema:latitude", 41.3879).addRdfTypeValue("schema:geoRadius", 0.5);

        final CountDownLatch lock = new CountDownLatch(1);
        offering.accessOneTime(accessParameters, new AccessResponseSuccessHandler() {

            @Override
            public void processResponseOnSuccess(IOfferingCore reference, AccessResponse response) {
                lock.countDown();

            }
        });
        assertEquals(true, lock.await(10000, TimeUnit.MILLISECONDS));
    }

    @Test(expected = IllegalAccessParameterException.class)
    public void testAccessParameterFail()
            throws IOException, InterruptedException, ExecutionException, AccessToNonSubscribedOfferingException,
            AccessToNonActivatedOfferingException, IllegalAccessParameterException, NotRegisteredException,
            IllegalEndpointException, IncompleteOfferingDescriptionException, FailedDiscoveryException {
        List<SubscribableOfferingDescriptionCore> offeringDescriptions = consumer.discoverBlocking(query);
        assertEquals(1, offeringDescriptions.size());
        assertEquals("PI", ((SubscribableOfferingDescriptionCore) offeringDescriptions.get(0)).getName());
        // Just select the first Offering in the list
        SubscribableOfferingDescriptionCore selectedOfferingDescription = offeringDescriptions.get(0);

        // Instantiation of Offering Access objects via subscribe
        OfferingCore offering = selectedOfferingDescription.subscribeBlocking();

        // Prepare access parameters
        AccessParameters accessParameters = AccessParameters.create().addRdfTypeValue((String) null, 2.1342)
                .addRdfTypeValue("test", 41.3879).addRdfTypeValue("schema:geoRadius", 0.5);

        final CountDownLatch lock = new CountDownLatch(1);
        offering.accessOneTime(accessParameters, new AccessResponseSuccessHandler() {

            @Override
            public void processResponseOnSuccess(IOfferingCore reference, AccessResponse response) {
                // assertFalse(offeringDescription.getActive());
                lock.countDown();

            }
        });
        assertEquals(true, lock.await(10000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testAccessContinuous() throws IOException, InterruptedException, AccessToNonSubscribedOfferingException,
            AccessToNonActivatedOfferingException, NotRegisteredException, IllegalEndpointException,
            IncompleteOfferingDescriptionException, FailedDiscoveryException {
        List<SubscribableOfferingDescriptionCore> offeringDescriptions = consumer.discoverBlocking(query);
        SubscribableOfferingDescriptionCore selectedOfferingDescription = offeringDescriptions.get(0);
        OfferingCore offering = selectedOfferingDescription.subscribeBlocking();

        AccessParameters accessParameters = AccessParameters.create().addNameValue("areaSpecification",
                AccessParameters
                        .create().addNameValue("geoCoordinates", AccessParameters.create()
                                .addNameValue("latitude", 41.3879).addNameValue("longitude", 2.1342))
                        .addNameValue("radius", 0.5));

        final CountDownLatch lock = new CountDownLatch(1);

        AccessFeed feed = offering.accessContinuous(accessParameters, 10000, new FeedNotificationSuccessHandler() {
            @Override
            public void processNotificationOnSuccess(IAccessFeed reference, AccessResponse response)
                    throws InterruptedException, ExecutionException {
                lock.countDown();
                assertEquals("[2.1442]", response.asJsonNode().get("results").findValuesAsText("longitude").toString());
                assertEquals("[41.3979]", response.asJsonNode().get("results").findValuesAsText("latitude").toString());
            }
        }, new FeedNotificationFailureHandler() {

            @Override
            public void processNotificationOnFailure(IAccessFeed reference, FeedFailureException failure) {
                assertFalse(true);

            }
        });
        assertNotNull(feed.getStatus());
        assertEquals(true, lock.await(10000, TimeUnit.MILLISECONDS));

    }

    @Test
    public void testMultipleAccess() throws IOException, InterruptedException, AccessToNonSubscribedOfferingException,
            AccessToNonActivatedOfferingException, NotRegisteredException, IllegalEndpointException,
            IncompleteOfferingDescriptionException, FailedDiscoveryException {

        List<SubscribableOfferingDescriptionCore> offeringDescriptions = consumer.discoverBlocking(query);
        SubscribableOfferingDescriptionCore selectedOfferingDescription = offeringDescriptions.get(0);
        OfferingCore offering = selectedOfferingDescription.subscribeBlocking();
        OfferingCore offering1 = selectedOfferingDescription.subscribeBlocking();
        AccessParameters accessParameters = AccessParameters.create().addNameValue("areaSpecification",
                AccessParameters
                        .create().addNameValue("geoCoordinates", AccessParameters.create()
                                .addNameValue("latitude", 41.3879).addNameValue("longitude", 2.1342))
                        .addNameValue("radius", 0.5));

        final CountDownLatch lock = new CountDownLatch(1);
        final CountDownLatch lock2 = new CountDownLatch(1);

        AccessFeed feed = offering.accessContinuous(accessParameters, 10000, new FeedNotificationSuccessHandler() {
            @Override
            public void processNotificationOnSuccess(IAccessFeed reference, AccessResponse response)
                    throws InterruptedException, ExecutionException {
                lock.countDown();
                assertEquals("[2.1442]", response.asJsonNode().get("results").findValuesAsText("longitude").toString());
                assertEquals("[41.3979]", response.asJsonNode().get("results").findValuesAsText("latitude").toString());
            }
        }, new FeedNotificationFailureHandler() {

            @Override
            public void processNotificationOnFailure(IAccessFeed reference, FeedFailureException failure) {
                assertFalse(true);

            }
        });
        AccessFeed feed1 = offering1.accessContinuous(accessParameters, 10000, new FeedNotificationSuccessHandler() {
            @Override
            public void processNotificationOnSuccess(IAccessFeed reference, AccessResponse response)
                    throws InterruptedException, ExecutionException {
                lock2.countDown();
                assertEquals("[2.1442]", response.asJsonNode().get("results").findValuesAsText("longitude").toString());
                assertEquals("[41.3979]", response.asJsonNode().get("results").findValuesAsText("latitude").toString());

            }
        }, new FeedNotificationFailureHandler() {

            @Override
            public void processNotificationOnFailure(IAccessFeed reference, FeedFailureException failure) {
                assertFalse(true);
            }
        });
        assertNotNull(feed.getStatus());
        assertNotNull(feed1.getStatus());
        assertEquals(true, lock.await(10000, TimeUnit.MILLISECONDS));
        assertEquals(true, lock2.await(10000, TimeUnit.MILLISECONDS));
    }

    @After
    public void cleanup() {
    }

}
