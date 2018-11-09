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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.bridgeiot.lib.exceptions.FailedDiscoveryException;
import org.eclipse.bridgeiot.lib.exceptions.IllegalEndpointException;
import org.eclipse.bridgeiot.lib.exceptions.IncompleteOfferingDescriptionException;
import org.eclipse.bridgeiot.lib.exceptions.InvalidOfferingException;
import org.eclipse.bridgeiot.lib.exceptions.NotRegisteredException;
import org.eclipse.bridgeiot.lib.handlers.DiscoverResponseErrorHandler;
import org.eclipse.bridgeiot.lib.handlers.DiscoverResponseHandler;
import org.eclipse.bridgeiot.lib.misc.BridgeIotProperties;
import org.eclipse.bridgeiot.lib.misc.Helper;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.AccessInterfaceType;
import org.eclipse.bridgeiot.lib.offering.Offering;
import org.eclipse.bridgeiot.lib.offering.OfferingDescription;
import org.eclipse.bridgeiot.lib.offering.SubscribableOfferingDescription;
import org.eclipse.bridgeiot.lib.query.IOfferingQuery;
import org.eclipse.bridgeiot.lib.query.OfferingQueryChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 */
public class Consumer extends ConsumerCore implements IConsumer {

    private static final Logger logger = LoggerFactory.getLogger(Consumer.class);

    ObjectMapper mapper = new ObjectMapper();

    /**
     * Instantiates the Consumer instance
     * 
     * @param consumerId
     *            Identifier of the Consumer instance - as provided by the Marketplace.
     * @param marketplaceUri
     *            URI to the Marketplace API
     * @param certificateFile
     */
    public Consumer(String consumerId, String marketplaceUri) {
        super(consumerId, marketplaceUri);
    }

    /**
     * Instantiates the Consumer instance
     * 
     */
    public static Consumer create(String providerId, String marketplaceUri) {
        return new Consumer(providerId, marketplaceUri);
    }

    /**
     * Instantiates the Provider from configuration file
     * 
     * See the example using all supported fields
     * 
     * @param fileName
     * @return
     * @throws FileNotFoundException
     */
    public static Consumer create(String fileName) throws FileNotFoundException {
        // Load example properties file
        BridgeIotProperties prop = BridgeIotProperties.load(fileName);

        return Consumer.create(prop.CONSUMER_ID, prop.MARKETPLACE_URI).withProxy(prop.PROXY, prop.PROXY_PORT)
                .withProxyBypass(prop.PROXY_BYPASS).withClientSecret(prop.CONSUMER_SECRET);
    }

    /**
     * Authenticates instance at the Marketplace.
     * 
     * @param clientSecret
     *            API Key for authentication at the marketplace
     * @throws IOException
     */
    @Override
    public Consumer authenticate(String clientSecret) throws IOException {
        return (Consumer) super.authenticate(clientSecret);
    }

    /**
     * Authenticates instance at the Marketplace. The client secret can be specified either by the withClientSecret
     * method or by configuration based instantiation
     * 
     * @throws IOException
     */
    @Override
    public Consumer authenticate() throws IOException {
        return (Consumer) super.authenticate();
    }

    /**
     * Performs a marketplace discovery for Offerings matching the implementation as Offering Query. The call is
     * non-blocking as it returns the list of Offering Descriptions as a completable future.
     * 
     * @param offeringQuery
     *            Offering query used for discover
     * @return List of found Offering Descriptions matching Offering Query as CompletableFuture.
     */
    public CompletableFuture<List<SubscribableOfferingDescription>> discover(IOfferingQuery offeringQuery) {

        return CompletableFuture.supplyAsync(() -> {
            String jsonString = null;
            try {
                jsonString = discoverCall(offeringQuery);
                return discoverAndDecode(jsonString, SubscribableOfferingDescription.class, offeringQuery.getId());
            } catch (NotRegisteredException | FailedDiscoveryException | IOException e) {
                logger.error(e.getMessage(), e);
            }
            return null;
        });

    }

    /**
     * This function executes a query by a reference received by the marketplace in previous queries.
     * 
     * @param queryId
     *            QueryId
     * @return List of found Offering Descriptions matching Offering Query as CompletableFuture.
     */
    public CompletableFuture<List<SubscribableOfferingDescription>> discoverById(String queryId) {

        return CompletableFuture.supplyAsync(() -> {
            String jsonString = null;
            try {
                jsonString = discoverByIdCall(queryId);
            } catch (FailedDiscoveryException | IOException e) {
                logger.error(e.getMessage(), e);
            }
            return discoverAndDecode(jsonString, SubscribableOfferingDescription.class, queryId);
        });
    }

    /**
     * This helper function executes a query by a reference received by the marketplace in previous queries.
     * 
     * @param queryId
     *            QueryId
     * @return List of found Offering Descriptions matching Offering Query as CompletableFuture.
     */
    protected void discoverById(String queryId, DiscoverResponseHandler onSuccess,
            DiscoverResponseErrorHandler onFailure) {

        final CompletableFuture<List<SubscribableOfferingDescription>> completableFuture = CompletableFuture
                .supplyAsync(() -> {
                    String jsonString = null;
                    try {
                        jsonString = discoverByIdCall(queryId);
                    } catch (final IOException | FailedDiscoveryException e) {
                        logger.error(e.getMessage(), e);
                    }
                    final List<SubscribableOfferingDescription> offeringDescriptions = unmarshallDiscoverResponse(
                            jsonString);
                    initializeSubscribableOfferingDescription(offeringDescriptions, queryId);
                    return offeringDescriptions;
                });
        completableFuture.handle((list, exception) -> {
            // TODO: FIX THIS
            final OfferingQueryChain offeringQuery = new OfferingQueryChain(queryId);
            // FIX THIS
            if (list != null) {
                onSuccess.processResponse(offeringQuery, list);
            } else {
                if (onFailure != null) {
                    onFailure.processResponse(offeringQuery, null);
                }
            }
            return list;
        });
    }

