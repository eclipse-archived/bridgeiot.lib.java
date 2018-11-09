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
package org.eclipse.bridgeiot.lib;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import okhttp3.Response;
import okhttp3.ResponseBody;

import org.eclipse.bridgeiot.lib.configuration.LibConfiguration;
import org.eclipse.bridgeiot.lib.exceptions.FailedDiscoveryException;
import org.eclipse.bridgeiot.lib.exceptions.IllegalEndpointException;
import org.eclipse.bridgeiot.lib.exceptions.IncompleteOfferingDescriptionException;
import org.eclipse.bridgeiot.lib.exceptions.IncompleteOfferingQueryException;
import org.eclipse.bridgeiot.lib.exceptions.InvalidOfferingException;
import org.eclipse.bridgeiot.lib.exceptions.NotRegisteredException;
import org.eclipse.bridgeiot.lib.handlers.DiscoverFailureException;
import org.eclipse.bridgeiot.lib.handlers.DiscoverResponseErrorHandler;
import org.eclipse.bridgeiot.lib.handlers.DiscoverResponseHandler;
import org.eclipse.bridgeiot.lib.misc.MarketplaceClient;
import org.eclipse.bridgeiot.lib.model.Activation;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.AccessInterfaceType;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.EndpointType;
import org.eclipse.bridgeiot.lib.model.EndPoint;
import org.eclipse.bridgeiot.lib.offering.OfferingCore;
import org.eclipse.bridgeiot.lib.offering.OfferingDescription;
import org.eclipse.bridgeiot.lib.offering.SubscribableOfferingDescriptionCore;
import org.eclipse.bridgeiot.lib.query.IOfferingQuery;
import org.eclipse.bridgeiot.lib.query.OfferingQuery;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.class)
public class ConsumerCoreTest {

    private final static String JSON_ADD_OFFERING = "{\"data\":{\"addOfferingQuery\":{\"id\":\"superman\"}}}";
    private final static String JSON_MATCHING_OFFERINGS_EMPTY = "{\"data\":{\"matchingOfferings\":[]}}";
    private final static String JSON_MATCHING_OFFERINGS_TEMPLATE = "{\"data\":{\"matchingOfferings\":[{\"id\":\"%s\",\"activation\":{\"status\":true,\"expirationTime\":1513672278599} }]}}";
    private final static String JSON_SUBSCRIBE_CONSUMER_TEMPLATE = "{\"data\":{\"subscribeConsumerToOffering\":{\"id\":\"%s\",\"accessToken\":\"%s\"}}}";
    private final static String JSON_SUBSCRIBE_QUERY_TEMPLATE = "   {\"data\":{\"subscribeQueryToOffering\":{\"id\":\"%s\",\"accessToken\":\"%s\"}}}";
    private final static String INVALID_JSON = "wrong_data";

    private final static boolean SUCCESS = true;
    private final static boolean FAILED = false;

    private static IOfferingQuery minimalQuery;

    ConsumerCore consumerUnderTest;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Spy
    private List<SubscribableOfferingDescriptionCore> offeringDescriptions = new ArrayList<>();

    @Mock
    private MarketplaceClient marketMock;

    @Mock
    private IOfferingQuery offeringQueryMock;

    @Mock
    private DiscoverResponseHandler successMock;

    @BeforeClass
    public static void setupClass() throws IncompleteOfferingQueryException {
        minimalQuery = OfferingQuery.create("IncompleteOffering");
    }

    @Before
    public void initialize() {
        consumerUnderTest = new ConsumerCore("enlighted-consumer", "Hugh-IoT-Marketplace");
        consumerUnderTest.marketplaceClient = marketMock;

        // minimal behavior
        mockOfferingWithId(offeringQueryMock, "checkId");
    }

    @Test
    public void regressionTestTwoConsumerWithSingleThreadPool() throws Throwable {
        ConsumerCore consumer1 = new ConsumerCore("consumer1", "generalMarketplace");
        ConsumerCore consumer2 = new ConsumerCore("consumer2", "generalMarketplace");

        consumer1.marketplaceClient = mock(MarketplaceClient.class);
        consumer1.finalize();

        consumer2.discoverFuture(offeringQueryMock);
    }

