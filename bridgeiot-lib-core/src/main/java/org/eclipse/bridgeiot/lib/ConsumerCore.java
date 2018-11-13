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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import okhttp3.Response;

import org.eclipse.bridgeiot.lib.configuration.LibConfiguration;
import org.eclipse.bridgeiot.lib.exceptions.BridgeIoTException;
import org.eclipse.bridgeiot.lib.exceptions.FailedDiscoveryException;
import org.eclipse.bridgeiot.lib.exceptions.IllegalEndpointException;
import org.eclipse.bridgeiot.lib.exceptions.IncompleteOfferingDescriptionException;
import org.eclipse.bridgeiot.lib.exceptions.IncompleteOfferingQueryException;
import org.eclipse.bridgeiot.lib.exceptions.InvalidOfferingException;
import org.eclipse.bridgeiot.lib.exceptions.NotRegisteredException;
import org.eclipse.bridgeiot.lib.handlers.DiscoverFailureException;
import org.eclipse.bridgeiot.lib.handlers.DiscoverResponseErrorHandler;
import org.eclipse.bridgeiot.lib.handlers.DiscoverResponseHandler;
import org.eclipse.bridgeiot.lib.misc.Constants;
import org.eclipse.bridgeiot.lib.misc.GraphQLQueries;
import org.eclipse.bridgeiot.lib.misc.Helper;
import org.eclipse.bridgeiot.lib.offering.OfferingCore;
import org.eclipse.bridgeiot.lib.offering.OfferingCoreByLib;
import org.eclipse.bridgeiot.lib.offering.OfferingDescription;
import org.eclipse.bridgeiot.lib.offering.SubscribableOfferingDescriptionCore;
import org.eclipse.bridgeiot.lib.offering.internal.Accounting.AccountingReport;
import org.eclipse.bridgeiot.lib.offering.parameters.Parameter;
import org.eclipse.bridgeiot.lib.query.IOfferingQuery;
import org.eclipse.bridgeiot.lib.query.OfferingQueryChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Enables basic lifecycle operations on offerings for consumer. Consumer foresees discovery operations for offering
 * descriptions on the marketplace.
 * 
 * The interface functions are all non-blocking either by returning a completable future or passing delegate objects for
 * callback. CompletableFuture allows stream processing and reactive operations on the Type of the nested Object in the
 * CompletableFuture. Calling .get() returns the nested object in a synchronized manner. The call blocks until the
 * operation is finished. Alternatively method calls foresee callback via delegate objects. The relevant delegate types
 * are defined as functional interfaces, which allow lambda expression.
 * 
 *
 */
