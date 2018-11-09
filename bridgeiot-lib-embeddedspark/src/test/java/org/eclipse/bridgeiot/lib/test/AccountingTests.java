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

import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import org.eclipse.bridgeiot.lib.BridgeIotClientId;
import org.eclipse.bridgeiot.lib.Consumer;
import org.eclipse.bridgeiot.lib.Provider;
import org.eclipse.bridgeiot.lib.ProviderSpark;
import org.eclipse.bridgeiot.lib.exceptions.AccessToNonActivatedOfferingException;
import org.eclipse.bridgeiot.lib.exceptions.AccessToNonSubscribedOfferingException;
import org.eclipse.bridgeiot.lib.exceptions.BridgeIoTException;
import org.eclipse.bridgeiot.lib.exceptions.FailedDiscoveryException;
import org.eclipse.bridgeiot.lib.exceptions.IncompleteOfferingDescriptionException;
import org.eclipse.bridgeiot.lib.exceptions.IncompleteOfferingQueryException;
import org.eclipse.bridgeiot.lib.exceptions.NotRegisteredException;
import org.eclipse.bridgeiot.lib.feed.AccessFeed;
import org.eclipse.bridgeiot.lib.handlers.AccessRequestHandler;
import org.eclipse.bridgeiot.lib.misc.BridgeIotProperties;
import org.eclipse.bridgeiot.lib.misc.Helper;
import org.eclipse.bridgeiot.lib.misc.MarketplaceClient;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.LicenseType;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.PricingModel;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.ValueType;
import org.eclipse.bridgeiot.lib.model.Price.Euros;
import org.eclipse.bridgeiot.lib.offering.AccessResponse;
import org.eclipse.bridgeiot.lib.offering.Offering;
import org.eclipse.bridgeiot.lib.offering.OfferingDescription;
import org.eclipse.bridgeiot.lib.offering.RegistrableOfferingDescription;
import org.eclipse.bridgeiot.lib.offering.internal.Accounting.AccountingReport;
import org.eclipse.bridgeiot.lib.offering.parameters.AccessParameters;
import org.eclipse.bridgeiot.lib.query.OfferingQuery;
import org.eclipse.bridgeiot.lib.serverwrapper.BridgeIotHttpResponse;
import org.joda.time.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AccountingTests {

    private static Provider provider = null;
    private static Consumer consumer = null;
    private static OfferingQuery query = null;
    private static Offering offering = null;
    private static RegistrableOfferingDescription offeringDescription = null;

    private final static String TEST_OFFERING_LOCALID = "TestOffering_AT";
    private static String TEST_OFFERING_ID;
    private final static String FIND_OFF = "{\"query\": \"query { allOfferings { allOfferings { id } } }\"}";

    private static BridgeIotProperties testProperties;
    private static int accessCnt;

    static ObjectMapper mapper = new ObjectMapper();

    final static Logger logger = LoggerFactory.getLogger(AccountingTests.class);

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

        TEST_OFFERING_ID = testProperties.PROVIDER_ID + "-" + TEST_OFFERING_LOCALID;

        org.junit.Assume.assumeNoException(testMarketplaceConnections());

        initializeProvider();

    }

    public static void initializeConsumerAndQuery()
            throws IOException, IncompleteOfferingQueryException, InterruptedException {

        consumer = new Consumer(testProperties.CONSUMER_ID, testProperties.MARKETPLACE_URI);
        consumer.authenticate(testProperties.CONSUMER_SECRET);

        query = OfferingQuery.create("TestOfferingQuery").withName("Parking Information Query")
                .withCategory("urn:big-iot:ParkingSpaceCategory").inRegion("Barcelona")
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
                .inRegion("Barcelona").withPrice(Euros.amount(0.005)).withPricingModel(PricingModel.PER_ACCESS)
                .withLicenseType(LicenseType.OPEN_DATA_LICENSE).withAccessRequestHandler(accessCallbackDummy);

        offeringDescription.register();

    }

    @Before
    public void setUp() throws Exception {

        initializeConsumerAndQuery();

    }

    @Test
    public void testAccessOneTime() throws IOException, NotRegisteredException, FailedDiscoveryException,
            InterruptedException, ExecutionException {

        Offering offering = consumer.subscribeByOfferingId(TEST_OFFERING_ID).get();

        for (int i = 0; i < 3; i++) {

            AccessResponse response = offering.accessOneTime(AccessParameters.create()).get();

            AccountingReport consumerReport = consumer.getCurrentAccountingReports().get(0);
            AccountingReport providerReport = provider.getCurrentAccountingReports().get(0);

            Long numBytes = 14L;
            Long numRecords = 1L;
            assertEquals(numBytes, consumerReport.getRecord().getCurrentBytes());
            assertEquals(numBytes, consumerReport.getRecord().getTotalBytes());
            assertEquals(numBytes, providerReport.getRecord().getCurrentBytes());
            assertEquals(numBytes, providerReport.getRecord().getTotalBytes());
            assertEquals(numRecords, consumerReport.getRecord().getCurrentDataRecords());
            assertEquals(numRecords, consumerReport.getRecord().getTotalDataRecords());
            assertEquals(numRecords, providerReport.getRecord().getCurrentDataRecords());
            assertEquals(numRecords, providerReport.getRecord().getTotalDataRecords());

        }

    }

    @Test
    public void testAccessContinuous() throws IOException, NotRegisteredException, FailedDiscoveryException,
            InterruptedException, ExecutionException, AccessToNonActivatedOfferingException,
            AccessToNonSubscribedOfferingException, RuntimeException {

        Offering offering = consumer.subscribeByOfferingId(TEST_OFFERING_ID).get();

        // ensure that the provider accounting reports from previous tests are cleared.
        provider.getCurrentAccountingReports().clear();

        final int iterations = 5;
        Duration feedDuration = Duration.standardSeconds(iterations * 2);
        Duration feedInterval = Duration.standardSeconds(1);
        // final List<Integer> accessCntList = ArrayList<>();
        accessCnt = 0;
        Long numBytes = 14L;
        Long numRecords = 1L;
        AccessFeed accessFeed = offering.accessContinuous(AccessParameters.create(), feedDuration.getMillis(),
                feedInterval.getMillis(), (f, r) -> {
                    logger.info("Access Count {}: Incoming feed data: {} elements received. ", accessCnt + 1,
                            r.asJsonNode().size());
                    if (accessCnt < iterations) {
                        AccountingReport consumerReport = consumer.getCurrentAccountingReports().get(accessCnt);
                        AccountingReport providerReport = provider.getCurrentAccountingReports().get(accessCnt);
                        Long totalNumBytes = numBytes * (accessCnt + 1);
                        Long totalNumRecords = numRecords * (accessCnt + 1);
                        logger.info("===>>> ConsumerReport: bytes {}, total bytes {}, records {}, total records {}",
                                consumerReport.getRecord().getCurrentBytes(),
                                consumerReport.getRecord().getTotalBytes(),
                                consumerReport.getRecord().getCurrentDataRecords(),
                                consumerReport.getRecord().getTotalDataRecords());
                        logger.info("===>>> ProviderReport: bytes {}, total bytes {}, records {}, total records {}",
                                providerReport.getRecord().getCurrentBytes(),
                                providerReport.getRecord().getTotalBytes(),
                                providerReport.getRecord().getCurrentDataRecords(),
                                providerReport.getRecord().getTotalDataRecords());

                        if ((numBytes != consumerReport.getRecord().getCurrentBytes())
                                || (totalNumBytes != consumerReport.getRecord().getTotalBytes())
                                || (numBytes != providerReport.getRecord().getCurrentBytes())
                                || (totalNumBytes != providerReport.getRecord().getTotalBytes())
                                || (numRecords != consumerReport.getRecord().getCurrentDataRecords())
                                || (totalNumRecords != consumerReport.getRecord().getTotalDataRecords())
                                || (numRecords != providerReport.getRecord().getCurrentDataRecords())
                                || (totalNumRecords != providerReport.getRecord().getTotalDataRecords())) {
                            throw new RuntimeException("Accounting Error");
                        }
                    }
                    accessCnt++;
                }, (f, r) -> logger.info("Feed operation failed"));

        Thread.sleep(iterations * Helper.Second);

        accessFeed.stop();

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

}