    @Test
    public void createOfferingQuerySuccess() throws IncompleteOfferingQueryException {
        ConsumerCore.createOfferingQuery("");
    }

    @Test(expected = IncompleteOfferingQueryException.class)
    public void createOfferingQueryIncomplete() throws IncompleteOfferingQueryException {
        ConsumerCore.createOfferingQuery(null);
    }

    @Test(expected = NotRegisteredException.class)
    public void discoverCallNotRegisteredException()
            throws NotRegisteredException, FailedDiscoveryException, IOException {
        // Arrange
        ConsumerCore consumerUnderTest = new ConsumerCore("consumer", "generalMarketplace");

        // Act
        consumerUnderTest.discoverCall(offeringQueryMock);

        // Assert
        // see expect Annotation
    }

    @Test(expected = FailedDiscoveryException.class)
    public void discoverCallNotSuccessfulFailedDiscoveryException()
            throws NotRegisteredException, FailedDiscoveryException, IOException, IncompleteOfferingQueryException {
        // Arrange client response
        mockMarketEmptyResponse(marketMock, FAILED);

        // Act
        consumerUnderTest.discoverCall(minimalQuery);

        // Assert
        // see expect Annotation
    }

    @Test(expected = FailedDiscoveryException.class)
    public void discoverCallNotSuccessfulInvalidJson()
            throws NotRegisteredException, FailedDiscoveryException, IOException, IncompleteOfferingQueryException {
        // Arrange client response
        Response respMock = createResponseMock(INVALID_JSON, true);
        when(marketMock.request(anyString())).thenReturn(respMock);

        // Act
        consumerUnderTest.discoverCall(minimalQuery);

        // Assert
        // see expect Annotation
    }

    @Test
    public void discoverCallQueryAlreadyCreatedSuccess()
            throws NotRegisteredException, FailedDiscoveryException, IOException, IncompleteOfferingQueryException {
        // Arrange client response
        mockMarketEmptyResponse(marketMock, SUCCESS);

        // Arrange query
        OfferingQuery queryFixture = OfferingQuery.create("IncompleteOffering");
        queryFixture.setId("batman");
        queryFixture = spy(queryFixture);

        // Act
        consumerUnderTest.discoverCall(queryFixture); // creates query
        consumerUnderTest.discoverCall(queryFixture); // reuses query

        // Assert
        verify(queryFixture, times(1)).setId(anyString()); // newQueryId is only set in the first run
    }

    @Test
    public void discoverCallSuccess()
            throws IOException, NotRegisteredException, FailedDiscoveryException, IncompleteOfferingQueryException {
        // Arrange client response
        mockMarketEmptyResponse(marketMock, SUCCESS);

        // Act
        String resp = consumerUnderTest.discoverCall(minimalQuery);

        // Assert
        assertThat(resp).isEqualTo(JSON_MATCHING_OFFERINGS_EMPTY);
    }

    @Test(expected = FailedDiscoveryException.class)
    public void discoverByIdCallFailedDiscoveryException() throws FailedDiscoveryException, IOException {
        // Arrange client response
        mockMarketEmptyResponse(marketMock, FAILED);

        // Act
        consumerUnderTest.discoverByIdCall("any-superman-id");

        // Assert
        // see expect Annotation
    }

    @Test
    public void discoverByIdCallSuccess() throws IOException, FailedDiscoveryException {
        String queryId = "superman-id";
        // Arrange client response
        Response matchingRespMock = createResponseMock(String.format(JSON_MATCHING_OFFERINGS_TEMPLATE, queryId), true);
        when(marketMock.request(contains(queryId))).thenReturn(matchingRespMock);

        // Act
        String resp = consumerUnderTest.discoverByIdCall(queryId);

        // Assert
        assertThat(resp).isEqualTo(String.format(JSON_MATCHING_OFFERINGS_TEMPLATE, queryId));
    }