public class ConsumerCore extends BridgeIotAPI implements IConsumer {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerCore.class);

    protected ExecutorService executorPool;

    // Scheduled execture for continous discovery
    private Map<String, ScheduledExecutorService> discoveryExecutorMap;
    private Map<String, IOfferingQuery> offeringQueryMap;
    protected Map<String, OfferingCore> subscribedOfferingMap;

    protected static final int DEFAULT_DISCOVER_INTERVAL = 600; // in seconds (= 10 mins)
    protected static final int MIN_DISCOVER_INTERVAL = 5; // in seconds

    protected ObjectMapper mapper = new ObjectMapper();

    /**
     * Instantiates the Consumer instance
     * 
     * @param consumerId
     *            Identifier of the Consumer instance - as provided by the Marketplace.
     * @param marketplaceUri
     *            URI to the Marketplace API
     * @param certificateFile
     */
    public ConsumerCore(String consumerId, String marketplaceUri) {
        this(consumerId, marketplaceUri, Executors.newFixedThreadPool(LibConfiguration.EXECUTOR_POOL_SIZE),
                new HashMap<String, ScheduledExecutorService>(), new HashMap<String, IOfferingQuery>(),
                new HashMap<String, OfferingCore>());
        logger.info("Setting up consumer {} with marketplace at {}", consumerId, marketplaceUri);
    }

    /**
     * NOTE: This constructor is for testing purpose only!! It must not be instantiated by clients!!
     * 
     * @param consumerId
     *            Identifier of the Consumer instance - as provided by the Marketplace.
     * @param marketplaceUri
     *            URI to the Marketplace API
     * @param executorPool
     * @param discoveryExecutorMap
     * @param offeringQueryMap
     */
    ConsumerCore(String consumerId, String marketplaceUri, ExecutorService executorPool,
            Map<String, ScheduledExecutorService> discoveryExecutorMap, Map<String, IOfferingQuery> offeringQueryMap,
            Map<String, OfferingCore> subscribedOfferingMap) {
        super(new BridgeIotClientId(consumerId), marketplaceUri);
        this.executorPool = executorPool;
        this.discoveryExecutorMap = discoveryExecutorMap;
        this.offeringQueryMap = offeringQueryMap;
        this.subscribedOfferingMap = subscribedOfferingMap;
    }

    /**
     * Authenticates instance at the Marketplace.
     * 
     * @param clientSecret
     *            API Key for authentication at the marketplace
     * @throws IOException
     */
    @Override
    public ConsumerCore authenticate(String clientSecret) throws IOException {
        return (ConsumerCore) super.authenticate(clientSecret);
    }

    /**
     * Authenticates instance at the Marketplace. The client secret can be specified either by the withClientSecret
     * method or by configuration based object creation
     * 
     * @throws IOException
     */
    @Override
    public ConsumerCore authenticate() throws IOException {
        return (ConsumerCore) super.authenticate();
    }

    /**
     * Creates an empty offering query with a custom query id
     * 
     * @param localId
     * @return
     * @throws IncompleteOfferingQueryException
     */
    public static OfferingQueryChain createOfferingQuery(String localId) throws IncompleteOfferingQueryException {
        return OfferingQueryChain.create(localId);
    }

    /**
     * Performs a marketplace discovery for Offerings according to the offering query. The call is non-blocking as it
     * returns the list of offering descriptions as a future.
     *
     * @param offeringQuery
     *            Offering query used for discover
     * @return List of found Offering Descriptions matching Offering Query as CompletableFuture.
     */
    @Override
    public Future<List<SubscribableOfferingDescriptionCore>> discoverFuture(final IOfferingQuery offeringQuery)
            throws IOException {
        return executorPool.submit(new Callable<List<SubscribableOfferingDescriptionCore>>() {
            @Override
            public List<SubscribableOfferingDescriptionCore> call()
                    throws IOException, NotRegisteredException, FailedDiscoveryException {
                return discoverBlocking(offeringQuery);
            }
        });
    }

    /**
     * Performs a marketplace discovery for Offerings according to the offering query. The call is non-blocking. On
     * successful execution, onSuccess receives the result via executing its callback method. On failure the respective
     * callback method of onFailure is called.
     *
     * @param offeringQuery
     *            Offering query used for discover
     */
    @Override
    public void discover(final IOfferingQuery offeringQuery, final DiscoverResponseHandler onSuccessDiscover,
            final DiscoverResponseErrorHandler onFailureDiscover) {
        try {
            discover(offeringQuery, onSuccessDiscover, onFailureDiscover, discoverBlocking(offeringQuery));
        } catch (NotRegisteredException | IOException | FailedDiscoveryException | BridgeIoTException e) {
            String msg = "Discover Request to eXchange failed!";
            logger.error(msg);
            onFailureDiscover.processResponse(offeringQuery, new DiscoverFailureException(msg, e));
        }
    }

    /**
     * @param offeringQuery
     *            Offering query used for discover
     * @param onSuccessDiscover
     *            onSuccessDiscover object for processing successful discoveries
     * @param onFailureDiscover
     *            Delegate object for failing discoveries
     * @param offeringDescritions
     */
    void discover(final IOfferingQuery offeringQuery, final DiscoverResponseHandler onSuccessDiscover,
            final DiscoverResponseErrorHandler onFailureDiscover,
            final List<SubscribableOfferingDescriptionCore> offeringDescritions) {
        executorPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    onSuccessDiscover.processResponse(offeringQuery, offeringDescritions);
                } catch (Exception e) {
                    String msg = "Processing response failed!";
                    logger.error(msg);
                    onFailureDiscover.processResponse(offeringQuery, new DiscoverFailureException(msg, e));
                }
            }
        });
    }

    /**
     * Performs a marketplace discovery for Offerings according to the offering query. The call is non-blocking as it
     * returns the list of offering descriptions as a future.
     *
     * @param offeringQuery
     *            Offering query used for discover
     * @return List of found Offering Descriptions matching Offering Query as CompletableFuture.
     */
    @Override
    public Future<List<SubscribableOfferingDescriptionCore>> discoverByIdFuture(final String queryId)
            throws IOException {
        return executorPool.submit(new Callable<List<SubscribableOfferingDescriptionCore>>() {
            @Override
            public List<SubscribableOfferingDescriptionCore> call()
                    throws IOException, NotRegisteredException, FailedDiscoveryException {
                return discoverByIdBlocking(queryId);
            }
        });
    }

    /**
     * Performs a marketplace discovery for Offerings according to the offering query. The call is blocking.
     *
     * @param offeringQuery
     *            Offering query used for discover
     * @return List of found Offering Descriptions matching Offering Query as CompletableFuture.
     * @throws FailedDiscoveryException
     */
    public List<SubscribableOfferingDescriptionCore> discoverBlocking(IOfferingQuery offeringQuery)
            throws IOException, NotRegisteredException, FailedDiscoveryException {

        String jsonString = discoverCall(offeringQuery);
        return discoverAndDecode(jsonString, SubscribableOfferingDescriptionCore.class, offeringQuery.getId());
    }

    public List<SubscribableOfferingDescriptionCore> discoverByIdBlocking(String queryId)
            throws IOException, FailedDiscoveryException, NotRegisteredException {
        String jsonString = discoverByIdCall(queryId);
        return discoverAndDecode(jsonString, SubscribableOfferingDescriptionCore.class, queryId);
    }

    protected <T extends SubscribableOfferingDescriptionCore> List<T> discoverAndDecode(String jsonString,
            Class<T> tClass, String queryId) {

        List<OfferingDescription> receivedOfferingDescriptions = Helper.unmarshallDiscoverResponse(jsonString,
                OfferingDescription.class);
        // Workaround for Offering Description Formats, which are not supported by Marketplace
        final List<T> offeringDescriptions = new LinkedList<>();

        for (final OfferingDescription subscribableOfferingDescription : receivedOfferingDescriptions) {

            String ios = subscribableOfferingDescription.getExtension1();

            if (ios != null) {
                try {

                    // Fix inputs and outputs with the help of extension1
                    JsonNode jsonObject = mapper.reader()
                            .readTree(ios.replaceAll("\\" + Constants.DOUBLE_QUOTE_ESCAPE, "\"")
                                    .replaceAll("\\" + Constants.BACKSLASH_ESCAPE, "\\\\"));
                    JsonNode inputs = jsonObject.get("inputs");
                    JsonNode outputs = jsonObject.get("outputs");
                    subscribableOfferingDescription
                            .setInputs((Parameter) mapper.readerFor(Parameter.class).readValue(inputs));
                    subscribableOfferingDescription
                            .setOutputs((Parameter) mapper.readerFor(Parameter.class).readValue(outputs));
                } catch (IOException e1) {
                    String msg = "Unmarshalling bypassed io parameter specification failed!";
                    logger.error(msg);
                    throw new BridgeIoTException(msg, e1);
                }
            }

            // Invalidate extension field
            subscribableOfferingDescription.setExtension1("");

            T x;
            try {
                x = tClass.getDeclaredConstructor(IConsumer.class).newInstance(this);
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                throw new BridgeIoTException("Cannot instantiate offering class");
            }

            x.updateOfferingDescription(subscribableOfferingDescription);
            offeringDescriptions.add(x);

        }

        initializeSubscribableOfferingDescription(offeringDescriptions, queryId);
        return offeringDescriptions;
    }

    protected String discoverCall(IOfferingQuery offeringQuery)
            throws IOException, NotRegisteredException, FailedDiscoveryException {

        if (marketplaceClient == null) {
            throw new NotRegisteredException();
        }

        String responseString = "";
        String queryId = offeringQuery.getId();

        // check if offering query has already been created
        if ((queryId != null) && offeringQueryMap.containsKey(queryId)
                && offeringQueryMap.get(queryId).sameQuery(offeringQuery)) {

            responseString = discoverByIdCall(queryId);

        } else {

            // if offering query is new or has been updated
            String offeringQueryString = offeringQuery.toOfferingQueryString(clientId);
            logger.info("New Discovery Query: {}", offeringQueryString);

            try {
                Response response = marketplaceClient.request(offeringQueryString);

                String firstResponseString = response.body().string();
                if (!response.isSuccessful()) {
                    logger.error("Discover Request to eXchange was not successful: {}", firstResponseString);
                    throw new FailedDiscoveryException();
                }

                response.close();

                JsonNode rootNode = mapper.reader().readTree(firstResponseString);
                String rootName = rootNode.fieldNames().next();
                JsonNode queryResult = rootNode.get(rootName).get("addOfferingQuery");
                String newQueryId = queryResult.get("id").asText();

                offeringQuery.setId(newQueryId);

                // Store created Offering Query in hash map
                offeringQueryMap.put(newQueryId, offeringQuery);

                responseString = discoverByIdCall(newQueryId);

            } catch (IOException e) {
                logger.error("Discover Request to eXchange failed: {}", e.getMessage());
                throw new FailedDiscoveryException(e);
            }

        }

        return responseString;

    }

    protected String discoverByIdCall(String queryId) throws IOException, FailedDiscoveryException {

        String offeringQueryString = GraphQLQueries.getFindMatchingOfferingsString(queryId);

        logger.info("Discovery Request: {}", offeringQueryString);

        Response response = marketplaceClient.request(offeringQueryString);
        String responseString = response.body().string();

        if (!response.isSuccessful()) {
            logger.error("Discover Request to eXchange was not successful: {}", responseString);
            throw new FailedDiscoveryException();
        }

        response.close();

        return responseString;
    }

    /**
     * Performs a marketplace discovery for Offerings according to the offering query. The call is non-blocking, on
     * success the call back is executed.
     *
     *
     * @param offeringQuery
     *            Offering query used for discover
     * @return List of found Offering Descriptions matching Offering Query as CompletableFuture.
     * @throws IncompleteOfferingQueryException
     */
    @Override
    public void discover(IOfferingQuery offeringQuery, DiscoverResponseHandler onSuccessDiscover)
            throws IncompleteOfferingQueryException {
        discover(offeringQuery, onSuccessDiscover, null);
    }

    /**
     * Subscribes to an offering. The call is non-blocking and returns a future on an OfferingCore.
     *
     * @param offeringId
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public Future<OfferingCore> subscribeByOfferingIdFuture(final String offeringId) {
        return executorPool.submit(new Callable<OfferingCore>() {
            @Override
            public OfferingCore call() throws IOException, IllegalEndpointException,
                    IncompleteOfferingDescriptionException, InvalidOfferingException {

                return subscribeByOfferingIdBlocking(offeringId);
            }
        });
    }

    /**
     * Subscribes to an offering. The call is blocking and returns an OfferingCore.
     *
     * @param offeringId
     * @return an offering
     * @throws IncompleteOfferingDescriptionException
     * @throws IllegalEndpointException
     * @throws IOException
     * @throws InvalidOfferingException
     */
    public OfferingCore subscribeByOfferingIdBlocking(String offeringId) throws InvalidOfferingException, IOException,
            IllegalEndpointException, IncompleteOfferingDescriptionException {

        OfferingDescription fetchedOfferingDescription = marketplaceClient.getOfferingDescription(offeringId);

        if ((fetchedOfferingDescription != null) && (fetchedOfferingDescription.getActivation() != null)
                && fetchedOfferingDescription.getActivation().getStatus()) {
            // if offering is active, set the Consumer instance
            return subscribeByOfferingIdBlocking(fetchedOfferingDescription);
        }

        logger.warn("Offering with ID={} is inactive - subscription failed!", offeringId);
        return null;
    }

    /**
     * Subscribes to an offering. The call is blocking and returns an OfferingCore.
     * 
     * @param fetchedOfferingDescription
     * @return an offering
     * @throws IllegalEndpointException
     * @throws IncompleteOfferingDescriptionException
     */
    OfferingCore subscribeByOfferingIdBlocking(OfferingDescription fetchedOfferingDescription)
            throws IllegalEndpointException, IncompleteOfferingDescriptionException {
        // set the Consumer instance
        SubscribableOfferingDescriptionCore subscribableOffering = SubscribableOfferingDescriptionCore.create(this);
        subscribableOffering.updateOfferingDescription(fetchedOfferingDescription);
        return subscribableOffering.subscribeBlocking();
    }

    /**
     * @param offeringDescriptions
     * @param queryId
     */
    protected <T extends SubscribableOfferingDescriptionCore> void initializeSubscribableOfferingDescription(
            List<T> offeringDescriptions, String queryId) {
        // Set reference to consumer - this is needed for subscribe/unsubscribe, accounting, certificatefiles, etc.
        Iterator<T> listIterator = offeringDescriptions.iterator();
        while (listIterator.hasNext()) {
            T offeringDescription = listIterator.next();
            if (offeringDescription.getActivation().getStatus()) {
                // if offering is active, set the Consumer instance
                offeringDescription.setConsumer(this);
                offeringDescription.setQueryId(queryId);
            } else {
                // if offering is deactivated, remove it from the list
                listIterator.remove();
            }
        }
    }

    /**
     * Terminates a consumer session.
     */
    @Override
    public void terminate() {

        super.terminate();

        if (executorPool != null) {
            executorPool.shutdownNow();
        }

        for (ScheduledExecutorService s : discoveryExecutorMap.values()) {
            s.shutdownNow();
        }

        marketplaceClient.close();
    }

    // Needed to force termination of all executor threads (even if the Consumer/Provider does not call the .terminate()
    // method)
    @Override
    protected void finalize() throws Throwable {
        try {
            terminate();
        } finally {
            super.finalize();
        }
    }

    /**
     * Discover new offerings regularly. Scheduler is registered in Hashmap for de-activation.
     */
    @Override
    public void discoverContinous(final IOfferingQuery offeringQuery, final DiscoverResponseHandler onSuccess,
            final DiscoverResponseErrorHandler onFailure) throws IncompleteOfferingQueryException {
        discoverContinous(offeringQuery, onSuccess, onFailure, DEFAULT_DISCOVER_INTERVAL);
    }

    @Override
    public void discoverContinous(final IOfferingQuery offeringQuery, final DiscoverResponseHandler onSuccess,
            final DiscoverResponseErrorHandler onFailure, int discoverInterval)
            throws IncompleteOfferingQueryException {

        Runnable discoverRunnable = new Runnable() {
            public void run() {
                discover(offeringQuery, onSuccess, onFailure);
            }
        };

        discoverContinous(offeringQuery, discoverInterval, discoverRunnable);
    }

    void discoverContinous(final IOfferingQuery offeringQuery, int discoverInterval, Runnable discoverRunnable) {

        if (discoveryExecutorMap.containsKey(offeringQuery.getLocalId())) {
            logger.info("There is already a continous query running for this ID. Please stop it first");
            return;
        }
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleWithFixedDelay(discoverRunnable, 0, Math.max(MIN_DISCOVER_INTERVAL, discoverInterval),
                TimeUnit.SECONDS);
        discoveryExecutorMap.put(offeringQuery.getId(), executor);

    }

    @Override
    public void stopDiscoverContinuous(IOfferingQuery offeringQuery) {
        ScheduledExecutorService s = discoveryExecutorMap.get(offeringQuery.getLocalId());
        if (s == null) {
            logger.info("Scheduler for ID: {} not found", offeringQuery.getLocalId());
            return;
        }
        s.shutdownNow();
        discoveryExecutorMap.remove(offeringQuery.getLocalId());
    }

    public void addSubscribedOffering(OfferingCore subscribedOffering) {
        this.subscribedOfferingMap.put(subscribedOffering.getOfferingDescription().getId(), subscribedOffering);
    }

    public void removeSubscribedOffering(OfferingCore subscribedOffering) {
        addAccountingReports(((OfferingCoreByLib) subscribedOffering).getCurrentAccountingReports());
        this.subscribedOfferingMap.remove(subscribedOffering.getOfferingDescription().getId());
    }

    @Override
    protected void prepareAccountingReport() {
        List<AccountingReport> list;
        for (OfferingCore offering : subscribedOfferingMap.values()) {
            list = offering.getCurrentAccountingReports();
            addAccountingReports(list);
        }
    }

    @Override
    public ConsumerCore withAutoProxy(String proxyHost, int proxyPort) {
        return (ConsumerCore) super.withAutoProxy(proxyHost, proxyPort);
    }

    public ConsumerCore withProxyBypass(String host) {
        this.addProxyBypass(host);
        return this;
    }

    public ConsumerCore withProxy(String host, int port) {
        this.setProxy(host, port);
        return this;
    }

    public ConsumerCore withClientSecret(String clientSecret) {

        return (ConsumerCore) super.withClientSecret(clientSecret);
    }

}