    /**
     * Performs a marketplace discovery for Offerings according to the offering query. The call is non-blocking. On
     * successful execution, onSuccess receives the result via executing its callback method. On failure the respective
     * callback method of onFailure is called.
     *
     * @param offeringQuery
     *            Offering query used for discover
     * @param onSuccess
     *            Delegate object for processing successful discoveries
     * @param onFailure
     *            Delegate object for failing discoveries
     */
    @Override
    public void discover(IOfferingQuery offeringQuery, DiscoverResponseHandler onSuccess,
            DiscoverResponseErrorHandler onFailure) {

        final CompletableFuture<List<SubscribableOfferingDescription>> offeringDescriptionsFuture = discover(
                offeringQuery);
        offeringDescriptionsFuture.handle((list, exception) -> {
            if (list != null) {
                onSuccess.processResponse(offeringQuery, list);
            } else {
                if (onFailure != null) {
                    onFailure.processResponse(offeringQuery, null);
                }
            }
            return list;
        });
    }

    /**
     * Performs a marketplace discovery for Offerings according to the offering query. The call is non-blocking. On
     * successful execution, onSuccess receives the result via executing its callback method. On failure nothing is
     * done.
     * 
     * @param offeringQuery
     *            Offering query used for discover
     * @param onSuccess
     *            Delegate object for processing successful discoveries
     */
    @Override
    public void discover(IOfferingQuery offeringQuery, DiscoverResponseHandler onSuccess) {
        discover(offeringQuery, onSuccess, null);
    }

    private List<SubscribableOfferingDescription> unmarshallDiscoverResponse(String jsonString) {
        List<SubscribableOfferingDescription> offeringDescriptions = Helper.unmarshallDiscoverResponse(jsonString,
                SubscribableOfferingDescription.class);
        adaptOfferingDescriptionsDueToSpecificationGap(offeringDescriptions);
        return offeringDescriptions;
    }

    private void adaptOfferingDescriptionsDueToSpecificationGap(
            List<SubscribableOfferingDescription> offeringDescriptions) {

        offeringDescriptions.stream().forEach(o -> {
            if (o.getEndpoints() != null && !o.getEndpoints().isEmpty()
                    && o.getEndpoints().get(0).getAccessInterfaceType() == AccessInterfaceType.EXTERNAL) {
                o.setAccessInterfaceType(AccessInterfaceType.EXTERNAL);
            }
        });

    }

    /**
     * Subscribes to an offering. The call is non-blocking and returns a CompletableFuture on an Offering.
     * 
     * @param offeringId
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public CompletableFuture<Offering> subscribe(SubscribableOfferingDescription consumableOfferingDescription) {
        return consumableOfferingDescription.subscribe();
    }

    /**
     * Subscribes to an offering. The call is blocking and returns an Offering.
     * 
     * @param offeringId
     * @return
     * @throws IncompleteOfferingDescriptionException
     * @throws IllegalEndpointException
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public Offering subscribeBlocking(SubscribableOfferingDescription consumableOfferingDescription)
            throws IllegalEndpointException, IncompleteOfferingDescriptionException {

        return consumableOfferingDescription.subscribeBlocking();
    }

    /**
     * Subscribes to an offering. The call is non-blocking and returns a future on an OfferingCore.
     *
     * @param offeringId
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public CompletableFuture<Offering> subscribeByOfferingId(final String offeringId) {
        return CompletableFuture.supplyAsync(() -> {
            Offering offering = null;
            try {
                offering = subscribeByOfferingIdBlocking(offeringId);
            } catch (IllegalEndpointException | IncompleteOfferingDescriptionException | InvalidOfferingException
                    | IOException e) {
                logger.error(e.getMessage(), e);
            }
            return offering;
        });
    }

    /**
     * Subscribes to an offering. The call is blocking and returns an OfferingCore.
     *
     * @param offeringId
     * @return
     * @throws IncompleteOfferingDescriptionException
     * @throws IllegalEndpointException
     * @throws IOException
     * @throws InvalidOfferingException
     */
    @Override
    public Offering subscribeByOfferingIdBlocking(String offeringId) throws IllegalEndpointException,
            IncompleteOfferingDescriptionException, InvalidOfferingException, IOException {

        OfferingDescription fetchedOfferingDescription = marketplaceClient.getOfferingDescription(offeringId);

        if ((fetchedOfferingDescription != null) && (fetchedOfferingDescription.getActivation() != null)
                && fetchedOfferingDescription.getActivation().getStatus()) {
            // if offering is active, set the Consumer instance
            SubscribableOfferingDescription subscribableOffering = SubscribableOfferingDescription.create(this);
            subscribableOffering.updateOfferingDescription(fetchedOfferingDescription);
            return subscribableOffering.subscribeBlocking();
        }

        logger.warn("Offering with ID={} is inactive - subscription failed!", offeringId);
        return null;
    }

    @Override
    public Consumer withAutoProxy(String proxyHost, int proxyPort) {
        return (Consumer) super.withAutoProxy(proxyHost, proxyPort);
    }

    @Override
    public Consumer withProxyBypass(String host) {
        this.addProxyBypass(host);
        return this;
    }

    @Override
    public Consumer withProxy(String host, int port) {
        this.setProxy(host, port);
        return this;
    }

    @Override
    public Consumer withClientSecret(String clientSecret) {

        return (Consumer) super.withClientSecret(clientSecret);
    }
}