    @Test
    public void discoverSuccess() throws InterruptedException, IOException, IncompleteOfferingQueryException {
        // Arrange client response
        // Arrange CountDownLatch
        final CountDownLatch lock = new CountDownLatch(1);
        // Arrange SuccessHandler
        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                lock.countDown();
                return null;
            }
        }).when(successMock).processResponse(any(IOfferingQuery.class),
                ArgumentMatchers.<List<SubscribableOfferingDescriptionCore>>any());

        // Act
        consumerUnderTest.discover(minimalQuery, successMock, mock(DiscoverResponseErrorHandler.class),
                offeringDescriptions);

        // Assert
        assertEquals(true, lock.await(1000, TimeUnit.MILLISECONDS));
        verify(successMock, times(1)).processResponse(any(IOfferingQuery.class),
                ArgumentMatchers.<List<SubscribableOfferingDescriptionCore>>any());
    }

    @Test
    public void discoverProcessingRequestException()
            throws IOException, IncompleteOfferingQueryException, InterruptedException {
        // Arrange client response
        // Arrange CountDownLatch
        final CountDownLatch lock = new CountDownLatch(1);

        // Arrange SuccessHandler
        doThrow(new RuntimeException()).when(successMock).processResponse(any(IOfferingQuery.class),
                ArgumentMatchers.<List<SubscribableOfferingDescriptionCore>>any());

        // Arrange ErrorHandler
        DiscoverResponseErrorHandler errorMock = mock(DiscoverResponseErrorHandler.class);
        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                Object[] arguments = invocation.getArguments();
                if (arguments != null && arguments.length > 1 && arguments[0] != null && arguments[1] != null) {
                    DiscoverFailureException error = (DiscoverFailureException) arguments[1];
                    assertThat(error.getMessage()).isEqualTo("Processing response failed!");
                }
                lock.countDown();
                return null;
            }
        }).when(errorMock).processResponse(any(IOfferingQuery.class), any(DiscoverFailureException.class));

        // Act
        consumerUnderTest.discover(minimalQuery, successMock, errorMock, offeringDescriptions);

        // Assert
        assertEquals(true, lock.await(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void discoverJsonUnmarshallException()
            throws InterruptedException, IOException, IncompleteOfferingQueryException {
        // Arrange client response
        Response respMock = createResponseMock(JSON_ADD_OFFERING, true);
        when(marketMock.request(anyString())).thenReturn(respMock);

        // Arrange CountDownLatch
        final CountDownLatch lock = new CountDownLatch(1);

        // Arrange ErrorHandler
        DiscoverResponseErrorHandler errorMock = mock(DiscoverResponseErrorHandler.class);
        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                Object[] arguments = invocation.getArguments();
                if (arguments != null && arguments.length > 1 && arguments[0] != null && arguments[1] != null) {
                    DiscoverFailureException error = (DiscoverFailureException) arguments[1];
                    assertThat(error.getMessage()).isEqualTo("Discover Request to eXchange failed!");
                }
                lock.countDown();
                return null;
            }
        }).when(errorMock).processResponse(any(IOfferingQuery.class), any(DiscoverFailureException.class));

        // Act
        consumerUnderTest.discover(minimalQuery, successMock, errorMock);

        // Assert
        assertEquals(true, lock.await(1000, TimeUnit.MILLISECONDS));
    }

    @Test(expected = IncompleteOfferingQueryException.class)
    public void discoverBlockingIncompleteOffering()
            throws NotRegisteredException, FailedDiscoveryException, IncompleteOfferingQueryException, IOException {
        // Arrange client response
        // nothing

        // Act
        consumerUnderTest.discoverBlocking(OfferingQuery.create(null));

        // Assert
        // see expect Annotation

    }

    @Test
    public void discoverBlockingEmptyResult()
            throws NotRegisteredException, FailedDiscoveryException, IncompleteOfferingQueryException, IOException {
        // Arrange client response
        mockMarketEmptyResponse(marketMock, SUCCESS);

        // Act
        List<SubscribableOfferingDescriptionCore> discoverBlocking = consumerUnderTest.discoverBlocking(minimalQuery);

        // Assert
        assertThat(discoverBlocking).isEmpty();
    }

    @Test
    public void discoverBlockingSuccess()
            throws NotRegisteredException, FailedDiscoveryException, IncompleteOfferingQueryException, IOException {
        // Arrange client response
        mockMarketResponse(marketMock, "superman-offering", SUCCESS);

        // Act
        List<SubscribableOfferingDescriptionCore> discoverBlocking = consumerUnderTest.discoverBlocking(minimalQuery);

        // Assert
        assertThat(discoverBlocking).hasSize(1).first().hasFieldOrProperty("id").hasFieldOrPropertyWithValue("id",
                "superman-offering");
    }

    @Test
    public void discoverdiscoverByIdBlockingEmptyResult()
            throws IOException, FailedDiscoveryException, NotRegisteredException {
        String queryId = "superman-id";
        // Arrange client response
        Response matchingRespMock = createResponseMock(String.format(JSON_MATCHING_OFFERINGS_EMPTY, queryId), true);
        when(marketMock.request(contains("matchingOfferings"))).thenReturn(matchingRespMock);

        // Act
        List<SubscribableOfferingDescriptionCore> discoverBlocking = consumerUnderTest.discoverByIdBlocking(queryId);

        // Assert
        assertThat(discoverBlocking).isEmpty();
    }

    @Test
    public void discoverdiscoverByIdBlockingSuccess()
            throws IOException, FailedDiscoveryException, NotRegisteredException {
        String queryId = "superman-id";
        // Arrange client response
        Response matchingRespMock = createResponseMock(String.format(JSON_MATCHING_OFFERINGS_TEMPLATE, queryId), true);
        when(marketMock.request(contains(queryId))).thenReturn(matchingRespMock);

        // Act
        List<SubscribableOfferingDescriptionCore> discoverBlocking = consumerUnderTest.discoverByIdBlocking(queryId);

        // Assert
        assertThat(discoverBlocking).hasSize(1).first().hasFieldOrProperty("id").hasFieldOrPropertyWithValue("id",
                queryId);
    }

    @Test
    public void discoverByIdFutureSuccess() throws FailedDiscoveryException, IOException, InterruptedException,
            ExecutionException, NotRegisteredException {
        String queryId = "superman-id";
        // Arrange client response
        ConsumerCore spyedConsumer = spy(consumerUnderTest); // use spy only to be able to count method invocation

        // Arrange CountDownLatch
        final CountDownLatch lock = new CountDownLatch(1);

        // Arrange SuccessHandler
        doAnswer(new Answer<List<SubscribableOfferingDescriptionCore>>() {
            public List<SubscribableOfferingDescriptionCore> answer(InvocationOnMock invocation) {
                lock.countDown();
                return Arrays.asList(mock(SubscribableOfferingDescriptionCore.class));
            }
        }).when(spyedConsumer).discoverByIdBlocking(queryId);

        // Act
        Future<List<SubscribableOfferingDescriptionCore>> discoverByIdFuture = spyedConsumer
                .discoverByIdFuture(queryId);

        // Assert
        assertEquals(true, lock.await(1000, TimeUnit.MILLISECONDS));
        verify(spyedConsumer, times(1)).discoverByIdBlocking(queryId);
        assertThat(discoverByIdFuture).isNotNull();
        assertThat(discoverByIdFuture.get()).hasSize(1);
    }

    @Test(expected = ExecutionException.class)
    public void discoverByIdFutureExecutionException() throws FailedDiscoveryException, IOException,
            InterruptedException, ExecutionException, NotRegisteredException {
        String queryId = "superman-id";
        // Arrange client response
        ConsumerCore spyedConsumer = spy(consumerUnderTest);
        doThrow(new FailedDiscoveryException()).when(spyedConsumer).discoverByIdBlocking(queryId);

        // Act
        Future<List<SubscribableOfferingDescriptionCore>> discoverByIdFuture = spyedConsumer
                .discoverByIdFuture(queryId);
        discoverByIdFuture.get();

        // Assert
        // see expect Annotation
    }

    @Test
    public void discoverContinousAlreadyRunningQuery() throws IncompleteOfferingQueryException {
        // Arrange
        Map<String, ScheduledExecutorService> discoveryExecutorMap = new HashMap<String, ScheduledExecutorService>();
        discoveryExecutorMap.put("checkId", null);
        Map<String, ScheduledExecutorService> discoveryExecutorMapSpy = spy(discoveryExecutorMap);
        Map<String, IOfferingQuery> offeringQueryMap = new HashMap<>();
        Map<String, OfferingCore> subscribedOfferingMap = new HashMap<>();
        ConsumerCore consumerUnderTest = new ConsumerCore("consumer", "generalMarketplace",
                Executors.newFixedThreadPool(LibConfiguration.EXECUTOR_POOL_SIZE), discoveryExecutorMapSpy,
                offeringQueryMap, subscribedOfferingMap);

        // Act
        consumerUnderTest.discoverContinous(offeringQueryMock, successMock, mock(DiscoverResponseErrorHandler.class),
                ConsumerCore.DEFAULT_DISCOVER_INTERVAL);

        // Assert
        verify(discoveryExecutorMapSpy, times(0)).put(anyString(), any(ScheduledExecutorService.class));
    }

    @Test
    public void discoverContinousSuccess() throws IncompleteOfferingQueryException {
        // Arrange
        Map<String, ScheduledExecutorService> discoveryExecutorMap = new HashMap<String, ScheduledExecutorService>();
        Map<String, ScheduledExecutorService> discoveryExecutorMapSpy = spy(discoveryExecutorMap);
        Map<String, IOfferingQuery> offeringQueryMap = new HashMap<>();
        Map<String, OfferingCore> subscribedOfferingMap = new HashMap<>();
        ConsumerCore consumerUnderTest = new ConsumerCore("consumer", "generalMarketplace",
                Executors.newFixedThreadPool(LibConfiguration.EXECUTOR_POOL_SIZE), discoveryExecutorMapSpy,
                offeringQueryMap, subscribedOfferingMap);

        // Act
        consumerUnderTest.discoverContinous(offeringQueryMock, successMock, mock(DiscoverResponseErrorHandler.class),
                ConsumerCore.DEFAULT_DISCOVER_INTERVAL);

        // Assert
        verify(discoveryExecutorMapSpy, times(1)).put(anyString(), any(ScheduledExecutorService.class));
    }

    @Test
    public void discoverFutureSuccess()
            throws IOException, IncompleteOfferingQueryException, InterruptedException, ExecutionException {
        // Arrange client response
        mockMarketEmptyResponse(marketMock, SUCCESS);

        ConsumerCore spyedConsumer = spy(consumerUnderTest); // use spy only to be able to count method invocation

        // Act
        Future<List<SubscribableOfferingDescriptionCore>> future = spyedConsumer.discoverFuture(minimalQuery);
        future.get();

        // Assert
        verify(spyedConsumer, times(1)).discoverFuture(any(OfferingQuery.class));
    }

    @Test(expected = ExecutionException.class)
    public void discoverFutureExecutionException() throws IncompleteOfferingQueryException, NotRegisteredException,
            FailedDiscoveryException, IOException, InterruptedException, ExecutionException {
        IOfferingQuery query = minimalQuery;
        // Arrange client response
        ConsumerCore spyedConsumer = spy(consumerUnderTest);
        doThrow(new FailedDiscoveryException()).when(spyedConsumer).discoverBlocking(query);

        // Act
        Future<List<SubscribableOfferingDescriptionCore>> future = spyedConsumer.discoverFuture(query);
        future.get();

        // Assert
        // see expect Annotation
    }

    @Test
    public void initializeSubscribableOfferingDescription2InactiveRemoved()
            throws IOException, IncompleteOfferingQueryException, InterruptedException, ExecutionException {
        // Arrange client response
        SubscribableOfferingDescriptionCore mockedInactive = mock(SubscribableOfferingDescriptionCore.class);
        when(mockedInactive.getActivation()).thenReturn(new Activation(false));
        offeringDescriptions.add(mockedInactive);
        offeringDescriptions.add(mockedInactive);

        // Act
        consumerUnderTest.initializeSubscribableOfferingDescription(offeringDescriptions, "someQuery");

        // Assert
        assertThat(offeringDescriptions).hasSize(0);
        verify(mockedInactive, never()).setQueryId("someQuery");
    }

    @Test
    public void initializeSubscribableOfferingDescription1InactiveRemoved()
            throws IOException, IncompleteOfferingQueryException, InterruptedException, ExecutionException {
        // Arrange client response
        SubscribableOfferingDescriptionCore mockedInactive = mock(SubscribableOfferingDescriptionCore.class);
        when(mockedInactive.getActivation()).thenReturn(new Activation(false));
        SubscribableOfferingDescriptionCore mockedActive = mock(SubscribableOfferingDescriptionCore.class);
        when(mockedActive.getActivation()).thenReturn(new Activation(true));
        offeringDescriptions.add(mockedInactive);
        offeringDescriptions.add(mockedActive);

        // Act
        consumerUnderTest.initializeSubscribableOfferingDescription(offeringDescriptions, "someQuery");

        // Assert
        assertThat(offeringDescriptions).hasSize(1);
        verify(mockedActive, times(1)).setQueryId("someQuery");
        verify(mockedInactive, never()).setQueryId("someQuery");
    }

    @Test
    public void stopDiscoverContinuousSuccess() throws IOException, IncompleteOfferingQueryException {
        // Arrange client response
        mockMarketEmptyResponse(marketMock, SUCCESS);

        // Arrange consumer
        Map<String, ScheduledExecutorService> discoveryExecutorMapSpy = spy(
                new HashMap<String, ScheduledExecutorService>());
        Map<String, IOfferingQuery> offeringQueryMap = new HashMap<>();
        Map<String, OfferingCore> subscribedOfferingMap = new HashMap<>();
        ConsumerCore consumerUnderTest = new ConsumerCore("consumer", "generalMarketplace",
                Executors.newFixedThreadPool(LibConfiguration.EXECUTOR_POOL_SIZE), discoveryExecutorMapSpy,
                offeringQueryMap, subscribedOfferingMap);
        consumerUnderTest.marketplaceClient = marketMock;

        // Act
        consumerUnderTest.discoverContinous(offeringQueryMock, successMock, mock(DiscoverResponseErrorHandler.class),
                ConsumerCore.DEFAULT_DISCOVER_INTERVAL);
        consumerUnderTest.stopDiscoverContinuous(offeringQueryMock);

        // Assert
        assertThat(discoveryExecutorMapSpy).hasSize(0);
    }

    @Test
    public void stopDiscoverContinuousNothingToDo() throws IOException {
        // Arrange client response
        mockMarketEmptyResponse(marketMock, SUCCESS);

        // Arrange consumer
        Map<String, ScheduledExecutorService> discoveryExecutorMapSpy = spy(
                new HashMap<String, ScheduledExecutorService>());
        Map<String, IOfferingQuery> offeringQueryMap = new HashMap<>();
        Map<String, OfferingCore> subscribedOfferingMap = new HashMap<>();
        ConsumerCore consumerUnderTest = new ConsumerCore("consumer", "generalMarketplace",
                Executors.newFixedThreadPool(LibConfiguration.EXECUTOR_POOL_SIZE), discoveryExecutorMapSpy,
                offeringQueryMap, subscribedOfferingMap);
        consumerUnderTest.marketplaceClient = marketMock;

        // Act
        consumerUnderTest.stopDiscoverContinuous(offeringQueryMock);

        // Assert
        assertThat(discoveryExecutorMapSpy).hasSize(0);
    }

    @Test
    public void stopDiscoverContinuousStopOne() throws IOException, IncompleteOfferingQueryException {
        // Arrange client response
        mockMarketEmptyResponse(marketMock, SUCCESS);

        // Arrange consumer
        Map<String, ScheduledExecutorService> discoveryExecutorMapSpy = spy(
                new HashMap<String, ScheduledExecutorService>());
        Map<String, IOfferingQuery> offeringQueryMap = new HashMap<>();
        Map<String, OfferingCore> subscribedOfferingMap = new HashMap<>();
        ConsumerCore consumerUnderTest = new ConsumerCore("consumer", "generalMarketplace",
                Executors.newFixedThreadPool(LibConfiguration.EXECUTOR_POOL_SIZE), discoveryExecutorMapSpy,
                offeringQueryMap, subscribedOfferingMap);
        consumerUnderTest.marketplaceClient = marketMock;

        // Act
        consumerUnderTest.discoverContinous(offeringQueryMock, successMock, mock(DiscoverResponseErrorHandler.class),
                ConsumerCore.DEFAULT_DISCOVER_INTERVAL);
        consumerUnderTest.discoverContinous(mockOfferingWithId(mock(IOfferingQuery.class), "silversurfer"), successMock,
                mock(DiscoverResponseErrorHandler.class), ConsumerCore.DEFAULT_DISCOVER_INTERVAL);
        consumerUnderTest.discoverContinous(mockOfferingWithId(mock(IOfferingQuery.class), "wolverine"), successMock,
                mock(DiscoverResponseErrorHandler.class), ConsumerCore.DEFAULT_DISCOVER_INTERVAL);
        consumerUnderTest.stopDiscoverContinuous(offeringQueryMock);

        // Assert
        assertThat(discoveryExecutorMapSpy).hasSize(2);
    }

    @Test
    public void subscribeByOfferingIdBlockingInactiveSubscription()
            throws IOException, IncompleteOfferingQueryException, InterruptedException, ExecutionException,
            InvalidOfferingException, IllegalEndpointException, IncompleteOfferingDescriptionException {
        // Arrange client response
        mockMarketEmptyResponse(marketMock, SUCCESS);
        OfferingDescription offeringDescMock = mock(OfferingDescription.class);
        when(marketMock.getOfferingDescription("cool-offering-description")).thenReturn(offeringDescMock);

        // Act
        OfferingCore offering = consumerUnderTest.subscribeByOfferingIdBlocking("cool-offering-description");

        // Assert
        assertThat(offering).isNull();
    }

    @Test
    public void subscribeByOfferingIdBlockingUnspecifiedIntegrationMode()
            throws IOException, IncompleteOfferingQueryException, InterruptedException, ExecutionException,
            InvalidOfferingException, IllegalEndpointException, IncompleteOfferingDescriptionException {
        // Setup expected exception rule
        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage(
                "Cannot create Offering Access Object due to unsupported or unspecified integration mode");

        // Arrange client response
        mockMarketEmptyResponse(marketMock, SUCCESS);
        String queryId = "consumer-superman";
        Response consumerSubRespMock = createResponseMock(
                String.format(JSON_SUBSCRIBE_CONSUMER_TEMPLATE, queryId, "1234567890"), SUCCESS);
        when(marketMock.request(contains("subscribeConsumerToOffering"))).thenReturn(consumerSubRespMock);

        SubscribableOfferingDescriptionCore offeringDescMock = mock(SubscribableOfferingDescriptionCore.class);
        when(offeringDescMock.getActivation()).thenReturn(new Activation(true));
        when(offeringDescMock.getId()).thenReturn(queryId);
        when(offeringDescMock.getAccessInterfaceType()).thenReturn(AccessInterfaceType.UNSPECIFIED);
        when(marketMock.getOfferingDescription("cool-offering-description")).thenReturn(offeringDescMock);

        // Act
        consumerUnderTest.subscribeByOfferingIdBlocking("cool-offering-description");

        // Assert
        // see expectedException Rule
    }

    @Test
    public void subscribeByOfferingIdBlockingSuccess()
            throws IOException, IncompleteOfferingQueryException, InterruptedException, ExecutionException,
            InvalidOfferingException, IllegalEndpointException, IncompleteOfferingDescriptionException {
        // Arrange client response
        mockMarketEmptyResponse(marketMock, SUCCESS);
        String queryId = "consumer-superman";
        String token = "1234567890";
        Response consumerSubRespMock = createResponseMock(
                String.format(JSON_SUBSCRIBE_CONSUMER_TEMPLATE, queryId, token), SUCCESS);
        when(marketMock.request(contains("subscribeConsumerToOffering"))).thenReturn(consumerSubRespMock);

        // Arrange offering description
        SubscribableOfferingDescriptionCore offeringDescMock = mock(SubscribableOfferingDescriptionCore.class);
        when(offeringDescMock.getActivation()).thenReturn(new Activation(true));
        when(offeringDescMock.getId()).thenReturn(queryId);
        when(offeringDescMock.getAccessInterfaceType()).thenReturn(AccessInterfaceType.BRIDGEIOT_LIB);
        when(offeringDescMock.getEndpoints()).thenReturn(Arrays
                .asList(new EndPoint(EndpointType.HTTP_GET, AccessInterfaceType.EXTERNAL, "http://superman.com")));
        when(marketMock.getOfferingDescription("cool-offering-description")).thenReturn(offeringDescMock);

        // Act
        OfferingCore offering = consumerUnderTest.subscribeByOfferingIdBlocking("cool-offering-description");

        // Assert
        assertThat(offering).isNotNull();
        assertThat(offering.getOfferingToken()).contains(token);
    }

    @Test(expected = ExecutionException.class)
    public void subscribeByOfferingIdFutureExecutionException()
            throws IOException, InterruptedException, ExecutionException, FailedDiscoveryException,
            InvalidOfferingException, IllegalEndpointException, IncompleteOfferingDescriptionException {
        // Arrange client response
        String queryId = "cool-offering-description";
        ConsumerCore spyedConsumer = spy(consumerUnderTest);
        doThrow(new IllegalEndpointException()).when(spyedConsumer).subscribeByOfferingIdBlocking(queryId);

        // Act
        Future<OfferingCore> future = spyedConsumer.subscribeByOfferingIdFuture(queryId);
        future.get();

        // Assert
        // see expect Annotation
    }

    @Test
    public void subscribeByOfferingIdFutureSuccess()
            throws IOException, IncompleteOfferingQueryException, InterruptedException, ExecutionException {
        // Arrange client response
        String queryId = "cool-offering-description";
        mockMarketEmptyResponse(marketMock, SUCCESS);
        ConsumerCore spyedConsumer = spy(consumerUnderTest); // use spy only to be able to count method invocation

        // Act
        Future<OfferingCore> future = spyedConsumer.subscribeByOfferingIdFuture(queryId);
        future.get();

        // Assert
        verify(spyedConsumer, times(1)).subscribeByOfferingIdFuture(anyString());
    }

    private static IOfferingQuery mockOfferingWithId(IOfferingQuery offeringQueryMock, String id) {
        when(offeringQueryMock.getLocalId()).thenReturn(id);
        when(offeringQueryMock.getId()).thenReturn(id);

        return offeringQueryMock;
    }

    private static void mockMarketEmptyResponse(MarketplaceClient marketMock, boolean isSuccessful) throws IOException {
        // Arrange client response
        Response respMock = createResponseMock(JSON_ADD_OFFERING, isSuccessful);
        when(marketMock.request(contains("addOfferingQuery"))).thenReturn(respMock);
        Response matchingRespMock = createResponseMock(JSON_MATCHING_OFFERINGS_EMPTY, isSuccessful);
        when(marketMock.request(contains("matchingOfferings"))).thenReturn(matchingRespMock);
    }

    private static void mockMarketResponse(MarketplaceClient marketMock, String queryId, boolean isSuccessful)
            throws IOException {
        // Arrange client response
        Response respMock = createResponseMock(JSON_ADD_OFFERING, isSuccessful);
        when(marketMock.request(contains("addOfferingQuery"))).thenReturn(respMock);
        Response matchingRespMock = createResponseMock(String.format(JSON_MATCHING_OFFERINGS_TEMPLATE, queryId), true);
        when(marketMock.request(contains("matchingOfferings"))).thenReturn(matchingRespMock);
    }

    private static Response createResponseMock(String body, boolean isSuccessful) throws IOException {
        Response respMock = mock(Response.class);
        ResponseBody bodyMock = mock(ResponseBody.class);
        when(bodyMock.string()).thenReturn(body);
        when(respMock.body()).thenReturn(bodyMock);
        when(respMock.isSuccessful()).thenReturn(isSuccessful);

        return respMock;
    }

}
